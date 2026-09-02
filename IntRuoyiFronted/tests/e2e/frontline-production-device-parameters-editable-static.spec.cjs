const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const panelPath = path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const source = fs.readFileSync(panelPath, 'utf8')

const detailStart = source.indexOf(
  'v-for="parameter in getProductionDeviceDetailParameters(activeProductionDevice)"'
)
const detailEnd = source.indexOf(
  'class="frontline-production-clearance-confirmations"',
  detailStart
)
assert.ok(detailStart >= 0 && detailEnd > detailStart, 'must locate production device parameter detail template')
const detailTemplate = source.slice(detailStart, detailEnd)

assert.doesNotMatch(
  detailTemplate,
  /v-if="isTextStandardParameter\(parameter\)"[\s\S]*?<span[\s\S]*?data-frontline-text-parameter-standard/,
  'TEXT_STANDARD device parameters must not render as read-only span text.'
)
assert.match(
  detailTemplate,
  /<input[\s\S]*v-if="isTextStandardParameter\(parameter\)"[\s\S]*data-frontline-text-parameter-input[\s\S]*@input="updateProductionDeviceParameter\(activeProductionDevice\.key, parameter\.parameterCode, \$event\)"/,
  'TEXT_STANDARD device parameters must render editable inputs wired to the parameter draft.'
)

const syncStart = source.indexOf('const syncProductionDeviceParameterDraft = (devices: ProductionDeviceCard[]) => {')
const syncEnd = source.indexOf('const resetProductionDeviceParameterDraft = () => {', syncStart)
assert.ok(syncStart >= 0 && syncEnd > syncStart, 'must locate device parameter draft sync')
const syncBlock = source.slice(syncStart, syncEnd)
assert.doesNotMatch(
  syncBlock,
  /!\s*parameter\.parameterCode\s*\|\|\s*isTextStandardParameter\(parameter\)/,
  'TEXT_STANDARD parameters must not be skipped by the empty-code guard.'
)
assert.match(
  syncBlock,
  /isTextStandardParameter\(parameter\)[\s\S]*parameter\.defaultText[\s\S]*parameter\.standardText/,
  'TEXT_STANDARD parameters must initialize from defaultText or standardText.'
)

const payloadStart = source.indexOf('const buildProductionDeviceParameterReadingsFromDraft = (')
const payloadEnd = source.indexOf('const buildProductionDeviceParameterReadingsPayload', payloadStart)
assert.ok(payloadStart >= 0 && payloadEnd > payloadStart, 'must locate device parameter payload builder')
const payloadBlock = source.slice(payloadStart, payloadEnd)
assert.doesNotMatch(
  payloadBlock,
  /\.filter\(\(parameter\) => !isTextStandardParameter\(parameter\)\)/,
  'TEXT_STANDARD parameters must not be filtered out of submitted readings.'
)
assert.match(
  payloadBlock,
  /isTextStandardParameter\(parameter\)[\s\S]*textValue[\s\S]*parameterStatus:\s*'NORMAL'/,
  'TEXT_STANDARD parameters must submit trimmed textValue with NORMAL status.'
)

console.log('PASS: frontline production device parameters editable static contract')
