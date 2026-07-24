const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const flowScript = fs.readFileSync(
  path.join(__dirname, '..', '..', 'scripts', 'dcc-incremental-backup-restore-real-flow-gate.mjs'),
  'utf8'
)

assert.match(flowScript, /const DCC_BASE_URL =/)
assert.match(flowScript, /const RUNTIME_BASE_URL =/)
assert.doesNotMatch(flowScript, /RUNTIME_CONTROL_E2E_BASE_URL:\s*BASE_URL/)
assert.match(flowScript, /DCC_BACKUP_E2E_BASE_URL:\s*DCC_BASE_URL/)
assert.match(flowScript, /RUNTIME_CONTROL_E2E_BASE_URL:\s*RUNTIME_BASE_URL/)

console.log('PASS dcc flow split DCC and runtime base URLs')
