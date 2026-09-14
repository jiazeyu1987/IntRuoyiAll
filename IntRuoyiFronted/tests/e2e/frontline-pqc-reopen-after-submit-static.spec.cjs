const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const panelSource = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const contextSource = read('src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts')

const blockBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start + startToken.length)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

assert.match(
  contextSource,
  /export const invalidateFrontlinePqcProcessCacheForActiveOrder\s*=/,
  'Submitting PQC must have an explicit cache invalidation helper for the active order.'
)
const invalidateBlock = blockBetween(
  contextSource,
  'export const invalidateFrontlinePqcProcessCacheForActiveOrder',
  'const getPqcProcessesWithCache'
)
assert.match(
  invalidateBlock,
  /pqcProcessOptionsCache\.delete\(cacheKey\)/,
  'PQC active-order cache invalidation must remove cached process snapshots.'
)
assert.match(
  invalidateBlock,
  /pqcProcessOptionsRequests\.delete\(requestKey\)/,
  'PQC active-order cache invalidation must also discard in-flight request handles.'
)
assert.match(
  contextSource,
  /pqcProcessCacheInvalidationVersionByOrder/,
  'PQC process cache writes must be guarded against stale in-flight responses after invalidation.'
)

const importBlock = blockBetween(
  panelSource,
  'import {\n  FRONTLINE_PQC_NO_PENDING_ORDER_TEXT',
  "} from './frontlineDeviceEmployeeContext'"
)
assert.match(
  importBlock,
  /invalidateFrontlinePqcProcessCacheForActiveOrder/,
  'PQC submit panel must import the active-order cache invalidation helper.'
)

assert.match(
  panelSource,
  /const updatePqcSubmittedTasksInProcess\s*=\s*\(/,
  'PQC submit panel must update task status through a reusable process snapshot helper.'
)
assert.match(
  panelSource,
  /const syncPqcSubmittedTasksInProcessOptions\s*=\s*\(/,
  'PQC submit panel must synchronize the process picker options after submit.'
)
const syncOptionsBlock = blockBetween(
  panelSource,
  'const syncPqcSubmittedTasksInProcessOptions',
  'const markPqcTasksSubmittedAndSelectNext'
)
assert.match(
  syncOptionsBlock,
  /deviceState\.processOptions = deviceState\.processOptions\.map/,
  'Submitting PQC must update cached picker options, not only the currently selected process.'
)
assert.match(
  syncOptionsBlock,
  /updatePqcSubmittedTasksInProcess\(candidate, submittedTaskIds\)/,
  'Picker synchronization must reuse the same submitted-task status update as the selected process.'
)

const markBlock = blockBetween(
  panelSource,
  'const markPqcTasksSubmittedAndSelectNext',
  'const markPqcTaskSubmittedAndSelectNext'
)
assert.match(
  markBlock,
  /updatePqcSubmittedTasksInProcess\(process, submittedTaskIds\)/,
  'Submitting PQC must mark the selected process task as SUBMITTED before choosing the next task.'
)
assert.match(
  markBlock,
  /syncPqcSubmittedTasksInProcessOptions\(process, submittedTaskIds\)/,
  'Submitting PQC must synchronize the process picker so reopening the process cannot reuse the old pqcTaskId.'
)
assert.match(
  markBlock,
  /invalidateFrontlinePqcProcessCacheForActiveOrder\(deviceState, process\.activeOrderId\)/,
  'Submitting PQC must invalidate the active-order process cache so later reopen fetches fresh task identities.'
)

console.log('frontline-pqc-reopen-after-submit-static: PASS')
