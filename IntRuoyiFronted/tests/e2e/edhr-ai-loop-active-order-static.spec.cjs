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

assert.match(
  teamLeaderPage,
  /data-team-leader-active-order-work-order-filter/,
  'active-order pool needs a visible work-order filter so repeatable E2E can locate this run order beyond the first page'
)
assert.match(
  teamLeaderPage,
  /data-team-leader-active-order-candidate-select/,
  'legacy active-order candidate select can remain for manual operations'
)
assert.match(
  teamLeaderPage,
  /data-team-leader-copy-latest-simulation-order/,
  'active-order row needs a copy-test-order button for repeatable AI E2E'
)
assert.match(
  runner,
  /async function filterActiveOrderPool\(page, workOrderCode\)/,
  'runner must filter active-order pool by fixed source and copied work-order code before row assertions'
)
assert.match(
  runner,
  /data-team-leader-active-order-work-order-filter/,
  'runner must use the visible active-order work-order filter'
)

assert.match(
  runner,
  /async function copyTemplateActiveOrder\(page, manifestOrder, manifest, ready\)/,
  'runner must copy the fixed source active order instead of adding generated ERP orders'
)
assert.doesNotMatch(
  runner,
  /active-order\/add/,
  'runner must not use the legacy active-order add endpoint for AI loop setup'
)

console.log('PASS: eDHR AI loop active-order page selectors')

