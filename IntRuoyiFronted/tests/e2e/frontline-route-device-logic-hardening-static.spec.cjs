const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
).replace(/\r\n/g, '\n')
const routeDesigner = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
).replace(/\r\n/g, '\n')

assert.match(
  panel,
  /inMeteringValidityPeriod:\s*meteringValidityDraft\[device\.key\] !== false/,
  '逐物料 selectedDevices 必须携带对应设备计量状态。'
)
assert.match(
  panel,
  /deviceMeteringValidity:\s*buildProductionDeviceMeteringValidityForSubmitScope\(materialDetails\)/,
  'rawPayload 顶层设备计量状态必须从实际提交物料聚合。'
)

const normalizeStart = panel.indexOf('function normalizeProductionParameter')
const normalizeEnd = panel.indexOf('const toFiniteProductionParameterNumber', normalizeStart)
assert.ok(normalizeStart >= 0 && normalizeEnd > normalizeStart, '必须能定位生产参数数值规范化函数。')
const normalizeBlock = panel.slice(normalizeStart, normalizeEnd)
assert.doesNotMatch(normalizeBlock, /Math\.max\(0, scaled\)/, '合法负数设备参数不得被强制改成零。')

assert.match(
  routeDesigner,
  /syncProductionProcessConfigJsonDraftFromCache[\s\S]*refreshProductionProcessConfigSnapshot/,
  '结构化设备参数保存后必须同步刷新生产配置 JSON 草稿和基线。'
)
assert.match(
  routeDesigner,
  /const saved = await saveSelectedRouteProcessDeviceParameterRule[\s\S]*if \(saved\)[\s\S]*routeProcessDeviceParameterDialogVisible\.value = false/,
  '设备参数保存失败时弹框必须保持打开。'
)

const loadStart = routeDesigner.indexOf('const loadSelectedRouteProcessDeviceParameterConfig = async () => {')
const loadEnd = routeDesigner.indexOf('const refreshProductionProcessConfigSnapshot', loadStart)
assert.ok(loadStart >= 0 && loadEnd > loadStart, '必须能定位设备参数加载函数。')
assert.doesNotMatch(
  routeDesigner.slice(loadStart, loadEnd),
  /message\.error\([\s\S]*throw error/,
  '设备参数加载错误已经显示后不得继续抛成全局系统异常。'
)

console.log('PASS: frontline route device logic hardening frontend contract')
