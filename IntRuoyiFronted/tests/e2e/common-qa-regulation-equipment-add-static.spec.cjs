const fs = require('fs')
const path = require('path')
const assert = require('assert')

const frontendRoot = path.resolve(__dirname, '..', '..')
const page = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/processpool/QaRegulationPage.vue'),
  'utf8'
)
const api = fs.readFileSync(
  path.join(frontendRoot, 'src/api/mes/qc/template/index.ts'),
  'utf8'
)

const commonItemsStart = page.indexOf('data-qa-common-items')
const qaOverviewStart = page.indexOf('data-qa-regulation-common-binding-control', commonItemsStart)
assert(commonItemsStart >= 0, 'RED: common regulation items block is missing')
assert(qaOverviewStart > commonItemsStart, 'RED: common regulation items block boundary is missing')

const commonItemsBlock = page.slice(commonItemsStart, qaOverviewStart)
const commonSaveItemStart = page.indexOf('const buildCommonRegulationSaveItem')
const commonSaveProcessesStart = page.indexOf('const buildCommonRegulationSaveProcesses', commonSaveItemStart)
assert(
  commonSaveItemStart >= 0 && commonSaveProcessesStart > commonSaveItemStart,
  'RED: common item save builder is missing'
)
const commonSaveItemBlock = page.slice(commonSaveItemStart, commonSaveProcessesStart)

assert(
  commonItemsBlock.includes('data-qa-common-item-equipment-add') &&
    commonItemsBlock.includes('addCommonRegulationItemEquipment(row)'),
  'RED: common item equipment column must expose a QA-style add equipment button'
)

for (const expected of [
  'v-model="equipment.equipmentId"',
  'getAvailableQaMachinery(row, equipmentIndex)',
  'removeCommonRegulationItemEquipment(row, equipmentIndex)'
]) {
  assert(
    commonItemsBlock.includes(expected),
    `RED: common item equipment column must include ${expected}`
  )
}

assert(
  /handleCommonRegulationItemEquipmentChange\(\s*row,\s*equipmentIndex,\s*\$event\s*\)/.test(
    commonItemsBlock
  ),
  'RED: common item equipment column must change the selected device through the common handler'
)

assert(
  page.includes('const addCommonRegulationItemEquipment = (row: CommonRegulationItem)') &&
    page.includes('const removeCommonRegulationItemEquipment = (') &&
    page.includes('const handleCommonRegulationItemEquipmentChange = ('),
  'RED: common item equipment add/change/remove handlers must be explicit and scoped to common rows'
)

assert(
  commonSaveItemBlock.includes('equipmentOptions: buildCommonRegulationItemEquipmentOptions(item, itemName)'),
  'RED: common item save payload must carry selected equipment options into the upgrade request'
)

assert(
  api.includes('export interface QaCommonRegulationSetItemsUpgradeItemReqVO') &&
    api.includes('equipmentOptions: QaInspectionRegulationItemEquipmentVO[]') &&
    api.includes('processes: QaCommonRegulationSetItemsUpgradeProcessReqVO[]'),
  'RED: frontend common-set upgrade API type must allow saving equipment options'
)

console.log('GREEN: common regulation item equipment add contract is present')
