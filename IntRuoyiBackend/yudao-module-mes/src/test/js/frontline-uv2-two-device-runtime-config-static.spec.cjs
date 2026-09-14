const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repositoryRoot = path.resolve(__dirname, '../../../../..')
const frontendPanelPath = path.join(
  repositoryRoot,
  'IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const runtimeServicePath = path.join(
  repositoryRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRuntimeConfigServiceImpl.java'
)
const migrationRelativePath =
  'IntRuoyiBackend/sql/mysql/20260811_mes_process_pool_uv2_two_device_runtime_config.sql'
const migrationPath = path.join(repositoryRoot, migrationRelativePath)

const panel = fs.readFileSync(frontendPanelPath, 'utf8').replace(/\r\n/g, '\n')
const runtimeService = fs.readFileSync(runtimeServicePath, 'utf8').replace(/\r\n/g, '\n')

assert.match(
  panel,
  /const configuredDeviceCards = computed<ProductionDeviceCard\[]>\(\(\) =>[\s\S]*runtimeConfig\?\.devices[\s\S]*parameters: device\.parameters \|\| \[\]/,
  '前端必须直接渲染运行配置返回的全部正式设备及其参数。'
)
assert.match(
  panel,
  /const visibleDeviceCards = computed\(\(\) => configuredDeviceCards\.value\)/,
  '设备页签不得截断正式运行配置设备列表。'
)
assert.doesNotMatch(
  panel,
  /FRONTLINE_PRODUCTION_METERING_VALIDITY_DEVICE_CODES|appendProductionMeteringValidityParameter|submitAsReading/,
  '光固II设备和参数必须来自正式绑定，不得按设备编码在前端合成。'
)
assert.match(
  runtimeService,
  /listProcessDeviceBindings\(process\.processId\(\)\)[\s\S]*toDeviceOptions\(processDeviceBindings, process, leaderUserIds\)/,
  '后端运行配置必须从正式工序设备绑定构建设备列表。'
)
assert.match(
  runtimeService,
  /for \(MesProcessPoolTeamProcessDeviceDO binding : processDeviceBindings\)[\s\S]*emittedDeviceIds\.add\(device\.getId\(\)\)[\s\S]*new MesFrontlineTeamDeviceOption/,
  '后端必须把每个不同的启用正式设备绑定转换为运行态设备。'
)

assert.ok(fs.existsSync(migrationPath), '必须提供光固II A05075/A05059 双设备正式迁移。')
const migration = fs.readFileSync(migrationPath, 'utf8').replace(/\r\n/g, '\n')

assert.match(
  migration,
  /release-migration:[^\n]*dependsOn=20260810_mes_process_pool_device_parameter_select_options;\s*type=data;/,
  '迁移必须声明正式依赖和 data 类型。'
)
assert.match(migration, /光固Ⅱ/, '迁移必须限定光固II工序。')
assert.match(migration, /A05075/, '迁移必须以现有 A05075 正式绑定为来源。')
assert.match(migration, /A05059/, '迁移必须补齐 A05059 正式设备。')
assert.match(migration, /SIGNAL SQLSTATE '45000'/, '正式前置缺失时迁移必须 fail fast。')
assert.match(
  migration,
  /INSERT INTO \x60mes_pro_process_pool_team_process_device\x60/,
  '迁移必须写入正式工序设备绑定表。'
)
assert.match(
  migration,
  /INSERT INTO \x60mes_pro_process_pool_device_parameter_rule\x60/,
  '迁移必须为 A05059 补齐当前光固II正式设备参数规则。'
)
assert.match(migration, /'METERING_VALID'/, '光固II两台设备都必须包含计量效期正式参数。')
assert.match(
  migration,
  /COUNT\(DISTINCT device\.\x60device_code\x60\)[\s\S]*<> 2/,
  '迁移后置校验必须确认光固II正式设备数为 2。'
)
assert.match(
  migration,
  /DECLARE EXIT HANDLER FOR SQLEXCEPTION[\s\S]*ROLLBACK;[\s\S]*RESIGNAL;/,
  '迁移必须在失败时回滚并暴露错误。'
)
assert.match(
  migration,
  /START TRANSACTION;[\s\S]*UPDATE \x60mes_pro_process_pool_team_process_device\x60[\s\S]*CALL postflight_mes_pp_uv2_two_device_runtime_config\(\);[\s\S]*COMMIT;/,
  '光固II关联 DML 必须在同一事务中执行，且后置校验通过后才提交。'
)
assert.match(migration, /NOT EXISTS/, '迁移必须按正式业务键幂等写入。')
assert.doesNotMatch(migration, /\bDELETE\b|\bTRUNCATE\b/i, '迁移不得删除业务数据。')

console.log('frontline-uv2-two-device-runtime-config-static PASS')
