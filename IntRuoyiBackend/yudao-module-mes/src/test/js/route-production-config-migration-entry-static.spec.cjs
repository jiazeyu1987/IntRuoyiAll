const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '../../..')
const read = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')

const request = read(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/vo/version/MesProRouteVersionCreateReqVO.java'
)
const workflow = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteVersionWorkflowServiceImpl.java'
)
const processConfigService = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderProcessConfigServiceImpl.java'
)
const processConfigDevice = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderProcessConfigDevice.java'
)

assert.match(request, /Boolean migrateLegacyProductionConfig/, 'RED: candidate request needs explicit migration flag')
assert.match(request, /BigDecimal missingOveragePercent/, 'RED: candidate request needs explicit missing-overage value')
assert.match(
  workflow,
  /Boolean\.TRUE\.equals\(reqVO\.getMigrateLegacyProductionConfig\(\)\)[\s\S]*migrateLegacyProductionConfigs/,
  'RED: only the explicit flag may invoke formal production-config migration'
)
for (const field of ['routeProcessId', 'processId', 'overagePercent', 'lossReasons', 'deviceSelectionGroups', 'parameterRules']) {
  assert.ok(workflow.includes(`"${field}"`), `RED: migrated snapshot missing ${field}`)
}
assert.match(processConfigDevice, /String deviceGroupKey[\s\S]*String selectionMode/, 'RED: formal projection must preserve device group identity')
assert.match(
  processConfigService,
  /setDeviceGroupKey\(binding\.getDeviceGroupKey\(\)\)[\s\S]*setSelectionMode\(binding\.getSelectionMode\(\)\)/,
  'RED: process config service must project formal device group key and mode'
)
assert.match(
  workflow,
  /canonicalizeMigratedParameterRule/,
  'RED: migration must canonicalize stale type-incompatible parameter fields before validation'
)

console.log('GREEN: explicit formal production-config migration backend contract is present')
