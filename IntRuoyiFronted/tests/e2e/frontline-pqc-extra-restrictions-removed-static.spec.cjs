const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const backendRoot = path.join(workspaceRoot, 'IntRuoyiBackend')

const read = (filePath) => fs.readFileSync(filePath, 'utf8').replace(/\r\n/g, '\n')

const blockBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const panelSource = read(path.join(
  frontendRoot,
  'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
))
const feedbackApiSource = read(path.join(frontendRoot, 'src/api/mes/pro/feedback/index.ts'))
const submitReqSource = read(path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesFrontlinePqcSubmitReqVO.java'
))
const pqcContextSource = read(path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java'
))
const activeOrderServiceSource = read(path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java'
))
const eventServiceSource = read(path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventServiceImpl.java'
))
const createTablesSource = read(path.join(
  backendRoot,
  'yudao-module-mes/src/test/resources/sql/create_tables.sql'
))

assert.match(
  feedbackApiSource,
  /export interface FrontlinePqcInspectionSubmitReqVO[\s\S]*scrapQuantity: number[\s\S]*signaturePassword: string/,
  'Frontend PQC submit payload must keep formal quantity and signature fields.'
)
assert.doesNotMatch(
  feedbackApiSource,
  /interface\s+FrontlinePqcInspectionSubmitReqVO[\s\S]*productionSubmitEventId/,
  'Frontend PQC submit payload must not expose manual production submit event binding.'
)
assert.doesNotMatch(
  submitReqSource,
  /@NotNull\(message = "绑定的生产提交事件不能为空"\)[\s\S]{0,120}private Long productionSubmitEventId;/,
  'Backend PQC submit request must not bean-validate productionSubmitEventId as required.'
)

assert.doesNotMatch(
  panelSource,
  /data-pqc-production-submit-select/,
  'Frontend PQC flow must not render a production submit binding select.'
)
assert.doesNotMatch(
  panelSource,
  /productionSubmitCandidates\.value[\s\S]{0,260}productionSubmitEvent/,
  'Frontend PQC readiness must not block on missing or multiple production submit candidates.'
)
assert.doesNotMatch(
  panelSource,
  /selectedPqcProductionSubmitEventId/,
  'Frontend PQC flow must not maintain a required selected production submit event state.'
)

const handleValidateBlock = blockBetween(
  panelSource,
  'const handleValidate = async () => {',
  'const recoverPqcSubmitReceiptAfterUncertainError'
)
assert.match(
  handleValidateBlock,
  /assertPqcSignatureAndQuantityReady\(\)/,
  'Frontend PQC submit preflight must require an electronic signature path and positive inspection quantity.'
)
assert.doesNotMatch(
  handleValidateBlock,
  /productionSubmitCandidates|selectedPqcProductionSubmitEventId|assertFormalPayloadContext\(\)|FrontlineTemplateApi\.validatePayload|assertPqcSubmissionSampleQuantities\(\)/,
  'Frontend PQC submit preflight must not block on manual production-submit binding, template validation, or exact sample quantities.'
)

assert.doesNotMatch(
  panelSource,
  /@click="selectPqcInspectionType\('FINAL'\)"/,
  'Frontend one-line PQC flow must not expose final inspection selection.'
)
assert.match(
  panelSource,
  /type InspectionType = 'FIRST' \| 'PATROL'/,
  'Frontend one-line PQC flow must only support FIRST and PATROL.'
)
assert.doesNotMatch(
  panelSource,
  /isFinalInspectionSelectable/,
  'Frontend one-line PQC flow must not gate the page on final-inspection applicability.'
)

assert.match(
  panelSource,
  /const formatPqcInspectionItemTabLabel = \(item: PqcInspectionItem\) =>\s*\n\s*item\.itemName\s*\|\|\s*'未配置检验项目名称'/,
  'PQC inspection item tabs must display the inspection item name, not the method summary.'
)

assert.doesNotMatch(
  panelSource,
  /assertPqcSubmissionItemEquipmentSelections\(\)/,
  'Frontend PQC submit path must not force equipment or equipment option selection.'
)
assert.doesNotMatch(
  panelSource,
  /PQC检验不合格时必须手动填写不良说明/,
  'Frontend PQC submit path must not require a manual defect description for failed inspection.'
)
assert.doesNotMatch(
  panelSource,
  /validatePqcDefectDescription\(\)/,
  'Frontend PQC submit path must not call failed-inspection defect-description validation.'
)

