const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { pathToFileURL } = require('node:url')

const repoRoot = path.resolve(__dirname, '../..')
const packageJsonPath = path.join(repoRoot, 'package.json')
const sharedPath = path.join(repoRoot, 'scripts/dcc-write-control-env.mjs')
const consumerPaths = [
  'scripts/dcc-incremental-backup-restore-readiness-gate.mjs',
  'scripts/dcc-readiness-remediation-runner.mjs',
  'scripts/dcc-incremental-backup-restore-real-flow-gate.mjs',
  'scripts/dcc-incremental-backup-restore-preflight-check.mjs'
]

function readUtf8(filePath) {
  assert.ok(fs.existsSync(filePath), `missing required file: ${filePath}`)
  return fs.readFileSync(filePath, 'utf8')
}

const packageJson = JSON.parse(readUtf8(packageJsonPath))
assert.equal(
  packageJson.scripts?.['e2e:dcc:write-control-env:check'],
  'node tests/e2e/dcc-write-control-env-static.spec.js',
  'package.json must expose the shared write-control env inventory check'
)

const sharedSource = readUtf8(sharedPath)
for (const fragment of [
  'export const writeControlEnvNames',
  'Object.freeze',
  'stripWriteControlEnv',
  'findPresentWriteControlEnvNames',
  'redactApprovalTokens'
]) {
  assert.ok(sharedSource.includes(fragment), `shared write-control env module must include ${fragment}`)
}

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
  'RUNTIME_CONTROL_ALLOW_REAL_DR',
  'RUNTIME_CONTROL_ALLOW_REAL_RELEASE_BACKUP_SETUP',
  'DCC_READINESS_REMEDIATION_RUN_ACTION_ID',
  'DCC_READINESS_REMEDIATION_RUN_ALLOW_WRITE'
]) {
  assert.ok(sharedSource.includes(envName), `shared write-control env module must include ${envName}`)
}

for (const relativePath of consumerPaths) {
  const source = readUtf8(path.join(repoRoot, relativePath))
  assert.ok(
    source.includes("from './dcc-write-control-env.mjs'"),
    `${relativePath} must consume the shared write-control env module`
  )
  assert.ok(
    !/const (writeModeEnvNames|writeControlEnvNames|blockedWriteEnvNames) = \[[\s\S]*?\n\]/.test(source),
    `${relativePath} must not redeclare the write-control env inventory`
  )
}

assert.ok(!/ALLOW_TEST_[A-Z0-9_]+/.test(sharedSource), 'shared write-control env module must not contain approval tokens')

;(async () => {
  const sharedModule = await import(pathToFileURL(sharedPath).href)
  assert.equal(
    sharedModule.redactApprovalTokens('test ALLOW_TEST_EXAMPLE prod ALLOW_PROD_EXAMPLE done'),
    'test <redacted-approval-token> prod <redacted-approval-token> done',
    'shared redactor must redact test and production approval token values'
  )
  console.log('PASS: shared DCC write-control env inventory is used by all gates')
})().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
