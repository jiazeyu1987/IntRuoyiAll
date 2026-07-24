const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const gatePath = path.join(repoRoot, 'scripts/dcc-incremental-backup-restore-readiness-gate.mjs')
const packageJsonPath = path.join(repoRoot, 'package.json')

function readUtf8(filePath) {
  assert.ok(fs.existsSync(filePath), `missing required file: ${filePath}`)
  return fs.readFileSync(filePath, 'utf8')
}

const packageJson = JSON.parse(readUtf8(packageJsonPath))
const gateSource = readUtf8(gatePath)
const sharedEnvSource = readUtf8(path.join(repoRoot, 'scripts/dcc-write-control-env.mjs'))

assert.equal(
  packageJson.scripts?.['e2e:dcc:incremental-backup-restore:readiness:check'],
  'node tests/e2e/dcc-incremental-backup-restore-readiness-gate-static.spec.js',
  'package.json must expose the DCC incremental backup/restore readiness static gate'
)
assert.equal(
  packageJson.scripts?.['e2e:dcc:incremental-backup-restore:readiness'],
  'node scripts/dcc-incremental-backup-restore-readiness-gate.mjs',
  'package.json must expose the DCC incremental backup/restore readiness gate'
)

for (const fragment of [
  'DCC_INCREMENTAL_BACKUP_RESTORE_READINESS_RESULT',
  'DCC_INCREMENTAL_BACKUP_RESTORE_READINESS_ARTIFACT',
  '172.30.30.57',
  '测试租户',
  'writeMode: false',
  'dcc-incremental-backup-preflight.e2e.js',
  'mdm-tenant-package-real-setup.e2e.js',
  'mdm-role-menu-real-setup.e2e.js',
  'mdm-product-real-setup.e2e.js',
  'dcc-upload-size-policy-readiness.e2e.js',
  'manualResolutions',
  'ready',
  'blockedSteps',
  'remediationActions',
  'compactBlocker',
  'redactApprovalTokens'
]) {
  assert.ok(gateSource.includes(fragment), `readiness gate must include ${fragment}`)
}

assert.ok(
  gateSource.includes("from './dcc-write-control-env.mjs'") &&
    gateSource.includes('findPresentWriteControlEnvNames') &&
    gateSource.includes('redactApprovalTokens') &&
    gateSource.includes('stripWriteControlEnv(childEnv)'),
  'readiness gate must use the shared write-control inventory and redactor before child steps'
)
assert.ok(
  !/function redactApprovalTokens\(text\)/.test(gateSource),
  'readiness gate must not keep a local approval-token redactor'
)
assert.ok(
  gateSource.includes('function assertNoWriteModeEnv()') &&
    gateSource.includes('readiness gate refuses write-mode environment variables') &&
    gateSource.includes('findPresentWriteControlEnvNames()'),
  'readiness gate must fail fast when parent env contains write-mode variables'
)
assert.ok(
  gateSource.includes('function resolveRequiredBaseUrl()') &&
    gateSource.includes('DCC_BACKUP_E2E_BASE_URL or RUNTIME_CONTROL_E2E_BASE_URL is required for readiness gate') &&
    !gateSource.includes("'http://localhost:8081'") &&
    !gateSource.includes('"http://localhost:8081"'),
  'readiness gate must require an explicit base URL instead of silently using localhost:8081'
)
assert.ok(
  gateSource.includes("const noisyMarkers = ['; tree=', ' body=', ' dropdown=']") &&
    gateSource.includes('const noisyIndex = normalized.indexOf(marker)') &&
    gateSource.includes('redactApprovalTokens(normalized)'),
  'readiness gate must remove noisy DOM/menu dumps and approval tokens from blockedSteps.blocker'
)
assert.ok(
  gateSource.includes('function outputTail') && gateSource.includes('redactApprovalTokens(normalized.slice(-6000))'),
  'readiness gate outputTail must redact approval tokens before writing the artifact'
)
assert.ok(
  gateSource.includes('const consoleArtifact =') &&
    gateSource.includes('blockedSteps: artifact.blockedSteps') &&
    gateSource.includes('remediationActions: artifact.remediationActions') &&
    gateSource.includes('artifactPath: artifact.artifactPath') &&
    !gateSource.includes('DCC_INCREMENTAL_BACKUP_RESTORE_READINESS_RESULT ${JSON.stringify(artifact)}'),
  'readiness gate console result must be a compact summary while the artifact keeps full step output'
)
assert.ok(
  gateSource.includes('function buildRemediationActions') &&
    gateSource.includes('actionId') &&
    gateSource.includes('prepare_isolated_test_tenant_package') &&
    gateSource.includes('assign_test_tenant_mdm_product_menu') &&
    gateSource.includes('prepare_test_tenant_dcc_product') &&
    gateSource.includes('TENANT_PACKAGE_E2E_ALLOW_WRITE') &&
    gateSource.includes('MDM_ROLE_E2E_ALLOW_ASSIGN') &&
    gateSource.includes('MDM_PRODUCT_E2E_ALLOW_CREATE') &&
    gateSource.includes('DCC upload size policy readiness') &&
    gateSource.includes('prepare_test_tenant_dcc_upload_size_policy') &&
    gateSource.includes('dcc-upload-size-policy-real-setup.e2e.js') &&
    gateSource.includes('DCC_UPLOAD_POLICY_E2E_ALLOW_WRITE') &&
    gateSource.includes('DCC_UPLOAD_POLICY_E2E_APPROVAL') &&
    gateSource.includes('nextRunCommandTemplate') &&
    gateSource.includes('approvalValuePolicy') &&
    gateSource.includes('omitted-from-readiness-artifact') &&
    gateSource.includes('powershell') &&
    gateSource.includes('DCC_BACKUP_E2E_BASE_URL') &&
    gateSource.includes('explicit user approval') &&
    gateSource.includes('writeScope'),
  'readiness gate artifact must describe blocked-step remediation actions without running writes'
)

