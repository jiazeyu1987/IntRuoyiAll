const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const backendRoot = path.resolve(__dirname, '../../../..')
const read = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const migration = read('sql/mysql/20260820_mes_pqc_item_equipment_config.sql')
const h2Schema = read('yudao-module-mes/src/test/resources/sql/create_tables.sql')
const sliceBetween = (source, start, end) => {
  const startIndex = source.indexOf(start)
  assert.notEqual(startIndex, -1, `missing start marker: ${start}`)
  const endIndex = source.indexOf(end, startIndex + start.length)
  assert.notEqual(endIndex, -1, `missing end marker: ${end}`)
  return source.slice(startIndex, endIndex)
}
const h2ConfigTables = sliceBetween(
  h2Schema,
  'CREATE TABLE IF NOT EXISTS "mes_pqc_item_equipment_config"',
  'CREATE TABLE IF NOT EXISTS "mes_pqc_process_inspection_aggregate_detail"'
)
const configDo = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/pqc/MesPqcItemEquipmentConfigDO.java'
)
const numberDo = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/pqc/MesPqcItemEquipmentNumberConfigDO.java'
)
const service = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/pqc/MesPqcItemEquipmentConfigServiceImpl.java'
)
const configMapper = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/pqc/MesPqcItemEquipmentConfigMapper.java'
)
const numberConfigMapper = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/pqc/MesPqcItemEquipmentNumberConfigMapper.java'
)
const controller = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/pqc/MesPqcItemEquipmentConfigController.java'
)
const releaseReader = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionReaderImpl.java'
)
const contextService = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java'
)
const releaseWriter = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl.java'
)

for (const source of [migration, configDo, numberDo]) {
  assert.doesNotMatch(source, /regulation_version_id|regulationVersionId/i)
  assert.doesNotMatch(source, /inspection_type|inspectionType/i)
}

assert.match(migration, /CREATE TABLE IF NOT EXISTS `mes_pqc_item_equipment_config`/)
assert.match(migration, /CREATE TABLE IF NOT EXISTS `mes_pqc_item_equipment_number_config`/)
assert.match(h2Schema, /CREATE TABLE IF NOT EXISTS "mes_pqc_item_equipment_config"/)
assert.match(h2Schema, /CREATE TABLE IF NOT EXISTS "mes_pqc_item_equipment_number_config"/)
assert.match(migration, /UNIQUE KEY `uk_mes_pqc_item_equipment` \(`tenant_id`, `item_code`, `equipment_id`, `deleted`\)/)
assert.match(
  migration,
  /UNIQUE KEY `uk_mes_pqc_item_equipment_number` \(`tenant_id`, `item_code`, `equipment_id`, `equipment_number`, `deleted`\)/
)
assert.doesNotMatch(
  h2ConfigTables,
  /regulation_version_id|inspection_type/i
)

assert.match(controller, /@RequestMapping\("\/mes\/pqc\/item-equipment"\)/)
assert.match(controller, /@GetMapping\("\/items"\)/)
assert.match(controller, /@GetMapping\("\/config"\)/)
assert.match(controller, /@PostMapping\("\/config"\)/)
assert.match(service, /replaceItemConfig/)
assert.match(service, /selectListByItemCode/)
assert.match(service, /listEnabledEquipmentOptionsByItemCodes/)
assert.match(service, /itemCode/)
assert.doesNotMatch(service, /leaderUserId/)
assert.match(configMapper, /WHERE tenant_id = #\{tenantId\} AND item_code = #\{itemCode\}/)
assert.match(numberConfigMapper, /WHERE tenant_id = #\{tenantId\} AND item_code = #\{itemCode\}/)

assert.match(contextService, /MesPqcItemEquipmentConfigService/)
assert.match(contextService, /pqcItemEquipmentConfigService\.listEnabledEquipmentOptionsByItemCodes/)
const runtimeBlock = contextService.slice(
  contextService.indexOf('public List<MesFrontlinePqcProcessRespVO> listProcessesByActiveOrder'),
  contextService.indexOf('private MesFrontlinePqcProcessRespVO toPqcProcessRespVO')
)
assert.match(runtimeBlock, /listEnabledEquipmentOptionsByItemCodes/)
const publishedItemBlock = contextService.slice(
  contextService.indexOf('private static MesFrontlinePqcProcessRespVO.PqcInspectionItem buildPublishedInspectionItemResponse'),
  contextService.indexOf('private static List<MesFrontlinePqcInspectionItem> toOverlayInspectionItems')
)
assert.match(publishedItemBlock, /item\.setEquipmentRequired\(!equipmentOptions\.isEmpty\(\)\)/)
assert.doesNotMatch(publishedItemBlock, /source\.getEquipmentOptions|getEquipmentOptions\(\)\.stream\(\)/)

const submitBlock = contextService.slice(
  contextService.indexOf('private List<MesFrontlinePqcInspectionItem> resolveSubmittedInspectionItems'),
  contextService.indexOf('private Map<ProductionProcessIdentity')
)
assert.match(submitBlock, /listEnabledEquipmentOptionsByItemCodes/)
assert.doesNotMatch(submitBlock, /regulationItemEquipmentMapper|MesQaInspectionRegulationItemEquipmentDO/)
const submitInspectionItemBuilder = contextService.slice(
  contextService.indexOf('private static MesFrontlinePqcInspectionItem buildInspectionItem'),
  contextService.indexOf('private static MesFrontlinePqcInspectionItem.EquipmentOption toEquipmentOption')
)
assert.match(submitInspectionItemBuilder, /boolean equipmentRequired = CollUtil\.isNotEmpty\(equipmentOptions\)/)

assert.doesNotMatch(releaseReader, /MesQaInspectionRegulationItemEquipment|regulationItemEquipmentMapper|getRegulationItemEquipment/)
const releaseWriterImports = releaseWriter.slice(0, releaseWriter.indexOf('public class '))
assert.doesNotMatch(releaseWriterImports, /MesQaInspectionRegulationItemEquipment/)
assert.doesNotMatch(releaseWriter, /getRegulationItemEquipment|hashEquipment|regulationItemEquipment|matchesQaEquipment/)
assert.match(releaseWriter, /getSelectedEquipmentName\(\)/)
assert.match(releaseWriter, /getSelectedEquipmentNumber\(\)/)

console.log('PASS: tenant-level PQC item equipment config is the only equipment source')
