const test = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const { generateKeyPairSync, sign } = require('node:crypto');

const context = vm.createContext({ console });
vm.runInContext(fs.readFileSync('dist/Code.gs', 'utf8'), context);
const { privateKey, publicKey } = generateKeyPairSync('rsa', { modulusLength: 2048 });
const pem = publicKey.export({ type: 'spki', format: 'pem' });
const now = Math.floor(Date.now() / 1000);
const claims = { iss: 'https://accounts.google.com', aud: 'web-client', sub: 'google-id', email: 'student@example.edu', email_verified: true, iat: now - 10, exp: now + 3600 };
function token(payload = claims, header = { alg: 'RS256', kid: 'test-key' }, key = privateKey) {
  const body = [header, payload].map(x => Buffer.from(JSON.stringify(x)).toString('base64url')).join('.');
  return body + '.' + sign('RSA-SHA256', Buffer.from(body), key).toString('base64url');
}
function verify(jwt) { return context.verifyGoogleToken(jwt, 'web-client', { 'test-key': pem }, now); }
test('generated Apps Script bundle verifies an independently signed RSA token', () => {
  assert.equal(verify(token()).sub, claims.sub);
});
test('signature tampering, wrong signing key and algorithm confusion are rejected', () => {
  const valid = token();
  const parts = valid.split('.');
  parts[1] = Buffer.from(JSON.stringify({ ...claims, email: 'admin@example.edu' })).toString('base64url');
  const otherKey = generateKeyPairSync('rsa', { modulusLength: 2048 }).privateKey;
  for (const jwt of [parts.join('.'), token(claims, { alg: 'RS256', kid: 'test-key' }, otherKey), token(claims, { alg: 'none', kid: 'test-key' }), token(claims, { alg: 'HS256', kid: 'test-key' }), token(claims, { alg: 'RS256', kid: 'unknown' }), 'not-a-token']) {
    assert.throws(() => verify(jwt), e => e.code === 'UNAUTHENTICATED');
  }
});
test('signed but invalid audience, issuer and time claims are rejected', () => {
  for (const override of [{ aud: 'other-client' }, { iss: 'evil' }, { exp: now }, { exp: undefined }, { iat: now + 100 }, { nbf: now + 100 }, { email_verified: false }]) {
    assert.throws(() => verify(token({ ...claims, ...override })), e => e.code === 'UNAUTHENTICATED');
  }
});
test('Google-style X509 certificates and Unicode claims are supported', () => {
  const forge = require('node-forge');
  const certificate = forge.pki.createCertificate();
  certificate.publicKey = forge.pki.publicKeyFromPem(pem);
  certificate.serialNumber = '01';
  certificate.validity.notBefore = new Date(now * 1000);
  certificate.validity.notAfter = new Date((now + 86400) * 1000);
  certificate.setSubject([{ name: 'commonName', value: 'Test signing key' }]);
  certificate.setIssuer([{ name: 'commonName', value: 'Test signing key' }]);
  certificate.sign(forge.pki.privateKeyFromPem(privateKey.export({ type: 'pkcs8', format: 'pem' })), forge.md.sha256.create());
  const jwt = token({ ...claims, name: 'अनिता' });
  assert.equal(context.verifyGoogleToken(jwt, 'web-client', { 'test-key': forge.pki.certificateToPem(certificate) }, now).name, 'अनिता');
});
