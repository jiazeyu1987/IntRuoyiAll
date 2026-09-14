const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const qaPage = read('src/views/mes/pro/processpool/QaRegulationPage.vue')
const qaApi = read('src/api/mes/qc/template/index.ts')
const frontlinePanel = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')

assert.doesNotMatch(qaPage, /PqcItemEquipmentConfigPanel|检验设备.*tab|name=["']equipment["']/)
assert.match(qaPage, /v-model="row\.inspectionTool"[\s\S]*检验器具及设备说明/)
assert.match(qaPage, /equipmentOptions/)
assert.match(qaPage, /equipmentId/)
assert.match(qaPage, /新增设备|删除设备|addQaItemEquipment|removeQaItemEquipment/)
assert.match(qaPage, /DvMachineryApi|getMachineryPage/)
assert.match(qaPage, /equipmentOptions[\s\S]*equipmentId[\s\S]*equipmentCode[\s\S]*equipmentNumber/)
assert.match(qaApi, /equipmentOptions: QaInspectionRegulationItemEquipmentVO\[\]/)
assert.match(qaApi, /interface QaInspectionRegulationItemEquipmentVO[\s\S]*equipmentId: number[\s\S]*equipmentNumber: string/)
assert.match(qaApi, /type QaInspectionRegulationSaveItemVO = Omit</)
const saveItemTypeBlock = qaApi.slice(
  qaApi.indexOf('export type QaInspectionRegulationSaveItemVO'),
  qaApi.indexOf('export interface QaInspectionRegulationSaveRespVO')
)
assert.doesNotMatch(saveItemTypeBlock, /equipmentOptions\s*:/)
assert.match(qaApi, /processes: QaInspectionRegulationProcessVO\[\]/)

const frontlineEquipmentBlock = frontlinePanel.slice(
  frontlinePanel.indexOf('data-pqc-equipment-card'),
  frontlinePanel.indexOf('data-pqc-standard-button')
)
assert.match(frontlineEquipmentBlock, /data-pqc-equipment-select/)
assert.doesNotMatch(frontlineEquipmentBlock, /data-pqc-equipment-number-select/)
assert.match(frontlinePanel, /selectedEquipmentNumber\s*=\s*selectedOption\?\.equipmentNumber/)
assert.match(frontlinePanel, /selectedEquipmentId[\s\S]*selectedEquipmentNumber/)

console.log('PASS: QA single-ledger equipment binding and frontline single-device selector contract')
