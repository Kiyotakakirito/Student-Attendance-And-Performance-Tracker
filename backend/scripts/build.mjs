import { readFileSync, mkdirSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = file => readFileSync(path.join(root, file), 'utf8');
mkdirSync(path.join(root, 'dist'), { recursive: true });

const crypto = read('node_modules/node-forge/dist/forge.min.js');
writeFileSync(path.join(root, 'dist', 'Code.gs'), 'var window = {};\n' + crypto + '\nvar forge = window.forge;\n' + ['core.js', 'auth.js', 'apps-script.js'].map(f => read('src/' + f)).join('\n'));
writeFileSync(path.join(root, 'dist', 'THIRD_PARTY_LICENSE.txt'), read('node_modules/node-forge/LICENSE'));
writeFileSync(path.join(root, 'dist', 'appsscript.json'), JSON.stringify({ timeZone: 'Asia/Kolkata', runtimeVersion: 'V8', exceptionLogging: 'STACKDRIVER', oauthScopes: ['https://www.googleapis.com/auth/spreadsheets', 'https://www.googleapis.com/auth/script.external_request'] }, null, 2));
console.log('Built dist/Code.gs and dist/appsscript.json');
