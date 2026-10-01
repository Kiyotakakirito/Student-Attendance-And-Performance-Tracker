function fault(code, message) {
  const error = new Error(message);
  error.code = code;
  throw error;
}
function required(value, label, max = 160) {
  if (typeof value !== 'string' || !value.trim() || value.trim().length > max) {
    fault('VALIDATION', label + ' is required (maximum ' + max + ' characters).');
  }
  return value.trim();
}
function numeric(value, label, min, max) {
  if (!['number', 'string'].includes(typeof value) || (typeof value === 'string' && !value.trim()) || !Number.isFinite(Number(value)) || Number(value) < min || Number(value) > max) {
    fault('VALIDATION', label + ' must be between ' + min + ' and ' + max + '.');
  }
  return Number(value);
}
function emailAddress(value) {
  const email = required(value, 'Email', 254).toLowerCase();
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) fault('VALIDATION', 'Enter a valid email.');
  return email;
}
function validateIdentity(claims, audience, now) {
  if (!claims || !['accounts.google.com', 'https://accounts.google.com'].includes(claims.iss) ||
      claims.aud !== audience || typeof claims.exp !== 'number' || claims.exp <= now ||
      !Number.isFinite(claims.exp) || typeof claims.iat !== 'number' || !Number.isFinite(claims.iat) || claims.iat > now + 30 || claims.iat >= claims.exp || typeof claims.sub !== 'string' || !claims.sub ||
      (claims.nbf !== undefined && (!Number.isFinite(claims.nbf) || claims.nbf > now)) ||
      claims.email_verified !== true || typeof claims.email !== 'string') {
    fault('UNAUTHENTICATED', 'Your Google session is invalid or expired. Sign in again.');
  }
  return claims;
}
function principalFor(claims, users) {
  const matches = users.filter(u => String(u.email).toLowerCase() === claims.email.toLowerCase());
  const user = matches.length === 1 ? matches[0] : null;
  if (!user || String(user.active).toLowerCase() !== 'true' || !['admin', 'faculty', 'student'].includes(user.role) ||
      (user.googleSubject && String(user.googleSubject) !== claims.sub)) {
    fault('FORBIDDEN', 'This account has not been approved. Contact your administrator.');
  }
  return { email: claims.email.toLowerCase(), role: user.role, profileId: String(user.profileId || ''), name: String(user.name || claims.name || ''), expiresAt: claims.exp };
}

