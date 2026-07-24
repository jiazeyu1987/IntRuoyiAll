const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const scriptPath = path.resolve(__dirname, 'edhr-extra-form-slots-real-flow.e2e.js')
const source = fs.readFileSync(scriptPath, 'utf8')

const requiredSnippets = [
  ['MAIN slot', "'MAIN'"],
  ['PROCESS_INSPECTION slot', "'PROCESS_INSPECTION'"],
  ['LOSS_REPORT slot', "'LOSS_REPORT'"],
  ['PARAMETER_RECORD slot', "'PARAMETER_RECORD'"],
  ['test tenant lock', "REQUIRED_TEST_TENANT = '测试租户'"],
  ['test user lock', "REQUIRED_TEST_USERNAME = 'aoteman'"],
  ['admin readonly lock', "REQUIRED_ADMIN_TENANT = '芋道源码'"],
  ['admin password gate', 'EDHR_EXTRA_SLOTS_ADMIN_PASSWORD'],
  ['official login preflight', 'login-preflight.mjs'],
  ['route config API', 'route/flow-config/process-config-list'],
  ['batch detail API', 'edhr-batch-execution/get'],
  ['archive latest API', 'edhr-batch-execution-archive/latest'],
  ['admin readonly request guard', 'mutatingRequests'],
  ['no fake pass blocker', "result.status = error.blocked ? 'BLOCKED' : 'FAIL'"]
]

for (const [label, snippet] of requiredSnippets) {
  assert.ok(source.includes(snippet), `Missing static contract snippet: ${label}`)
}

assert.ok(!source.includes('mock'), 'E2E gate must not introduce mock paths.')
assert.ok(!source.includes('fallback'), 'E2E gate must not introduce fallback paths.')
assert.ok(!source.includes('admin123'), 'E2E gate must not rely on historical admin123 credentials.')

console.log('PASS: edhr-extra-form-slots static E2E contract')
