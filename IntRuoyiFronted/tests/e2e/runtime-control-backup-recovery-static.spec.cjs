const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const root = path.resolve(__dirname, '../..')
const page = fs.readFileSync(path.join(root, 'src/views/infra/runtime-control/index.vue'), 'utf8')
const api = fs.readFileSync(path.join(root, 'src/api/infra/runtimeControl/index.ts'), 'utf8')

const buttons = [...page.matchAll(/<el-button\b[\s\S]*?<\/el-button>/g)].map(m => m[0])
const inspect = buttons.find(b => b.includes('@click="inspectBackupRecovery(row)"'))
assert.ok(inspect, 'recovery-required workflows need a real inspection entry')
for (const token of ["row.state === 'RECOVERY_REQUIRED'", "row.targetEnvironment === 'backup'", 'canPublishBackup', 'workflowBusy', '检查恢复']) {
  assert.ok(inspect.includes(token), `missing inspection guard: ${token}`)
}
const confirm = buttons.find(b => b.includes('@click="confirmBackupRecovery"'))
assert.ok(confirm, 'missing controlled recovery confirmation')
for (const token of ['backupRecoveryPreview?.eligible', "backupRecoveryDialog.confirmText !== 'PROD'", '!canPublishBackup', 'workflowBusy']) {
  assert.ok(confirm.includes(token), `missing confirmation guard: ${token}`)
}
for (const token of ['backupRecoveryPreview.blockers', 'backupRecoveryPreview.nextAction', '@closed="clearBackupRecoveryPreview"']) {
  assert.ok(page.includes(token), `missing recovery feedback: ${token}`)
}
const handlerStart = page.indexOf('const confirmBackupRecovery =')
const handlerEnd = page.indexOf('\nconst ', handlerStart + 1)
const handler = page.slice(handlerStart, handlerEnd < 0 ? undefined : handlerEnd)
for (const token of ['workflowBusy.value', "backupRecoveryDialog.confirmText !== 'PROD'", 'preview?.eligible', '!canPublishBackup.value', 'preview.workflowId', 'preview.previewId', 'preview.expectedStateVersion', 'reportActionError(error)', 'loadReleaseWorkflows()']) {
  assert.ok(handler.includes(token), `missing recovery execution contract: ${token}`)
}
assert.match(api, /backup-recovery\/inspect/)
assert.match(api, /backup-recovery\/recover/)
for (const name of ['inspectRuntimeControlBackupRecovery', 'recoverRuntimeControlBackup']) {
  const start = api.indexOf(`export const ${name} =`)
  const end = api.indexOf('\nexport const ', start + 1)
  assert.match(api.slice(start, end < 0 ? undefined : end), /timeout: 180_000/, `${name} must cover the bounded server operation`)
}
assert.match(api, /encodeURIComponent\(workflowId\)/)
assert.doesNotMatch(handler, /catch\s*\{\s*\}/)
console.log('backup recovery entry static contract: PASS')
