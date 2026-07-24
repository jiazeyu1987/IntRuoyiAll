const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/scheduleorder/index.ts')
const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')

const apiSource = fs.readFileSync(apiPath, 'utf8')
const pageSource = fs.readFileSync(pagePath, 'utf8')

for (const token of [
  '/mes/pro/schedule-order/update',
  '/mes/pro/schedule-order/freeze',
  '/mes/pro/schedule-order/batch-delete',
  '/mes/pro/schedule-order/operation-log',
  'MesProScheduleOrderUpdateReqVO',
  'MesProScheduleOrderBatchReqVO',
  'MesProScheduleOrderOperationLogVO',
  'frozen',
  'freezeReason'
]) {
  assert(apiSource.includes(token), `Schedule order API must include ${token}.`)
}

assert(pageSource.includes('批量冻结'), 'Schedule order page must expose batch freeze action.')
assert(pageSource.includes('批量删除'), 'Schedule order page must expose batch delete action.')
assert(pageSource.includes('冻结状态'), 'Schedule order list must expose freeze state.')
assert(!pageSource.includes('openAdjustDialog'), 'Schedule order row must not expose the legacy adjust dialog.')
assert(pageSource.includes('openPriorityDialog'), 'Schedule order row must expose the priority adjust dialog.')
assert(pageSource.includes('openPromiseDateDialog'), 'Schedule order row must keep the promise date dialog.')
assert(pageSource.includes('openFreezeDialog'), 'Schedule order row must open the freeze dialog.')
assert(pageSource.includes('openOperationLogDialog'), 'Schedule order page must expose operation trace dialog.')
assert(!pageSource.includes('调整排产工单'), 'Removed adjust dialog must not remain in the page.')

for (const label of ['调整', '交期', '冻结', '设为已完成', '撤销已完成', '追溯']) {
  assert(pageSource.includes(label), `Schedule order page must include two-character action ${label}.`)
}

for (const handler of [
  'submitPriorityAdjust',
  'submitPromiseDateReset',
  'submitScheduleOrderFreeze',
  'submitScheduleOrderDelete',
  'handleBatchFreeze',
  'handleBatchDelete'
]) {
  assert(pageSource.includes(handler), `Schedule order page must include ${handler}.`)
}

assert(
  pageSource.includes("v-hasPermi=\"['mes:pro-schedule-order:delete']\""),
  'Delete actions must be protected by mes:pro-schedule-order:delete permission.'
)
assert(
  pageSource.includes("v-hasPermi=\"['mes:pro-schedule-order:update']\""),
  'Priority, freeze, and promise date actions must be protected by update permission.'
)

console.log('PASS: MES schedule order freeze audit static contract')
