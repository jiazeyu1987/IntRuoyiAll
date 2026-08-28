const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
const realFlowPath = path.resolve(
  process.cwd(),
  'tests/e2e/mes-pro-schedule-order-manual-finish-real-flow.e2e.js'
)

const pageSource = fs.readFileSync(pagePath, 'utf8')
const realFlowSource = fs.readFileSync(realFlowPath, 'utf8')

const forceActionStart = pageSource.indexOf(
  'v-hasPermi="[\'mes:pro-schedule-order:manual-finish\']"'
)
const forceActionEnd = pageSource.indexOf('</el-button>', forceActionStart)
assert(forceActionStart >= 0 && forceActionEnd > forceActionStart, '完成操作按钮必须存在。')
const forceActionSource = pageSource.slice(forceActionStart, forceActionEnd)
assert(forceActionSource.includes('完成'), '有权限人员执行的关闭动作用户可见名称必须显示为“完成”。')
assert(!forceActionSource.includes('强制完成'), '操作列完成按钮不得继续显示“强制完成”。')

const revokeActionStart = pageSource.indexOf(
  'v-hasPermi="[\'mes:pro-schedule-order:revoke-complete\']"'
)
const revokeActionEnd = pageSource.indexOf('</el-button>', revokeActionStart)
assert(
  revokeActionStart >= 0 && revokeActionEnd > revokeActionStart,
  '撤销完成操作按钮必须存在。'
)
const revokeActionSource = pageSource.slice(revokeActionStart, revokeActionEnd)
assert(revokeActionSource.includes('撤销完成'), '撤销动作用户可见名称必须显示为“撤销完成”。')
assert(!revokeActionSource.includes('撤销强制完成'), '操作列撤销按钮不得继续显示“撤销强制完成”。')

for (const copy of [
  '排产工单完成',
  '撤销排产工单完成',
  '完成原因',
  '撤销完成原因',
  '有权限人员执行的完成操作',
  '完成后汇总按 100% 展示',
  '真实工序进度仍保留',
  '可撤销',
  '撤销后将根据真实工序进度恢复汇总状态',
  '排产工单已完成',
  '排产工单已撤销完成'
]) {
  assert(pageSource.includes(copy), `排产工单页面必须显示：${copy}`)
}

for (const oldCopy of [
  '排产工单人工完成',
  '撤销排产工单人工完成',
  '排产工单强制完成',
  '撤销排产工单强制完成',
  '强制完成',
  '撤销强制完成',
  '强制关闭',
  '设为已完成',
  '撤销已完成',
  '排产工单已强制完成',
  '排产工单已撤销强制完成',
  '排产工单已设为已完成',
  '排产工单已撤销人工完成'
]) {
  assert(!pageSource.includes(oldCopy), `排产工单页面不得继续显示旧动作文案：${oldCopy}`)
}
assert(!pageSource.includes('人工完成'), '排产工单页面不得残留“人工完成”用户可见文案。')
assert(!realFlowSource.includes('人工完成'), '完成真实流不得继续使用“人工完成”文案或定位器。')
assert(!realFlowSource.includes('强制完成'), '完成真实流不得继续使用“强制完成”文案或定位器。')
assert(
  pageSource.includes("{ label: '已完成', value: 'COMPLETED' }"),
  '正常完成状态筛选必须继续显示“已完成”，不得与完成动作混淆。'
)

for (const locatorCopy of [
  'name: /^完成$/',
  "hasText: '排产工单完成'",
  'name: /^撤销完成$/',
  "hasText: '撤销排产工单完成'",
  "getByText('排产工单已完成')",
  "getByText('排产工单已撤销完成')"
]) {
  assert(realFlowSource.includes(locatorCopy), `真实流 E2E 必须使用新文案定位：${locatorCopy}`)
}

assert(
  pageSource.includes('v-hasPermi="[\'mes:pro-schedule-order:manual-finish\']"') &&
    pageSource.includes('v-hasPermi="[\'mes:pro-schedule-order:revoke-complete\']"'),
  '完成和撤销权限码必须保持不变。'
)
assert(
  pageSource.includes('manualFinishScheduleOrder') &&
    pageSource.includes('revokeManualFinishScheduleOrder'),
  '完成和撤销必须继续使用现有 API 包装。'
)

console.log('PASS: MES schedule order completion copy static contract')
