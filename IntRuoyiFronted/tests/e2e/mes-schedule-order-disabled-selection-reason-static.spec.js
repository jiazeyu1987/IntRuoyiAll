const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
const pageSource = fs.readFileSync(pagePath, 'utf8')

assert.match(
  pageSource,
  /label="重排状态"[\s\S]*?fixed="left"[\s\S]*?v-if="!isScheduleOrderReplanable\(row\)"[\s\S]*?schedule-order-pool__replan-block-reason[\s\S]*?不可重排[\s\S]*?getScheduleOrderReplanBlockReason\(row\)/,
  'Disabled rows must show their reason in a fixed column that user column settings cannot hide.'
)

for (const reason of ['已冻结', '已完成', '已取消']) {
  assert(pageSource.includes(`return '${reason}'`), `Replan block reason must cover ${reason}.`)
}

const replanableMatch = pageSource.match(
  /const isScheduleOrderReplanable = \(row: MesProScheduleOrderVO\) => \{([\s\S]*?)\n\}/
)
assert(replanableMatch, 'Schedule order page must define the manual-replan row gate.')
assert(
  replanableMatch[1].includes('Number(row.status)'),
  'Manual-replan eligibility must normalize numeric status values before comparison.'
)
assert(
  !/blockingIssueCount|productionMaterialListCount|currentProcessId/.test(replanableMatch[1]),
  'Manual-replan checkbox must not infer new gates from warning or display-only fields.'
)

assert(
  pageSource.includes('role="status"') && pageSource.includes(':aria-label="`不可重排：'),
  'Visible replan reasons must expose an accessible status label.'
)

assert(
  pageSource.includes('schedule-order-pool__replan-available') && pageSource.includes('可重排'),
  'Selectable rows must expose the positive replan state in the same stable column.'
)

console.log('PASS: schedule-order disabled selection reason static contract')
