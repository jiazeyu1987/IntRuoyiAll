const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const workOrderPage = fs.readFileSync(
  path.resolve(process.cwd(), 'src/views/mes/pro/workorder/index.vue'),
  'utf8'
)
const teamLeaderPage = fs.readFileSync(
  path.resolve(process.cwd(), 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)
const runner = fs.readFileSync(
  path.resolve(process.cwd(), 'tests/e2e/edhr-ai-loop/runner.cjs'),
  'utf8'
)

assert.match(workOrderPage, /data-work-order-sync-kingdee/, 'work-order sync button needs a stable selector')
assert.match(workOrderPage, /data-work-order-code/, 'work-order code cell needs a stable selector')
assert.match(workOrderPage, /data-work-order-quantity/, 'work-order quantity cell needs a stable selector')

assert.match(
  teamLeaderPage,
  /data-team-leader-active-order-work-order-filter/,
  'active-order pool needs a visible work-order filter so repeatable E2E can locate this run order beyond the first page'
)
assert.match(
  teamLeaderPage,
  /data-team-leader-active-order-candidate-select/,
  'active-order candidate select needs a stable selector'
)
assert.match(
  teamLeaderPage,
  /data-team-leader-active-order-candidate-option/,
  'active-order candidate option needs a stable selector'
)
assert.match(
  teamLeaderPage,
  /data-team-leader-active-order-candidate-code/,
  'active-order candidate option must expose the work-order code'
)
assert.match(
  teamLeaderPage,
  /data-team-leader-active-order-candidate-state/,
  'active-order candidate option must expose the candidate state'
)
assert.match(
  teamLeaderPage,
  /data-team-leader-active-order-add-submit/,
  'active-order add submit button needs a stable selector'
)
assert.match(
  runner,
  /async function filterActiveOrderPool\(page, workOrderCode\)/,
  'runner must filter active-order pool by this run work-order code before row assertions'
)
assert.match(
  runner,
  /data-team-leader-active-order-work-order-filter/,
  'runner must use the visible active-order work-order filter'
)

console.log('PASS: eDHR AI loop active-order page selectors')
