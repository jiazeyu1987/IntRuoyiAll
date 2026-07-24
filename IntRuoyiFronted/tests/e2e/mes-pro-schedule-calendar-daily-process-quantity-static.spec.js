const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/task/calendar/index.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/scheduleCalendar/index.ts')

assert(fs.existsSync(pagePath), 'Schedule calendar page must exist.')
assert(fs.existsSync(apiPath), 'Schedule calendar API contract must exist.')

const source = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

assert.ok(
  apiSource.includes('dailyQuantity?: number'),
  'Calendar task API contract must expose dailyQuantity for selected-day allocated quantity.'
)

assert.ok(
  source.includes('function resolveTaskDailyQuantity'),
  'Schedule calendar must centralize selected-day quantity resolution.'
)

assert.ok(
  source.includes('<label>当日工序量</label>'),
  'Day detail primary quantity card must clearly say 当日工序量.'
)

assert.ok(
  source.includes('buildQuantityLabel(selectedDayDailyProcessQuantity)'),
  'Day detail primary quantity card must aggregate selected-day daily quantities.'
)

assert.ok(
  source.includes('const selectedDayTotalProcessTaskQuantity = computed(() =>'),
  'Day detail must keep a separate total process task quantity for task totals.'
)

assert.ok(
  source.includes('label="当日完成量"') &&
    source.includes('buildQuantityLabel(resolveTaskDailyQuantity(row))'),
  'Task detail table must show each process daily quantity.'
)

assert.ok(
  source.includes('label="总任务量"') && source.includes('buildQuantityLabel(row.quantity)'),
  'Task detail table must keep total task quantity beside daily quantity.'
)

assert.ok(
  source.includes('dailyProcessQuantity') &&
    source.includes('const dailyQuantity = resolveTaskDailyQuantity(task)') &&
    source.includes('group.dailyProcessQuantity += dailyQuantity'),
  'Grouped work-order cards must aggregate daily process quantity.'
)

assert.ok(
  source.includes('current.dailyProcessQuantity += dailyQuantity'),
  'Work-order rows must aggregate daily process quantity separately from total process task quantity.'
)

assert.ok(
  !source.includes('const selectedDayProcessTaskQuantity = computed(() => {') ||
    !source.includes('buildQuantityLabel(selectedDayProcessTaskQuantity)'),
  'Primary day detail metric must not keep using raw task quantity totals.'
)

assert.doesNotMatch(source, /catch\s*\{\s*\}/, 'Daily quantity display must not introduce empty catch blocks.')

console.log('PASS: MES schedule calendar daily process quantity static contract')
