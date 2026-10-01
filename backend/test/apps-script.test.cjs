const test = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const { generateKeyPairSync, sign } = require('node:crypto');
const bundle = fs.readFileSync('dist/Code.gs', 'utf8');
const { publicKey, privateKey } = generateKeyPairSync('rsa', { modulusLength: 2048 });
function fixture() {
  let locked = false, writes = 0, fetches = 0;
  const sheets = {
    TrackerUsers: [['email','role','profileId','name','active','googleSubject'], ['admin@example.edu','admin','','Admin',true,'admin-id']],
    TrackerRecords: [['kind','id','data','at','actor','action']]
  };
  const sheet = name => ({
    getDataRange: () => ({ getValues: () => structuredClone(sheets[name]) }),
    getLastRow: () => sheets[name].length,
    getRange: (row, column, count, width) => ({
      setNumberFormat(format) { assert.equal(format, '@'); },
      setValues(rows) { assert.equal(locked, true); assert.equal(column, 1); assert.equal(count, 1); assert.equal(rows[0].length, width); sheets[name][row - 1] = rows[0]; writes++; }
    })
  });
  const certificates = JSON.stringify({ 'test-key': publicKey.export({ type: 'spki', format: 'pem' }) });
  const context = vm.createContext({
    ContentService: { MimeType: { JSON: 'application/json' }, createTextOutput: text => ({ setMimeType: () => JSON.parse(text) }) },
    PropertiesService: { getScriptProperties: () => ({ getProperty: key => ({ SPREADSHEET_ID: 'test-sheet', GOOGLE_WEB_CLIENT_ID: 'web-client' })[key] }) },
    CacheService: { getScriptCache: () => ({ get: () => certificates, put: () => {} }) },
    UrlFetchApp: { fetch: () => { fetches++; throw new Error('Unexpected external request'); } },
    LockService: { getScriptLock: () => ({ tryLock: () => { locked = true; return true; }, hasLock: () => locked, releaseLock: () => { locked = false; } }) },
    SpreadsheetApp: { openById: id => { assert.equal(id, 'test-sheet'); assert.equal(locked, true); return { getSheetByName: name => sheet(name) }; }, flush: () => {} },
    Utilities: { formatDate: () => new Date().toISOString().slice(0, 10) },
    Session: { getScriptTimeZone: () => 'Asia/Kolkata' }
  });
  vm.runInContext(bundle, context);
  const now = Math.floor(Date.now() / 1000);
  const body = [{ alg: 'RS256', kid: 'test-key' }, { iss: 'https://accounts.google.com', aud: 'web-client', sub: 'admin-id', email: 'admin@example.edu', email_verified: true, exp: now + 3600, iat: now - 10 }].map(x => Buffer.from(JSON.stringify(x)).toString('base64url')).join('.');
  const idToken = body + '.' + sign('RSA-SHA256', Buffer.from(body), privateKey).toString('base64url');
  return { context, sheets, post: data => context.doPost({ postData: { contents: JSON.stringify({ idToken, ...data }) } }), get writes() { return writes; }, get locked() { return locked; }, get fetches() { return fetches; } };
}
test('complete dispatcher authenticates, persists an audited record and reads its projection', () => {
  const f = fixture();
  const result = f.post({ action: 'addStudent', rollNumber: '001', name: 'Student', email: 'student@example.edu', department: 'CS' });
  assert.equal(result.status, 'success'); assert.equal(result.apiVersion, 2);
  assert.equal(f.writes, 1); assert.equal(f.locked, false); assert.equal(f.fetches, 0);
  assert.equal(f.sheets.TrackerRecords[1][1], '"001"');
  assert.equal(JSON.parse(f.sheets.TrackerRecords[1][4]), 'admin@example.edu');
  assert.equal(f.post({ action: 'getStudents' }).students[0].rollNumber, '001');
});
test('unauthenticated POST and every GET are denied without touching spreadsheet records', () => {
  const f = fixture();
  assert.equal(f.post({ action: 'getStudents', idToken: 'forged' }).code, 'UNAUTHENTICATED');
  assert.equal(f.context.doGet().code, 'METHOD_NOT_ALLOWED');
  assert.equal(f.writes, 0); assert.equal(f.locked, false);
});
test('malformed JSON is rejected and lock is released on domain validation errors', () => {
  const f = fixture();
  assert.equal(f.context.doPost({ postData: { contents: '{bad-json' } }).code, 'VALIDATION');
  assert.equal(f.post({ action: 'addStudent', rollNumber: '001' }).code, 'VALIDATION');
  assert.equal(f.locked, false); assert.equal(f.writes, 0);
});
test('spreadsheet identifiers preserve leading zeroes and cannot become executable formulas', () => {
  const f = fixture();
  assert.equal(f.post({ action: 'addStudent', rollNumber: '=IMPORTXML("x")', name: '=DANGER', email: 's@example.edu', department: 'CS' }).status, 'success');
  assert.ok(!f.sheets.TrackerRecords[1].some(cell => /^[=+@\-]/.test(String(cell))));
  assert.equal(f.post({ action: 'getStudents' }).students[0].rollNumber, '=IMPORTXML("x")');
});
