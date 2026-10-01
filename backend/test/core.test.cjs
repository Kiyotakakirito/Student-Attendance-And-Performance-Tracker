const test = require('node:test');
const assert = require('node:assert/strict');
const { createService, principalFor, validateIdentity } = require('../src/core.js');

function fixture() {
  const records = new Map();
  const events = [];
  const key = (kind, id) => JSON.stringify([kind, id]);
  const store = {
    get: (kind, id) => records.get(key(kind, id)),
    list: kind => [...records].filter(([k]) => JSON.parse(k)[0] === kind).map(([, v]) => v),
    put(kind, id, data, audit) { records.set(key(kind, id), structuredClone(data)); events.push({ kind, id, data, audit }); }
  };
  const service = (role, profileId = '') => createService(store, { role, profileId, email: role + '@example.edu' }, () => '2026-10-01T12:00:00.000Z');
  const admin = service('admin');
  admin.execute({ action: 'addFaculty', employeeId: 'F1', name: 'Teacher', email: 'teacher@example.edu', department: 'CS' });
  ['S1', 'S2', 'S3'].forEach(id => admin.execute({ action: 'addStudent', rollNumber: id, name: id, email: id + '@example.edu', department: 'CS' }));
  admin.execute({ action: 'addSubject', subjectCode: 'CS101', subjectName: 'Computing', facultyId: 'F1', semester: '1', totalClasses: '40' });
  admin.execute({ action: 'saveEnrollment', subjectCode: 'CS101', studentIds: ['S1', 'S2'], revision: 0 });
  return { store, events, service, admin, faculty: service('faculty', 'F1'), student: service('student', 'S1') };
}
function fails(code, operation) { assert.throws(operation, error => error.code === code); }
function attendance(overrides = {}) {
  return { action: 'saveAttendance', subjectCode: 'CS101', date: '2026-09-30', slot: 'Period 1', requestId: 'attendance-1', records: [{ studentId: 'S1', status: 'present' }, { studentId: 'S2', status: 'absent' }], ...overrides };
}
function assessment(overrides = {}) {
  return { action: 'saveAssessment', subjectCode: 'CS101', assessmentId: 'A1', title: 'Quiz', maxMarks: 20, revision: 0, requestId: 'assessment-1', published: false, marks: [{ studentId: 'S1', status: 'scored', score: 0 }, { studentId: 'S2', status: 'pending' }], ...overrides };
}

