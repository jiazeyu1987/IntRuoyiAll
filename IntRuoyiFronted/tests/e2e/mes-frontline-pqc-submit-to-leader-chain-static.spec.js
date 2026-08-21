const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const panelPath = path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const feedbackApiPath = path.join(root, 'src/api/mes/pro/feedback/index.ts')
const leaderPagePath = path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const backendControllerPath = path.join(
  root,
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/MesFrontlineDeviceAccountController.java'
)
const backendServicePath = path.join(
  root,
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java'
)
const backendMapperPath = path.join(
  root,
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/MesProProcessPoolEventMapper.java'
)

const read = (file) => fs.readFileSync(file, 'utf8')

const panel = read(panelPath)
const feedbackApi = read(feedbackApiPath)
const leaderPage = read(leaderPagePath)
const backendController = read(backendControllerPath)
const backendService = read(backendServicePath)
const backendMapper = read(backendMapperPath)

assert(
  feedbackApi.includes('/mes/pro/feedback/frontline/submit') &&
    feedbackApi.includes('frontlineSubmit') &&
    feedbackApi.includes('/mes/pro/feedback/frontline/device-account/pqc/submit') &&
    feedbackApi.includes('submitFrontlinePqcInspection') &&
    feedbackApi.includes('/mes/pro/feedback/frontline/device-account/pqc/submit-receipt') &&
    feedbackApi.includes('getFrontlinePqcSubmitReceipt'),
  'PQC 检验员提交必须存在正式持久化 API wrapper 和只读回执确认 API wrapper。'
)

const handleValidateStart = panel.indexOf('const handleValidate = async () => {')
const handleValidateEnd = panel.indexOf('const closePqcSignatureDialog', handleValidateStart)
const handleValidateBlock = panel.slice(handleValidateStart, handleValidateEnd)
const formalReadyIndex = handleValidateBlock.indexOf('assertPqcFormalSubmissionReady()')
const quantityReadyIndex = handleValidateBlock.indexOf('assertPqcSignatureAndQuantityReady()')
const allMethodReadyIndex = handleValidateBlock.indexOf('assertPqcCurrentProcessAllMethodSubmissionReady()')
const displayFieldsReadyIndex = handleValidateBlock.indexOf('assertPqcInspectionDisplayFieldsReady()')
const signatureDialogIndex = handleValidateBlock.indexOf('pqcSignatureDialogVisible.value = true')
const confirmStart = panel.indexOf('const handleConfirmPqcSubmit = async () => {')
const confirmEnd = panel.indexOf('const assertFormalPayloadContext', confirmStart)
const confirmBlock = panel.slice(confirmStart, confirmEnd)
const pqcSubmitIndex = confirmBlock.indexOf('ProFeedbackApi.submitFrontlinePqcInspection')
const resetIndex = confirmBlock.indexOf('resetPqcSubmissionDrafts(submitPayloads.map((payload) => payload.pqcTaskId))')
const successIndex = confirmBlock.indexOf('message.success(')
const recoverIndex = confirmBlock.indexOf('recoverPqcSubmitReceiptAfterUncertainError')
assert(
  formalReadyIndex >= 0 &&
    quantityReadyIndex > formalReadyIndex &&
    allMethodReadyIndex > quantityReadyIndex &&
    displayFieldsReadyIndex > allMethodReadyIndex &&
    signatureDialogIndex > displayFieldsReadyIndex &&
    pqcSubmitIndex >= 0 &&
    resetIndex > pqcSubmitIndex &&
    successIndex > resetIndex &&
    recoverIndex > pqcSubmitIndex,
  'PQC 检验员提交必须先完成本地正式校验，再电子签名、调用正式接口；明确成功后复位本次草稿，提交异常后先尝试只读恢复确认。'
)