function createService(store, user, now = () => new Date().toISOString(), today = () => now().slice(0, 10)) {
  const all = kind => store.list(kind).filter(x => x.active !== false);
  const admin = () => { if (user.role !== 'admin') fault('FORBIDDEN', 'Administrator access required.'); };
  const get = (kind, id) => {
    const value = store.get(kind, id);
    if (!value || value.active === false) fault('NOT_FOUND', 'Record not found or inactive.');
    return value;
  };
  const subjectFor = id => {
    const subject = get('subject', id);
    if (user.role === 'faculty' && subject.facultyId === user.profileId) return subject;
    if (user.role !== 'admin') fault('FORBIDDEN', 'You are not assigned to this subject.');
    return subject;
  };
  const save = (kind, id, data, action) => {
    const value = { ...data, updatedAt: now() };
    if (JSON.stringify(value).length > 40000) fault('CAPACITY', 'This record exceeds the supported size. Contact your administrator.');
    store.put(kind, id, value, { actor: user.email, action, at: now() });
    return value;
  };
  const roster = subjectCode => {
    const enrollment = store.get('enrollment', subjectCode);
    return (enrollment ? enrollment.studentIds : []).map(id => store.get('student', id)).filter(s => s && s.active !== false);
  };
  const minimal = s => ({ studentId: s.rollNumber, name: s.name });
  const validateRoster = (code, records) => {
    const members = roster(code);
    if (!members.length) fault('VALIDATION', 'No students are enrolled in this subject.');
    if (!Array.isArray(records) || records.some(r => !r || typeof r !== 'object') || records.length !== members.length || new Set(records.map(r => r.studentId)).size !== members.length ||
        records.some(r => !members.some(s => s.rollNumber === r.studentId))) {
      fault('CONFLICT', 'The class roster changed. Reload it before submitting.');
    }
    return members;
  };
  const attendanceReport = code => {
    const sessions = all('attendance').filter(s => s.subjectCode === code);
    const students = new Map(roster(code).map(s => [s.rollNumber, s.name]));
    sessions.forEach(s => s.records.forEach(r => students.set(r.studentId, r.name)));
    return Array.from(students, ([studentId, name]) => {
      const entries = sessions.flatMap(s => s.records.filter(r => r.studentId === studentId && r.status !== 'excused'));
      const present = entries.filter(r => r.status === 'present' || r.status === 'late').length;
      return { studentId, name, totalPresent: String(present), totalClasses: String(entries.length), percentage: entries.length ? (100 * present / entries.length).toFixed(1) : '' };
    });
  };
  function crud(action, request) {
    admin();
    const match = /^(add|update|delete)(Student|Faculty|Subject)$/.exec(action);
    if (!match) fault('VALIDATION', 'Unknown action.');
    const [, operation, type] = match;
    const kind = type.toLowerCase();
    const key = { student: 'rollNumber', faculty: 'employeeId', subject: 'subjectCode' }[kind];
    const id = required(request[key], key, 80);
    const old = store.get(kind, id);
    if (operation === 'add' && old) fault('CONFLICT', 'This identifier already exists, including archived records.');
    if (operation !== 'add' && (!Number.isInteger(request.revision) || request.revision < 0)) fault('VALIDATION', 'Invalid record revision.');
    if (operation === 'delete') {
      if (old?.active === false && old.revision === request.revision + 1) return { message: type + ' already deactivated.' };
      get(kind, id);
      if ((old.revision || 0) !== request.revision) fault('CONFLICT', 'This record changed. Reload before deactivating it.');
      if (kind === 'faculty' && all('subject').some(s => s.facultyId === id)) fault('CONFLICT', 'Reassign this faculty member’s subjects before deactivation.');
      save(kind, id, { ...old, active: false, revision: (old.revision || 0) + 1 }, action);
      return { message: type + ' deactivated. Historical records were retained.' };
    }
    const fields = {
      student: ['rollNumber','name','email','phone','department','batch','year','semester','section','parentName','parentPhone','parentEmail'],
      faculty: ['employeeId','name','email','phone','department','designation','joiningDate'],
      subject: ['subjectCode','subjectName','facultyId','semester','totalClasses']
    }[kind];
    const data = {};
    fields.forEach(field => { data[field] = String(request[field] || '').trim(); if (data[field].length > 254) fault('VALIDATION', field + ' is too long.'); });
    if (operation === 'update') {
      get(kind, id);

      if (old.revision === request.revision + 1 && fields.every(field => old[field] === data[field])) return { message: type + ' already saved.' };
      if ((old.revision || 0) !== request.revision) fault('CONFLICT', 'This record changed. Reload before saving.');
    }
    required(data[kind === 'subject' ? 'subjectName' : 'name'], 'Name');
    if (kind === 'subject') {
      get('faculty', required(data.facultyId, 'Faculty'));
      if (!Number.isInteger(numeric(data.totalClasses, 'Planned classes', 1, 1000))) fault('VALIDATION', 'Planned classes must be a whole number.');
      required(data.semester, 'Semester');
    } else {
      data.email = emailAddress(data.email);
      required(data.department, 'Department');
      if (all(kind).some(v => v[key] !== id && v.email.toLowerCase() === data.email)) fault('CONFLICT', 'This email is already registered.');
      if (data.parentEmail) emailAddress(data.parentEmail);
    }
    save(kind, id, { ...data, active: true, revision: (old?.revision || 0) + 1 }, action);
    return { message: type + ' saved.' };
  }
  function execute(request) {
    const action = required(request.action, 'Action', 60);
    if (user.role !== 'admin') get(user.role, user.profileId);
    switch (action) {
      case 'getUserRole': return { ...user };
      case 'getStudent': admin(); return { student: get('student', request.rollNumber) };
      case 'getFacultyMember': admin(); return { facultyMember: get('faculty', request.employeeId) };
      case 'getSubject': admin(); return { subject: get('subject', request.subjectCode) };
      case 'getStudents': admin(); return { students: all('student') };
      case 'getFaculty': admin(); return { faculty: all('faculty') };
      case 'getSubjects': {
        if (user.role === 'student') return { subjects: all('subject').filter(s => (store.get('enrollment', s.subjectCode)?.studentIds || []).includes(user.profileId)) };
        return { subjects: all('subject').filter(s => user.role === 'admin' || s.facultyId === user.profileId) };
      }
      case 'getDashboard': {
        admin();
        return { dashboard: { students: all('student').length, faculty: all('faculty').length, subjects: all('subject').length, departments: new Set(all('student').map(s => s.department)).size } };
      }
      case 'getRoster': {
        subjectFor(request.subjectCode);
        const enrollment = store.get('enrollment', request.subjectCode);
        return { students: roster(request.subjectCode).map(minimal), revision: enrollment?.revision || 0 };
      }
      case 'saveEnrollment': {
        admin(); get('subject', request.subjectCode);
        const old = store.get('enrollment', request.subjectCode);
        if (!Array.isArray(request.studentIds) || request.studentIds.length > 250 || new Set(request.studentIds).size !== request.studentIds.length) fault('VALIDATION', 'Select up to 250 distinct students.');
        if (!Number.isInteger(request.revision) || request.revision < 0) fault('VALIDATION', 'Invalid enrollment revision.');
        if (old && old.revision === request.revision + 1 && JSON.stringify([...old.studentIds].sort()) === JSON.stringify([...request.studentIds].sort())) return { message: 'Enrollment already saved.', revision: old.revision };
        if ((old?.revision || 0) !== request.revision) fault('CONFLICT', 'Enrollment changed. Reload and try again.');
        request.studentIds.forEach(id => get('student', id));
        const enrollment = save('enrollment', request.subjectCode, { studentIds: request.studentIds, revision: (old?.revision || 0) + 1 }, action);
        return { message: 'Enrollment saved.', revision: enrollment.revision };
      }
      case 'saveAttendance': {
        subjectFor(request.subjectCode);
        const date = required(request.date, 'Date', 10);
        const parsed = new Date(date + 'T00:00:00Z');
        if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || !Number.isFinite(parsed.getTime()) || parsed.toISOString().slice(0, 10) !== date || date > today()) fault('VALIDATION', 'Enter a valid class date that is not in the future.');
        const slot = required(request.slot, 'Session', 40);
        const requestId = required(request.requestId, 'Request ID', 80);
        const id = JSON.stringify([request.subjectCode, date, slot.toLowerCase()]);
        const old = store.get('attendance', id);
        if (old) {
          if (old.requestId === requestId && Array.isArray(request.records) && JSON.stringify(old.records.map(r => [r.studentId, r.status]).sort()) === JSON.stringify(request.records.map(r => r && [r.studentId, r.status]).sort())) return { message: 'Attendance already submitted.' };
          fault('CONFLICT', 'Attendance for this session has already been submitted.');
        }
        const members = validateRoster(request.subjectCode, request.records);
        if (request.records.some(r => !['present','absent','late','excused'].includes(r.status))) fault('VALIDATION', 'Choose a status for every student.');
        save('attendance', id, { subjectCode: request.subjectCode, date, slot, requestId, records: request.records.map(r => ({ studentId: r.studentId, name: members.find(s => s.rollNumber === r.studentId).name, status: r.status })) }, action);
        return { message: 'Attendance submitted.' };
      }
      case 'getAttendanceReport': subjectFor(request.subjectCode); return { report: attendanceReport(request.subjectCode) };
      case 'getAssessments': {
        subjectFor(request.subjectCode);
        return { assessments: all('assessment').filter(a => a.subjectCode === request.subjectCode) };
      }
      case 'saveAssessment': {
        subjectFor(request.subjectCode);
        const id = required(request.assessmentId, 'Assessment ID', 80);
        const old = store.get('assessment', id);
        if (old && old.subjectCode !== request.subjectCode) fault('FORBIDDEN', 'Assessment belongs to another subject.');

        const requestId = required(request.requestId, 'Request ID', 80);
        if (!Number.isInteger(request.revision) || request.revision < 0 || typeof request.published !== 'boolean') fault('VALIDATION', 'Invalid assessment revision or publication state.');
        if (old && old.requestId === requestId) {
          const entries = rows => rows.map(m => m && [m.studentId, m.status, m.status === 'scored' ? Number(m.score) : null]).sort();
          if (typeof request.title !== 'string' || request.title.trim() !== old.title || Number(request.maxMarks) !== old.maxMarks || request.published !== old.published || !Array.isArray(request.marks) || JSON.stringify(entries(request.marks)) !== JSON.stringify(entries(old.marks))) fault('CONFLICT', 'This request ID was already used with different data.');
          return { message: 'Assessment already saved.', assessment: old };
        }
        if ((old?.revision || 0) !== request.revision) fault('CONFLICT', 'Assessment changed. Reload before saving.');
        if (old?.published) fault('CONFLICT', 'Published assessments are locked.');
        const title = required(request.title, 'Assessment title');
        const maxMarks = numeric(request.maxMarks, 'Maximum marks', 1, 10000);
        const members = validateRoster(request.subjectCode, request.marks);
        const marks = request.marks.map(m => {
          if (!['scored','absent','pending'].includes(m.status)) fault('VALIDATION', 'Invalid marks status.');
          if (request.published && m.status === 'pending') fault('VALIDATION', 'Complete all marks before publishing.');
          return { studentId: m.studentId, name: members.find(s => s.rollNumber === m.studentId).name, status: m.status, score: m.status === 'scored' ? numeric(m.score, 'Marks', 0, maxMarks) : null };
        });
        const assessment = save('assessment', id, { assessmentId: id, subjectCode: request.subjectCode, title, maxMarks, marks, published: request.published === true, revision: (old?.revision || 0) + 1, requestId }, action);
        return { message: request.published ? 'Results published.' : 'Draft saved.', assessment };
      }
      case 'getMyProgress': {
        if (user.role !== 'student') fault('FORBIDDEN', 'Student access required.');
        const courses = store.list('subject').filter(s => (store.get('enrollment', s.subjectCode)?.studentIds || []).includes(user.profileId) || all('attendance').some(a => a.subjectCode === s.subjectCode && a.records.some(r => r.studentId === user.profileId)));
        return { attendance: courses.map(s => ({ subjectCode: s.subjectCode, subjectName: s.subjectName, ...(attendanceReport(s.subjectCode).find(r => r.studentId === user.profileId) || { totalPresent: '0', totalClasses: '0', percentage: '' }) })), results: all('assessment').filter(a => a.published && a.marks.some(m => m.studentId === user.profileId)).map(a => {
          const mark = a.marks.find(m => m.studentId === user.profileId);
          return { assessmentId: a.assessmentId, title: a.title, subjectCode: a.subjectCode, maxMarks: a.maxMarks, score: mark.score, status: mark.status };
        }) };
      }
      default: return crud(action, request);
    }
  }
  return { execute };
}
if (typeof module !== 'undefined') module.exports = { createService, validateIdentity, principalFor, fault };
