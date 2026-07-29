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
  assert.match(viewSource, new RegExp(`['"]${field}['"]`), `production UI must render ${field}.`)
}
assert.doesNotMatch(viewSource, /el-date-picker/, 'fixed frontline template must not render editable date picker.')
assert.doesNotMatch(viewSource, /feedbackTime|submitTime|submittedAt/, 'fixed template UI must not expose editable submit time.')
assert.match(viewSource, /DETECTION_SUCCESS/, 'PQC UI must include detection success.')
assert.match(viewSource, /DETECTION_FAILED/, 'PQC UI must include detection failure.')
assert.match(viewSource, /buildFrontlineTemplatePayload/, 'fixed template UI must build the formal payload contract.')
