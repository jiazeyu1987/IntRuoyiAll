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

const fixedTemplate = 'SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891'

assert.match(manifest, new RegExp(fixedTemplate), 'manifest must default to the fixed source work order')
assert.match(runner, /FIXED_TEMPLATE_WORK_ORDER_CODE/, 'runner must import the fixed source work order constant')
assert.match(
  runner,
  /const templateWorkOrderCode = arg\('template-work-order-code'\) \|\| process\.env\.EDHR_AI_E2E_TEMPLATE_WORK_ORDER_CODE \|\| FIXED_TEMPLATE_WORK_ORDER_CODE/,
  'runner must use the fixed source work order constant when no override is provided'
)
assert.match(
  runner,
  /buildManifest\(\{ runId, mode, templateWorkOrderCode \}\)/,
  'runner must pass the resolved source work order into the run manifest'
)
assert.match(runner, /active-order\/simulation\/copy-latest/, 'runner must create repeatable test orders through the copy-latest page action')
assert.match(runner, /data-team-leader-copy-latest-simulation-order/, 'runner must click the real copy-test-order button')
assert.match(runner, /确认复制/, 'runner must confirm the frontend copy-test-order dialog')
assert.match(runner, /erpFixedQuantitySnapshot/, 'runner must read copied-order quantity from active-order page data')
assert.match(runner, /simulationRunId/, 'runner must bind each copied order to the run-specific simulationRunId')
assert.match(runner, /targetRequestLabel[\s\S]*SIMULATION_COPY_LATEST/, 'runner must label copy-latest request evidence')
assert.doesNotMatch(runner, /create-ai-e2e-production-order/, 'runner must not depend on K3 or ERP write-style order creation')
assert.doesNotMatch(runner, /data-edhr-ai-e2e-open-global/, 'runner must not open the old AI E2E create-order dialog')
assert.doesNotMatch(runner, /active-order\/add/, 'runner must not add generated ERP orders through the old active-order add flow')
assert.match(teamLeaderPage, /data-team-leader-copy-latest-simulation-order/, 'frontend must expose the copy-test-order button for Playwright')

console.log('PASS: eDHR AI loop fixed-template copy contract')
