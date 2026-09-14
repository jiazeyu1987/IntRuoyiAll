const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const controller = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/MesProRouteFlowConfigController.java')
const service = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteFlowConfigServiceImpl.java')
const migration = read('sql/mysql/20260909_mes_route_owned_device_parameter_rules.sql')
const submitService = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceImpl.java')

assert.match(
  controller,
  /@GetMapping\("\/process-device-parameters"\)[\s\S]*getRouteProcessDeviceParameterConfig/,
  'Route flow controller must expose route process device parameter query.'
)
assert.match(
  controller,
  /@PostMapping\("\/process-device-parameter-rule\/save"\)[\s\S]*saveRouteProcessDeviceParameterRule/,
  'Route flow controller must expose route process device parameter save.'
)
assert.match(
  service,
  /saveProductionProcessConfigs[\s\S]*routeCandidateConfigService\.saveConfigSnapshots/,
  'Route-owned device parameter save must update the candidate route snapshot.'
)
assert.doesNotMatch(
  submitService,
  /\.in\(MesProcessPoolDeviceParameterRuleDO::getLeaderUserId/,
  'Frontline snapshot parameter validation must not query route rules by production leader.'
)
assert.match(
  migration,
  /MODIFY COLUMN `leader_user_id` bigint DEFAULT NULL COMMENT '最后维护人用户ID，设备参数规则归属工艺路线工序'/,
  'Migration must make device parameter leader_user_id nullable and document route ownership.'
)

console.log('PASS: route flow route-owned device parameter backend static contract')
