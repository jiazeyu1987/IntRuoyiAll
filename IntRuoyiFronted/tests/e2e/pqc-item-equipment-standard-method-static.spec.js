const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const apiSource = read('src/api/mes/pro/feedback/index.ts')
const panelSource = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')

assert.match(
  apiSource,
  /export interface FrontlinePqcEquipmentOptionVO/,
  'PQC API contract must expose item-level equipment options from the regulation snapshot.'
)
assert.match(
  apiSource,
  /equipmentOptions\?: FrontlinePqcEquipmentOptionVO\[\]/,
  'PQC inspection items must carry equipment options instead of one page-level device.'
)
for (const field of [
  'equipmentRequired?: boolean',
  'standardLowerLimit?: number | string',
  'standardUpperLimit?: number | string',
  'standardUnit?: string',
  'standardPrecision?: number'
]) {
  assert.ok(apiSource.includes(field), `PQC inspection item type must include ${field}.`)
}
assert.match(
  apiSource,
  /export interface FrontlinePqcItemResultSubmitReqVO[\s\S]*itemCode: string[\s\S]*selectedEquipmentId: number[\s\S]*selectedEquipmentNumber: string[\s\S]*sampleValues: string\[\]/,
  'PQC submit contract must include itemResults with item code, selected equipment, equipment number, and sample values.'
)
assert.match(
  apiSource,
  /itemResults: FrontlinePqcItemResultSubmitReqVO\[\]/,
  'PQC submit request must send itemResults as the formal item-level facts.'
)

for (const selector of [
  'data-pqc-equipment-select',
  'data-pqc-equipment-number-select',
  'data-pqc-standard-button',
  'data-pqc-method-button',
  'data-pqc-standard-dialog',
  'data-pqc-method-dialog'
]) {
  assert.ok(panelSource.includes(selector), `PQC fill page must expose ${selector}.`)
}
assert.match(
  panelSource,
  /activePqcStandardItem[\s\S]*standardLowerLimit[\s\S]*standardUpperLimit[\s\S]*standardUnit/,
  'PQC standard dialog must display regulation version standard text, lower/upper bounds, and unit.'
)
assert.match(
  panelSource,
  /activePqcMethodItem[\s\S]*inspectionMethod/,
  'PQC method dialog must display the formal regulation inspection method.'
)
assert.match(
  panelSource,
  /const pqcItemSelections = reactive<Record<PqcInspectionItemKey, PqcItemSelection>>/,
  'PQC page must keep item-level equipment selections separate from piece values.'
)
assert.match(
  panelSource,
  /buildPqcItemResultsPayload[\s\S]*selectedEquipmentId[\s\S]*selectedEquipmentNumber[\s\S]*sampleValues/,
  'PQC submit payload must be built from item-level selections and samples.'
)
assert.match(
  panelSource,
  /itemResults: buildPqcItemResultsPayload\(\)/,
  'PQC submit request must include itemResults outside rawPayload.'
)
assert.doesNotMatch(
  panelSource,
  /方法: \$\{item\.inspectionMethod \|\| '未配置'\}[\s\S]*标准: \$\{item\.standardText \|\| '未配置'\}/,
  'PQC item cards must use explicit standard/method actions instead of only compressing the facts into meta text.'
)

console.log('PASS: PQC item equipment, standard, and method static contract')
