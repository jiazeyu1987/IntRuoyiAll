const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const service = read('src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java')
const saveReq = read('src/main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/vo/MesQaInspectionRegulationSaveReqVO.java')
const publishedResp = read('src/main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/vo/MesQaInspectionRegulationPublishedVersionRespVO.java')
const frontline = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java')

assert.match(publishedResp, /class EquipmentOption/)
assert.match(publishedResp, /String equipmentNumber/)
assert.match(service, /itemEquipmentMapper\.deleteByVersionId/)
assert.doesNotMatch(service, /saveItemEquipmentSnapshots|itemEquipmentMapper\.insert|MesDvMachineryService|getMachineryMap/)
assert.match(service, /equipmentRequired\(false\)/)
assert.match(frontline, /MesPqcItemEquipmentConfigService|pqcItemEquipmentConfigService/)
assert.match(frontline, /listEnabledEquipmentOptionsByItemCodes/)
assert.doesNotMatch(frontline, /source\.getEquipmentOptions\(\)/)
assert.match(frontline, /selectedEquipmentNumber/)
assert.match(frontline, /MesPqcInspectionPieceDetailDO\.builder\(\)[\s\S]*selectedEquipmentCode/)

console.log('PASS: QA version excludes equipment snapshot and frontline reads live tenant config contract')
