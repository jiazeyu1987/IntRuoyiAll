const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  const filePath = path.join(repoRoot, relativePath)
  assert.ok(fs.existsSync(filePath), `missing required file: ${filePath}`)
  return fs.readFileSync(filePath, 'utf8')
}

const gateSource = readUtf8('scripts/dcc-incremental-backup-restore-real-flow-gate.mjs')
const runtimeControlHelperSource = readUtf8('tests/e2e/runtime-control-ops-e2e-helper.js')

const guardedScripts = [
  {
    label: 'DCC upload',
    path: 'tests/e2e/dcc-upload-test-file.e2e.js',
    allowName: 'DCC_BACKUP_E2E_ALLOW_WRITE',
    approvalName: 'DCC_BACKUP_E2E_APPROVAL',
    approvalToken: 'ALLOW_TEST_DCC_FILE_WRITE'
  },
  {
    label: 'DCC withdraw/delete',
    path: 'tests/e2e/dcc-withdraw-delete-file.e2e.js',
    allowName: 'DCC_BACKUP_E2E_ALLOW_WRITE',
    approvalName: 'DCC_BACKUP_E2E_APPROVAL',
    approvalToken: 'ALLOW_TEST_DCC_FILE_WRITE'
  },
  {
    label: 'runtime-control real test backup',
    path: 'tests/e2e/runtime-control-real-test-backup-setup.e2e.js',
    allowName: 'RUNTIME_CONTROL_ALLOW_REAL_TEST_BACKUP_SETUP',
    approvalName: 'RUNTIME_CONTROL_REAL_TEST_BACKUP_APPROVAL',
    approvalToken: 'ALLOW_TEST_RUNTIME_BACKUP_WRITE'
  },
  {
    label: 'runtime-control real restore-data',
    path: 'tests/e2e/runtime-control-real-restore-data.e2e.js',
    allowName: 'RUNTIME_CONTROL_ALLOW_REAL_RESTORE_DATA',
    approvalName: 'RUNTIME_CONTROL_REAL_RESTORE_DATA_APPROVAL',
    approvalToken: 'ALLOW_TEST_RUNTIME_RESTORE_WRITE'
  }
]

for (const item of guardedScripts) {
  const source = readUtf8(item.path)
  assert.ok(source.includes(item.allowName), `${item.label} must keep an explicit allow-write switch`)
  assert.ok(source.includes(item.approvalName), `${item.label} must require an explicit approval token`)
  assert.ok(source.includes(item.approvalToken), `${item.label} must check the expected approval token`)
  assert.ok(
    source.includes('throw new Error') && source.includes('explicit user approval'),
    `${item.label} must fail fast before writes when approval is missing`
  )
}

const realBackupSource = readUtf8('tests/e2e/runtime-control-real-test-backup-setup.e2e.js')
const realRestoreSource = readUtf8('tests/e2e/runtime-control-real-restore-data.e2e.js')
const realRehearsalSource = readUtf8('tests/e2e/runtime-control-real-rehearsal.e2e.js')
assert.ok(
  realBackupSource.includes('172.30.30.57') &&
    realBackupSource.includes('RUNTIME_CONTROL_E2E_BASE_URL') &&
    realBackupSource.includes('RUNTIME_CONTROL_E2E_ACTION_ORIGIN'),
  'runtime-control real test backup must reject protected production base/action origins before browser writes'
)
assert.ok(
  realBackupSource.includes('20260609-release-and-backup-implementation/artifacts') &&
    !realBackupSource.includes('20260530-runtime-control-nas-assets'),
  'runtime-control real test backup artifacts must default to the current release-and-backup task artifact directory'
)
assert.ok(
  runtimeControlHelperSource.includes('assertRuntimeControlTestAccountBoundary') &&
    runtimeControlHelperSource.includes('RUNTIME_CONTROL_E2E_TENANT') &&
    runtimeControlHelperSource.includes('RUNTIME_CONTROL_E2E_USERNAME') &&
    runtimeControlHelperSource.includes('测试租户') &&
    runtimeControlHelperSource.includes('aoteman'),
  'runtime-control helper must expose an explicit test-tenant account boundary assertion'
)
for (const [label, source] of [
  ['runtime-control real test backup', realBackupSource],
  ['runtime-control real restore-data', realRestoreSource]
]) {
  assert.ok(
    source.includes('assertRuntimeControlTestAccountBoundary') &&
      source.includes('requireExplicitApproval'),
    `${label} must assert the test tenant account boundary before browser writes`
  )
}

for (const [label, source] of [
  ['runtime-control real restore-data', realRestoreSource],
  ['runtime-control real rehearsal', realRehearsalSource]
]) {
  assert.ok(
    source.includes('readOperationLogPayload') &&
      source.includes("content-type") &&
      source.includes("application/json"),
    `${label} must ignore non-JSON operation log responses during service restarts`
  )
}

for (const fragment of [
  'DCC_BACKUP_E2E_ALLOW_WRITE',
  'DCC_BACKUP_E2E_APPROVAL',
  'ALLOW_TEST_DCC_FILE_WRITE',
  'RUNTIME_CONTROL_REAL_TEST_BACKUP_APPROVAL',
  'ALLOW_TEST_RUNTIME_BACKUP_WRITE',
  'RUNTIME_CONTROL_REAL_RESTORE_DATA_APPROVAL',
  'ALLOW_TEST_RUNTIME_RESTORE_WRITE'
]) {
  assert.ok(gateSource.includes(fragment), `real flow gate must forward write approval token ${fragment}`)
}

console.log('PASS: DCC real operation write guards require explicit approval tokens')