for (const envName of [
  'DCC_INCREMENTAL_BACKUP_RESTORE_E2E_ALLOW_REAL_WRITE',
  'DCC_INCREMENTAL_BACKUP_RESTORE_E2E_APPROVAL',
  'TENANT_PACKAGE_E2E_ALLOW_WRITE',
  'TENANT_PACKAGE_E2E_APPROVAL',
  'MDM_ROLE_E2E_ALLOW_ASSIGN',
  'MDM_ROLE_E2E_APPROVAL',
  'MDM_PRODUCT_E2E_ALLOW_CREATE',
  'MDM_PRODUCT_E2E_APPROVAL',
  'DCC_BACKUP_E2E_ALLOW_WRITE',
  'DCC_BACKUP_E2E_APPROVAL',
  'RUNTIME_CONTROL_ALLOW_REAL_TEST_BACKUP_SETUP',
  'RUNTIME_CONTROL_REAL_TEST_BACKUP_APPROVAL',
  'RUNTIME_CONTROL_ALLOW_REAL_RESTORE_DATA',
  'RUNTIME_CONTROL_REAL_RESTORE_DATA_APPROVAL',
  'RUNTIME_CONTROL_ALLOW_REAL_PUBLISH',
  'RUNTIME_CONTROL_REAL_PUBLISH_APPROVAL',
  'RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_BACKUP',
  'RUNTIME_CONTROL_REAL_PROMOTE_BACKUP_APPROVAL',
  'RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_PROD',
  'RUNTIME_CONTROL_REAL_PROMOTE_PROD_APPROVAL',
  'RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_WITH_DATA',
  'DCC_READINESS_REMEDIATION_RUN_ACTION_ID',
  'DCC_READINESS_REMEDIATION_RUN_ALLOW_WRITE'
]) {
  assert.ok(sharedEnvSource.includes(envName), `shared write-control env module must include ${envName}`)
}

for (const forbiddenToken of [
  'ALLOW_TEST_DCC_INCREMENTAL_BACKUP_RESTORE',
  'ALLOW_TEST_TENANT_PACKAGE_WRITE',
  'ALLOW_TEST_MDM_ROLE_MENU_WRITE',
  'ALLOW_TEST_MDM_PRODUCT_WRITE',
  'ALLOW_TEST_DCC_FILE_WRITE'
]) {
  assert.ok(!gateSource.includes(forbiddenToken), `readiness gate must not contain approval token ${forbiddenToken}`)
}

console.log('PASS: DCC incremental backup/restore readiness gate is read-only')
