function decodeJwtPart_(part) {
  if (!/^[A-Za-z0-9_-]+$/.test(part) || part.length % 4 === 1) throw new Error('Invalid JWT encoding.');
  return forge.util.decode64(part.replace(/-/g, '+').replace(/_/g, '/') + '='.repeat((4 - part.length % 4) % 4));
}
function decodeJwtJson_(part) { return JSON.parse(forge.util.decodeUtf8(decodeJwtPart_(part))); }
function verifyGoogleToken(token, audience, certificates, nowSeconds) {
  try {
    if (typeof token !== 'string' || token.length > 16000 || token.split('.').length !== 3) throw new Error();
    const parts = token.split('.');
    const header = decodeJwtJson_(parts[0]);
    if (!header || header.alg !== 'RS256' || header.crit || !header.kid || !Object.prototype.hasOwnProperty.call(certificates, header.kid)) throw new Error();
    const pem = certificates[header.kid];
    const publicKey = pem.includes('BEGIN CERTIFICATE') ? forge.pki.certificateFromPem(pem).publicKey : forge.pki.publicKeyFromPem(pem);
    if (publicKey.n.bitLength() < 2048 || publicKey.e.toString() !== '65537') throw new Error();
    const digest = forge.md.sha256.create();
    digest.update(parts[0] + '.' + parts[1], 'utf8');
    const valid = publicKey.verify(digest.digest().bytes(), decodeJwtPart_(parts[2]), undefined, { _parseAllDigestBytes: true });
    if (!valid) throw new Error();
    const claims = decodeJwtJson_(parts[1]);
    return validateIdentity(claims, audience, nowSeconds);
  } catch (_) {
    fault('UNAUTHENTICATED', 'Your Google session is invalid or expired. Sign in again.');
  }
}
