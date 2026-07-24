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

for (const taskDialogType of [
  "tasks: '任务详情'",
  "dayShift: '白班详情'",
  "nightShift: '夜班详情'",
  "locked: '锁定详情'"
]) {
  assert.ok(source.includes(taskDialogType), `Task dialog type must keep title mapping: ${taskDialogType}`)
}

for (const requiredToken of [
  'day-summary-task-group-layout',
  'day-summary-workorder-list',
  'day-summary-workorder-card',
  'selectedDaySummaryWorkOrderKey',
  'daySummaryTaskWorkOrderGroups',
  'selectedDaySummaryTaskRows',
  'selectDaySummaryWorkOrder',
  'resetSelectedDaySummaryWorkOrder'
]) {
  assert.ok(source.includes(requiredToken), `Task summary dialog must include work-order grouping token: ${requiredToken}`)
}

assert.ok(
  dialogSource.includes('v-else class="day-summary-task-group-layout"'),
  'Task dialog fallback branch must render grouped work-order layout.'
)

assert.ok(
  dialogSource.includes('v-for="group in daySummaryTaskWorkOrderGroups"') &&
    dialogSource.includes(':class="{ active: group.key === selectedDaySummaryWorkOrderKey }"') &&
    dialogSource.includes('@click="selectDaySummaryWorkOrder(group.key)"') &&
    dialogSource.includes('@keydown.enter.prevent="selectDaySummaryWorkOrder(group.key)"'),
  'Left work-order list must render one card per grouped work order and switch the selected key.'
)

assert.ok(
  dialogSource.includes(':data="selectedDaySummaryTaskRows"'),
  'Right task table must render only rows for the selected work order.'
)

assert.ok(
  !dialogSource.includes('<el-table v-else :data="daySummaryDialogTaskRows"'),
  'Task detail dialog must not keep the old flat task table branch.'
)

for (const keptColumn of [
  'label="工序"',
  'label="产品编码"',
  'label="产品名称"',
  'label="当日完成量"',
  'label="总任务量"',
  'label="已报工"',
  'label="锁定"',
  'label="排产冻结"',
  'label="产线"'
]) {
  assert.ok(dialogSource.includes(keptColumn), `Selected work-order task table must keep ${keptColumn}.`)
}

for (const lineNameToken of [
  'lineNames: string',
  'lineNames: task.lineNameTitle',
  'lineNames.add(task.lineNameTitle)',
  'group.lineNames = [...lineNames].filter(Boolean).join(\' / \')',
  '{{ group.lineNames }}'
]) {
  assert.ok(source.includes(lineNameToken), `Left grouped task card must display line names via ${lineNameToken}.`)
}

const cardTitleStart = dialogSource.indexOf('day-summary-workorder-card__line-name')
const cardMetaStart = dialogSource.indexOf('day-summary-workorder-card__meta', cardTitleStart)
const cardEnd = dialogSource.indexOf('<div class="day-summary-selected-task-table"', cardTitleStart)

assert.notEqual(cardTitleStart, -1, 'Grouped task card must have a dedicated line-name title.')
assert.notEqual(cardMetaStart, -1, 'Grouped task card meta must follow the line-name title.')
assert.notEqual(cardEnd, -1, 'Grouped task card block must end before the selected task table.')
assert.ok(
  !dialogSource.slice(cardTitleStart, cardMetaStart).includes('group.workOrderCode'),
  'Grouped task card title must not render the work-order code.'
)

const cardSource = dialogSource.slice(cardTitleStart, cardEnd)
assert.ok(
  !cardSource.includes('{{ group.workOrderCode }}') &&
    !cardSource.includes('<span v-else>{{ group.workOrderCode }}</span>'),
  'Grouped task card must not visibly render work-order or line code text.'
)

for (const previewLineNameToken of [
  'const previewLineName = resolvePreviewTaskLineName(workOrderId)',
  'lineTitle: previewLineName === \'--\' ? \'未绑定产线\' : previewLineName',
  'lineNameTitle: previewLineName'
]) {
  assert.ok(source.includes(previewLineNameToken), `Preview task rows must resolve real line names via ${previewLineNameToken}.`)
}

for (const forbiddenPreviewToken of [
  'routeName: task.line ?',
  'lineTitle: task.line ||',
  'lineNameTitle: task.line ||'
]) {
  assert.ok(!source.includes(forbiddenPreviewToken), `Preview task rows must not display internal preview line token: ${forbiddenPreviewToken}.`)
}

assert.ok(
  source.includes('return workOrderAnalysis.value?.lineName || \'--\''),
  'Work-order analysis line label must display the schedule route name as the business line label.'
)

assert.ok(
  !source.includes('[workOrderAnalysis.value?.lineCode, workOrderAnalysis.value?.lineName]'),
  'Work-order analysis line label must not join production line code with name.'
)

assert.ok(
  source.includes('const key = buildDaySummaryTaskWorkOrderKey(task)') &&
    source.includes('group.rows.push(task)') &&
    source.includes('group?.rows || []'),
  'Task grouping must aggregate source task rows by work order without changing the original rows.'
)

assert.ok(
  dialogSource.includes('openWorkOrderAnalysis(group.workOrderId, group.workOrderCode)'),
  'Grouped work-order card must preserve the work-order analysis entry.'
)

for (const protectedToken of [
  'daySummaryDialogType === \'orders\'',
  ':data="daySummaryDialogOrderRows"',
  'daySummaryDialogType === \'issues\'',
  ':data="daySummaryDialogIssueRows"'
]) {
  assert.ok(dialogSource.includes(protectedToken), `Non-task dialog branch must remain intact: ${protectedToken}`)
}

assert.doesNotMatch(source, /catch\s*\{\s*\}/, 'Work-order grouping must not introduce empty catch blocks.')

console.log('PASS: MES schedule calendar work-order grouped dialog static contract')
