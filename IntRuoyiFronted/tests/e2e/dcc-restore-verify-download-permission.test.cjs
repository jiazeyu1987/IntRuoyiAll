const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const verifyScript = fs.readFileSync(path.join(__dirname, 'dcc-restore-verify.e2e.js'), 'utf8')
const flowScript = fs.readFileSync(path.join(__dirname, '..', '..', 'scripts', 'dcc-incremental-backup-restore-real-flow-gate.mjs'), 'utf8')

assert.match(verifyScript, /allowDownloadAccessDenied/)
assert.match(verifyScript, /downloadAccess\s*=\s*'denied'/)
assert.match(verifyScript, /Current user cannot access this controlled file/)
assert.match(flowScript, /allowDownloadAccessDenied:\s*true/)

console.log('PASS dcc restore verify download permission semantics')
