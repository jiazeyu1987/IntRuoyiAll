const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../../..')
const service = fs.readFileSync(path.join(root,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteFlowConfigServiceImpl.java'), 'utf8')
const controller = fs.readFileSync(path.join(root,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/MesProRouteFlowConfigController.java'), 'utf8')

assert.match(controller, /process-device-parameters[\s\S]*routeVersionId[\s\S]*routeProcessId/)
assert.match(controller, /process-device-parameter-rule\/delete/)
assert.match(service, /saveProductionProcessConfigs\([\s\S]*expectedSnapshotSha256[\s\S]*saveConfigSnapshots\(/)
assert.match(service, /productionProcessConfigs[\s\S]*deviceSelectionGroups[\s\S]*parameterRules/)

const saveStart = service.indexOf('saveRouteProcessDeviceParameterRule')
const saveEnd = service.indexOf('\n    @Override', saveStart + 20)
const saveBlock = service.slice(saveStart, saveEnd > saveStart ? saveEnd : undefined)
assert.doesNotMatch(saveBlock, /deviceParameterRuleMapper\.(insert|updateById)/)

console.log('PASS: route device parameters are candidate-version owned')
