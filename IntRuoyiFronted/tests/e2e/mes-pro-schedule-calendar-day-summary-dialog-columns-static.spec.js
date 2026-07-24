const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/task/calendar/index.vue')

assert(fs.existsSync(pagePath), 'Schedule calendar page must exist.')

const source = fs.readFileSync(pagePath, 'utf8')
const dialogStart = source.indexOf('<Dialog :title="daySummaryDialogTitle"')
const dialogEnd = source.indexOf('<Dialog :title="workOrderAnalysisDialogTitle"', dialogStart)

assert.notEqual(dialogStart, -1, 'Day summary dialog must exist.')
assert.notEqual(dialogEnd, -1, 'Work order analysis dialog must follow day summary dialog.')

const dialogSource = source.slice(dialogStart, dialogEnd)
const taskTableStart = dialogSource.indexOf(':data="selectedDaySummaryTaskRows"')

assert.notEqual(taskTableStart, -1, 'Day summary task table must exist.')

const taskTableSource = dialogSource.slice(taskTableStart)

for (const hiddenToken of [
  'label="任务"',
  'prop="taskCode"',
  'label="班次"',
  'buildShiftModeLabel',
  'label="时间"',
  'buildTaskTimeRange',
  'label="车间"',
  'prop="workshopTitle"',
  'label="工序 / 产品"',
  'buildTaskSubTitle',
  'prop="lineTitle"'
]) {
  assert.ok(!taskTableSource.includes(hiddenToken), `Day summary task table must hide ${hiddenToken}.`)
}

for (const keptColumn of [
  'label="工序"',
  'label="产品编码"',
  'label="产品名称"',
  'label="当日完成量"',
  'label="总任务量"',
  'label="锁定"',
  'label="产线"'
]) {
  assert.ok(taskTableSource.includes(keptColumn), `Day summary task table must keep ${keptColumn}.`)
}

for (const keptToken of [
  'selectedDaySummaryTaskRows',
  'buildTaskProcessLabel',
  'buildTaskProductCodeLabel',
  'buildTaskProductNameLabel',
  'buildTaskLineNameLabel'
]) {
  assert.ok(taskTableSource.includes(keptToken), `Day summary task table must preserve ${keptToken}.`)
}

assert.ok(
  taskTableSource.includes('buildQuantityLabel(resolveTaskDailyQuantity(row))'),
  'Day summary task daily quantity must be rendered through integer quantity formatter.'
)
assert.ok(
  taskTableSource.includes('buildQuantityLabel(row.quantity)'),
  'Day summary task total quantity must be rendered through integer quantity formatter.'
)
assert.ok(
  taskTableSource.includes('buildQuantityLabel(row.reportedQuantity)'),
  'Day summary reported quantity must be rendered through integer quantity formatter.'
)
assert.ok(
  dialogSource.includes('{{ buildQuantityLabel(group.workOrderPlanQuantity) }} 件计划'),
  'Grouped work-order card plan quantity must be rendered through integer quantity formatter.'
)
assert.ok(
  dialogSource.includes('{{ buildQuantityLabel(group.dailyProcessQuantity) }} 件当日') &&
    dialogSource.includes('{{ buildQuantityLabel(group.processTaskQuantity) }} 件总量'),
  'Grouped work-order card daily and total process quantities must be rendered through integer quantity formatter.'
)
assert.ok(
  dialogSource.includes('buildQuantityLabel(row.totalQuantity)'),
  'Day summary order total quantity must be rendered through integer quantity formatter.'
)
assert.ok(
  dialogSource.includes('buildQuantityLabel(row.dailyProcessQuantity)') &&
    dialogSource.includes('buildQuantityLabel(row.processTaskQuantity)'),
  'Day summary order daily and total process quantities must be rendered through integer quantity formatter.'
)
assert.ok(
  !dialogSource.includes('prop="totalQuantity"') &&
    !dialogSource.includes('prop="quantity" width="100"') &&
    !dialogSource.includes('prop="reportedQuantity"'),
  'Day summary dialog must not render raw decimal quantity props directly.'
)

assert.ok(
  dialogSource.includes('openWorkOrderAnalysis(group.workOrderId, group.workOrderCode)'),
  'Day summary dialog must preserve the work-order analysis entry in the grouped work-order list.'
)

for (const lineNameToken of [
  'lineNameTitle',
  'buildLineNameTitle(line)',
  'lineNames.add(task.lineNameTitle)',
  'group.dailyProcessQuantity += dailyQuantity',
  'group.processTaskQuantity += totalTaskQuantity'
]) {
  assert.ok(source.includes(lineNameToken), `Day summary task rows must use line names via ${lineNameToken}.`)
}

assert.doesNotMatch(source, /catch\s*\{\s*\}/, 'Schedule calendar day summary column cleanup must not introduce empty catch blocks.')

console.log('PASS: MES schedule calendar day summary dialog columns static contract')
