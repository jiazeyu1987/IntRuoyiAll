const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr-batch',
  'BatchExecutionDetailPage.vue'
)

const detail = fs.readFileSync(detailPath, 'utf8')
const apiPath = path.join(repoRoot, 'src', 'api', 'mes', 'pro', 'edhr', 'batchExecution.ts')
const api = fs.readFileSync(apiPath, 'utf8')
const compact = detail.replace(/\s+/g, ' ')
const compactApi = api.replace(/\s+/g, ' ')

const assertIncludes = (needle, message) => assert(detail.includes(needle), message)
const assertNotIncludes = (needle, message) => assert(!detail.includes(needle), message)
const assertApiIncludes = (needle, message) => assert(api.includes(needle), message)

for (const marker of [
  'edhr-batch-detail__special-node-attachments',
  'selectedSpecialNodePendingAttachments',
  'selectedSpecialNodePersistedAttachments',
  'removeSelectedSpecialNodePendingAttachment',
  'syncSpecialNodePendingAttachmentsFromDetail',
  'previewSpecialNodeAttachment',
  '当前节点附件',
  '待提交附件',
  '已入账附件'
]) {
  assertIncludes(marker, `特殊节点中间附件面板缺少契约标记：${marker}`)
}

for (const marker of [
  'edhr-batch-detail__special-node-action-grid',
  'canUploadSpecialNodeAttachment',
  'triggerSelectedSpecialNodeUpload',
  'uploadSelectedSpecialNodeAttachment',
  'handleSkipSpecialNode(selectedTaskForEvidence)',
  'handleCompleteSpecialNode(selectedTaskForEvidence)',
  '上传文件',
  '跳过节点',
  '完成节点'
]) {
  assertIncludes(marker, `右侧特殊节点三按钮缺少契约标记：${marker}`)
}

assertIncludes(
  ':disabled="!canUploadSpecialNodeAttachment(selectedTaskForEvidence) || specialNodeAttachmentUploading"',
  '隐藏 el-upload 必须使用独立上传门禁，不能复用跳过/完成门禁导致已完成节点无法选择文件。'
)

assertIncludes(
  ':disabled="!canUploadSpecialNodeAttachment(selectedTaskForEvidence)"',
  '上传文件按钮必须使用独立上传门禁，放行前已填写完成的特殊节点仍应可上传。'
)

const uploadPermissionMatch = detail.match(
  /const canUploadSpecialNodeAttachment = \(row: EdhrBatchExecutionTaskRespVO\) =>([\s\S]*?)const canOperateSpecialNode/
)
assert(uploadPermissionMatch, '必须拆分 canUploadSpecialNodeAttachment，避免上传权限被完成/跳过权限误绑定。')
const uploadPermissionBody = uploadPermissionMatch[1]
assert(
  !uploadPermissionBody.includes("hasAllowedTaskAction(row, 'CLOSE')"),
  '上传文件不应要求 CLOSE 动作；放行前已填写完成节点没有 CLOSE 动作时也应可上传。'
)
assert(
  !uploadPermissionBody.includes('batchActionLocked.value'),
  '上传文件不应复用批次动作锁；已关闭但未放行的批次仍应允许补充特殊节点附件。'
)
assert(
  !uploadPermissionBody.includes('EDHR_BATCH_STATUS_CLOSED'),
  '上传文件不应排除已关闭批次；已关闭只是进入放行前阶段，未放行/未归档前仍应可上传。'
)
assert(
  uploadPermissionBody.includes("releaseStatus.value !== 'RELEASED'"),
  '上传文件必须以最终放行状态作为截止点，放行后不得继续上传特殊节点附件。'
)
assert(
  !uploadPermissionBody.includes('EDHR_BATCH_TASK_STATUS_APPROVED') &&
    !uploadPermissionBody.includes('EDHR_BATCH_TASK_STATUS_SKIPPED'),
  '上传文件不应排除已完成/已跳过特殊节点，放行前仍允许补充待提交附件。'
)

assert(
  /const triggerSelectedSpecialNodeUpload = \(\) =>[\s\S]*?canUploadSpecialNodeAttachment\(task\)/.test(
    detail
  ),
  '手动触发上传时必须检查独立上传门禁。'
)

assert(
  /const uploadSelectedSpecialNodeAttachment = async \(options: UploadRequestOptions\) =>[\s\S]*?canUploadSpecialNodeAttachment\(task\)/.test(
    detail
  ),
  '真正执行 prepare-upload 前必须检查独立上传门禁。'
)

assert(
  /const handleSkipSpecialNode = async \(row: EdhrBatchExecutionTaskRespVO\) =>[\s\S]*?!canOperateSpecialNode\(row\)/.test(
    detail
  ) &&
    /const handleCompleteSpecialNode = async \(row: EdhrBatchExecutionTaskRespVO\) =>[\s\S]*?!canOperateSpecialNode\(row\)/.test(
      detail
    ),
  '跳过节点和完成节点仍必须保留原 canOperateSpecialNode 门禁。'
)

assert(
  /const normalizeSpecialNodeAttachmentFileName = \(fileName\?: string \| null\) =>[\s\S]*?toLocaleLowerCase\(\)/.test(detail),
  '同名覆盖必须通过规范化文件名比较，避免大小写和首尾空格导致重复。'
)

