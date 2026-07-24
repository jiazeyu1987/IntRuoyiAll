const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const packageJsonPath = path.join(repoRoot, 'package.json')
const preflightPath = path.join(repoRoot, 'scripts/dcc-incremental-backup-restore-preflight-check.mjs')

function readUtf8(filePath) {
  assert.ok(fs.existsSync(filePath), `missing required file: ${filePath}`)
  return fs.readFileSync(filePath, 'utf8')
}

const packageJson = JSON.parse(readUtf8(packageJsonPath))
assert.equal(
  packageJson.scripts?.['e2e:dcc:incremental-backup-restore:preflight'],
  'node scripts/dcc-incremental-backup-restore-preflight-check.mjs',
  'package.json must expose the DCC incremental backup/restore read-only preflight'
)
assert.equal(
  packageJson.scripts?.['e2e:dcc:incremental-backup-restore:preflight:check'],
  'node tests/e2e/dcc-incremental-backup-restore-preflight-static.spec.js',
  'package.json must expose the DCC incremental backup/restore preflight static check'
)

const source = readUtf8(preflightPath)
const sharedEnvSource = readUtf8(path.join(repoRoot, 'scripts/dcc-write-control-env.mjs'))
for (const fragment of [
  'DCC_INCREMENTAL_BACKUP_RESTORE_PREFLIGHT_RESULT',
  'doesNotExecuteRealWrite',
  'sanitizeWriteEnv',
  'redactApprovalTokens',
  "from './dcc-write-control-env.mjs'",
  'runExpectedNoApprovalFailFast',
  'dcc-readiness-remediation-plan-check.mjs',
  'dcc-readiness-remediation-plan-check.spec.js',
  'dcc-readiness-remediation-runner-static.spec.js',
  'dcc-readiness-remediation-runner.spec.js',
  'dcc-write-control-env-static.spec.js',
  'dcc-incremental-backup-restore-readiness-gate-static.spec.js',
  'dcc-incremental-backup-restore-real-flow-gate-static.spec.js',
  'dcc-real-operation-write-guards-static.spec.js',
  'dcc-restore-verify-static.spec.js',
  'mdm-real-data-prerequisite-guards-static.spec.js',
  'DCC_INCREMENTAL_BACKUP_RESTORE_E2E_ALLOW_REAL_WRITE',
  'DCC upload'
]) {
  assert.ok(source.includes(fragment), `preflight check must include ${fragment}`)
}

for (const envName of [
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
  'ALLOW_TEST_DCC_FILE_WRITE',
  'ALLOW_TEST_RUNTIME_BACKUP_WRITE',
  'ALLOW_TEST_RUNTIME_RESTORE_WRITE'
]) {
  assert.ok(!source.includes(forbiddenToken), `preflight check must not contain approval token ${forbiddenToken}`)
}

assert.match(
  source,
  /import \{[\s\S]*redactApprovalTokens[\s\S]*stripWriteControlEnv[\s\S]*\} from '\.\/dcc-write-control-env\.mjs'/,
  'preflight check must use the shared write-control redactor and env sanitizer'
)
assert.match(
  source,
  /function runNodeStep\(step\) \{[\s\S]*throw new Error\([\s\S]*redactApprovalTokens\([\s\S]*result\.error\?\.message[\s\S]*result\.stdout[\s\S]*result\.stderr[\s\S]*\)/,
  'preflight check must redact child failure stdout/stderr before throwing'
)

console.log('PASS: DCC incremental backup/restore preflight static contract is present')
