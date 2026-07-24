const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/task/autoSchedule/index.ts')

assert(fs.existsSync(pagePath), 'Schedule order pool page must exist.')
assert(fs.existsSync(apiPath), 'Auto schedule API module must exist.')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

assert(
  apiSource.includes('workOrderCode?: string'),
  'Protected task API type must expose workOrderCode.'
)
assert(
  apiSource.includes('processName?: string'),
  'Protected task API type must expose processName.'
)

assert(
  pageSource.includes('replanFeedbackProtectionDialogVisible'),
  'Replan preview must maintain feedback protection dialog visible state.'
)
assert(
  pageSource.includes('replanFeedbackProtectedTasks'),
  'Replan preview must derive feedback protected tasks from protectedTasks.'
)
assert(
  pageSource.includes("task.protectionReason === 'FEEDBACK'") ||
    pageSource.includes('task.protectionReason === "FEEDBACK"'),
  'Feedback protection count must only include FEEDBACK protection reason.'
)
assert(
  pageSource.includes('replanFeedbackProtectionCount'),
  'Replan preview must expose feedback protection count for the button.'
)
assert(
  pageSource.includes('报工保护({{ replanFeedbackProtectionCount }})'),
  'Replan preview must show feedback protection button with count.'
)
assert(
  pageSource.includes('title="报工保护明细"'),
  'Feedback protected tasks must open in a dedicated dialog.'
)

const summaryStart = pageSource.indexOf('<div v-if="replanPreview" class="schedule-order-pool__replan-summary">')
const issueTableStart = pageSource.indexOf('v-if="replanIssueRows.length"', summaryStart)
assert(
  summaryStart >= 0 && issueTableStart > summaryStart,
  'Replan preview summary block must be parseable.'
)
const summarySource = pageSource.slice(summaryStart, issueTableStart)

assert(
  !summarySource.includes('v-if="replanPreview.protectedTasks?.length"'),
  'Protected task detail table must not be rendered inline in the preview summary.'
)

const protectedTableStart = pageSource.indexOf(':data="replanFeedbackProtectedTasks"')
const protectedTableEnd = pageSource.indexOf('</el-table>', protectedTableStart)
assert(
  protectedTableStart >= 0 && protectedTableEnd > protectedTableStart,
  'Feedback protected task dialog table must exist.'
)
const protectedTableSource = pageSource.slice(protectedTableStart, protectedTableEnd)

assert(
  !protectedTableSource.includes('prop="taskCode"'),
  'Protected task table must not display taskCode directly.'
)
assert(
  protectedTableSource.includes('label="任务"'),
  'Protected task table must keep the readable task column.'
)
assert(
  protectedTableSource.includes('formatProtectedTaskLabel'),
  'Protected task table must format task label from work order and process.'
)
assert(
  protectedTableSource.includes('formatProtectionReason'),
  'Protected task table must map protection reason to readable Chinese text.'
)

for (const [raw, readable] of [
  ['FEEDBACK', '已报工'],
  ['FINISHED', '已完成'],
  ['IN_PROGRESS', '进行中'],
  ['LOCKED', '已锁定'],
  ['MANUAL', '人工任务']
]) {
  assert(
    pageSource.includes(`${raw}: '${readable}'`) ||
      pageSource.includes(`'${raw}': '${readable}'`) ||
      pageSource.includes(`"${raw}": '${readable}'`) ||
      pageSource.includes(`'${raw}': "${readable}"`) ||
      pageSource.includes(`"${raw}": "${readable}"`),
    `Protected reason ${raw} must map to ${readable}.`
  )
}

console.log('PASS: MES schedule order protected task readable static contract')
