function doGet() { return jsonResponse_({ status: 'error', apiVersion: 2, code: 'METHOD_NOT_ALLOWED', message: 'Use authenticated POST requests.' }); }
function doPost(event) {
  let lock;
  try {
    const body = event && event.postData && event.postData.contents;
    if (!body || body.length > 100000) fault('VALIDATION', 'Invalid request size.');
    let request;
    try { request = JSON.parse(body); } catch (_) { fault('VALIDATION', 'Invalid JSON request.'); }
    if (!request || typeof request !== 'object' || Array.isArray(request)) fault('VALIDATION', 'Invalid request.');
    const config = PropertiesService.getScriptProperties();
    const audience = config.getProperty('GOOGLE_WEB_CLIENT_ID');
    const spreadsheetId = config.getProperty('SPREADSHEET_ID');
    if (!audience || !spreadsheetId) fault('CONFIGURATION', 'Backend setup is incomplete.');
    let certificates = googleCertificates_();

    try {
      const header = decodeJwtJson_(String(request.idToken).split('.')[0]);
      if (header && header.alg === 'RS256' && header.kid && !Object.prototype.hasOwnProperty.call(certificates, header.kid)) certificates = googleCertificates_(true);
    } catch (_) {   }
    const claims = verifyGoogleToken(request.idToken, audience, certificates, Math.floor(Date.now() / 1000));
    const domain = config.getProperty('ALLOWED_GOOGLE_DOMAIN');
    if (domain && claims.hd !== domain) fault('FORBIDDEN', 'Use your institutional Google account.');

    lock = LockService.getScriptLock();
    if (!lock.tryLock(10000)) fault('BUSY', 'The service is busy. Try again shortly.');
    const spreadsheet = SpreadsheetApp.openById(spreadsheetId);
    const usersSheet = spreadsheet.getSheetByName('TrackerUsers');
    const recordSheet = spreadsheet.getSheetByName('TrackerRecords');
    if (!usersSheet || !recordSheet) fault('CONFIGURATION', 'Run setupTracker in the Apps Script editor first.');
    const users = tableObjects_(usersSheet);
    const user = principalFor(claims, users);
    const store = sheetStore_(recordSheet);
    const service = createService(store, user, undefined, () => Utilities.formatDate(new Date(), Session.getScriptTimeZone(), 'yyyy-MM-dd'));
    const result = service.execute(request);
    SpreadsheetApp.flush();
    return jsonResponse_({ status: 'success', apiVersion: 2, ...result });
  } catch (error) {

    return jsonResponse_({ status: 'error', apiVersion: 2, code: error.code || 'INTERNAL', message: error.code ? error.message : 'The service could not complete this request. Try again.' });
  } finally { if (lock && lock.hasLock()) lock.releaseLock(); }
}
function jsonResponse_(value) { return ContentService.createTextOutput(JSON.stringify(value)).setMimeType(ContentService.MimeType.JSON); }
function googleCertificates_(refresh = false) {
  const cache = CacheService.getScriptCache();
  const cached = refresh ? null : cache.get('google-signing-certificates-v1');
  if (cached) return JSON.parse(cached);
  const response = UrlFetchApp.fetch('https://www.googleapis.com/oauth2/v1/certs', { muteHttpExceptions: true });
  if (response.getResponseCode() !== 200) fault('UNAVAILABLE', 'Sign-in verification is temporarily unavailable.');
  const body = response.getContentText();
  const headers = response.getAllHeaders();
  const cacheHeader = Object.keys(headers).find(k => k.toLowerCase() === 'cache-control');
  const maxAge = /max-age=(\d+)/.exec(String(cacheHeader ? headers[cacheHeader] : ''));
  if (maxAge && Number(maxAge[1]) > 0) cache.put('google-signing-certificates-v1', body, Math.min(21600, Number(maxAge[1])));
  return JSON.parse(body);
}
function tableObjects_(sheet) {
  const rows = sheet.getDataRange().getValues();
  const headers = rows.shift();
  return rows.filter(row => row[0]).map(row => Object.fromEntries(headers.map((h, i) => [h, row[i]])));
}
function sheetStore_(sheet) {
  const current = new Map();
  tableObjects_(sheet).forEach(row => current.set(JSON.stringify([row.kind, JSON.parse(row.id)]), JSON.parse(row.data)));
  return {
    get: (kind, id) => current.get(JSON.stringify([kind, id])),
    list: kind => Array.from(current, ([key, value]) => JSON.parse(key)[0] === kind ? value : null).filter(Boolean),
    put: (kind, id, data, audit) => {

      const row = [kind, JSON.stringify(id), JSON.stringify(data), audit.at, JSON.stringify(audit.actor), audit.action];
      const range = sheet.getRange(sheet.getLastRow() + 1, 1, 1, row.length);

      range.setNumberFormat('@');
      range.setValues([row]);
      current.set(JSON.stringify([kind, id]), data);
    }
  };
}

function setupTracker() {
  const properties = PropertiesService.getScriptProperties();
  const spreadsheetId = properties.getProperty('SPREADSHEET_ID');
  if (!spreadsheetId) throw new Error('Set SPREADSHEET_ID and GOOGLE_WEB_CLIENT_ID in Script Properties.');
  const spreadsheet = SpreadsheetApp.openById(spreadsheetId);
  const schemas = { TrackerUsers: ['email','role','profileId','name','active','googleSubject'], TrackerRecords: ['kind','id','data','at','actor','action'] };
  Object.keys(schemas).forEach(name => {
    if (!spreadsheet.getSheetByName(name)) {
      const sheet = spreadsheet.insertSheet(name);
      sheet.appendRow(schemas[name]); sheet.setFrozenRows(1);
    }
  });
}
