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

assert.match(publishedResp, /class EquipmentOption/)
assert.match(publishedResp, /String equipmentNumber/)
assert.match(qaService, /MesQaInspectionRegulationItemEquipmentMapper/)
assert.match(qaService, /itemEquipmentMapper\.deleteByVersionId/)
assert.doesNotMatch(qaService, /saveItemEquipmentSnapshots|itemEquipmentMapper\.insert|MesDvMachineryService|getMachineryMap/)
assert.match(qaService, /equipmentRequired\(false\)/)

assert.match(contextService, /MesPqcItemEquipmentConfigService|pqcItemEquipmentConfigService/)
assert.match(contextService, /listEnabledEquipmentOptionsByItemCodes/)
assert.doesNotMatch(contextService, /source\.getEquipmentOptions\(\)/)
assert.match(contextService, /selectedEquipmentNumber/)
assert.match(contextService, /MesPqcInspectionPieceDetailDO\.builder\(\)[\s\S]*selectedEquipmentCode/)

assert.match(releaseWriter, /getSelectedEquipmentName\(\)/)
assert.match(releaseWriter, /getSelectedEquipmentNumber\(\)/)
assert.doesNotMatch(releaseWriter, /MesQaInspectionRegulationItemEquipment|matchesQaEquipment|hashEquipment/)

console.log('PASS: live tenant equipment config is the runtime source; release uses submitted snapshots')
