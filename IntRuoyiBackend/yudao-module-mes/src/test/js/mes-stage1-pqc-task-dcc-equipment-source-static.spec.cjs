const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../../../..')
const servicePath = path.join(
  root,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderSimulationService.java'
)
const taskDoPath = path.join(
  root,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/pqc/MesPqcInspectionTaskDO.java'
)
const activeOrderServicePath = path.join(
  root,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java'
)

const service = fs.readFileSync(servicePath, 'utf8')
const taskDo = fs.readFileSync(taskDoPath, 'utf8')
const activeOrderService = fs.readFileSync(activeOrderServicePath, 'utf8')

assert.match(taskDo, /private\s+Long\s+dccProjectCodeId\s*;/,
  'PQC task DO must expose dccProjectCodeId from the existing table schema')
assert.match(taskDo, /private\s+String\s+dccProjectCode\s*;/,
  'PQC task DO must expose dccProjectCode snapshot')
assert.match(taskDo, /private\s+String\s+dccProjectName\s*;/,
  'PQC task DO must expose dccProjectName snapshot')
assert.match(taskDo, /private\s+Long\s+qaRegulationId\s*;/,
  'PQC task DO must expose qaRegulationId snapshot')
assert.match(taskDo, /private\s+String\s+qaRegulationVersionNo\s*;/,
  'PQC task DO must expose qaRegulationVersionNo snapshot')

assert.match(activeOrderService, /\.dccProjectCodeId\(regulation\.getDccProjectCodeId\(\)\)/,
  'Generated PQC tasks must persist their source regulation DCC id')
assert.match(activeOrderService, /\.qaRegulationId\(regulation\.getId\(\)\)/,
  'Generated PQC tasks must persist their source QA regulation id')
assert.match(activeOrderService, /\.qaRegulationVersionNo\(version\.getVersionNo\(\)\)/,
  'Generated PQC tasks must persist their source QA version number')

const resolverMatch = service.match(
  /private Long resolvePqcTaskDccProjectCodeId[\s\S]*?\n    }\n/
)
assert.ok(resolverMatch, 'Stage1 simulation must resolve DCC from the locked PQC task regulation version')
assert.match(resolverMatch[0], /inspectionRegulationVersionMapper\.selectById\(task\.getRegulationVersionId\(\)\)/,
  'Resolver must load the locked regulation version from the task')
assert.match(resolverMatch[0], /inspectionRegulationMapper\.selectById\(version\.getRegulationId\(\)\)/,
  'Resolver must load the source QA regulation for the version')
assert.match(resolverMatch[0], /regulation\.getDccProjectCodeId\(\)/,
  'Resolver must return the source regulation DCC id')

const equipmentBlock = service.match(
  /private PqcEquipment resolveDefaultPqcEquipment[\s\S]*?private Long requirePqcWorkstation/
)
assert.ok(equipmentBlock, 'resolveDefaultPqcEquipment block must exist')
assert.match(equipmentBlock[0], /Long dccProjectCodeId = resolvePqcTaskDccProjectCodeId\(task\);/,
  'PQC equipment lookup must use task-source DCC')
assert.match(equipmentBlock[0],
  /listEnabledEquipmentOptionsByProjectVersionAndItemCodes\(\s*dccProjectCodeId,\s*task\.getRegulationVersionId\(\),\s*List\.of\(itemCode\)\)/,
  'PQC equipment lookup must pass task-source DCC with the locked regulation version')
assert.doesNotMatch(equipmentBlock[0], /activeOrder\.getDccProjectCodeId\(\)/,
  'PQC equipment lookup must not use active order product DCC')

console.log('mes-stage1-pqc-task-dcc-equipment-source-static: PASS')
