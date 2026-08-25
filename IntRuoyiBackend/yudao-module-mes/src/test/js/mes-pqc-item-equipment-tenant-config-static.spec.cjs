const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const backendRoot = path.resolve(__dirname, '../../../..')
const read = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const qaService = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java'
)
const saveReq = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/vo/MesQaInspectionRegulationSaveReqVO.java'
)
const publishedResp = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/vo/MesQaInspectionRegulationPublishedVersionRespVO.java'
)
const contextService = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java'
)
const releaseWriter = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl.java'
)

assert.match(saveReq, /class InspectionEquipment/)
assert.match(saveReq, /Long equipmentId/)
assert.match(publishedResp, /class EquipmentOption/)
assert.match(publishedResp, /String equipmentNumber/)
assert.match(qaService, /MesQaInspectionRegulationItemEquipmentMapper/)
assert.match(qaService, /itemEquipmentMapper\.deleteByVersionId/)
assert.match(qaService, /itemEquipmentMapper\.insert/)
assert.match(qaService, /MesDvMachineryService/)
assert.match(qaService, /getMachineryMap/)
assert.match(qaService, /equipmentRequired\(CollUtil\.isNotEmpty\(item\.getEquipmentOptions\(\)\)\)/)

assert.match(contextService, /MesQaInspectionRegulationItemEquipmentMapper/)
assert.doesNotMatch(contextService, /MesPqcItemEquipmentConfigService|pqcItemEquipmentConfigService/)
assert.match(contextService, /source\.getEquipmentOptions\(\)/)
assert.match(contextService, /regulationItemEquipmentMapper\.selectListByVersionId/)
assert.match(contextService, /selectedEquipmentNumber/)
assert.match(contextService, /MesPqcInspectionPieceDetailDO\.builder\(\)[\s\S]*selectedEquipmentCode/)

assert.match(releaseWriter, /getSelectedEquipmentName\(\)/)
assert.match(releaseWriter, /getSelectedEquipmentNumber\(\)/)
assert.doesNotMatch(releaseWriter, /MesQaInspectionRegulationItemEquipment|matchesQaEquipment|hashEquipment/)

console.log('PASS: QA-version equipment bindings are the only runtime source; release uses submitted snapshots')