test('server roles deny student and faculty administration or another teacher’s subject', () => {
  const { student, faculty, service } = fixture();
  for (const action of ['getStudents', 'getFaculty', 'getDashboard', 'saveEnrollment', 'deleteStudent']) {
    fails('FORBIDDEN', () => student.execute({ action, subjectCode: 'CS101', rollNumber: 'S2' }));
    fails('FORBIDDEN', () => faculty.execute({ action, subjectCode: 'CS101', rollNumber: 'S2' }));
  }
  fails('FORBIDDEN', () => student.execute({ action: 'getRoster', subjectCode: 'CS101' }));
  fails('NOT_FOUND', () => service('faculty', 'OTHER').execute({ action: 'getSubjects' }));
  fails('FORBIDDEN', () => faculty.execute({ action: 'getMyProgress' }));
});
test('faculty roster is explicit enrollment and excludes private contact fields', () => {
  const { faculty } = fixture();
  const result = faculty.execute({ action: 'getRoster', subjectCode: 'CS101' });
  assert.deepEqual(result.students, [{ studentId: 'S1', name: 'S1' }, { studentId: 'S2', name: 'S2' }]);
});
test('enrollment rejects duplicates, nonexistent students and stale writes', () => {
  const { admin } = fixture();
  fails('VALIDATION', () => admin.execute({ action: 'saveEnrollment', subjectCode: 'CS101', studentIds: ['S1', 'S1'], revision: 1 }));
  fails('NOT_FOUND', () => admin.execute({ action: 'saveEnrollment', subjectCode: 'CS101', studentIds: ['MISSING'], revision: 1 }));
  fails('CONFLICT', () => admin.execute({ action: 'saveEnrollment', subjectCode: 'CS101', studentIds: ['S3'], revision: 0 }));
});
test('attendance retries do not append a second session and alternate submissions conflict', () => {
  const { faculty, store, events } = fixture();
  faculty.execute(attendance());
  const writes = events.length;
  faculty.execute(attendance());
  assert.equal(events.length, writes);
  assert.equal(store.list('attendance').length, 1);
  fails('CONFLICT', () => faculty.execute(attendance({ requestId: 'other-request', slot: 'period 1' })));
});
test('attendance validates complete distinct roster and real nonfuture dates', () => {
  const { faculty } = fixture();
  for (const date of ['2026-02-30', '2026-13-01', '2026-10-02', 'yesterday']) fails('VALIDATION', () => faculty.execute(attendance({ date })));
  fails('CONFLICT', () => faculty.execute(attendance({ records: [{ studentId: 'S1', status: 'present' }] })));
  fails('CONFLICT', () => faculty.execute(attendance({ records: [{ studentId: 'S1', status: 'present' }, { studentId: 'S1', status: 'absent' }] })));
  fails('VALIDATION', () => faculty.execute(attendance({ records: [{ studentId: 'S1', status: 'unknown' }, { studentId: 'S2', status: 'present' }] })));
});
test('attendance percentage counts late, excludes excused and never fabricates a zero percentage', () => {
  const { faculty } = fixture();
  const initially = faculty.execute({ action: 'getAttendanceReport', subjectCode: 'CS101' }).report;
  assert.equal(initially[0].percentage, '');
  faculty.execute(attendance());
  faculty.execute(attendance({ slot: 'Period 2', requestId: 'attendance-2', records: [{ studentId: 'S1', status: 'late' }, { studentId: 'S2', status: 'excused' }] }));
  const report = faculty.execute({ action: 'getAttendanceReport', subjectCode: 'CS101' }).report;
  assert.deepEqual(report[0], { studentId: 'S1', name: 'S1', totalPresent: '2', totalClasses: '2', percentage: '100.0' });
  assert.equal(report[1].totalClasses, '1'); assert.equal(report[1].percentage, '0.0');
});
test('newly enrolled students are not penalized for sessions they were never on the roster for', () => {
  const { admin, faculty } = fixture();
  faculty.execute(attendance());
  admin.execute({ action: 'saveEnrollment', subjectCode: 'CS101', studentIds: ['S1', 'S2', 'S3'], revision: 1 });
  const report = faculty.execute({ action: 'getAttendanceReport', subjectCode: 'CS101' }).report;
  assert.equal(report.find(r => r.studentId === 'S3').totalClasses, '0');
});
test('draft marks are private, zero is a score, publishing requires complete entries', () => {
  const { faculty, student } = fixture();
  faculty.execute(assessment());
  assert.equal(student.execute({ action: 'getMyProgress' }).results.length, 0);
  fails('VALIDATION', () => faculty.execute(assessment({ revision: 1, requestId: 'publish-1', published: true })));
  faculty.execute(assessment({ revision: 1, requestId: 'publish-2', published: true, marks: [{ studentId: 'S1', status: 'scored', score: 0 }, { studentId: 'S2', status: 'absent' }] }));
  const results = student.execute({ action: 'getMyProgress' }).results;
  assert.equal(results.length, 1); assert.equal(results[0].score, 0);
  assert.equal('marks' in results[0], false); assert.equal('studentId' in results[0], false);
});
test('marks reject invalid scores, stale edits and edits to published results', () => {
  const { faculty } = fixture();
  for (const score of [-1, 21, null, '', Infinity, true, []]) fails('VALIDATION', () => faculty.execute(assessment({ marks: [{ studentId: 'S1', status: 'scored', score }, { studentId: 'S2', status: 'absent' }] })));
  faculty.execute(assessment());
  fails('CONFLICT', () => faculty.execute(assessment({ requestId: 'stale' })));
  faculty.execute(assessment({ revision: 1, requestId: 'publish', published: true, marks: [{ studentId: 'S1', status: 'scored', score: 10 }, { studentId: 'S2', status: 'absent' }] }));
  fails('CONFLICT', () => faculty.execute(assessment({ revision: 2, requestId: 'change' })));
});
test('assessment retry is idempotent and student records stay isolated', () => {
  const { faculty, service, events } = fixture();
  const request = assessment({ published: true, marks: [{ studentId: 'S1', status: 'scored', score: 8 }, { studentId: 'S2', status: 'scored', score: 19 }] });
  faculty.execute(request); const count = events.length;
  faculty.execute(request); assert.equal(events.length, count);
  assert.equal(service('student', 'S1').execute({ action: 'getMyProgress', studentId: 'S2' }).results[0].score, 8);
});
test('deactivation preserves history and blocks access to inactive profiles', () => {
  const { admin, faculty, store, student } = fixture();
  faculty.execute(attendance());
  admin.execute({ action: 'deleteStudent', rollNumber: 'S1', revision: 1 });
  assert.equal(store.get('student', 'S1').active, false);
  assert.equal(admin.execute({ action: 'getStudents' }).students.length, 2);
  assert.equal(faculty.execute({ action: 'getAttendanceReport', subjectCode: 'CS101' }).report.find(r => r.studentId === 'S1').totalPresent, '1');
  fails('NOT_FOUND', () => student.execute({ action: 'getMyProgress' }));
  fails('CONFLICT', () => admin.execute({ action: 'deleteFaculty', employeeId: 'F1', revision: 1 }));
});
test('dashboard counts come from active records and every mutation has an audit actor', () => {
  const { admin, events } = fixture();
  assert.deepEqual(admin.execute({ action: 'getDashboard' }).dashboard, { students: 3, faculty: 1, subjects: 1, departments: 1 });
  assert.ok(events.every(e => e.audit.actor === 'admin@example.edu' && e.audit.action && e.audit.at));
});
test('user allowlist fails closed on inactive, duplicated, unrecognized or subject-mismatched users', () => {
  const claims = { email: 'student@example.edu', sub: 'google-id', name: 'Student', exp: 1000 };
  const user = { email: claims.email, role: 'student', profileId: 'S1', active: true };
  assert.equal(principalFor(claims, [user]).profileId, 'S1');
  for (const users of [[], [user, user], [{ ...user, active: false }], [{ ...user, role: 'owner' }], [{ ...user, googleSubject: 'other-id' }]]) fails('FORBIDDEN', () => principalFor(claims, users));
});
test('identity claims reject wrong audience/issuer, expiration, future issue time or unverified email', () => {
  const claims = { iss: 'https://accounts.google.com', aud: 'web-client', sub: 'google-id', email: 'student@example.edu', email_verified: true, exp: 2000, iat: 900 };
  assert.equal(validateIdentity(claims, 'web-client', 1000), claims);
  for (const override of [{ aud: 'attacker' }, { iss: 'evil' }, { exp: 1000 }, { iat: 1100 }, { email_verified: false }, { exp: undefined }]) fails('UNAUTHENTICATED', () => validateIdentity({ ...claims, ...override }, 'web-client', 1000));
});

