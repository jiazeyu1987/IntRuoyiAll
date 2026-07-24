const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduler-workbench/index.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/schedulerWorkbench/index.ts')

assert(fs.existsSync(pagePath), '排产员工作台页面必须存在。')
assert(fs.existsSync(apiPath), '排产员工作台 API 必须存在。')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

for (const token of [
  'todayActionSuggestion',
  'nightlyReplanText',
  'pendingApprovalFeedbackCount',
  'currentSchedulePlannedQuantity',
  'currentScheduleReportedQuantity',
  'currentScheduleScopeText',
  'globalRiskScopeText',
  'steps: SchedulerWorkbenchStepVO[]',
  'bottlenecks: SchedulerWorkbenchBottleneckVO[]',
  'reportedDeviationDetails: SchedulerWorkbenchReportedDeviationDetailVO[]'
]) {
  assert(apiSource.includes(token), `工作台 API 契约必须提供 ${token}。`)
}

for (const token of [
  '演练上下文',
  '今日建议',
  '处理顺序',
  '复盘摘要',
  '当前对象',
  '下一步入口',
  'A1 ERP同步',
  'A2 排产发布',
  'A3 工艺产能',
  'A4 报工执行',
  'A5 审批复盘',
  'scheduler-workbench__rehearsal',
  'scheduler-workbench__rehearsal-links',
  'scheduler-workbench__review-summary',
  'scheduler-workbench__action-card',
  'rehearsalLinks',
  'openRehearsalLink',
  'openStepTarget'
]) {
  assert(!pageSource.includes(token), `排产工作台不得显示或保留上下文卡片：${token}`)
}

for (const token of [
  'scheduler-workbench__side-panels',
  'scheduler-workbench__wip-tabs-panel',
  'scheduler-workbench__wip-tabs',
  'activeWipTab'
]) {
  assert(pageSource.includes(token), `黄框区域必须保留左右布局结构：${token}`)
}

assert(
  /<section class="scheduler-workbench__panel scheduler-workbench__wip-tabs-panel">[\s\S]*?<el-tabs[\s\S]*?<el-tab-pane[\s\S]*label="工序列表"[\s\S]*?<el-tab-pane[\s\S]*label="工艺路线在制订单"/.test(pageSource),
  '工序在制订单与工艺路线在制订单必须位于同一个 Tab 面板内。'
)
assert(!pageSource.includes('scheduler-workbench__process-wip-panel'), '工序在制订单不得继续作为独立卡片。')
assert(!pageSource.includes('scheduler-workbench__route-active-panel'), '工艺路线在制订单不得继续作为独立卡片。')
for (const token of [
  '瓶颈与异常',
  'scheduler-workbench__bottleneck-panel',
  'scheduler-workbench__bottleneck',
  'openBottleneckTarget',
  'buildBottleneckTargetPath',
  '配置该工序资源'
]) {
  assert(!pageSource.includes(token), `工作台不得继续显示瓶颈异常卡片：${token}`)
}

for (const token of [
  'useRouter()',
  'openProcessWipOrders',
  '@row-click="openProcessWipOrders"',
  "source: 'scheduler-workbench'"
]) {
  assert(pageSource.includes(token), `工作台关键入口必须可跳转：${token}`)
}
assert(!pageSource.includes("label: '排产阻塞项'"), '工作台不得再把全局治理项命名为排产阻塞项。')
assert(!pageSource.includes("label: '今日可用产能'"), '顶部指标必须删除今日可用产能卡片。')
assert(!pageSource.includes("path: '/mes/pro/puhui-schedule'"), 'A5 审批复盘不得指向璞慧本地沙盘。')
assert(!pageSource.includes('>处理</el-button>'), '工作台瓶颈主按钮不得继续使用泛化“处理”文案。')

console.log('PASS: MES scheduler workbench interaction static contract')
