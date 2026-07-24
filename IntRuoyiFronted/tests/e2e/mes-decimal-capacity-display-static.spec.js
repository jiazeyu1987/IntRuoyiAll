const assert = require('assert')
const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
const extractFunction = (source, functionName) => {
  const start = source.indexOf(`const ${functionName} =`)
  assert(start >= 0, `必须定义 ${functionName}`)
  const nextConst = source.indexOf('\nconst ', start + 1)
  return source.slice(start, nextConst >= 0 ? nextConst : source.length)
}
const extractDeclaredFunction = (source, functionName) => {
  const start = source.indexOf(`function ${functionName}(`)
  assert(start >= 0, `必须定义 ${functionName}`)
  const nextPlainFunction = source.indexOf('\nfunction ', start + 1)
  const nextExportFunction = source.indexOf('\nexport function ', start + 1)
  const nextFunction = [nextPlainFunction, nextExportFunction]
    .filter((index) => index >= 0)
    .sort((a, b) => a - b)[0]
  return source.slice(start, nextFunction >= 0 ? nextFunction : source.length)
}

const scheduleOrderSource = readSource('src/views/mes/pro/scheduleorder/index.vue')
const schedulerWorkbenchSource = readSource('src/views/mes/pro/scheduler-workbench/index.vue')
const routeProcessListSource = readSource('src/views/mes/pro/route/RouteProcessList.vue')
const routeFlowGraphSource = readSource('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const taskCalendarSource = readSource('src/views/mes/pro/task/calendar/index.vue')
const routeResourcePreviewSource = readSource(
  'src/views/mes/pro/route/components/RouteResourceCapacityPreview.vue'
)
const routeResourceTableSource = readSource('src/views/mes/pro/route/RouteResourceTable.vue')
const puhuiSchedulerSource = readSource('src/views/mes/pro/puhui-schedule/scheduler.ts')
const puhuiIndexSource = readSource('src/views/mes/pro/puhui-schedule/index.vue')
const puhuiCalendarSource = readSource(
  'src/views/mes/pro/puhui-schedule/components/PuhuiScheduleCalendar.vue'
)

assert(
  scheduleOrderSource.includes('formatCapacityIntegerNumber(row.hourlyCapacityTotal)') &&
    scheduleOrderSource.includes('formatCapacityIntegerNumber(row.shiftCapacityTotal)'),
  '排产工单工序明细列的小时产能和班次产能必须按最新列表要求使用整数产能格式化。'
)
assert(
  !scheduleOrderSource.includes('formatCapacityNumber(row.hourlyCapacityTotal)') &&
    !scheduleOrderSource.includes('formatCapacityNumber(row.shiftCapacityTotal)'),
  '排产工单工序明细列不得继续显示小数产能尾差。'
)
const scheduleOrderCapacityFormatter = extractFunction(scheduleOrderSource, 'formatCapacityIntegerNumber')
assert(
  scheduleOrderCapacityFormatter.includes('maximumFractionDigits: 0'),
  '排产工单产能整数格式化必须最多显示 0 位小数。'
)
assert(
  !scheduleOrderCapacityFormatter.includes('Math.round'),
  '排产工单产能整数格式化应仅处理展示精度，不得修改原始数值计算。'
)
assert(
  extractFunction(scheduleOrderSource, 'formatQuantity').includes('Math.round'),
  '排产工单数量格式化继续按整数展示，不得把产能小数要求扩大到数量字段。'
)

const processWipShiftCapacityColumn = schedulerWorkbenchSource.match(
  /prop="shiftCapacityTotal"[\s\S]*?<\/el-table-column>/
)?.[0]
assert(processWipShiftCapacityColumn, '排产员工作台工序列表必须保留班次产能列。')
assert(
  processWipShiftCapacityColumn.includes('formatProcessWipShiftCapacity(row.shiftCapacityTotal)'),
  '排产员工作台工序列表红框班次产能必须按用户要求使用整数展示函数。'
)
assert(
  !processWipShiftCapacityColumn.includes('formatNumber(row.shiftCapacityTotal)'),
  '排产员工作台工序列表红框班次产能不得继续直接展示小数尾差。'
)
const processWipShiftCapacityFormatter = extractFunction(
  schedulerWorkbenchSource,
  'formatProcessWipShiftCapacity'
)
assert(
  processWipShiftCapacityFormatter.includes('maximumFractionDigits: 0'),
  '排产员工作台工序列表红框班次产能整数展示必须最多显示 0 位小数。'
)
assert(
    schedulerWorkbenchSource.includes('prop="defaultFiniteHourlyCapacity"') &&
    schedulerWorkbenchSource.includes(':min="0.000001"') &&
    schedulerWorkbenchSource.includes(':precision="6"') &&
    schedulerWorkbenchSource.includes("positiveNumberRule('产能覆盖(产能/h)')"),
  '排产员工作台产能覆盖默认值输入与校验必须允许小数。'
)

for (const [name, source] of [
  ['RouteProcessList', routeProcessListSource],
  ['RouteFlowGraphDesigner', routeFlowGraphSource]
]) {
  const normalizer = extractFunction(source, 'normalizeShiftCapacity')
  assert(!normalizer.includes('Math.round'), `${name} 班次产能归一化不得取整。`)
  assert(
    normalizer.includes('toFixed(6)'),
    `${name} 班次产能归一化必须按 6 位精度保留后端小数。`
  )
}

assert(
  taskCalendarSource.includes("buildCapacityLabel(row.effectiveHourlyCapacity, '件/小时')") &&
    taskCalendarSource.includes(
      "buildCapacityLabel(workOrderAnalysis.value.bottleneckHourlyCapacity, '件/小时')"
    ),
  '任务日历有效小时产能和瓶颈小时产能必须使用小数产能格式化。'
)
const taskCalendarCapacityFormatter = extractDeclaredFunction(taskCalendarSource, 'buildCapacityLabel')
assert(
  taskCalendarCapacityFormatter.includes('maximumFractionDigits: 6') &&
    !taskCalendarCapacityFormatter.includes('Math.round'),
  '任务日历产能格式化必须最多保留 6 位小数且不得取整。'
)
assert(
  extractDeclaredFunction(taskCalendarSource, 'buildQuantityLabel').includes('Math.round'),
  '任务日历数量字段继续按整数展示。'
)

for (const [name, source, functionName] of [
  ['RouteResourceCapacityPreview', routeResourcePreviewSource, 'formatCapacity'],
  ['RouteResourceTable', routeResourceTableSource, 'formatNumber']
]) {
  const formatter = extractFunction(source, functionName)
  assert(
    formatter.includes('maximumFractionDigits: 6') && !formatter.includes('toFixed(2)'),
    `${name} 产能展示不得固定两位截断，必须最多保留 6 位小数。`
  )
}

assert(
  puhuiSchedulerSource.includes('export function formatCapacityNumber') &&
    extractDeclaredFunction(puhuiSchedulerSource, 'formatNumber').includes('Math.round') &&
    !extractDeclaredFunction(puhuiSchedulerSource, 'formatCapacityNumber').includes('Math.round') &&
    extractDeclaredFunction(puhuiSchedulerSource, 'formatCapacityNumber').includes(
      'maximumFractionDigits: 6'
    ),
  '普惠排程必须单独提供小数产能格式化，不能复用整数数量格式化。'
)
for (const requiredUsage of [
  'formatCapacityNumber(plan.summary.totalCapacity)',
  'formatCapacityNumber(row.daily_capacity)',
  'formatCapacityNumber(row.capacity_total)'
]) {
  assert(puhuiIndexSource.includes(requiredUsage), `普惠排程页面必须使用小数产能格式化：${requiredUsage}`)
}
assert(
  puhuiCalendarSource.includes('formatCapacityNumber(totalCapacity(date))'),
  '普惠排程日历总产能必须使用小数产能格式化。'
)

console.log('PASS: MES decimal capacity display static contract')
