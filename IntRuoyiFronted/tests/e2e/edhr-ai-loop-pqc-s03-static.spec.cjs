const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = process.cwd()
const runner = fs.readFileSync(path.resolve(root, 'tests/e2e/edhr-ai-loop/runner.cjs'), 'utf8')
const frontlinePanel = fs.readFileSync(path.resolve(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')
const teamLeaderPage = fs.readFileSync(path.resolve(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
const pqcLeaderPage = fs.readFileSync(path.resolve(root, 'src/views/mes/pro/processpool/PqcLeaderWorkbenchPage.vue'), 'utf8')

for (const selector of [
  'data-frontline-pqc-operator',
  'data-pqc-order-search-input',
  'data-pqc-order-option',
  'data-pqc-order-option-code',
  'data-pqc-process-current',
  'data-pqc-process-option',
  'data-pqc-inspection-type-tab',
  'data-pqc-task-option',
  'data-pqc-inspection-tab',
  'data-pqc-inspection-quantity',
  'data-pqc-scrap-quantity',
  'data-pqc-piece-open-button',
  'data-pqc-piece-number-input',
  'data-pqc-piece-confirm',
  'data-pqc-bulk-pass',
  'data-pqc-submit-open-signature',
  'data-pqc-signature-dialog',
  'data-pqc-signature-password',
  'data-pqc-submit-confirm-accept'
]) {
  assert.match(frontlinePanel, new RegExp(selector), `frontline PQC page missing ${selector}`)
}

assert.match(pqcLeaderPage, /data-pqc-leader-workbench-page/, 'PQC leader wrapper page missing data-pqc-leader-workbench-page')

for (const selector of [
  'data-pqc-leader-module-tab-management',
  'data-pqc-leader-work-order',
  'data-team-leader-review-event-id',
  'data-team-leader-review-dialog',
  'data-team-leader-review-status',
  'data-team-leader-review-signature-password',
  'data-team-leader-review-submit'
]) {
  assert.match(teamLeaderPage, new RegExp(selector), `PQC leader review page missing ${selector}`)
}

assert.match(runner, /async function submitPqcInspections\(page, manifestOrder, production\)/)
assert.match(runner, /async function submitOnePqcInspectionRound\(/)
assert.match(runner, /async function reviewPqcInspectionSubmission\(/)
assert.match(runner, /\/mes\/pro\/feedback\/edhr-batch-pqc-fill/)
assert.match(runner, /data-pqc-order-option/)
assert.match(runner, /data-pqc-process-option/)
assert.match(runner, /data-pqc-inspection-rule-tab/)
assert.match(runner, /data-pqc-task-option/)
assert.match(runner, /data-pqc-piece-number-input/)
assert.match(runner, /data-pqc-signature-password/)
assert.match(runner, /\/mes\/pro\/feedback\/frontline\/device-account\/pqc\/submit/)
assert.match(runner, /\/mes\/pro\/process-pool\/pqc-leader/)
assert.match(runner, /data-pqc-leader-work-order/)
assert.match(runner, /\/mes\/pro\/process-pool\/team-leader\/submission\/review/)
assert.match(runner, /pqcSubmissionCount:\s*8/, 'S03 should submit 8 visible rounds in the fixed main chain')
assert.match(runner, /pqcReviewCount:\s*8/, 'S03 should review 8 submitted PQC events in the fixed main chain')
assert.match(runner, /async function submitOnePqcInspectionForProcess\(page, manifestOrder, step\)/, 'runner must be able to submit each PQC round in the interleaved chain')
assert.match(runner, /processExecution\.pqc/, 'runner must carry S03 output from the interleaved chain into the final report')
assert.match(runner, /const completion = await runWithStage\('S04',[\s\S]*completeActiveOrderAndApplyRelease\(page, mainOrder\)/, 'runner must continue from S03 into S04 instead of stopping after PQC review')
assert.match(runner, /failedStage:\s*null/, 'after S08 succeeds, runner must clear failedStage')
assert.match(runner, /stageResults\(null,\s*'PASS'\)/, 'S01-S08 PASS must be explicit')
assert.doesNotMatch(runner, /page\.request\.(post|get|put|delete)/)
assert.doesNotMatch(runner, /fetch\(/)

console.log('PASS: eDHR AI loop S03 PQC submission and review static contract')
