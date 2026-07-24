const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const pageSource = readText('src/views/mes/pro/scheduler-workbench/index.vue')
const apiSource = readText('src/api/mes/pro/schedulerWorkbench/index.ts')
const shortcutsSource = readText('src/views/mes/home/HomeShortcuts.vue')

assert.doesNotMatch(pageSource, /<h3>排产员工作台<\/h3>/)
assert.doesNotMatch(pageSource, /按生产订单到报工复盘的顺序处理排产事项/)
assert.doesNotMatch(pageSource, /scheduler-workbench__actions/)
assert.doesNotMatch(pageSource, /scheduler-workbench__settings-panel/)
assert.doesNotMatch(pageSource, /scheduler-workbench__settings-entry-panel/)
assert.doesNotMatch(pageSource, /scheduler-workbench__settings-entry-copy/)
assert.doesNotMatch(pageSource, /工序在制、产能和异常概览/)
assert.match(pageSource, /schedulerSettingsDialogVisible/)
assert.match(pageSource, />\s*排产设置\s*</)
assert.match(pageSource, /scheduler-workbench__settings-grid/)
assert.doesNotMatch(pageSource, /scheduler-workbench__metrics/)
assert.doesNotMatch(pageSource, /metricCards/)
assert.doesNotMatch(pageSource, /handleMetricCardClick/)
assert.match(pageSource, /导出全部数据包/)
assert.match(pageSource, /导入全部数据包/)
assert.match(pageSource, /maximumFractionDigits:\s*0/)
assert.doesNotMatch(pageSource, /label: '今日可用产能'/)
assert.doesNotMatch(pageSource, /formatCapacityCardNumber\(summary\.value\.todayAvailableCapacity\)/)
assert.doesNotMatch(pageSource, /targetRouteName: 'MesProScheduleCalendar'[\s\S]*?todayAvailableCapacity/)
assert.doesNotMatch(pageSource, /feedbackDeviationDialogVisible/)
assert.doesNotMatch(pageSource, /openFeedbackDeviationDialog/)

for (const token of ['演练上下文', '今日建议', '处理顺序', '复盘摘要']) {
  assert.doesNotMatch(pageSource, new RegExp(token), `workbench must not show removed context card: ${token}`)
}
for (const token of ['行动链路', 'scheduler-workbench__steps-panel', 'openWorkbenchStep']) {
  assert.doesNotMatch(pageSource, new RegExp(token), `workbench must not show removed action card: ${token}`)
}
assert.doesNotMatch(pageSource, /瓶颈与异常/)
assert.doesNotMatch(pageSource, /scheduler-workbench__bottleneck/)
assert.doesNotMatch(
  pageSource,
  /<div class="scheduler-workbench__tab-head">\s*<span>工序在制订单<\/span>[\s\S]*?<\/div>/
)
assert.doesNotMatch(pageSource, /按当前工序统计几个订单在做/)
assert.match(pageSource, /工序在制订单/)
assert.match(pageSource, /activeWipTab/)
assert.match(pageSource, /process-list/)
assert.match(pageSource, /route-active/)
assert.match(pageSource, /<el-tabs[\s\S]*v-model="activeWipTab"/)
assert.match(pageSource, /<el-tab-pane[\s\S]*label="工序列表"[\s\S]*name="process-list"/)
assert.match(pageSource, /<el-tab-pane[\s\S]*label="工艺路线在制订单"[\s\S]*name="route-active"/)
assert.match(pageSource, /\.scheduler-workbench__wip-tabs-panel[\s\S]*grid-column:\s*1 \/ -1/)
assert.match(pageSource, /processWipStatistics/)
assert.match(pageSource, /scheduler-workbench__process-wip-table/)
assert.doesNotMatch(pageSource, /scheduler-workbench__process-wip-item/)
for (const label of [
  '工序编号',
  '工序名称',
  '在制单数',
  '班次产能',
  '班次状态',
  '未完需求',
  '预计完工',
  '今日报工'
]) {
  assert.match(pageSource, new RegExp(`label="${label}"`), `process WIP table missing: ${label}`)
}
assert.match(pageSource, /工艺路线在制订单/)
assert.match(pageSource, /routeActiveOrders/)
assert.match(pageSource, /routeActiveProductsText/)
assert.match(pageSource, /formatIntegerNumber\(row\.wipOrderCount\) \}\} 个订单在做/)
assert.doesNotMatch(pageSource, /openBottleneckTarget/)
assert.match(apiSource, /\/mes\/pro\/scheduler-workbench\/summary/)
assert.match(apiSource, /SchedulerWorkbenchStepVO/)
assert.match(apiSource, /SchedulerWorkbenchBottleneckVO/)
assert.match(apiSource, /SchedulerWorkbenchReportedDeviationDetailVO/)
assert.match(apiSource, /SchedulerWorkbenchRouteActiveOrderVO/)
assert.match(apiSource, /SchedulerWorkbenchRouteActiveProductVO/)
assert.match(apiSource, /reportedDeviationQuantity/)
assert.match(apiSource, /currentSchedulePlannedQuantity/)
assert.match(apiSource, /currentScheduleReportedQuantity/)
assert.match(apiSource, /reportedDeviationDetails/)
assert.match(apiSource, /routeActiveOrders/)
assert.match(apiSource, /todayAvailableCapacity/)
assert.match(shortcutsSource, /排产员工作台/)
assert.match(shortcutsSource, /MesProSchedulerWorkbench/)

console.log('mes-pro-scheduler-workbench-static: PASS')
