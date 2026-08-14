const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const panelPath = path.join(
  frontendRoot,
  'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const panel = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

assert.match(
  panel,
  /const PRODUCTION_DEVICE_METERING_VALIDITY_PARAMETER_CODES = new Set\(\[\s*'METERING_VALID',\s*'METERING_VALIDITY_WITHIN_PERIOD'\s*\]\)/,
  '逐设备计量效期必须按正式参数编码识别，并覆盖已存在的兼容编码。'
)
assert.match(
  panel,
  /const isProductionDeviceMeteringValidityParameter = \([\s\S]*PRODUCTION_DEVICE_METERING_VALIDITY_PARAMETER_CODES\.has\(\s*parameter\.parameterCode\.trim\(\)\.toUpperCase\(\)\s*\)/,
  '计量效期参数识别必须基于规范化 parameterCode，不能按设备编码或可见文案猜测。'
)
assert.match(
  panel,
  /const getProductionSubmittableParameters = \(device\?: ProductionDeviceCard\) => \{[\s\S]*return device\.parameters[\s\S]*\}\s*\n\s*const getProductionDeviceDetailParameters = \(device\?: ProductionDeviceCard\) =>\s*getProductionSubmittableParameters\(device\)\.filter\(\s*\(parameter\) => !isProductionDeviceMeteringValidityParameter\(parameter\)\s*\)/,
  '显示参数必须过滤逐设备计量效期，但正式可提交参数集合必须保持完整。'
)
assert.match(
  panel,
  /const syncProductionDeviceMeteringValidityParameterDraft = \([\s\S]*const params = deviceParameterDraft\[deviceKey\][\s\S]*getProductionDeviceMeteringValidityParameters\(device\)[\s\S]*params\[parameter\.parameterCode\] = checked/,
  '设备卡片状态必须同步到该设备的全部正式计量效期参数草稿。'
)
assert.match(
  panel,
  /const updateProductionDeviceMeteringValidity = \([\s\S]*const checked = \(event\.target as HTMLInputElement\)\.checked[\s\S]*deviceMeteringValidityDraft\[deviceKey\] = checked[\s\S]*syncProductionDeviceMeteringValidityParameterDraft\(deviceKey, checked\)/,
  '切换卡片 checkbox 时必须同时更新卡片状态和正式参数草稿。'
)
assert.match(
  panel,
  /const syncProductionDeviceMeteringValidityDraft = \(devices: ProductionDeviceCard\[\]\) => \{[\s\S]*syncProductionDeviceMeteringValidityParameterDraft\(\s*device\.key,\s*deviceMeteringValidityDraft\[device\.key\] !== false\s*\)/,
  '设备首次加载或重填恢复默认选中时，正式计量效期读数也必须恢复为 true。'
)
assert.match(
  panel,
  /v-else-if="isBooleanParameter\(parameter\)"[\s\S]*data-frontline-boolean-parameter[\s\S]*updateProductionDeviceBooleanParameter/,
  '非计量效期 BOOLEAN 参数的通用控件渲染必须保留。'
)
assert.match(
  panel,
  /data-frontline-device-metering-validity[\s\S]*在计量效期内/,
  '设备卡片下方的逐设备计量效期 checkbox 必须保留。'
)
assert.doesNotMatch(
  panel,
  /key: 'validity'[\s\S]*label: '效期'/,
  '底部全局效期确认项必须删除，只保留设备卡片逐设备计量效期 checkbox。'
)
assert.match(
  panel,
  /buildProductionDeviceParameterReadingsPayload[\s\S]*getProductionSubmittableParameters\(device\)[\s\S]*if \(isBooleanParameter\(parameter\)\)[\s\S]*value: booleanValue \? 1 : 0/,
  '正式提交必须继续包含隐藏计量效期参数的 1/0 读数。'
)

console.log('frontline-production-metering-validity-row-removal-static PASS')
