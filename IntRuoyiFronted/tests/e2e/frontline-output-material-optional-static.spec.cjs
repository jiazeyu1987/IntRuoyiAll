const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
)
const validationStart = source.indexOf('const assertProductionSubmissionReady = () => {')
const validationEnd = source.indexOf('const resolveProductionProgressQuantity', validationStart)
const validation = source.slice(validationStart, validationEnd)
assert.match(validation, /filledMaterials\.length === 0/)
assert.doesNotMatch(validation, /missingMaterials/)

const payloadStart = source.indexOf('const buildProductionMaterialDetailsPayload = (): ProFrontlineFeedbackMaterialReqVO[] => {')
const payloadEnd = source.indexOf('const buildProductionEquipmentParameterRulesPayload', payloadStart + 1)
const payload = source.slice(payloadStart, payloadEnd)
assert.match(payload, /configuredProductionMaterials\.value\.flatMap/)
assert.match(payload, /outputQuantity === undefined[\s\S]*return \[\]/)

console.log('PASS: frontline permits partial output-material submission')