assert.doesNotMatch(
  pqcContextSource,
  /requirePositive\(command\.getProductionSubmitEventId\(\), "productionSubmitEventId"\)/,
  'Backend PQC submit command validation must not require productionSubmitEventId.'
)
assert.match(
  pqcContextSource,
  /resolveUniqueProductionSubmitEvent\(activeOrder,\s*task\)[\s\S]*command\.setProductionSubmitEventId\(productionSubmit\.eventId\(\)\)/,
  'Backend PQC submit flow must auto-bind the unique same active-order and same-process production submit event.'
)
assert.match(
  pqcContextSource,
  /requirePqcEmployee\(loginUserId,\s*command\.getActualEmployeeId\(\)\)/,
  'Backend PQC submit flow must keep formal employee binding while removing manual production-submit binding.'
)
assert.doesNotMatch(
  pqcContextSource,
  /requireNonconformanceDescriptionWhenFailed/,
  'Backend PQC submit flow must not require nonconformanceDescription when inspection fails.'
)
assert.doesNotMatch(
  pqcContextSource,
  /inspectionItem\.equipmentOptions/,
  'Backend PQC item snapshot must not block no-equipment QA items because option rows are absent.'
)

const resolveSelectedEquipmentBlock = blockBetween(
  pqcContextSource,
  'private MesFrontlinePqcInspectionItem.EquipmentOption resolveSelectedEquipment(',
  'private MesProWorkOrderDO requireWorkOrder'
)
assert.match(
  resolveSelectedEquipmentBlock,
  /if \(!hasSelectedEquipment\) \{[\s\S]*return null;/,
  'Backend PQC item result must allow omitted selected equipment.'
)

const requirePqcSubmitCommandBlock = blockBetween(
  pqcContextSource,
  'private void requirePqcSubmitCommand(MesFrontlinePqcSubmitCommand command) {',
  'private void applyPqcTaskContext'
)
for (const requiredField of [
  'pqcTaskId',
  'activeOrderId',
  'regulationVersionId',
  'qaProcessId',
  'actualEmployeeId',
  'actualInspectionQuantity',
  'signaturePassword'
]) {
  assert.match(
    requirePqcSubmitCommandBlock,
    new RegExp(`"${requiredField}"`),
    `Backend PQC submit command must keep required formal field ${requiredField}.`
  )
}
for (const forbiddenRequiredField of [
  'workOrderId',
  'productionSubmitEventId',
  'routeId',
  'routeProcessId',
  'processId',
  'inspectionType',
  'businessDate',
  'shiftCode',
  'roundNo',
  'templateType',
  'rawPayload'
]) {
  assert.doesNotMatch(
    requirePqcSubmitCommandBlock,
    new RegExp(`"${forbiddenRequiredField}"`),
    `Backend PQC submit command must not require ${forbiddenRequiredField}.`
  )
}
assert.match(
  requirePqcSubmitCommandBlock,
  /getActualInspectionQuantity\(\)[\s\S]*<=\s*0|requirePositive\(command\.getActualInspectionQuantity\(\), "actualInspectionQuantity"\)/,
  'Backend PQC submit command must require actualInspectionQuantity > 0.'
)

const insertTaskBlock = blockBetween(
  activeOrderServiceSource,
  'private void insertPqcInspectionTasks',
  'private MesQaInspectionRegulationDO requirePublishedRegulation'
)
assert.doesNotMatch(
  insertTaskBlock,
  /SHIFT_AM|SHIFT_PM|SHIFT_FINAL|INSPECTION_TYPE_FINAL/,
  'Active-order PQC task generation must not pre-generate AM/PM patrols or FINAL tasks.'
)
assert.match(
  insertTaskBlock,
  /INSPECTION_TYPE_FIRST[\s\S]*INSPECTION_TYPE_PATROL/,
  'Active-order PQC task generation must keep one FIRST task and one PATROL task.'
)
assert.match(
  activeOrderServiceSource,
  /plannedQuantity\.multiply\(ratio\)\s*\.divide\(BigDecimal\.valueOf\(100\),\s*0,\s*RoundingMode\.CEILING\)/,
  'Patrol quantity must calculate plannedQuantity * samplingRatio / 100 with ceiling.'
)
assert.doesNotMatch(
  activeOrderServiceSource,
  /QA规程发布版本缺少末检适用性配置|QA规程发布版本缺少末检不适用依据/,
  'Active-order PQC path must not block first/patrol on final-inspection applicability metadata.'
)

assert.doesNotMatch(
  eventServiceSource,
  /requirePositive\(reqDTO\.getProductionSubmitEventId\(\), "productionSubmitEventId"\)/,
  'Process-pool PQC event creation and idempotency must not require productionSubmitEventId.'
)
assert.doesNotMatch(
  eventServiceSource,
  /missingContext\("productionSubmitEventId"\)/,
  'Process-pool PQC event idempotency must not fail when productionSubmitEventId is absent.'
)
assert.match(
  createTablesSource,
  /"mes_pro_process_pool_pqc_record"[\s\S]*"production_submit_event_id" bigint DEFAULT NULL/,
  'Test schema must allow PQC records without a production submit event binding.'
)

console.log('PASS: frontline PQC extra restrictions removed static contract')
