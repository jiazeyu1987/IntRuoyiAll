const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = process.cwd()
const workspaceRoot = path.resolve(frontendRoot, '..')
const backendRoot = path.join(workspaceRoot, 'IntRuoyiBackend')

const readFrontend = (relativePath) => fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')
const readBackend = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')

const page = readFrontend('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const api = readFrontend('src/api/mes/pro/processpool/teamLeader.ts')
const activeOrderVo = readBackend(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderRespVO.java'
)
const activeOrderRow = readBackend(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderRow.java'
)
const controller = readBackend(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'
)

const sliceFunction = (source, functionName) => {
  const start = source.indexOf(`const ${functionName}`)
  assert.notEqual(start, -1, `Expected ${functionName} in TeamLeaderWorkbenchPage.vue`)
  const nextConst = source.indexOf('\nconst ', start + 1)
  assert.notEqual(nextConst, -1, `Expected next const after ${functionName}`)
  return source.slice(start, nextConst)
}

const sliceAllocationSelect = () => {
  const marker = 'data-team-leader-allocation-table'
  const markerIndex = page.indexOf(marker)
  assert.notEqual(markerIndex, -1, 'Expected allocation table marker.')
  const selectStart = page.indexOf('<el-select', markerIndex)
  const selectEnd = page.indexOf('</el-select>', selectStart)
  assert.notEqual(selectStart, -1, 'Expected allocation active-order select.')
  assert.notEqual(selectEnd, -1, 'Expected allocation active-order select end.')
  return page.slice(selectStart, selectEnd)
}

const sliceScopedStyle = () => {
  const styleStart = page.indexOf('<style scoped>')
  const styleEnd = page.indexOf('</style>', styleStart)
  assert.notEqual(styleStart, -1, 'Expected scoped style block.')
  assert.notEqual(styleEnd, -1, 'Expected scoped style block end.')
  return page.slice(styleStart, styleEnd)
}

for (const field of [
  'workOrderCode?: string',
  'productName?: string',
  'productCode?: string',
  'quantity?: number | string'
]) {
  assert.match(api, new RegExp(field.replace(/[?|]/g, '\\$&')), `Frontend active-order type must expose ${field}.`)
}

for (const field of [
  'private String workOrderCode;',
  'private String productName;',
  'private String productCode;',
  'private BigDecimal quantity;'
]) {
  assert.match(activeOrderVo, new RegExp(field.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `Response VO must expose ${field}.`)
  assert.match(activeOrderRow, new RegExp(field.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `Read model must expose ${field}.`)
}

for (const setter of [
  'setWorkOrderCode(activeOrder.getWorkOrderCode())',
  'setProductName(activeOrder.getProductName())',
  'setProductCode(activeOrder.getProductCode())',
  'setQuantity(activeOrder.getQuantity())'
]) {
  assert.match(controller, new RegExp(setter.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `Controller must map ${setter}.`)
}

const optionHelpersStart = page.indexOf('const formatActiveOrderCode')
const optionHelpersEnd = page.indexOf('const formatTraceQuantity', optionHelpersStart)
assert.notEqual(optionHelpersStart, -1, 'Expected formatActiveOrderCode helper.')
assert.notEqual(optionHelpersEnd, -1, 'Expected formatTraceQuantity after active-order helpers.')
const optionFormatter = page.slice(optionHelpersStart, optionHelpersEnd)
assert.doesNotMatch(
  optionFormatter,
  /订单\s*\$\{order\.workOrderId\}\s*\/\s*活跃池\s*\$\{order\.id\}/,
  '手动分配候选不得继续展示生产订单 ID / 活跃池 ID。'
)
assert.doesNotMatch(
  optionFormatter,
  /order\.(workOrderId|id)\b/,
  '手动分配候选 formatter 不得再读取内部 ID 作为可见文案。'
)
assert.match(optionFormatter, /order\.workOrderCode/, '活跃订单候选主展示必须使用正式订单编号。')
assert.match(optionFormatter, /order\.productName|order\.productCode/, '活跃订单候选必须展示正式产品名称或编码。')
assert.match(optionFormatter, /order\.quantity|order\.erpFixedQuantitySnapshot/, '活跃订单候选必须展示正式数量。')

const allocationSelect = sliceAllocationSelect()
assert.match(
  allocationSelect,
  /v-for="order in getAvailableAllocationOrderOptions\(row\)"[\s\S]*data-team-leader-active-order-option/,
  '手动分配活跃订单下拉必须渲染当前行仍可选择的业务信息选项。'
)
assert.match(
  allocationSelect,
  /popper-class="team-leader-workbench__allocation-order-popper"/,
  '分配活跃订单下拉必须使用专属 popper class，避免多行选项仍套用 Element Plus 单行高度。'
)
for (const label of ['编码', '产品', '数量']) {
  assert.match(allocationSelect, new RegExp(label), `下拉选项必须显示 ${label}。`)
}
assert.match(
  allocationSelect,
  /:value="order\.id"/,
  '下拉提交值必须继续使用 activeOrderId。'
)

const scopedStyle = sliceScopedStyle()
const allocationItemStyleMatch = scopedStyle.match(
  /:global\(\.team-leader-workbench__allocation-order-popper\s+\.el-select-dropdown__item\)\s*\{([\s\S]*?)\n\}/
)
assert.ok(allocationItemStyleMatch, '分配活跃订单下拉必须声明专属 el-option 样式块。')
const allocationItemStyle = allocationItemStyleMatch[1]
assert.match(allocationItemStyle, /height:\s*auto/, '分配活跃订单下拉选项必须解除默认 height。')
assert.match(allocationItemStyle, /line-height:\s*normal/, '分配活跃订单下拉选项必须解除默认 line-height。')
assert.match(allocationItemStyle, /min-height:\s*68px/, '分配活跃订单下拉多行选项必须保留最小高度。')
assert.match(
  scopedStyle,
  /:global\(\.team-leader-workbench__allocation-order-popper\s+\.el-select-dropdown__item\s+\+ \.el-select-dropdown__item\)\s*\{[\s\S]*border-top:/,
  '分配活跃订单下拉多行候选之间必须有明确分隔，避免视觉粘连。'
)
