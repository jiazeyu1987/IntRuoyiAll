const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const sourcePath = path.join(repoRoot, 'tests/e2e/dcc-restore-verify.e2e.js')

function readUtf8(filePath) {
  assert.ok(fs.existsSync(filePath), `missing required file: ${filePath}`)
  return fs.readFileSync(filePath, 'utf8')
}

const source = readUtf8(sourcePath)

assert.ok(
  source.includes("process.env.DCC_BACKUP_E2E_BASE_URL || ''"),
  'restore verify must require an explicit DCC_BACKUP_E2E_BASE_URL instead of defaulting to localhost'
)
assert.ok(
  !source.includes('http://localhost:8081'),
  'restore verify must not silently default to localhost:8081'
)
assert.ok(
  source.includes('assertRestoreExpectations()'),
  'restore verify must validate restore expectations before opening the browser'
)
for (const fragment of [
  "parseExpectationArray('DCC_RESTORE_E2E_PRESENT'",
  "parseExpectationArray('DCC_RESTORE_E2E_ABSENT'",
  'must be a JSON array',
  'restore present expectation is missing id',
  'restore absent expectation is missing id'
]) {
  assert.ok(source.includes(fragment), `restore verify must include expectation validation: ${fragment}`)
}
assert.ok(
  source.includes('DCC restore download returned JSON') && source.includes('/json/i.test(download.contentType)'),
  'restore verify must reject JSON download responses instead of treating them as file bytes'
)
assert.ok(
  source.includes('assert.ok(BASE_URL') &&
    source.includes('172.30.30.57') &&
    source.includes("TENANT !== '测试租户'") &&
    source.includes("USERNAME !== 'aoteman'"),
  'restore verify must fail fast on missing base URL, protected production host, non-test tenant, or non-test user'
)

console.log('PASS: DCC restore verify static safety contract is present')
