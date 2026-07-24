const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/task/calendar/index.vue')

assert(fs.existsSync(pagePath), 'Schedule calendar page must exist.')

const source = fs.readFileSync(pagePath, 'utf8')
const detailPaneStart = source.indexOf('<el-tab-pane :label="selectedDayTitle" name="detail">')
const sidebarTabsEnd = source.indexOf('</el-tabs>', detailPaneStart)

assert.notEqual(detailPaneStart, -1, 'Day detail tab pane must exist.')
assert.notEqual(sidebarTabsEnd, -1, 'Sidebar tabs must close after day detail tab.')

const detailPaneSource = source.slice(detailPaneStart, sidebarTabsEnd)

for (const token of [
  "openDaySummaryDetail('tasks')",
  "openDaySummaryDetail('orders')",
  "openDaySummaryDetail('dayShift')",
  "openDaySummaryDetail('nightShift')",
  "openDaySummaryDetail('shortages')",
  "openDaySummaryDetail('materials')",
  "openDaySummaryDetail('issues')",
  "openDaySummaryDetail('locked')"
]) {
  assert.ok(detailPaneSource.includes(token), `Day detail tab must keep the summary card action ${token}.`)
}

const summaryButtonCount = (detailPaneSource.match(/class="summary-chip summary-chip-button/g) || []).length
assert.equal(summaryButtonCount, 9, 'Day detail tab must render exactly nine summary cards.')
assert.ok(
  detailPaneSource.includes('<label>工单计划量</label>'),
  'Day detail tab must include the de-duplicated work-order plan quantity card.'
)

for (const removedInlineToken of [
  '当日物料汇总',
  'selectedDayMaterialRows',
  'line-board',
  'selectedDayRouteRows',
  'route-group-list',
  'task-card',
  'detail-head-actions',
  "openShortageDialog('短缺明细', selectedDayShortages)"
]) {
  assert.ok(
    !detailPaneSource.includes(removedInlineToken),
    `Day detail tab must not inline ${removedInlineToken}; details belong in click dialogs.`
  )
}

const daySummaryDialogSource = source.slice(source.indexOf('<Dialog :title="daySummaryDialogTitle"'))
for (const dialogToken of [
  'daySummaryDialogTaskRows',
  'daySummaryDialogOrderRows',
  'openWorkOrderAnalysis',
  'buildTaskProcessLabel',
  'buildTaskProductCodeLabel',
  'buildTaskProductNameLabel'
]) {
  assert.ok(daySummaryDialogSource.includes(dialogToken), `Day summary dialog must preserve ${dialogToken}.`)
}

assert.match(
  source,
  /if \(type === 'shortages'\) \{[\s\S]*openShortageDialog\('短缺明细', selectedDayShortages\.value\)/,
  'Shortage summary card must open the shortage detail dialog.'
)

assert.doesNotMatch(source, /catch\s*\{\s*\}/, 'Schedule calendar detail card cleanup must not introduce empty catch blocks.')

console.log('PASS: MES schedule calendar detail cards only static contract')
