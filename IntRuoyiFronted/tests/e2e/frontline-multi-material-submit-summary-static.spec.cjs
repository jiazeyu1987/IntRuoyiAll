const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
)

const confirmationStart = source.indexOf('const buildProductionFormalSubmitConfirmation =')
const confirmationEnd = source.indexOf('const clearProductionFormalSubmitConfirmation', confirmationStart)
assert.ok(confirmationStart >= 0 && confirmationEnd > confirmationStart)
const confirmation = source.slice(confirmationStart, confirmationEnd)
assert.match(confirmation, /materialDetails[\s\S]*selectedDevices[\s\S]*deviceParameterReadings/)
assert.doesNotMatch(confirmation, /activeProductionDevice/)

const payloadStart = source.indexOf('const buildFrontlineFormalSubmitPayload =')
const payloadEnd = source.indexOf('const buildProductionDeviceParameterPayload', payloadStart)
assert.ok(payloadStart >= 0 && payloadEnd > payloadStart)
const payload = source.slice(payloadStart, payloadEnd)
assert.match(payload, /buildProductionSelectedDevicesForSubmitScope\(materialDetails\)/)
assert.match(payload, /buildProductionParameterReadingsForSubmitScope\(materialDetails\)/)

const submitScopeStart = source.indexOf('const buildProductionSelectedDevicesForSubmitScope =')
const submitScopeEnd = source.indexOf('const buildProductionLossDetailsForSubmitScope', submitScopeStart)
assert.ok(submitScopeStart >= 0 && submitScopeEnd > submitScopeStart)
const submitScope = source.slice(submitScopeStart, submitScopeEnd)
assert.match(submitScope, /buildProductionSelectedDevicesFromMaterialDetails\(materialDetails\)/)
assert.match(submitScope, /buildProductionParameterReadingsFromMaterialDetails\(materialDetails\)/)
assert.match(submitScope, /buildProductionSelectedDevicesPayload\(\)/)
assert.match(submitScope, /buildProductionDeviceParameterReadingsPayload\(\)/)

console.log('PASS: frontline multi-material submit summary contract')
