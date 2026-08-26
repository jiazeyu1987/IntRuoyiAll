const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replaceAll(String.fromCharCode(13), '')

const contextService = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java'
)
const qaService = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java'
)
const equipmentService = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/pqc/MesPqcItemEquipmentConfigServiceImpl.java'
)

assert.match(contextService, /MesPqcItemEquipmentConfigService/)
assert.match(contextService, /listEnabledEquipmentOptionsByItemCodes/)
assert.match(contextService, /listEnabledEquipmentOptionsByProjectAndItemCodes/)
assert.doesNotMatch(contextService, /source\.getEquipmentOptions\(\)/)
assert.match(qaService, /itemEquipmentMapper\.deleteByVersionId/)
assert.doesNotMatch(qaService, /saveItemEquipmentSnapshots/)
assert.match(equipmentService, /equipmentNumber/)
assert.match(equipmentService, /machinery\.getCode\(\)/)
assert.match(equipmentService, /getEquipmentNumbers\(\)\.size\(\)/)
assert.match(equipmentService, /selectLatestDraftByRegulationId/)
assert.match(equipmentService, /selectLatestPublishedByRegulationId/)

console.log('PASS: PQC reads live tenant equipment config and QA versions do not store equipment bindings')
