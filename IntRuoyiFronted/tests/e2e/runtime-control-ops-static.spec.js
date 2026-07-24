const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function assertContains(source, expected, label) {
  if (!source.includes(expected)) {
    throw new Error(`missing ${label}: ${expected}`)
  }
}

function assertNotContains(source, forbidden, label) {
  if (source.includes(forbidden)) {
    throw new Error(`forbidden ${label}: ${forbidden}`)
  }
}

const api = readUtf8('src/api/infra/runtimeControl/index.ts')
const page = readUtf8('src/views/infra/runtime-control/index.vue')
const incidentDrawer = readUtf8('src/views/infra/runtime-control/components/OpsIncidentDrawer.vue')
const candidatePicker = readUtf8('src/views/infra/runtime-control/components/OpsCandidatePicker.vue')

assertContains(api, '/infra/runtime-control/actions', 'operation action API')
assertContains(api, '/infra/runtime-control/operations/', 'operation log API base')
assertContains(api, 'executeRuntimeControlAction', 'action API function')
assertContains(api, 'getRuntimeControlOperationLog', 'operation log API function')
assertContains(api, 'RuntimeControlActionReqVO', 'action request type')
assertContains(api, 'RuntimeControlLogVO', 'log response type')
assertContains(api, 'publishScope?:', 'publish scope request type')
assertContains(api, 'targetEnvironment?:', 'backup target environment request type')
assertContains(api, 'testConclusion?: string', 'mark tested conclusion request type')
assertContains(api, 'checksumPresent?: boolean', 'release package checksum metadata type')
assertContains(api, 'tested?: boolean', 'release package tested metadata type')
assertContains(api, 'prodHistoryPath?: string', 'rollback prod history metadata type')

assertContains(page, '部署发布包到测试服', 'publish test button')
assertContains(page, '上线已验证发布包', 'promote production button')
assertContains(page, '上线备份服务器', 'promote backup button')
assertContains(page, 'Backup/ReleasePackage', 'release package NAS root')
assertContains(page, 'Backup/BackupPackage', 'backup package NAS root')
assertContains(page, 'operationTargetDirectoryText', 'operation target directory helper')
assertContains(page, 'operationExpectedResultText', 'operation expected result helper')
assertContains(page, 'operationRequiredOwnerText', 'operation owner gate helper')
assertContains(page, '只回滚应用版本', 'rollback application only boundary')
assertContains(page, '恢复同一恢复集的 MySQL / MinIO / 文件对象', 'restore data boundary')
assertContains(incidentDrawer, '不执行发布、备份、回滚或恢复命令', 'incident closure non-execution boundary')
assertContains(page, '发布范围', 'publish scope form item')
assertContains(page, '只发代码', 'code-only publish scope option')
assertContains(page, '带数据发布', 'with-data publish scope option')
assertContains(page, '覆盖目标环境数据库和文件对象', 'with-data risk hint')
assertContains(page, 'publishScope', 'publish scope dialog state')
assertContains(page, 'operationSupportsPublishScope', 'publish scope action guard')
assertContains(page, "publishScope: 'code-only'", 'default code-only publish scope')
assertContains(page, '立即备份', 'backup now button')
assertContains(page, '恢复演练', 'restore rehearsal button')
assertContains(page, '回滚版本', 'rollback button')
assertContains(page, '恢复数据', 'restore button')
assertContains(page, '查看日志', 'view log button')
assertContains(page, 'infra:runtime-control:operate', 'operate permission')
assertContains(page, 'operationDialog', 'operation confirmation dialog state')
assertContains(page, 'operationDialog.targetEnvironment', 'backup target environment dialog state')
assertContains(page, '备份环境', 'backup target environment selector label')
assertContains(page, '验证结论', 'mark tested conclusion field')
assertContains(page, 'logDialog', 'log dialog state')
assertContains(page, 'selectedImageCandidateId', 'rollback selected image candidate ID')
assertContains(page, 'selectedRecoverySetCandidateId', 'restore selected recovery-set candidate ID')
assertContains(page, "['rehearsal', 'restore-data', 'mark-release-tested'].includes(operationDialog.action)", 'rehearsal recovery-set candidate binding')
assertContains(page, 'OpsCandidatePicker', 'server candidate picker component')
assertNotContains(api, 'selectedImageTag?:', 'free text rollback image request type')
assertNotContains(api, 'selectedBackupId?:', 'free text restore backup request type')
assertNotContains(page, 'selectedImageTag', 'free text rollback image state')
assertNotContains(page, 'selectedBackupId', 'free text restore backup state')
assertNotContains(page, '<el-form-item label="备份点"', 'manual restore backup form label')
assertContains(page, 'executeRuntimeControlAction', 'action submit call')
assertContains(page, 'getRuntimeControlOperationLog', 'log fetch call')
assertContains(page, 'operationRequestedAtText', 'operation requested time formatter')
assertContains(page, 'operationPublishScopeText', 'operation publish scope formatter')
assertContains(page, 'formatDate', 'shared date formatter')
assertContains(page, 'PROD', 'production confirmation literal')
assertContains(page, "['promote-prod', 'promote-backup'].includes(action)", 'promotion production-grade guard')
assertContains(page, "['backup-now', 'rollback-app', 'restore-data'].includes(action)", 'target-scoped production-grade guard')
assertContains(
  page,
  'operationEnvironmentRequiresProdConfirm(operationDialog.targetEnvironment)',
  'target environment production confirmation guard'
)
assertContains(page, "if (action === 'promote-backup') return 'backup'", 'promote backup environment helper')
assertContains(page, 'targetEnvironment:', 'backup target environment submit payload')
assertContains(page, 'testConclusion:', 'mark tested conclusion submit payload')
assertContains(candidatePicker, 'checksum：', 'restore checksum candidate detail')
assertContains(candidatePicker, '正式服发布历史：', 'rollback production history candidate detail')
assertContains(candidatePicker, '演练报告：', 'restore rehearsal report candidate detail')
assertContains(candidatePicker, '现场快照：', 'restore snapshot candidate detail')

console.log('PASS: runtime control operation buttons and log dialog contracts are wired')
