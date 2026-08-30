const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const nasPage = read('src/views/system/nas/index.vue')
const nasApi = read('src/api/system/nas/index.ts')
const workflowApi = read('src/api/dcc/controlledFile/workflow.ts')
const packageJson = JSON.parse(read('package.json'))

assert.equal(
  packageJson.scripts['e2e:dcc:nas-original-path-sync:static'],
  'node tests/e2e/dcc-nas-original-path-sync-static.spec.js',
  'package.json must expose the NAS original-path sync static contract.'
)

for (const requiredField of [
  'originalPathSyncStatus',
  'originalPathSyncFileId',
  'originalPathSyncTaskId',
  'originalPathSyncTaskItemId',
  'originalPathSyncErrorCode',
  'originalPathSyncError'
]) {
  assert.match(nasApi, new RegExp(requiredField), `NAS audit file API must expose ${requiredField}.`)
}

assert.match(
  nasApi,
  /DccNasOriginalPathSyncReqVO[\s\S]*selectionScope[\s\S]*idempotencyKey[\s\S]*selectedFiles/,
  'NAS API must define a focused original-path sync request without local folder fields.'
)
assert.match(
  nasApi,
  /syncNasOriginalPathFiles[\s\S]*url: `\/dcc\/controlled-files\/nas-control-audit\/\$\{taskId\}\/original-path-sync`/,
  'NAS API must POST original-path sync requests from the audit task.'
)
assert.match(
  nasApi,
  /deleteNasOriginalPathSyncFile[\s\S]*url: `\/dcc\/controlled-files\/nas-control-audit\/original-path-sync\/\$\{syncFileId\}`/,
  'NAS API must expose removal of an active original-path sync record.'
)
assert.match(
  workflowApi,
  /ControlledFileNasTransferSourceType\s*=\s*'NAS'\s*\|\s*'LOCAL_FOLDER'\s*\|\s*'NAS_UNCONTROLLED_IMPORT'\s*\|\s*'NAS_ORIGINAL_PATH_SYNC'/,
  'transfer response type must include NAS_ORIGINAL_PATH_SYNC.'
)

for (const label of [
  '同步 1 个验证',
  '同步选中文件到系统',
  '同步全部未同步文件',
  '原路径同步',
  '移除同步记录'
]) {
  assert.match(nasPage, new RegExp(label), `NAS audit dialog must include action/column label: ${label}`)
}

for (const scope of [
  "selectionScope: 'FIRST_UNSYNCED'",
  "selectionScope: 'EXPLICIT_SELECTED_FILES'",
  "selectionScope: 'ALL_UNSYNCED'"
]) {
  assert.match(nasPage, new RegExp(scope), `page must send sync scope ${scope}`)
}

assert.match(
  nasPage,
  /isNasOriginalPathSyncSelectable[\s\S]*PENDING_RECOGNITION[\s\S]*sourceSignature[\s\S]*originalPathSyncStatus/,
  'original-path sync selection must allow pending-recognition files and guard active/running sync rows.'
)
assert.match(
  nasPage,
  /syncNasOriginalPathFiles[\s\S]*getNasTransferTaskState[\s\S]*loadNasControlAuditFilePage/,
  'after creating original-path sync, page must poll the backend task and refresh the audit file list.'
)
assert.match(
  nasPage,
  /deleteNasOriginalPathSyncFile[\s\S]*loadNasControlAuditFilePage/,
  'removing a sync record must refresh the audit file list.'
)
const syncAllConfirmBlock = nasPage.slice(
  nasPage.indexOf('const confirmNasOriginalPathSyncAll'),
  nasPage.indexOf('const handleSyncNasOriginalPathAll')
)
assert.notEqual(syncAllConfirmBlock, '', 'original-path sync all confirmation must exist.')
assert.match(
  syncAllConfirmBlock,
  /modalClass:\s*NAS_TRANSFER_CONFIRM_MODAL_CLASS/,
  'sync-all confirmation must render above the statistics dialog.'
)
const deleteOriginalPathSyncBlock = nasPage.slice(
  nasPage.indexOf('const handleDeleteNasOriginalPathSyncFile'),
  nasPage.indexOf('const handleNasControlAuditFilePageSizeChange')
)
assert.notEqual(deleteOriginalPathSyncBlock, '', 'original-path sync delete handler must exist.')
assert.match(
  deleteOriginalPathSyncBlock,
  /modalClass:\s*NAS_TRANSFER_CONFIRM_MODAL_CLASS/,
  'delete confirmation must render above the statistics dialog.'
)
assert.match(
  nasPage,
  /:global\(\.nas-transfer-confirm-message-box-overlay\)\s*\{[\s\S]*z-index:\s*4000\s*!important/,
  'NAS original-path confirmations must use a high-priority overlay above nested dialogs.'
)
assert.match(
  nasPage,
  /el-pagination[\s\S]*v-model:current-page="controlAuditFiles\.pageNo"[\s\S]*v-model:page-size="controlAuditFiles\.pageSize"[\s\S]*:total="controlAuditFiles\.total"/,
  'audit file dialog must paginate beyond the visible table rows.'
)
const originalPathSyncHandlerBlock = nasPage.slice(
  nasPage.indexOf('const handleSyncNasOriginalPathFiles'),
  nasPage.indexOf('const createNasUncontrolledImportIdempotencyKey')
)
assert.notEqual(originalPathSyncHandlerBlock, '', 'original-path sync handlers must exist.')
assert.doesNotMatch(
  originalPathSyncHandlerBlock,
  /showDirectoryPicker/,
  'original-path sync must be server-side and must not require browser local directory selection.'
)