const submitCallPattern =
  /submitFrontlinePqcInspection\s*\(|ProFeedbackApi\.submitFrontlinePqcInspection\s*\(/
assert(
  submitCallPattern.test(panel),
  'PQC 检验员提交按钮必须调用 /mes/pro/feedback/frontline/submit 或正式 PQC 提交接口。'
)

for (const token of ['pqcDraft', 'pqcPieceValues', 'rawPayload']) {
  assert(
    handleValidateBlock.includes(token) || panel.includes(token),
    `PQC 提交持久化 payload 必须保留检验员填写明细字段: ${token}`
  )
}

assert(
  leaderPage.includes('resolvePqcItemSnapshotDetails') &&
    leaderPage.includes('resolvePqcDetailStructuredItems') &&
    leaderPage.includes('data-pqc-leader-item-snapshot-table') &&
    /pqcItemDetails|itemResults/.test(leaderPage),
  'PQC 组长列表必须按检验员正式项目级明细解析展示，不能只展示汇总。'
)

const resolvePqcResultStart = panel.indexOf('const resolvePqcResult = () => {')
const resolvePqcResultEnd = panel.indexOf('const normalizePqcDefectDescription', resolvePqcResultStart)
assert(
  resolvePqcResultStart >= 0 && resolvePqcResultEnd > resolvePqcResultStart,
  'PQC 检测结果必须有独立解析逻辑。'
)
const resolvePqcResultBlock = panel.slice(resolvePqcResultStart, resolvePqcResultEnd)
assert(
  !resolvePqcResultBlock.includes('getPqcExactPieceValuesForSubmit'),
  'PQC 草稿检测结果计算不能调用提交专用逐件数量强断言，避免批量合格填写过程中抛未处理异常。'
)
assert(
  resolvePqcResultBlock.includes('resolvePqcResultForTask') ||
    resolvePqcResultBlock.includes('getPqcCurrentChoiceValues'),
  'PQC 草稿检测结果计算应使用当前逐件草稿值，提交前强校验由 assertPqcSubmissionSampleQuantities 负责。'
)

const recoverStart = panel.indexOf('const recoverPqcSubmitReceiptAfterUncertainError')
const recoverEnd = panel.indexOf('const handleConfirmPqcSubmit', recoverStart)
assert(
  recoverStart >= 0 && recoverEnd > recoverStart,
  'PQC 正式提交必须有提交结果不确定后的只读回执恢复函数。'
)
const recoverBlock = panel.slice(recoverStart, recoverEnd)
assert(
  recoverBlock.includes('ProFeedbackApi.getFrontlinePqcSubmitReceipt') &&
    recoverBlock.includes('resetPqcSubmissionDraft(recoveredReceipt.pqcTaskId)') &&
    recoverBlock.includes('pqcSubmitResultUncertain.value = true'),
  'PQC 提交异常后必须按 pqcTaskId 查询正式回执；已提交则按成功提交复位并进入下一次提交，确认失败才进入不确定锁定态。'
)
assert(
  /const isPqcSubmitBlocked = computed\(\(\) =>[\s\S]*payloadLoading\.value[\s\S]*pqcSubmitResultUncertain\.value/.test(panel),
  'PQC 提交按钮只在提交中或结果不确定时锁定，明确成功或明确失败后必须可继续提交。'
)

assert(
  backendController.includes('@GetMapping("/pqc/submit-receipt")') &&
    backendController.includes('getPqcSubmitReceipt') &&
    backendController.includes('pqcContextService.getSubmittedPqcInspection'),
  '后端必须提供 PQC 正式提交回执只读查询接口。'
)
assert(
  backendService.includes('getSubmittedPqcInspection') &&
    backendService.includes('requireUniqueSubmittedEvent') &&
    backendService.includes('selectListPqcByTaskId') &&
    backendService.includes('getSubmittedEventId') &&
    backendService.includes('loadPqcSubmitResult'),
  'PQC 回执只读查询必须复用正式事件和 PQC record 来源生成回执。'
)
assert(
  backendMapper.includes('selectListPqcByTaskId') &&
    backendMapper.includes('EVENT_TYPE_PQC_INSPECTION') &&
    backendMapper.includes('getFeedbackSourceId'),
  'PQC 回执只读查询必须按任务稳定 ID 查询正式 PQC_INSPECTION 事件。'
)

console.log('mes-frontline-pqc-submit-to-leader-chain-static PASS')
