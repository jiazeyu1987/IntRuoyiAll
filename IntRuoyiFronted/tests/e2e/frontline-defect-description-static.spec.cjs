const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
).replace(/\r\n/g, '\n')

const defectTemplateBlock = panel.match(
  /<section class="frontline-production-defect-section[\s\S]*?<\/section>/
)?.[0] || ''
assert.match(
  defectTemplateBlock,
  /class="frontline-production-defect-name defect-name">\{\{ defect\.label }}<\/span>/,
  'frontline defect cards must render the configured user-facing label.'
)

const configuredDefectReasonBlock = panel.match(
  /const configuredDefectReasons\s*=\s*computed[\s\S]*?(?=\nconst configuredDeviceCards)/
)?.[0] || ''
assert.match(
  configuredDefectReasonBlock,
  /reasonCode:\s*reason\.reasonCode/,
  'frontline defect options must retain the internal reason code for structured submission.'
)
assert.match(
  configuredDefectReasonBlock,
  /label:\s*reason\.reasonName\s*\n?\s*}\)/,
  'frontline defect cards must display the formal reason description directly.'
)
assert.doesNotMatch(
  configuredDefectReasonBlock,
  /label:[^\n]*(?:reasonCode|不良原因)/,
  'frontline defect labels must not fall back to an internal code or generated identifier.'
)

const lossDetailPayloadBlock = panel.match(
  /const buildProductionLossDetailsFromDraft[\s\S]*?(?=\nconst buildProductionSelectedDeviceFromDevice)/
)?.[0] || ''
for (const field of ['reasonId: defect.reasonId', 'reasonCode: defect.reasonCode', 'reasonName: defect.label']) {
  assert.match(
    lossDetailPayloadBlock,
    new RegExp(field.replace('.', '\\.')),
    `structured loss details must preserve ${field}.`
  )
}

console.log('PASS: frontline defect cards display descriptions while submissions retain reason identity')
