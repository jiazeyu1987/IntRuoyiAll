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
  apiSource.includes('processCapacitySummary?: ProScheduleCalendarProcessCapacitySummaryVO'),
  'Day detail API contract must expose processCapacitySummary.'
)
assert.ok(
  apiSource.includes('interface ProScheduleCalendarProcessCapacityItemVO') &&
    apiSource.includes('maxCapacity?: number') &&
    apiSource.includes('scheduledQuantity?: number') &&
    apiSource.includes('remainingCapacity?: number') &&
    apiSource.includes('overCapacity?: number') &&
    apiSource.includes('utilizationRate?: number'),
  'Process capacity item must expose max, scheduled, remaining, overrun and utilization fields.'
)
assert.ok(
  source.includes('selectedDayProcessCapacityRows'),
  'Schedule calendar page must derive selected-day process capacity rows from day detail.'
)
assert.ok(
  source.includes('工序产能利用'),
  'Day detail must include a process capacity utilization section.'
)
assert.ok(
  source.includes('最大产能') &&
    source.includes('已排产能') &&
    source.includes('剩余产能') &&
    source.includes('超出产能') &&
    source.includes('利用率'),
  'Process capacity section must show max capacity, scheduled capacity, remaining or overrun capacity and utilization.'
)
assert.ok(
  source.includes('buildProcessCapacityStatusType'),
  'Process capacity rows must classify utilization status for scheduler readability.'
)
assert.ok(
  source.includes('utilizationRate > 100') &&
    source.includes("return 'danger'"),
  'Process capacity overrun must be shown as danger, not success.'
)
assert.doesNotMatch(
  source,
  /catch\s*\{\s*\}/,
  'Process capacity display must not introduce empty catch blocks.'
)

console.log('PASS: MES schedule calendar process capacity summary static contract')
