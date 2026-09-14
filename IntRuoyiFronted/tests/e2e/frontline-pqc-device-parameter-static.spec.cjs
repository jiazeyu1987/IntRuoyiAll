const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const api = read('src/api/mes/pro/feedback/index.ts')
const panel = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const pqcSubmitPayloadStart = panel.indexOf('const buildPqcInspectionSubmitPayloadForTask =')
const pqcSubmitPayloadEnd = panel.indexOf('function buildPqcInspectionSubmitPayloads', pqcSubmitPayloadStart)
assert.ok(pqcSubmitPayloadStart >= 0 && pqcSubmitPayloadEnd > pqcSubmitPayloadStart, 'PQC submit payload builder must be locatable.')
const pqcSubmitPayloadBlock = panel.slice(pqcSubmitPayloadStart, pqcSubmitPayloadEnd)

assert.match(
  api,
  /export interface FrontlinePqcEquipmentOptionVO[\s\S]*parameters\?: FrontlinePqcDeviceParameterVO\[\]/,
  'Frontline PQC equipment option API type must include device parameters.'
)
assert.match(
  api,
  /export interface FrontlinePqcDeviceParameterVO[\s\S]*parameterCode: string[\s\S]*lowerLimit\?: number \| string[\s\S]*upperLimit\?: number \| string[\s\S]*optionValues\?: string\[\]/,
  'Frontline PQC device parameter API type must include code, range, and select options.'
)
assert.match(
  panel,
  /data-pqc-equipment-parameter-summary/,
  'Frontline PQC page must render a selected-equipment parameter summary.'
)
assert.match(
  panel,
  /getPqcSelectedEquipmentParameters[\s\S]*getPqcSelectedEquipmentOption\(item\)\?\.parameters \|\| \[\]/,
  'Frontline PQC page must read parameters from the selected formal equipment option.'
)
assert.match(
  panel,
  /formatPqcSelectedEquipmentParameters[\s\S]*parameter\.parameterName \|\| parameter\.parameterCode/,
  'Frontline PQC parameter summary must show formal parameter names or codes.'
)
assert.doesNotMatch(
  pqcSubmitPayloadBlock,
  /deviceParameterReadings/,
  'Frontline PQC must not submit production device parameter readings as PQC results.'
)

console.log('PASS: frontline PQC device parameter frontend static contract')