test('profile updates and deactivation detect conflicts and retry committed revisions safely', () => {
  const { admin, store, events } = fixture();
  const student = store.get('student', 'S1');
  const update = { ...student, action: 'updateStudent', name: 'Updated Name', revision: 1 };
  admin.execute(update);
  const writes = events.length;
  admin.execute(update);
  assert.equal(events.length, writes);
  fails('CONFLICT', () => admin.execute({ ...update, name: 'Stale Writer' }));
  fails('CONFLICT', () => admin.execute({ action: 'deleteStudent', rollNumber: 'S1', revision: 1 }));
  admin.execute({ action: 'deleteStudent', rollNumber: 'S1', revision: 2 });
  admin.execute({ action: 'deleteStudent', rollNumber: 'S1', revision: 2 });
  assert.equal(store.get('student', 'S1').revision, 3);
});
test('enrollment retries acknowledge a previously committed identical selection', () => {
  const { admin, events } = fixture();
  const request = { action: 'saveEnrollment', subjectCode: 'CS101', studentIds: ['S1', 'S2', 'S3'], revision: 1 };
  admin.execute(request); const writes = events.length;
  admin.execute(request); assert.equal(events.length, writes);
});
test('retry operation identifiers cannot hide a changed attendance or marks payload', () => {
  const { faculty } = fixture();
  faculty.execute(attendance());
  fails('CONFLICT', () => faculty.execute(attendance({ records: [{ studentId: 'S1', status: 'absent' }, { studentId: 'S2', status: 'absent' }] })));
  faculty.execute(assessment());
  fails('CONFLICT', () => faculty.execute(assessment({ title: 'Different title' })));
});
test('faculty cannot read or write another faculty member’s assigned subject', () => {
  const { admin, service } = fixture();
  admin.execute({ action: 'addFaculty', employeeId: 'F2', name: 'Other teacher', email: 'other@example.edu', department: 'CS' });
  const other = service('faculty', 'F2');
  assert.equal(other.execute({ action: 'getSubjects' }).subjects.length, 0);
  for (const action of ['getRoster', 'getAttendanceReport', 'getAssessments']) fails('FORBIDDEN', () => other.execute({ action, subjectCode: 'CS101' }));
  fails('FORBIDDEN', () => other.execute(attendance()));
  fails('FORBIDDEN', () => other.execute(assessment()));
});
test('institution-local date allows attendance after local midnight before UTC midnight', () => {
  const { store } = fixture();
  const faculty = createService(store, { role: 'faculty', profileId: 'F1', email: 'teacher@example.edu' }, () => '2026-09-30T20:00:00.000Z', () => '2026-10-01');
  assert.equal(faculty.execute(attendance({ date: '2026-10-01' })).message, 'Attendance submitted.');
});