assert(
  /const upsertSpecialNodePendingAttachment = \([\s\S]*?normalizeSpecialNodeAttachmentFileName\(attachment\.fileName\)[\s\S]*?filter\([\s\S]*?normalizeSpecialNodeAttachmentFileName\(candidate\.fileName\) !== normalizedFileName[\s\S]*?\[taskId\] = \[/.test(
    detail
  ),
  '待提交附件必须按同名覆盖写入，不能同名追加多条。'
)

assert(
  detail.includes('pendingSpecialNodeAttachments') &&
    /syncSpecialNodePendingAttachmentsFromDetail[\s\S]*?task\.pendingSpecialNodeAttachments/.test(detail),
  '重新打开详情时必须从后端 pendingSpecialNodeAttachments 恢复待提交附件列表。'
)

assert(
  detail.includes('deleteEdhrBatchSpecialNodePendingAttachment') &&
    /removeSelectedSpecialNodePendingAttachment[\s\S]*?deleteEdhrBatchSpecialNodePendingAttachment\(\{ taskId, attachment \}\)/.test(
      detail
    ),
  '删除待提交附件必须调用后端删除接口，不能只删前端内存。'
)

assert(
  compact.includes('attachments: buildSpecialNodeSubmitAttachments(taskId)'),
  '跳过和完成节点提交 payload 必须复用同一份待提交附件列表。'
)

assert(
  compact.includes('clearSpecialNodePendingAttachments(taskId)') &&
    compact.includes('await loadDetail()'),
  '跳过或完成成功刷新详情后必须清空该节点待提交附件。'
)

assert(
  /const parseSpecialNodePayload = \(row: EdhrBatchExecutionTaskRespVO\)[\s\S]*?JSON\.parse\(row\.specialPayloadJson\)/.test(
    detail
  ) &&
    /const selectedSpecialNodePersistedAttachments = computed[\s\S]*?payload\.attachments/.test(detail),
  '已入账附件必须从 specialPayloadJson 解析并在中间区域只读展示。'
)

assert(
  /const previewSpecialNodeAttachment = \(attachment:[\s\S]*?attachment\.fileId[\s\S]*?buildEdhrSpecialNodeAttachmentPreviewSource/.test(
    detail
  ) &&
    !/window\.open\(attachment\.fileUrl, '_blank'/.test(detail),
  '附件预览必须通过 fileId 走统一在线预览能力，不能继续裸开后端 fileUrl。'
)

assertApiIncludes(
  'savePendingEdhrBatchSpecialNodeAttachments',
  '放行前必须接入批量保存待提交特殊节点附件 API，不能只提示不保存。'
)

assert(
  compactApi.includes(
    'url: `${BATCH_EXECUTION_BASE_URL}/task/special-node/attachment/save-pending`'
  ),
  '批量保存待提交特殊节点附件必须复用 eDHR 特殊节点附件命名空间。'
)

assert(
  /const pendingSpecialNodeAttachmentCount = computed\(\(\) =>[\s\S]*?specialNodePendingAttachments[\s\S]*?reduce/.test(
    detail
  ),
  '页面必须按全批次 specialNodePendingAttachments 统计待保存附件数量。'
)

assert(
  /const ensurePendingSpecialNodeAttachmentsSavedBeforeRelease = async \(\) =>[\s\S]*?pendingSpecialNodeAttachmentCount[\s\S]*?ElMessageBox\.confirm[\s\S]*?savePendingEdhrBatchSpecialNodeAttachments[\s\S]*?await loadDetail\(\)[\s\S]*?return true/.test(
    detail
  ),
  '放行前保存门禁必须弹出确认，确认后调用保存 API 并刷新详情。'
)

assert(
  /const ensurePendingSpecialNodeAttachmentsSavedBeforeRelease = async \(\) =>[\s\S]*?return false/.test(
    detail
  ),
  '用户取消保存或保存失败时，放行门禁必须返回 false 并中止原放行动作。'
)

assert(
  /const handleReleasePrecheck = async \(\) =>[\s\S]*?await ensurePendingSpecialNodeAttachmentsSavedBeforeRelease\(\)[\s\S]*?await precheckEdhrRelease/.test(
    detail
  ),
  '执行放行预检前必须先检查并保存待提交特殊节点附件。'
)

assert(
  /const openReleaseTransactionDialog = async \(mode: ReleaseTransactionMode\) =>[\s\S]*?if \(mode === 'submit'\)[\s\S]*?await ensurePendingSpecialNodeAttachmentsSavedBeforeRelease\(\)/.test(
    detail
  ),
  '提交放行弹窗打开前必须先检查并保存待提交特殊节点附件。'
)

for (const obsolete of [
  'specialNodeSkipFileList',
  'specialNodeCompleteFileList',
  'uploadSpecialNodeSkipAttachment',
  'uploadSpecialNodeCompleteAttachment',
  'removeSpecialNodeSkipAttachment',
  'removeSpecialNodeCompleteAttachment'
]) {
  assertNotIncludes(obsolete, `特殊节点附件不得再使用弹窗私有上传状态：${obsolete}`)
}

console.log('PASS: eDHR special node attachment actions static contract')
