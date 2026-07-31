const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../../..')
const viewSource = fs.readFileSync(path.join(__dirname, 'FrontlineFixedTemplatePanel.vue'), 'utf8')
const helperSource = fs.readFileSync(path.join(__dirname, 'frontlineTemplate.ts'), 'utf8')
const feedbackApiSource = fs.readFileSync(path.join(root, 'api/mes/pro/feedback/index.ts'), 'utf8')
const apiSource = fs.readFileSync(
  path.join(root, 'api/mes/pro/feedbackFrontlineTemplate.ts'),
  'utf8'
)

const productionContractFields = [
  'DEVICE',
  'DEVICE_PARAMETERS',
  'OUTPUT_QUANTITY',
  'SCRAP_QUANTITY'
]
const productionDefectLabels = [
  '密封件划伤',
  '装配不到位',
  '外观磕碰',
  '尺寸超差',
  '泄漏',
  '压力异常',
  '其他不良'
]

assert.match(apiSource, /PRODUCTION_SIMPLIFIED/, 'API contract must expose production simplified template code.')
assert.match(apiSource, /PQC_SIMPLIFIED/, 'API contract must expose PQC simplified template code.')
for (const field of productionContractFields) {
  assert.match(helperSource, new RegExp(`['"]${field}['"]`), `production template must include ${field}.`)
}
assert.doesNotMatch(apiSource, /PREVIOUS_PROCESS_INPUT_QUANTITY/, 'API field codes must not expose previous-process input quantity.')
assert.doesNotMatch(feedbackApiSource, /previousProcessInputQuantity/, 'recordbook submit API must not require previous-process input quantity.')
assert.doesNotMatch(helperSource, /PREVIOUS_PROCESS_INPUT_QUANTITY/, 'production template allowed fields must not include previous-process input quantity.')
assert.doesNotMatch(viewSource, /PREVIOUS_PROCESS_INPUT_QUANTITY|previousProcessInputQuantity|previousInputQuantity/, 'production UI payload must not submit previous-process input quantity.')
assert.doesNotMatch(viewSource, /el-date-picker/, 'fixed frontline template must not render editable date picker.')
assert.doesNotMatch(viewSource, /feedbackTime|submitTime|submittedAt/, 'fixed template UI must not expose editable submit time.')
assert.match(viewSource, /data-frontline-production-operator/, 'production UI must expose the simplified production operator surface.')
assert.match(viewSource, /data-frontline-pqc-operator/, 'PQC UI must expose the simplified PQC operator surface.')
assert.match(viewSource, /完成数量/, 'production UI must use the operator-facing completion quantity label.')
assert.match(viewSource, /不良明细/, 'production UI must show inline defect details.')
assert.match(viewSource, /productionScrapQuantity/, 'production scrap quantity must be derived from defect quantities.')
for (const defectLabel of productionDefectLabels) {
  assert.match(viewSource, new RegExp(defectLabel), `production UI must include defect control: ${defectLabel}.`)
}
assert.match(viewSource, /frontline-production-main[\s\S]*is-no-device/, 'production UI must have a no-device full-width layout.')
assert.match(viewSource, /v-if="visibleDeviceCards\.length"/, 'device panel must render only when the process has devices.')
assert.match(viewSource, /selectedProductionDeviceKey/, 'device process UI must allow switching among the visible devices.')
assert.doesNotMatch(viewSource, /frontline-no-device/, 'production UI must not show the old no-device equipment placeholder panel.')
assert.doesNotMatch(viewSource, /上工序输入数量/, 'production operator UI must not ask the worker for previous-process input quantity.')
assert.doesNotMatch(viewSource, />输出数量</, 'production operator UI must not expose the old output quantity wording.')
assert.doesNotMatch(viewSource, /el-dialog|defect-dialog/, 'production defect entry must stay inline without a popup dialog.')
assert.match(viewSource, /检验内容/, 'PQC UI must show editable inspection content.')
assert.match(viewSource, /长度/, 'PQC UI must include length input.')
assert.match(viewSource, /外观/, 'PQC UI must include appearance input.')
assert.match(viewSource, /密封/, 'PQC UI must include seal input.')
assert.match(viewSource, /压力/, 'PQC UI must include pressure input.')
assert.match(viewSource, /首检/, 'PQC UI must include first inspection selector.')
assert.match(viewSource, /巡检/, 'PQC UI must include patrol inspection selector.')
assert.match(viewSource, /末检/, 'PQC UI must include final inspection selector.')
assert.doesNotMatch(viewSource, /DETECTION_SUCCESS|DETECTION_FAILED/, 'PQC operator UI must not show production-style success/failure buttons.')
assert.doesNotMatch(viewSource, /检验方法/, 'PQC operator UI must not show inspection method row.')
assert.doesNotMatch(viewSource, /生产工单/, 'production operator UI must not expose work order fields.')
assert.match(viewSource, /buildFrontlineTemplatePayload/, 'fixed template UI must build the formal payload contract.')
