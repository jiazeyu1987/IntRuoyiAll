const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
assert(fs.existsSync(pagePath), 'Schedule order pool page must exist.')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const sourceColumnStart = pageSource.indexOf("isScheduleOrderColumnVisible('erpWorkOrderCode')")
const sourceColumnEnd = pageSource.indexOf(
  "isScheduleOrderColumnVisible('productCode')",
  sourceColumnStart
)
const resolverStart = pageSource.indexOf('const getScheduleOrderSourceCodeText')
const resolverEnd = pageSource.indexOf('const getScheduleOrderProductCodeClass', resolverStart)

assert(
  sourceColumnStart >= 0 && sourceColumnEnd > sourceColumnStart,
  'Schedule order page must keep the source production work order column.'
)
assert(
  resolverStart >= 0 && resolverEnd > resolverStart,
  'Schedule order page must define a bounded source work order display text resolver.'
)

const sourceColumn = pageSource.slice(sourceColumnStart, sourceColumnEnd)
const resolverSource = pageSource.slice(resolverStart, resolverEnd)

assert(
  sourceColumn.includes('{{ getScheduleOrderSourceCodeText(row) }}'),
  'Source production work order links must render the resolved completed-state text.'
)
assert(
  resolverSource.includes('row.manualFinished') &&
    resolverSource.includes('Number(row.status) === SCHEDULE_ORDER_STATUS_FINISHED'),
  'Completed-state text must cover both manual completion and the formal finished status.'
)
assert(
  resolverSource.includes("'(已完成)'") && resolverSource.includes('row.erpWorkOrderCode'),
  'Completed schedule orders must append the Chinese completed marker to the source code.'
)
assert(
  !resolverSource.includes('progressPercent') &&
    !resolverSource.includes('completedQuantity') &&
    !resolverSource.includes('uncompletedQuantity'),
  'Completed-state text must not infer completion from progress or quantity projections.'
)

console.log('PASS: MES schedule order completed source label static contract')
