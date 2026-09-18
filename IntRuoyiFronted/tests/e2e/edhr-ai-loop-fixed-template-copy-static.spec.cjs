const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const runner = fs.readFileSync(
  path.resolve(process.cwd(), 'tests/e2e/edhr-ai-loop/runner.cjs'),
  'utf8'
)
const manifest = fs.readFileSync(
  path.resolve(process.cwd(), 'tests/e2e/edhr-ai-loop/manifest.cjs'),
  'utf8'
)
const teamLeaderPage = fs.readFileSync(
  path.resolve(process.cwd(), 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

const fixedResetOrder = 'SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891'

assert.match(manifest, new RegExp(fixedResetOrder), 'manifest must default to the fixed reset work order')
assert.match(runner, /FIXED_RESET_WORK_ORDER_CODE/, 'runner must import the fixed reset work order constant')
assert.match(
  runner,
  /const resetWorkOrderCode = arg\('reset-work-order-code'\) \|\| process\.env\.EDHR_AI_E2E_RESET_WORK_ORDER_CODE \|\| FIXED_RESET_WORK_ORDER_CODE/,
  'runner must use the fixed reset work order constant when no reset override is provided'
)
assert.match(
  runner,
  /buildManifest\(\{ runId, mode, resetWorkOrderCode \}\)/,
  'runner must pass the resolved reset work order into the run manifest'
)
assert.match(runner, /active-order\/simulation\/test-reset/, 'runner must create repeatable test orders through the reset-fixed-test-order page action')
assert.match(runner, /data-team-leader-reset-fixed-active-order/, 'runner must click the real reset-fixed-test-order button')
assert.doesNotMatch(runner, /confirmDialog|confirmText/, 'S01 must reset directly without confirmation')
assert.match(runner, /erpFixedQuantitySnapshot/, 'runner must read reset-order quantity from active-order page data')
assert.match(runner, /aiRunOrderSlotId:[\s\S]*manifestOrder\.simulationRunId/, 'runner must bind the reset result to the run-specific order slot')
assert.match(runner, /targetRequestLabel[\s\S]*SIMULATION_TEST_RESET/, 'runner must label test-reset request evidence')
assert.doesNotMatch(runner, /create-ai-e2e-production-order/, 'runner must not depend on K3 or ERP write-style order creation')
assert.doesNotMatch(runner, /data-edhr-ai-e2e-open-global/, 'runner must not open the old AI E2E create-order dialog')
assert.doesNotMatch(runner, /active-order\/add/, 'runner must not add generated ERP orders through the old active-order add flow')
assert.match(teamLeaderPage, /data-team-leader-reset-fixed-active-order/, 'frontend must expose the reset-fixed-test-order button for Playwright')

console.log('PASS: eDHR AI loop fixed test reset contract')
