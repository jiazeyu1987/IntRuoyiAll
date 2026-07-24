const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/task/calendar/index.vue')

assert(fs.existsSync(pagePath), 'Schedule calendar page must exist.')

const source = fs.readFileSync(pagePath, 'utf8')
const detailStart = source.indexOf('<div class="detail-summary-grid">')
const detailEnd = source.indexOf('<p class="shift-rule-hint"', detailStart)

assert.notEqual(detailStart, -1, 'Day detail summary grid must exist.')
assert.notEqual(detailEnd, -1, 'Shift rule hint must follow day detail summary grid.')

const detailSource = source.slice(detailStart, detailEnd)

assert.ok(
  detailSource.includes('class="detail-total-quantity-card"'),
  'Day detail must include a prominent process-task quantity card.'
)
assert.ok(
  detailSource.includes('aria-label="当天当日工序量"'),
  'Day detail prominent card must expose selected-day process quantity semantics.'
)
assert.ok(
  detailSource.includes('<label>当日工序量</label>'),
  'Day detail prominent card must be labelled 当日工序量.'
)
assert.ok(
  detailSource.includes('buildQuantityLabel(selectedDayDailyProcessQuantity)'),
  'Day detail daily process quantity must use the integer quantity formatter.'
)
assert.ok(
  detailSource.includes('<label>工单计划量</label>'),
  'Day detail summary grid must show the de-duplicated work-order plan quantity.'
)
assert.ok(
  detailSource.includes('buildQuantityLabel(selectedDayWorkOrderPlanQuantity)'),
  'Day detail work-order plan quantity must use the integer quantity formatter.'
)
assert.ok(
  !detailSource.includes('<label>总数量</label>'),
  'Day detail must not label process-task quantity as 总数量.'
)
assert.ok(
  source.includes('const selectedDayDailyProcessQuantity = computed(() =>'),
  'Day detail daily process quantity must be computed from selected-day task rows.'
)
assert.ok(
  source.includes('const selectedDayTotalProcessTaskQuantity = computed(() =>'),
  'Day detail total process task quantity must be computed separately.'
)
assert.ok(
  source.includes('const selectedDayWorkOrderPlanQuantity = computed(() =>'),
  'Day detail work-order plan quantity must be computed separately from task load.'
)
assert.ok(
  source.includes('selectedDayTaskRows.value.reduce'),
  'Day detail process-task quantity must aggregate selected-day task quantities.'
)
assert.ok(
  source.includes('function resolveTaskDailyQuantity'),
  'Day detail quantities must use the selected-day quantity helper.'
)
assert.ok(
  source.includes('current.dailyProcessQuantity += dailyQuantity') &&
    source.includes('current.processTaskQuantity += totalTaskQuantity'),
  'Work-order rows must preserve daily and total process quantities separately.'
)
assert.ok(
  !source.includes('current.totalQuantity += Number(task.quantity || 0)'),
  'Work-order plan quantity must not repeatedly add every process task quantity.'
)
assert.ok(
  source.includes('.detail-total-quantity-card strong') && source.includes('font-size: 32px'),
  'Day detail daily process quantity value must use visibly larger typography.'
)
assert.doesNotMatch(
  source,
  /catch\s*\{\s*\}/,
  'Detail quantity display must not introduce empty catch blocks.'
)

console.log('PASS: MES schedule calendar detail quantity semantics static contract')
