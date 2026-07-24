const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/task/calendar/index.vue')

assert(fs.existsSync(pagePath), 'Schedule calendar page must exist.')

const source = fs.readFileSync(pagePath, 'utf8')
const cellStart = source.indexOf('<div class="calendar-grid">')
const sidebarStart = source.indexOf('<aside class="sidebar-column">')

assert.notEqual(cellStart, -1, 'Schedule calendar grid must exist.')
assert.notEqual(sidebarStart, -1, 'Schedule calendar sidebar must exist.')

const gridSource = source.slice(cellStart, sidebarStart)

for (const token of [
  'calendar-metric-button',
  "@click.stop=\"openCalendarMetricDetail(cell.date, 'tasks')\"",
  "@click.stop=\"openCalendarMetricDetail(cell.date, 'orders')\"",
  "@click.stop=\"openCalendarMetricDetail(cell.date, 'shortages')\""
]) {
  assert.ok(gridSource.includes(token), `Schedule calendar month grid must expose clickable metric token: ${token}`)
}

for (const forbiddenToken of [
  "@click.stop=\"openCalendarMetricDetail(cell.date, 'dayShift')\"",
  "@click.stop=\"openCalendarMetricDetail(cell.date, 'nightShift')\"",
  'class="calendar-metric-button is-readonly"'
]) {
  assert.ok(!gridSource.includes(forbiddenToken), `Schedule calendar month grid must not expose forbidden metric click token: ${forbiddenToken}`)
}

assert.match(
  source,
  /type CalendarMetricDialogType = 'tasks' \| 'orders' \| 'shortages'/,
  'Schedule calendar must define explicit month metric dialog types without day/night shift.'
)

assert.match(
  source,
  /async function openCalendarMetricDetail\(date: string, type: CalendarMetricDialogType\)/,
  'Schedule calendar must define a dedicated month metric detail opener.'
)

assert.match(
  source,
  /await selectCalendarDate\(date\)/,
  'Month metric detail opener must first switch the selected date.'
)

assert.match(
  source,
  /if \(type === 'shortages'\) \{[\s\S]*openShortageDialog\('短缺明细', selectedDayShortages\.value\)/,
  'Month shortage metric must reuse the shortage detail dialog after selecting the clicked date.'
)

assert.match(
  source,
  /showDaySummaryDialog\(type\)/,
  'Month task/order metrics must open the day summary dialog after selecting the clicked date.'
)

assert.ok(
  source.includes('夜班由工艺流程排产配置的工序配置决定'),
  'Schedule calendar must preserve the rule hint that night shift comes from route process configuration.'
)

assert.doesNotMatch(
  source,
  /catch\s*\{\s*\}/,
  'Schedule calendar month metric detail behavior must not introduce empty catch blocks.'
)

console.log('PASS: MES schedule calendar month metric dialog static contract')
