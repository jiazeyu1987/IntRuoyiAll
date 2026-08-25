const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const pqcWorkbench = read('src/views/mes/pro/processpool/PqcLeaderWorkbenchPage.vue')
const teamLeaderWorkbench = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const qaPage = read('src/views/mes/pro/processpool/QaRegulationPage.vue')
const panel = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')

assert.doesNotMatch(pqcWorkbench, /show-pqc-equipment-tab|检验设备/)
assert.doesNotMatch(teamLeaderWorkbench, /data-production-leader-module-tab-equipment|PqcItemEquipmentConfigPanel|showPqcEquipment/)
assert.match(qaPage, /检验器具及设备说明/)
assert.match(qaPage, /equipmentOptions/)
assert.match(qaPage, /DvMachineryApi|getMachineryPage/)

const validateBlock = panel.slice(
  panel.indexOf('function assertPqcItemEquipmentSelection'),
  panel.indexOf('function buildPqcInspectionSubmitPayloads')
)
assert.match(validateBlock, /hasPqcEquipmentOptions\(item\)/)
assert.match(validateBlock, /未选择检验设备/)
assert.match(validateBlock, /selectedEquipmentId/)
assert.match(validateBlock, /selectedEquipmentNumber/)

console.log('PASS: standalone equipment tab removed; QA item bindings drive frontline equipment selection')
