const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const schedulerPath = path.resolve(process.cwd(), 'src/views/mes/pro/puhui-schedule/scheduler.ts')
const calendarPath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/puhui-schedule/components/PuhuiScheduleCalendar.vue'
)

assert(fs.existsSync(schedulerPath), 'Puhui scheduler module must exist.')
assert(fs.existsSync(calendarPath), 'Puhui schedule calendar component must exist.')

const schedulerSource = fs.readFileSync(schedulerPath, 'utf8')
const calendarSource = fs.readFileSync(calendarPath, 'utf8')

assert.match(
  schedulerSource,
  /export function formatNumber\(value: unknown\)[\s\S]*Math\.round\(n\)/,
  'Puhui schedule default number formatter must round to an integer.'
)
assert.doesNotMatch(
  schedulerSource,
  /export function formatNumber\(value: unknown,\s*digits\s*=\s*1\)[\s\S]*toFixed\(digits\)/,
  'Puhui schedule default number formatter must not keep one decimal place.'
)
assert.ok(
  calendarSource.includes('formatNumber(totalAssigned(date))') &&
    calendarSource.includes('formatCapacityNumber(totalCapacity(date))') &&
    calendarSource.includes('metaParts.push(`数量:${formatNumber(lineTotalQty)}`)'),
  'Puhui calendar order quantity labels must reuse the integer formatter, while total capacity keeps decimal capacity formatting.'
)
assert.ok(
  schedulerSource.includes('return String(Math.max(0, Math.round(n)))') &&
    !/return String\(Math\.round\(n \* 1000\) \/ 1000\)/.test(schedulerSource),
  'Puhui schedule export workload quantity must be rounded to integer.'
)
assert.doesNotMatch(schedulerSource + calendarSource, /catch\s*\{\s*\}/, 'Integer quantity fix must not introduce empty catch blocks.')

console.log('PASS: MES Puhui schedule integer quantity static contract')
