const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const packageJsonPath = path.join(repoRoot, 'package.json')
const planCheckPath = path.join(repoRoot, 'scripts/dcc-readiness-remediation-plan-check.mjs')

function readUtf8(filePath) {
  assert.ok(fs.existsSync(filePath), `missing required file: ${filePath}`)
  return fs.readFileSync(filePath, 'utf8')
}

const packageJson = JSON.parse(readUtf8(packageJsonPath))
assert.equal(
  packageJson.scripts?.['e2e:dcc:incremental-backup-restore:readiness:plan'],
  'node scripts/dcc-readiness-remediation-plan-check.mjs',
  'package.json must expose the read-only remediation plan check'
)
assert.equal(
  packageJson.scripts?.['e2e:dcc:incremental-backup-restore:readiness:plan:check'],
  'node tests/e2e/dcc-readiness-remediation-plan-static.spec.js',
  'package.json must expose the remediation plan static check'
)

const source = readUtf8(planCheckPath)
for (const fragment of [
  'DCC_READINESS_REMEDIATION_PLAN_CHECK',
  'DCC_READINESS_REMEDIATION_PLAN_ARTIFACT',
  'expectedActionOrder',
  'prepare_isolated_test_tenant_package',
  'assign_test_tenant_mdm_product_menu',
  'prepare_test_tenant_dcc_product',
  'prepare_test_tenant_dcc_upload_size_policy',
  'DCC_UPLOAD_POLICY_E2E_ALLOW_WRITE',
  'DCC_UPLOAD_POLICY_E2E_APPROVAL',
  'nextRunCommandTemplate',
  'omitted-from-readiness-artifact',
  '<set-after-explicit-user-approval>',
  'powershell',
  'does not execute remediation actions'
]) {
  assert.ok(source.includes(fragment), `remediation plan check must include ${fragment}`)
}

for (const forbiddenToken of [
  'ALLOW_TEST_DCC_INCREMENTAL_BACKUP_RESTORE',
  'ALLOW_TEST_TENANT_PACKAGE_WRITE',
  'ALLOW_TEST_MDM_ROLE_MENU_WRITE',
  'ALLOW_TEST_MDM_PRODUCT_WRITE',
  'ALLOW_TEST_DCC_UPLOAD_POLICY_WRITE',
  'ALLOW_TEST_DCC_FILE_WRITE',
  'ALLOW_PROD_RUNTIME_PROMOTE_WRITE'
]) {
  assert.ok(!source.includes(forbiddenToken), `plan check must not contain approval token ${forbiddenToken}`)
}

assert.ok(
  source.includes('ALLOW_(TEST|PROD)_[A-Z0-9_]+'),
  'plan check must reject both test and production approval token values in artifacts'
)

console.log('PASS: DCC readiness remediation plan static contract is present')
