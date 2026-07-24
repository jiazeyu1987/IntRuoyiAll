const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const realE2eRelativePath = 'tests/e2e/runtime-control-edhr-archive-health.e2e.js'
const realE2ePath = path.join(repoRoot, realE2eRelativePath)
const packageJsonPath = path.join(repoRoot, 'package.json')

function assertFileExists(filePath, label) {
  if (!fs.existsSync(filePath)) {
    throw new Error(`missing ${label}: ${path.relative(repoRoot, filePath)}`)
  }
}

function assertContains(source, expected, label) {
  if (!source.includes(expected)) {
    throw new Error(`missing ${label}: ${expected}`)
  }
}

function assertPackageScript(scripts, name, expected) {
  if (!Object.prototype.hasOwnProperty.call(scripts, name)) {
    throw new Error(`missing package script: ${name}`)
  }
  if (scripts[name] !== expected) {
    throw new Error(`unexpected package script ${name}: ${scripts[name]}`)
  }
}

assertFileExists(realE2ePath, 'real eDHR archive business health E2E script')
assertFileExists(packageJsonPath, 'frontend package manifest')

const source = fs.readFileSync(realE2ePath, 'utf8')
const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'))
const scripts = packageJson.scripts || {}

assertPackageScript(
  scripts,
  'e2e:edhr:archive-health:check',
  'node tests/e2e/runtime-control-edhr-archive-health-static.spec.js'
)
assertPackageScript(
  scripts,
  'e2e:edhr:archive-health',
  'node tests/e2e/runtime-control-edhr-archive-health.e2e.js'
)

assertContains(source, '/infra/monitors/runtime-control', 'runtime-control frontend route')
assertContains(source, '/admin-api/infra/runtime-control/business-health', 'business health API response capture')
assertContains(source, 'edhr-archive-integrity', 'business health item code')
assertContains(source, 'eDHR 归档完整性', 'business health item name')
assertContains(source, 'login?redirect=/infra/monitors/runtime-control', 'login redirect real user path')
assertContains(source, 'button:has-text("登录")', 'real frontend login submit')
assertContains(source, 'RUNTIME_CONTROL_E2E_BASE_URL', 'frontend base URL env marker')
assertContains(source, 'RUNTIME_CONTROL_E2E_TEST_TENANT', 'test tenant env marker')
assertContains(source, 'RUNTIME_CONTROL_E2E_TEST_USERNAME', 'test username env marker')
assertContains(source, 'RUNTIME_CONTROL_E2E_TEST_PASSWORD', 'test password env marker')
assertContains(source, 'writeRequests', 'read-only request collection')
assertContains(source, "request.method() !== 'GET'", 'non-GET runtime-control guard')

console.log('PASS: eDHR archive business health real E2E static contract is wired')
