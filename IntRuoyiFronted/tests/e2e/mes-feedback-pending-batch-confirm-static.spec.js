const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = process.cwd()
const workspaceRoot = path.resolve(frontendRoot, '..')
const pagePath = path.resolve(frontendRoot, 'src/views/mes/pro/feedback/index.vue')
const apiPath = path.resolve(frontendRoot, 'src/api/mes/pro/feedback/index.ts')
const backendReqPath = path.resolve(
  workspaceRoot,
  'ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/importrecord/MesProFeedbackImportConfirmBatchReqVO.java'
)
const backendSummaryPath = path.resolve(
  workspaceRoot,
  'ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/importrecord/MesProFeedbackImportBatchSummaryRespVO.java'
)
const backendServicePath = path.resolve(
  workspaceRoot,
  'ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/MesProFeedbackImportRecordService.java'
)

for (const filePath of [pagePath, apiPath, backendReqPath, backendSummaryPath, backendServicePath]) {
  assert(fs.existsSync(filePath), `待归属批量确认相关文件必须存在：${filePath}`)
}

const pageSource = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')
const backendReqSource = fs.readFileSync(backendReqPath, 'utf8')
const backendSummarySource = fs.readFileSync(backendSummaryPath, 'utf8')
const backendServiceSource = fs.readFileSync(backendServicePath, 'utf8')

for (const fragment of [
  'feedback-import-batch-summary',
  'currentImportBatchSummary',
  '确认报工',
  'handleConfirmBatch',
  'label="报工人"',
  'label="当前审批人"',
  'label="备注"',
  '其他订单',
  '确认报工时将跳过',
  'isImportRecordConfirmable',
  'isImportRecordSkippedExternalOtherOrder'
]) {
  assert(pageSource.includes(fragment), `待归属页必须支持批量确认与其他订单跳过：${fragment}`)
}

for (const forbiddenFragment of ['查看正式报工', 'openFeedbackFromImportRecord']) {
  assert(!pageSource.includes(forbiddenFragment), `待归属页不得继续保留旧入口：${forbiddenFragment}`)
}

for (const apiFragment of [
  'getImportRecordBatchSummary',
  'confirmImportRecordBatch',
  '/mes/pro/feedback/import-record/batch-summary',
  '/mes/pro/feedback/import-record/confirm-batch',
  'ProFeedbackImportConfirmBatchReqVO',
  'ProFeedbackImportBatchSummaryVO'
]) {
  assert(apiSource.includes(apiFragment), `前端 API 必须接入批量确认工作台合同：${apiFragment}`)
}

for (const backendFragment of [
  'class MesProFeedbackImportConfirmBatchReqVO',
  'private List<Long> importRecordIds;',
  'private List<Row> rows;',
  'private Long importRecordId;',
  'private Long feedbackUserId;',
  'private Long approveUserId;',
  'private LocalDateTime feedbackTime;'
]) {
  assert(backendReqSource.includes(backendFragment), `后端批量确认请求模型必须覆盖行内字段：${backendFragment}`)
}

for (const backendFragment of [
  'class MesProFeedbackImportBatchSummaryRespVO',
  'private String sourceFileName;',
  'private Integer totalCount;',
  'private Integer pendingCount;',
  'private Integer attributedCount;',
  'private Integer confirmableCount;',
  'private Integer skippedOtherOrderCount;'
]) {
  assert(backendSummarySource.includes(backendFragment), `后端批次摘要模型必须存在：${backendFragment}`)
}

for (const backendFragment of ['getImportRecordBatchSummary', 'confirmImportRecordBatch']) {
  assert(backendServiceSource.includes(backendFragment), `后端服务接口必须提供批次摘要/批量确认能力：${backendFragment}`)
}

assert(
  /const handleConfirmBatch = async \(\) => \{[\s\S]*message\.alertSuccess\('报工成功'\)[\s\S]*activeTab\.value = 'feedback'/.test(
    pageSource
  ),
  '确认报工成功后必须弹框提示“报工成功”，并切回正式报工 tab。'
)

console.log('PASS: MES feedback pending batch confirm static contract')
