const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const source = readUtf8('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')

const extractBlock = (startNeedle, endNeedle) => {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `missing block start: ${startNeedle}`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.ok(end > start, `missing block end: ${endNeedle}`)
  return source.slice(start, end)
}

const devicePanel = extractBlock(
  'frontline-production-device-panel',
  'data-production-clearance-confirmations'
)

assert.match(
  source,
  /PRESSURE_PUMP_DETECTION_DEVICE_CODE\s*=\s*'G01143'/,
  '检测工序生产自检说明必须限定到正式 G01143 检测设备。'
)
assert.match(
  source,
  /PRESSURE_PUMP_DETECTION_PROCESS_KEYWORD\s*=\s*'检测'/,
  '检测工序生产自检说明必须同时限定当前检测工序，不能影响同设备其它上下文。'
)
assert.match(
  source,
  /PRESSURE_PUMP_ASSEMBLY_I_DEVICE_CODE\s*=\s*'C01017'/,
  '组装Ⅰ工序生产自检说明必须限定到正式 C01017 设备。'
)
assert.match(
  source,
  /PRESSURE_PUMP_ASSEMBLY_I_PROCESS_KEYWORD\s*=\s*'组装Ⅰ'/,
  '组装Ⅰ工序生产自检说明必须同时限定当前组装Ⅰ工序。'
)
assert.match(
  source,
  /压力泵整体外观无黑点、杂质、花纹、划痕等外观缺陷；气密性检测合格。/,
  '生产自检说明必须包含用户指定的合格标准。'
)
assert.match(
  source,
  /外观检测方法：对组装完成的球囊扩张压力泵产品进行外观检测/,
  '生产自检说明必须包含外观检测方法。'
)
assert.match(
  source,
  /低压检验：将整体组装检测合格的压力泵装在气密性检测工装上/,
  '生产自检说明必须包含低压气密性检测方法。'
)
assert.match(
  source,
  /高压检测：将低压检测合格的压力泵装到气密性检测工装上/,
  '生产自检说明必须包含高压气密性检测方法。'
)
assert.match(
  source,
  /1、撤压检测：产品加压至规定压力，将产品放至撤压机（气压：2ATM，缸径20MM）上应能顺利撤压；/,
  '组装Ⅰ C01017 生产自检说明必须包含撤压检测合格标准。'
)
assert.match(
  source,
  /2、耐压检测：产品加压至规定压力，无跳压情况。/,
  '组装Ⅰ C01017 生产自检说明必须包含耐压检测合格标准。'
)
assert.match(
  source,
  /将待检推杆与专用套筒（吸入10m检测用纯化水）组装，将压力打至20-25ATM，用撤压机撤压；/,
  '组装Ⅰ C01017 生产自检说明必须包含撤压检验方法。'
)
assert.match(
  source,
  /将撤压合格推杆继续打压至30ATM。/,
  '组装Ⅰ C01017 生产自检说明必须包含耐压检验方法。'
)
assert.match(
  source,
  /PRESSURE_PUMP_PRODUCTION_SELF_CHECK_NARRATIVE_RULES[\s\S]*PRESSURE_PUMP_DETECTION_DEVICE_CODE[\s\S]*PRESSURE_PUMP_ASSEMBLY_I_DEVICE_CODE/,
  '生产自检说明规则必须同时覆盖检测 G01143 和组装Ⅰ C01017。'
)

assert.match(
  devicePanel,
  /data-frontline-production-self-check/,
  '生产填写设备红框区域必须提供稳定锚点显示生产自检说明。'
)
assert.match(
  devicePanel,
  /v-if="activeProductionSelfCheckNarrative"/,
  '生产自检说明必须由当前正式设备和工序计算状态控制显示。'
)
assert.match(
  devicePanel,
  /<template\s+v-else>[\s\S]*v-for="parameter in getProductionDeviceDetailParameters\(activeProductionDevice\)"/,
  '只有不显示生产自检说明时才渲染通用设备参数列表。'
)

const validationBlock = extractBlock(
  'const assertProductionSubmissionReady = () => {',
  'const buildProductionFormalSubmitConfirmation'
)
assert.match(
  validationBlock,
  /getProductionSubmittableParameters\(device\)/,
  '生产自检说明设备不得继续要求填写隐藏的设备参数。'
)

const parameterPayloadBlock = extractBlock(
  'const buildProductionDeviceParameterPayload = (deviceKey: string) => {',
  'const buildProductionLossDetailsPayload'
)
assert.match(
  parameterPayloadBlock,
  /getProductionSubmittableParameters\(device\)/,
  '生产自检说明设备仍需保留设备卡片计量效期确认。'
)

const readingsPayloadBlock = extractBlock(
  'const buildProductionDeviceParameterReadingsPayload =',
  'const buildProductionEquipmentParameterRulesPayload'
)
assert.match(
  readingsPayloadBlock,
  /getProductionSubmittableParameters\(device\)/,
  '生产自检说明设备不得把隐藏参数写入正式 deviceParameterReadings。'
)

console.log('pressure-pump-production-self-check-text-static PASS')
