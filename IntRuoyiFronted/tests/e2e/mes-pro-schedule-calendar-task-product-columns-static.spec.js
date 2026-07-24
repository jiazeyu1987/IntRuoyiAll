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

assert.notEqual(taskTableStart, -1, 'Selected work-order task table must exist.')

const taskTableSource = dialogSource.slice(taskTableStart)

for (const splitColumn of ['label="产品编码"', 'label="产品名称"']) {
  assert.ok(taskTableSource.includes(splitColumn), `Task table must split product info into ${splitColumn}.`)
}

assert.ok(
  taskTableSource.includes('buildTaskProductCodeLabel(row)') &&
    taskTableSource.includes('buildTaskProductNameLabel(row)'),
  'Task table must render product code and product name from separate helpers.'
)

for (const hiddenColumn of [
  'label="产品"',
  'buildTaskProductLabel',
  'label="待检"',
  'prop="pendingInspectionQuantity"',
  'label="执行状态"',
  'buildTaskExecutionStatusText(row)',
  'buildTaskExecutionStatusTag(row)'
]) {
  assert.ok(!taskTableSource.includes(hiddenColumn), `Task table must hide ${hiddenColumn}.`)
}

for (const keptColumn of [
  'label="工序"',
  'label="数量"',
  'label="已报工"',
  'label="锁定"',
  'label="排产冻结"',
  'label="产线"'
]) {
  assert.ok(taskTableSource.includes(keptColumn), `Task table must keep ${keptColumn}.`)
}

for (const protectedToken of [
  'daySummaryTaskWorkOrderGroups',
  'selectedDaySummaryTaskRows',
  'daySummaryDialogType === \'orders\'',
  ':data="daySummaryDialogOrderRows"',
  'daySummaryDialogType === \'issues\'',
  ':data="daySummaryDialogIssueRows"'
]) {
  assert.ok(dialogSource.includes(protectedToken), `Existing grouped dialog behavior must remain: ${protectedToken}`)
}

assert.doesNotMatch(source, /catch\s*\{\s*\}/, 'Task product column adjustment must not introduce empty catch blocks.')

console.log('PASS: MES schedule calendar task product columns static contract')

