const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = process.cwd()
const runner = fs.readFileSync(path.resolve(root, 'tests/e2e/edhr-ai-loop/runner.cjs'), 'utf8')
const frontlinePanel = fs.readFileSync(path.resolve(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')
const teamLeaderPage = fs.readFileSync(path.resolve(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')

for (const selector of [
  'data-frontline-production-order-search-input',
  'data-frontline-production-order-option',
  'data-frontline-production-order-option-code',
  'data-frontline-production-process-current',
  'data-frontline-production-process-option',
  'data-frontline-production-employee-option',
  'data-production-output-quantity',
  'data-production-submit-open-confirmation',
  'data-production-submit-signature-password',
  'data-production-submit-confirm-accept',
  'data-production-submit-success-dialog'
]) {
  assert.match(frontlinePanel, new RegExp(selector), `frontline production page missing ${selector}`)
}

for (const selector of [
  'data-team-leader-submission-work-order-code',
  'data-team-leader-review-event-id',
  'data-team-leader-review-dialog',
  'data-team-leader-review-status',
  'data-team-leader-review-signature-password',
  'data-team-leader-fifo-allocation',
  'data-team-leader-allocation-summary',
  'data-team-leader-review-submit'
]) {
  assert.match(teamLeaderPage, new RegExp(selector), `team leader review page missing ${selector}`)
}

assert.match(runner, /for \(const step of baseline.productionProcesses\)/)
assert.match(runner, /async function submitOneProductionReport\(/)
assert.match(runner, /async function ensureProductionSession/, 'long-running production E2E must re-authenticate through the real login page after session expiry')
assert.match(runner, /ensureProductionSession\(page, manifestOrder, step\.processKey\)/, 'production submission must restore the current order and process after re-authentication')
assert.match(runner, /async function reviewProductionReport\(/)
assert.match(runner, /\/mes\/pro\/feedback\/edhr-batch-production-fill/)
assert.match(runner, /data-frontline-production-order-option/)
assert.match(runner, /data-frontline-production-process-option/)
assert.match(runner, /data-production-output-quantity/)
assert.match(runner, /data-production-submit-signature-password/)
assert.match(runner, /\/mes\/pro\/feedback\/frontline\/submit/)
assert.match(runner, /data-team-leader-review-event-id/)
assert.match(runner, /data-team-leader-fifo-allocation/)
assert.match(runner, /\/mes\/pro\/process-pool\/team-leader\/submission\/allocation\/confirm/)
assert.doesNotMatch(runner, /data-team-leader-review-signature-password\] input/, 'team leader signature locator must target the rendered input directly')
assert.match(runner, /async function executeProductionAndPqcInterleaved\(page, manifestOrder, activeOrder\)/, 'runner must interleave S02 production with S03 PQC instead of batching all production first')
assert.match(runner, /const processExecution = await runWithStage\('S02',[\s\S]*executeProductionAndPqcInterleaved\(page, mainOrder, activeOrders\[0\]\)/, 'runner must use the interleaved S02/S03 execution plan')
assert.doesNotMatch(runner, /const production = await submitProductionReports\(page, ordersForMode\(manifest\)\[0\], activeOrders\[0\]\)[\s\S]*const pqc = await submitPqcInspections\(page, ordersForMode\(manifest\)\[0\], production\)/, 'runner must not complete all production before all PQC')
assert.doesNotMatch(runner, /page\.request\.(post|get|put|delete)/)
assert.doesNotMatch(runner, /fetch\(/)

console.log('PASS: eDHR AI loop S02 production submission and review static contract')

assert.match(runner, /data-production-report-allocation-event-id/)
