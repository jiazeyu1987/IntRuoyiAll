const fs = require('fs')
const path = require('path')
const assert = require('assert')

const moduleRoot = path.resolve(__dirname, '..', '..')
const saveVo = fs.readFileSync(
  path.join(
    moduleRoot,
    'main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/vo/MesQaInspectionRegulationSaveReqVO.java'
  ),
  'utf8'
)
const responseVo = fs.readFileSync(
  path.join(
    moduleRoot,
    'main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/vo/MesQaInspectionRegulationPublishedVersionRespVO.java'
  ),
  'utf8'
)
const service = fs.readFileSync(
  path.join(
    moduleRoot,
    'main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java'
  ),
  'utf8'
)

assert(
  /class InspectionItem[\s\S]*private List<EquipmentOption> equipmentOptions;/.test(saveVo),
  'RED: QA/common regulation save item must accept equipment options'
)
assert(
  /public static class EquipmentOption[\s\S]*private Long equipmentId;[\s\S]*private String equipmentCode;[\s\S]*private String equipmentName;[\s\S]*private String equipmentNumber;[\s\S]*private Boolean defaultFlag;[\s\S]*private Integer sort;/.test(
    saveVo
  ),
  'RED: save VO equipment option must carry device identity, number, default flag, and sort'
)
assert(
  /equipmentOptions\(normalizeInspectionItemEquipmentOptions\(snapshotItem,\s*versionId\)\)/.test(service),
  'RED: published response must read saved equipment options instead of returning an empty list'
)
assert(
  /private static List<MesQaInspectionRegulationPublishedVersionRespVO\.EquipmentOption>\s+normalizeInspectionItemEquipmentOptions/.test(
    service
  ),
  'RED: service must normalize item equipment options from the saved snapshot'
)
assert(
  /equipmentOptions\(List\.of\(\)\)/.test(service) === false,
  'RED: published response must not discard saved equipment options'
)
assert(
  /class InspectionItem[\s\S]*private List<EquipmentOption> equipmentOptions;/.test(responseVo),
  'RED: response VO must keep returning equipment options to the frontend'
)

console.log('GREEN: common QA equipment options save/read contract is present')
