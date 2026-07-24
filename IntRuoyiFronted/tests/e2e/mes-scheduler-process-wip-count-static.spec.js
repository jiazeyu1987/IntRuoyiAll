const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const pageSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue'),
  'utf8'
)
const processWipTableSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/components/ProcessWipTable.vue'),
  'utf8'
)
const scheduleOrderApiSource = fs.readFileSync(
  path.join(repoRoot, 'src/api/mes/pro/scheduleorder/index.ts'),
  'utf8'
)

assert.match(pageSource, /工序在制订单/, '智能排产页必须展示工序在制订单统计区。')
assert.match(pageSource, /processWipStatistics/, '智能排产页必须维护工序在制订单统计状态。')
assert.match(pageSource, /loadProcessWipStatistics/, '智能排产页必须加载后端工序在制订单统计。')
assert.match(pageSource, /openProcessWipOrders/, '智能排产页必须支持从工序统计查看对应订单。')
assert.match(pageSource, /个订单在做/, '工序在制统计必须使用“几个订单在做”的业务表达。')
assert.match(pageSource, /<el-table[\s\S]*processWipStatistics/, '工序在制订单必须改为表格列表。')
assert.match(pageSource, /<ProcessWipTable/, '工序在制订单列表必须通过 ProcessWipTable 承载。')
assert.match(
  processWipTableSource,
  /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.schedulerWorkbench\.processWip"/,
  '工序在制订单列表必须接入标准列表模板。'
)
assert.doesNotMatch(pageSource, /scheduler-workbench__process-wip-item/, '工序在制订单不得继续使用卡片项。')
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
  assert.match(pageSource, new RegExp(`label="${label}"`), `工序在制列表缺少列：${label}`)
}
assert.match(
  pageSource,
  /MesProScheduleOrderApi\.getProcessWipStatistics/,
  '智能排产页必须调用排产工单正式统计接口，不得前端伪造统计。'
)
assert.match(
  pageSource,
  /currentProcessId/,
  '查看订单跳转必须携带当前工序筛选条件。'
)

assert.match(
  scheduleOrderApiSource,
  /MesProScheduleOrderProcessWipVO/,
  '排产工单 API 必须声明工序在制订单统计类型。'
)
for (const field of [
  'shiftCapacityTotal',
  'shiftStatus',
  'unfinishedDemandQuantity',
  'estimatedCompletionTime',
  'todayFeedbackQuantity'
]) {
  assert.match(scheduleOrderApiSource, new RegExp(field), `排产工单工序在制 API 类型缺少字段：${field}`)
}
assert.match(
  scheduleOrderApiSource,
  /\/mes\/pro\/schedule-order\/process-wip-statistics/,
  '排产工单 API 必须调用后端工序在制订单统计接口。'
)

console.log('mes-scheduler-process-wip-count-static.spec.js passed')
