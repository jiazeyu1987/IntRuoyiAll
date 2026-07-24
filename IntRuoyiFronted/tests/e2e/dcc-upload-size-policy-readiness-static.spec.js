const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const scriptPath = path.join(repoRoot, 'tests/e2e/dcc-upload-size-policy-readiness.e2e.js')
const packageJsonPath = path.join(repoRoot, 'package.json')

function readUtf8(filePath) {
  assert.ok(fs.existsSync(filePath), `missing required file: ${filePath}`)
  return fs.readFileSync(filePath, 'utf8')
}

const source = readUtf8(scriptPath)
const packageJson = JSON.parse(readUtf8(packageJsonPath))

assert.equal(
  packageJson.scripts?.['e2e:dcc:upload-policy:readiness:check'],
  'node tests/e2e/dcc-upload-size-policy-readiness-static.spec.js',
  'package.json must expose the DCC upload policy readiness static gate'
)
assert.equal(
  packageJson.scripts?.['e2e:dcc:upload-policy:readiness'],
  'node tests/e2e/dcc-upload-size-policy-readiness.e2e.js',
  'package.json must expose the DCC upload policy read-only readiness path'
)

for (const fragment of [
  "require('playwright')",
  'DCC_UPLOAD_POLICY_E2E_BASE_URL',
  'DCC_UPLOAD_POLICY_E2E_TENANT',
  'DCC_UPLOAD_POLICY_E2E_USERNAME',
  '172.30.30.57',
  '测试租户',
  'aoteman',
  'Codex Local DCC Category',
  'SOURCE',
  '/dcc/controlled-file/upload',
  '/admin-api/dcc/file-categories',
  '/admin-api/dcc/protection/upload-size-policies/effective',
  'DCC_UPLOAD_SIZE_POLICY_READINESS_RESULT',
  'readOnly: true',
  'missing approved upload size policy data'
]) {
  assert.ok(source.includes(fragment), `upload policy readiness script must contain ${fragment}`)
}

assert.ok(
  !source.includes("method: 'POST'") &&
    !source.includes('method: "POST"') &&
    !source.includes("method: 'PUT'") &&
    !source.includes('method: "PUT"') &&
    !source.includes('DCC_UPLOAD_POLICY_E2E_ALLOW_WRITE') &&
    !source.includes('DCC_UPLOAD_POLICY_E2E_APPROVAL') &&
    !source.includes('ALLOW_TEST_DCC_UPLOAD_POLICY_WRITE'),
  'upload policy readiness must remain read-only and must not expose write approval switches'
)
assert.ok(
  source.includes('page.goto(`${BASE_URL}/dcc/controlled-file/upload`') &&
    source.includes('categoryResponse') &&
    source.includes('findCategory'),
  'upload policy readiness must derive category id from the real authenticated upload page category response'
)

console.log('PASS: DCC upload size policy readiness static contract is present')
