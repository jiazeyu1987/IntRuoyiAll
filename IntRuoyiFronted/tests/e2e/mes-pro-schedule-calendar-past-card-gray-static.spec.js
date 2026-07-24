const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/task/calendar/index.vue')

assert(fs.existsSync(pagePath), 'Schedule calendar page must exist.')

const pageSource = fs.readFileSync(pagePath, 'utf8')

assert(
  pageSource.includes("'is-readonly-past': !canEditCalendarDate(cell.date)"),
  'Past calendar cells must keep the readonly-past class binding.'
)
assert.match(
  pageSource,
  /\.calendar-cell\.is-readonly-past\s*\{[\s\S]*cursor:\s*default;[\s\S]*background:\s*#f3f4f6;/,
  'Readonly past calendar cells must use a light gray background.'
)
assert.match(
  pageSource,
  /\.calendar-cell\.is-readonly-past:hover\s*\{[\s\S]*background:\s*#f3f4f6;/,
  'Readonly past calendar cells must keep the light gray background on hover.'
)

console.log('PASS: MES schedule calendar past card gray static contract')
