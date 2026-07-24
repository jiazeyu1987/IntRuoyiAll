const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
assert(fs.existsSync(pagePath), 'Schedule order page must exist.')

const source = fs.readFileSync(pagePath, 'utf8')

assert.ok(
  source.includes('const replanPreviewStale = computed(() => {'),
  'Manual replan preview stale guard must exist.'
)
assert.ok(
  source.includes('const buildReplanRequestIfReady = (): ProTaskAutoSchedulePreviewReqVO | undefined => {'),
  'Manual replan preview stale guard must compare against a rebuildable request.'
)
assert.ok(
  source.includes('lastReplanRequest.value = request'),
  'Preview generation must record the last replan request.'
)

const staleMarker = "JSON.stringify(lastReplanRequest.value) !== JSON.stringify(currentRequest)"
assert.ok(source.includes(staleMarker), 'Preview stale check must compare last request with current request.')

const buildRequestStart = source.indexOf('const buildReplanRequest = (startTime?: string): ProTaskAutoSchedulePreviewReqVO => {')
assert.ok(buildRequestStart >= 0, 'buildReplanRequest block must exist.')
const buildRequestEnd = source.indexOf('\nconst buildPreflightRequestByReplanRequest = (', buildRequestStart)
assert.ok(buildRequestEnd > buildRequestStart, 'buildReplanRequest block must end before buildPreflightRequestByReplanRequest.')
const buildRequestBlock = source.slice(buildRequestStart, buildRequestEnd)

assert.ok(
  !buildRequestBlock.includes('reason: replanForm.reason?.trim()'),
  'Preview request must not include business reason; otherwise the new preview is immediately judged stale.'
)

const buildIfReadyStart = source.indexOf('const buildReplanRequestIfReady = (): ProTaskAutoSchedulePreviewReqVO | undefined => {')
assert.ok(buildIfReadyStart >= 0, 'buildReplanRequestIfReady block must exist.')
const buildIfReadyEnd = source.indexOf('\nconst buildPreflightRequestIfReady =', buildIfReadyStart)
assert.ok(buildIfReadyEnd > buildIfReadyStart, 'buildReplanRequestIfReady block must end before buildPreflightRequestIfReady.')
const buildIfReadyBlock = source.slice(buildIfReadyStart, buildIfReadyEnd)

assert.ok(
  buildIfReadyBlock.includes('preserveManualLockedTasks: replanForm.preserveManualLockedTasks'),
  'Preview stale request must continue tracking manual-lock preservation flag.'
)
assert.ok(
  !buildIfReadyBlock.includes('reason: replanForm.reason?.trim()'),
  'Business reason must not participate in preview stale comparison.'
)
assert.ok(
  source.includes('reason: replanForm.reason?.trim() || undefined'),
  'Apply request must still append the latest business reason when user finally clicks apply, but allow blank reason to be omitted.'
)

console.log('PASS: MES schedule order replan apply enabled static regression contract')
