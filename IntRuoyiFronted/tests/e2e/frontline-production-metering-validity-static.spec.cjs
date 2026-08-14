const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const repositoryRoot = path.resolve(frontendRoot, '..')
const panelPath = path.join(frontendRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const migrationPath = path.join(
  repositoryRoot,
  'IntRuoyiBackend/sql/mysql/20260811_mes_process_pool_uv2_two_device_runtime_config.sql'
)
const panel = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

assert.match(
  panel,
  /const configuredDeviceCards = computed<ProductionDeviceCard\[]>\(\(\) =>[\s\S]*runtimeConfig\?\.devices[\s\S]*parameters: device\.parameters \|\| \[\]/,
  '光固II设备页签必须直接使用运行配置返回的全部正式设备。'
)
assert.match(
  panel,
  /const visibleDeviceCards = computed\(\(\) => configuredDeviceCards\.value\)/,
  '设备页签不得截断 A05075、A05059 正式设备集合。'
)
assert.doesNotMatch(
  panel,
  /FRONTLINE_PRODUCTION_METERING_VALIDITY_DEVICE_CODES|appendProductionMeteringValidityParameter|submitAsReading/,
  '光固II设备和计量效期参数不得由前端按设备编码合成。'
)
assert.match(panel, /const isBooleanParameter =/, '一线页面必须识别正式 BOOLEAN 参数。')
assert.match(
  panel,
  /v-else-if="isBooleanParameter\(parameter\)"[\s\S]*type="checkbox"[\s\S]*data-frontline-boolean-parameter[\s\S]*updateProductionDeviceBooleanParameter/,
  '非计量效期 BOOLEAN 参数仍必须在当前设备参数区渲染为 checkbox。'
)
assert.match(
  panel,
  /const getProductionDeviceDetailParameters = \(device\?: ProductionDeviceCard\) =>\s*getProductionSubmittableParameters\(device\)\.filter\(\s*\(parameter\) => !isProductionDeviceMeteringValidityParameter\(parameter\)\s*\)/,
  '正式计量效期参数必须从设备参数列表移除，但不能从可提交参数集合删除。'
)
assert.match(
  panel,
  /data-frontline-device-metering-validity[\s\S]*syncProductionDeviceMeteringValidityParameterDraft/,
  '逐设备计量效期必须由设备卡片 checkbox 操作并同步到正式参数草稿。'
)
assert.match(
  panel,
  /if \(isBooleanParameter\(parameter\)\)[\s\S]*value:\s*booleanValue\s*\?\s*1\s*:\s*0/,
  '正式计量效期 checkbox 必须按未选 0、已选 1 提交。'
)
assert.match(
  panel,
  /type ProductionClearanceConfirmationKey = 'workplace' \| 'validity' \| 'material' \| 'cleaning'[\s\S]*key: 'validity'[\s\S]*label: '效期'/,
  '底部全局效期确认含义不同，必须完整保留。'
)

assert.ok(fs.existsSync(migrationPath), '必须提供光固II A05075/A05059 双设备正式迁移。')
const migration = fs.readFileSync(migrationPath, 'utf8').replace(/\r\n/g, '\n')

assert.match(migration, /光固Ⅱ/, '迁移必须限定光固II工序。')
assert.match(migration, /A05075/, '迁移必须保留 A05075 正式设备。')
assert.match(migration, /A05059/, '迁移必须补齐 A05059 正式设备。')
assert.match(
  migration,
  /INSERT INTO `mes_pro_process_pool_team_process_device`/,
  'A05059 必须写入正式工序设备绑定。'
)
assert.match(
  migration,
  /INSERT INTO `mes_pro_process_pool_device_parameter_rule`/,
  'A05059 必须按光固II正式参数规则补齐参数。'
)
assert.match(migration, /'METERING_VALID'/, '两台光固II设备必须包含正式计量效期参数。')
assert.match(
  migration,
  /COUNT\(DISTINCT device\.`device_code`\)[\s\S]*<> 2/,
  '迁移后置校验必须确认光固II设备数为 2。'
)

console.log('PASS: UV curing II formally exposes A05075 and A05059 with metering validity')
