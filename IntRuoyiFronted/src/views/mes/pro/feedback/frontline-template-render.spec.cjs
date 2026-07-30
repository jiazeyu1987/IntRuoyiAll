const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../../..')
const viewSource = fs.readFileSync(path.join(__dirname, 'FrontlineFixedTemplatePanel.vue'), 'utf8')
const helperSource = fs.readFileSync(path.join(__dirname, 'frontlineTemplate.ts'), 'utf8')
const apiSource = fs.readFileSync(
  path.join(root, 'api/mes/pro/feedbackFrontlineTemplate.ts'),
  'utf8'
)

const productionFields = [
  'PREVIOUS_PROCESS_INPUT_QUANTITY',
  'DEVICE',
  'DEVICE_PARAMETERS',
  'OUTPUT_QUANTITY',
  'SCRAP_QUANTITY'
]

assert.match(apiSource, /PRODUCTION_SIMPLIFIED/, 'API contract must expose production simplified template code.')
assert.match(apiSource, /PQC_SIMPLIFIED/, 'API contract must expose PQC simplified template code.')
for (const field of productionFields) {
  assert.match(helperSource, new RegExp(`['"]${field}['"]`), `production template must include ${field}.`)
  assert.match(viewSource, new RegExp(field), `production UI must render ${field}.`)
}
assert.doesNotMatch(viewSource, /el-date-picker/, 'fixed frontline template must not render editable date picker.')
assert.doesNotMatch(viewSource, /feedbackTime|submitTime|submittedAt/, 'fixed template UI must not expose editable submit time.')
assert.match(viewSource, /data-frontline-production-operator/, 'production UI must expose the simplified production operator surface.')
assert.match(viewSource, /data-frontline-pqc-operator/, 'PQC UI must expose the simplified PQC operator surface.')
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
