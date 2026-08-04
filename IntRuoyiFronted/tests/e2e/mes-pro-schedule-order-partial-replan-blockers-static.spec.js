const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
const scheduleOrderApiPath = path.resolve(process.cwd(), 'src/api/mes/pro/scheduleorder/index.ts')
const autoScheduleApiPath = path.resolve(process.cwd(), 'src/api/mes/pro/task/autoSchedule/index.ts')

assert(fs.existsSync(pagePath), 'Schedule order page must exist.')
assert(fs.existsSync(scheduleOrderApiPath), 'Schedule order API type file must exist.')
assert(fs.existsSync(autoScheduleApiPath), 'Auto schedule API type file must exist.')

const source = fs.readFileSync(pagePath, 'utf8')
const scheduleOrderApi = fs.readFileSync(scheduleOrderApiPath, 'utf8')
const autoScheduleApi = fs.readFileSync(autoScheduleApiPath, 'utf8')

assert.ok(
  scheduleOrderApi.includes('blockingIssueCount?: number'),
  'Schedule order rows must expose the persisted blocking issue count.'
)
assert.ok(
  scheduleOrderApi.includes('latestBlockingIssueMessage?: string'),
  'Schedule order rows must expose the latest blocking issue reason.'
)
assert.ok(
  autoScheduleApi.includes('appliedWorkOrderCount: number'),
  'Replan summary must expose applied work order count.'
)
assert.ok(
  autoScheduleApi.includes('blockedWorkOrderCount: number'),
  'Replan summary must expose blocked work order count.'
)
assert.ok(
  autoScheduleApi.includes('skippedWorkOrderCount: number'),
  'Replan summary must expose skipped work order count.'
)

const rowClassStart = source.indexOf('const getScheduleOrderRowClassName')
assert.ok(rowClassStart >= 0, 'Schedule order row class helper must exist.')
const rowClassEnd = source.indexOf('\nconst getScheduleOrderReplanBlockReason', rowClassStart)
assert.ok(rowClassEnd > rowClassStart, 'Schedule order row class helper block must be bounded.')
const rowClassBlock = source.slice(rowClassStart, rowClassEnd)
assert.ok(
  rowClassBlock.includes('schedule-order-pool__row--blocked'),
  'Rows with blocking issues must receive a red blocked row class.'
)
assert.ok(
  rowClassBlock.includes('row.blockingIssueCount'),
  'Blocked row class must be driven by blockingIssueCount from the backend.'
)

assert.ok(
  source.includes('latestBlockingIssueMessage'),
  'Schedule order table must render or tooltip the latest blocking reason.'
)
assert.ok(
  source.includes('schedule-order-pool__blocking-reason'),
  'Blocked work order reason must use a stable visible class for review.'
)

const projectionStart = source.indexOf('const resolveScheduleReplanProjection = () => {')
assert.ok(projectionStart >= 0, 'Replan projection helper must exist.')
const projectionEnd = source.indexOf('\nconst replanProjectionState', projectionStart)
assert.ok(projectionEnd > projectionStart, 'Replan projection helper block must be bounded.')
const projectionBlock = source.slice(projectionStart, projectionEnd)
assert.ok(
  !projectionBlock.includes("preflightHasBlockedIssue.value && '排产前检查存在阻断问题，不能应用重排'"),
  'Preflight issues attributable to selected work orders must not disable applying the schedulable remainder.'
)
assert.ok(
  !projectionBlock.includes("replanPreviewHasBlockedIssue.value && '重排预览存在阻断问题，不能应用重排'"),
  'Preview issues attributable to selected work orders must not disable applying the schedulable remainder.'
)
assert.ok(
  !/locked:\s*preflightHasBlockedIssue\.value\s*\|\|\s*replanPreviewHasBlockedIssue\.value/.test(
    projectionBlock
  ),
  'Projection lock must not treat every blocking issue as a whole-action lock.'
)

const confirmStart = source.indexOf('const confirmApplyReplanStartChoice = async () => {')
assert.ok(confirmStart >= 0, 'Replan apply confirmation helper must exist.')
const confirmEnd = source.indexOf('\nconst openDailyCompareDialog', confirmStart)
assert.ok(confirmEnd > confirmStart, 'Replan apply confirmation helper block must be bounded.')
const confirmBlock = source.slice(confirmStart, confirmEnd)
assert.ok(
  !confirmBlock.includes("throw new Error('排产前检查存在阻断问题，不能应用重排')"),
  'Apply path must not throw on attributable preflight blockers before applying the schedulable remainder.'
)
assert.ok(
  !confirmBlock.includes("throw new Error('重排预览存在阻断问题，不能应用重排')"),
  'Apply path must not throw on attributable preview blockers before applying the schedulable remainder.'
)
assert.ok(
  confirmBlock.includes('await confirmSkippedSelectedReplanRows(freshPreview)'),
  'Apply path must still explicitly confirm selected rows skipped by blockers before applying the remainder.'
)

console.log('PASS: MES schedule order partial replan blockers static contract')
