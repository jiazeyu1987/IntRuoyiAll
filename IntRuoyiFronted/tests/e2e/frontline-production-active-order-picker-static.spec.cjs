const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
)
const context = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts'),
  'utf8'
)
const api = fs.readFileSync(
  path.join(root, 'src/api/mes/pro/feedback/index.ts'),
  'utf8'
)

const productionStart = panel.indexOf('data-frontline-production-operator')
const productionEnd = panel.indexOf('<main class="frontline-operator-main', productionStart)
assert.ok(productionStart >= 0 && productionEnd > productionStart, '必须存在一线生产顶部区域')
const productionHeader = panel.slice(productionStart, productionEnd)

const orderIndex = productionHeader.indexOf('data-frontline-production-active-order-card')
const processIndex = productionHeader.indexOf('data-frontline-production-process-nav-card')
const employeeIndex = productionHeader.indexOf('data-frontline-production-employee-card')
const fullscreenIndex = productionHeader.indexOf('data-production-fullscreen-toggle')
assert.ok(orderIndex >= 0, '一线生产顶部必须有活跃订单选择入口')
assert.equal(
  (productionHeader.match(/data-frontline-production-selection-card/g) || []).length,
  3,
  '活跃订单、工序、员工必须全部纳入一线生产顶部选择区域契约'
)
assert.ok(
  orderIndex < processIndex && processIndex < employeeIndex && employeeIndex < fullscreenIndex,
  '一线生产顶部必须按活跃订单、工序、员工、最大化排列'
)
assert.match(
  productionHeader,
  /activePicker === 'order'[\s\S]*activeOrderKeyword/,
  '一线生产订单弹框必须复用订单搜索输入'
)
assert.match(panel, /const filteredActiveOrderOptions = computed[\s\S]*activeOrderKeyword/)
assert.match(panel, /activePicker\.value === 'order'[\s\S]*filteredActiveOrderOptions\.value/)
assert.match(context, /export const loadFrontlineProductionActiveOrders/)
assert.match(api, /getFrontlineProductionActiveOrders[\s\S]*device-account\/active-orders/)

console.log('PASS: frontline production active-order picker static contract')
