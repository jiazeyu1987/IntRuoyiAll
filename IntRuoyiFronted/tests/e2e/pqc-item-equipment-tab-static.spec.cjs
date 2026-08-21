const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const teamLeaderApi = read('src/api/mes/pro/processpool/teamLeader.ts')
const pqcWorkbench = read('src/views/mes/pro/processpool/PqcLeaderWorkbenchPage.vue')
const teamLeaderWorkbench = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const panel = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const configPanel = read('src/views/mes/pro/processpool/PqcItemEquipmentConfigPanel.vue')

assert.match(teamLeaderApi, /\/mes\/pqc\/item-equipment\/items/)
assert.match(teamLeaderApi, /\/mes\/pqc\/item-equipment\/config/)
assert.match(teamLeaderApi, /export interface PqcItemEquipmentConfigVO/)
assert.match(teamLeaderApi, /savePqcItemEquipmentConfig/)
assert.doesNotMatch(teamLeaderApi, /qa\/inspection-regulation[\s\S]*itemEquipment|regulationVersionId:\s*number/)

assert.match(pqcWorkbench, /show-pqc-equipment-tab/)
assert.match(teamLeaderWorkbench, /检验设备/)
assert.match(teamLeaderWorkbench, /data-production-leader-module-tab-equipment/)
assert.match(teamLeaderWorkbench, /PqcItemEquipmentConfigPanel/)
assert.match(configPanel, /当前租户内共用/)
assert.match(configPanel, /检验项目[\s\S]*检验设备[\s\S]*设备编号/)
assert.match(configPanel, /getPqcItemEquipmentItems/)
assert.match(configPanel, /getPqcItemEquipmentConfig/)
assert.match(configPanel, /savePqcItemEquipmentConfig/)
assert.doesNotMatch(configPanel, /regulationVersionId|QA版本|待发布/)

const validateBlock = panel.slice(
  panel.indexOf('function assertPqcItemEquipmentSelection'),
  panel.indexOf('function buildPqcInspectionSubmitPayloads')
)
assert.match(validateBlock, /hasPqcEquipmentOptions\(item\)/)
assert.match(validateBlock, /未选择检验设备/)
assert.match(validateBlock, /未选择设备编号/)
assert.match(validateBlock, /selectedEquipmentId/)
assert.match(validateBlock, /selectedEquipmentNumber/)

console.log('PASS: PQC leader tenant equipment tab and frontline equipment precheck contract')
