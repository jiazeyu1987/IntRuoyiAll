const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replaceAll(String.fromCharCode(13), '')

const qaPage = read('src/views/mes/pro/processpool/QaRegulationPage.vue')
const qaApi = read('src/api/mes/qc/template/index.ts')

assert.match(qaApi, /savePqcItemEquipmentConfigBatch/)
assert.match(qaPage, /autoSaveQaItemEquipment/)
assert.match(qaPage, /addQaItemEquipment[\s\S]*autoSaveQaItemEquipment/)
assert.match(qaPage, /removeQaItemEquipment[\s\S]*autoSaveQaItemEquipment/)

const saveItemBlock = qaPage.slice(
  qaPage.indexOf('const buildQaRegulationSaveItem'),
  qaPage.indexOf('const buildQaRegulationProcesses')
)
assert.doesNotMatch(saveItemBlock, /equipmentOptions/)
assert.match(qaPage, /getPqcItemEquipmentConfigBatch/)

console.log('PASS: QA equipment binding auto-save and QA version isolation contract')
