const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const moduleRoot = path.resolve(__dirname, '../../..')
const workspaceRoot = path.resolve(moduleRoot, '../..')
const readModule = (relativePath) => fs.readFileSync(path.resolve(moduleRoot, relativePath), 'utf8')
const readWorkspace = (relativePath) => fs.readFileSync(path.resolve(workspaceRoot, relativePath), 'utf8')

const batchExecutionService = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java'
)
const localStateSampleService = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrLocalStateSampleServiceImpl.java'
)
const workTaskService = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskServiceImpl.java'
)
const releaseService = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java'
)
const frontendPresentation = readWorkspace(
  'IntRuoyiFronted/src/views/mes/pro/edhr/shared/releaseCheckPresentation.ts'
)
const frontendReleaseApi = readWorkspace('IntRuoyiFronted/src/api/mes/pro/edhr/release.ts')
const frontendBatchExecutionApi = readWorkspace('IntRuoyiFronted/src/api/mes/pro/edhr/batchExecution.ts')
const frontendBatchExecutionDetail = readWorkspace(
  'IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'
)
const frontendOperationAuditPane = readWorkspace(
  'IntRuoyiFronted/src/views/mes/pro/edhr/components/OperationAuditListPane.vue'
)

for (const operationType of [
  'LOCAL_STATE_SAMPLE_CREATE',
  'ATTACHMENT_PREPARE_UPLOAD',
  'ATTACHMENT_PENDING_DELETE',
  'ATTACHMENT_SAVE_PENDING'
]) {
  assert(
    (batchExecutionService + localStateSampleService).includes(operationType),
    `${operationType} must be recorded as an eDHR operation audit event.`
  )
}

for (const operationType of [
  'WORK_TASK_RULE_SAVE',
  'CANDIDATE_SIGNATURE_COMPLETE',
  'FILL_TASK_REASSIGN'
]) {
  assert(
    workTaskService.includes(operationType),
    `${operationType} must be recorded by work task service.`
  )
}

assert(
  releaseService.includes('EVENT_TYPE_PRECHECK') &&
    releaseService.includes('recordPrecheckTransactionEvent') &&
    releaseService.includes('recordPrecheckOperationAudit'),
  'Release precheck must write both release transaction event and operation audit evidence.'
)

for (const [sourceName, sourceText] of [
  ['local state sample audit', localStateSampleService],
  ['attachment audit', batchExecutionService],
  ['work task audit', workTaskService],
  ['release precheck audit', releaseService]
]) {
  assert(sourceText.includes('requestSource'), `${sourceName} must persist request source metadata.`)
  assert(sourceText.includes('idempotencyKey'), `${sourceName} must persist idempotency key metadata.`)
  assert(sourceText.includes('associatedSignatureId'), `${sourceName} must persist signature binding metadata.`)
  assert(sourceText.includes('permissionDecision'), `${sourceName} must persist permission decision metadata.`)
  assert(sourceText.includes('resultStatus'), `${sourceName} must persist result status metadata.`)
}

for (const label of [
  '本地状态样本创建',
  '附件上传预登记',
  '待提交附件删除',
  '待提交附件保存',
  '工作任务规则保存',
  '候选签名完成',
  '填写任务重新派发'
]) {
  assert(frontendPresentation.includes(label), `Operation audit label missing: ${label}`)
}

assert(
  frontendReleaseApi.includes("'PRECHECK'") && frontendPresentation.includes('PRECHECK: \'预检\''),
  'Frontend release event types and labels must expose PRECHECK in trace drawer.'
)

assert(
  frontendBatchExecutionApi.includes('reason: string') &&
    frontendBatchExecutionDetail.includes('ElMessageBox.prompt') &&
    frontendBatchExecutionDetail.includes('reason: \'放行前保存待提交特殊节点附件\''),
  'Frontend attachment delete/save requests must carry an explicit reason for audit why evidence.'
)

assert(
  frontendOperationAuditPane.includes('shouldQueryBatchContextOnly') &&
    frontendOperationAuditPane.includes('objectType: shouldQueryBatchContextOnly.value ? undefined') &&
    frontendOperationAuditPane.includes('objectId: shouldQueryBatchContextOnly.value ? undefined'),
  'Batch trace operation audit pane must query by batchExecutionId without filtering out non-BATCH_EXECUTION objects.'
)

console.log('PASS: eDHR FDA operation audit coverage static contract')
