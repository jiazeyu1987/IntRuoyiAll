const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const backendRoot = path.resolve(__dirname, '../../../..')
const repoRoot = path.resolve(backendRoot, '..')
const read = (file) => fs.readFileSync(path.join(repoRoot, file), 'utf8')

const panel = read('IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const submitService = read('IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceImpl.java')
const submitResp = read('IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesProFrontlineFeedbackSubmitRespVO.java')

function sliceBetween(source, startNeedle, endNeedle) {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `missing start: ${startNeedle}`)
  const end = source.indexOf(endNeedle, start)
  assert.ok(end > start, `missing end: ${endNeedle}`)
  return source.slice(start, end)
}

const assertProductionSubmissionReady = sliceBetween(
  panel,
  'const assertProductionSubmissionReady =',
  'const resolveProductionProgressQuantity'
)
assert.match(
  assertProductionSubmissionReady,
  /const\s+materialDetails\s*=\s*buildProductionMaterialDetailsPayload\(\)/,
  '提交前校验必须先固化并读取实际提交的 materialDetails。'
)
assert.match(
  assertProductionSubmissionReady,
  /findMissingProductionDeviceParameters\(materialDetails\)/,
  '设备参数缺失校验必须逐物料读取 materialDetails，不能只读当前页签草稿。'
)
assert.doesNotMatch(
  assertProductionSubmissionReady,
  /activeProductionDevice|selectedProductionDeviceKeys\.value/,
  '提交前设备参数校验不得依赖当前激活设备或当前物料页签。'
)

const formalPayloadBuilder = sliceBetween(
  panel,
  'const buildFrontlineFormalSubmitPayload =',
  'const buildProductionDeviceParameterPayload'
)
assert.match(
  formalPayloadBuilder,
  /buildProductionEquipmentParameterRulesPayloadFromMaterialDetails\(materialDetails\)/,
  'recordbook 设备参数规则快照必须基于实际提交物料的设备集合。'
)
assert.match(
  formalPayloadBuilder,
  /buildProductionSelectedDevicesForSubmitScope\(materialDetails\)/,
  '正式 payload 的设备摘要必须兼容有物料和无物料两种提交范围。'
)
assert.match(
  formalPayloadBuilder,
  /buildProductionParameterReadingsForSubmitScope\(materialDetails\)/,
  '正式 payload 的设备参数读数必须兼容有物料和无物料两种提交范围。'
)
assert.match(
  formalPayloadBuilder,
  /lossDetails:\s*buildProductionLossDetailsForSubmitScope\(materialDetails\)/,
  '正式 payload 的损耗明细必须来自实际提交物料聚合。'
)

const submitScopeHelpers = sliceBetween(
  panel,
  'const buildProductionSelectedDevicesForSubmitScope =',
  'const findProductionDeviceCardById'
)
assert.match(
  submitScopeHelpers,
  /materialDetails\.length[\s\S]*buildProductionSelectedDevicesFromMaterialDetails\(materialDetails\)[\s\S]*buildProductionSelectedDevicesPayload\(\)/,
  '设备提交范围 helper 必须在有物料时读 materialDetails，无物料时保留工序级设备。'
)
assert.match(
  submitScopeHelpers,
  /materialDetails\.length[\s\S]*buildProductionParameterReadingsFromMaterialDetails\(materialDetails\)[\s\S]*buildProductionDeviceParameterReadingsPayload\(\)/,
  '参数读数提交范围 helper 必须在有物料时读 materialDetails，无物料时保留工序级读数。'
)

const structuredRawPayload = sliceBetween(
  panel,
  'const buildProductionStructuredRawPayload =',
  'const buildProductionFieldValues'
)
assert.match(
  structuredRawPayload,
  /buildProductionSelectedDevicesForSubmitScope\(materialDetails\)/,
  'rawPayload.selectedDevices 必须来自实际提交的物料明细聚合。'
)
assert.match(
  structuredRawPayload,
  /buildProductionParameterReadingsForSubmitScope\(materialDetails\)/,
  'rawPayload.deviceParameterReadings 必须来自实际提交的物料参数读数聚合。'
)
assert.match(
  structuredRawPayload,
  /lossDetails:\s*buildProductionLossDetailsForSubmitScope\(materialDetails\)[\s\S]*lossReasonDetails:\s*buildProductionLossDetailsForSubmitScope\(materialDetails\)/,
  'rawPayload 顶层损耗明细必须与实际提交物料一致。'
)

const fieldValues = sliceBetween(
  panel,
  'const buildProductionFieldValues =',
  'const buildPqcFieldValues'
)
assert.match(
  fieldValues,
  /\[FRONTLINE_FIELD_CODES\.OUTPUT_QUANTITY\]:\s*resolveProductionProgressQuantity\(materialDetails\)[\s\S]*\[FRONTLINE_FIELD_CODES\.SCRAP_QUANTITY\]:\s*resolveProductionLossQuantity\(materialDetails\)/,
  'recordbook fieldValues 的完成/损耗数量必须按实际提交范围计算。'
)

const respItem = sliceBetween(
  submitResp,
  'public static class ParameterAuditItemRespVO',
  '\n    }\n\n}'
)
assert.match(respItem, /private\s+Long\s+materialId\s*;/, '参数审计响应明细必须包含 materialId。')
assert.match(respItem, /private\s+String\s+materialName\s*;/, '参数审计响应明细必须包含 materialName。')

const toAuditItemResp = sliceBetween(
  submitService,
  'private MesProFrontlineFeedbackSubmitRespVO.ParameterAuditItemRespVO toAuditItemResp',
  'private void validateSubmitContext'
)
assert.match(
  toAuditItemResp,
  /\.setMaterialId\(item\.getMaterialId\(\)\)[\s\S]*\.setMaterialName\(item\.getMaterialName\(\)\)/,
  '后端参数审计响应转换必须透传物料身份。'
)

console.log('PASS: frontline production material-scoped device parameter contract')
