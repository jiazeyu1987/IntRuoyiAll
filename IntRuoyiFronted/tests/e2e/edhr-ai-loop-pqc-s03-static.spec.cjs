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
  'data-pqc-inspection-item-code',
  'data-pqc-inspection-rule-tab',
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

assert.match(frontlinePanel, /const scopeItemKeys = new Set/, 'PQC submit validation must derive item scope from configured task options')
assert.match(frontlinePanel, /const scopeInspectionItems = pqcInspectionItems\.value\.filter/, 'PQC submit validation must not require unconfigured inspection items')
assert.match(frontlinePanel, /const buildPqcSelectedProcessSnapshot = \(/, 'PQC submit payload must build a task-scoped formal process snapshot')
assert.match(frontlinePanel, /selectedProcess: buildPqcSelectedProcessSnapshot\(process, taskOption\)/, 'PQC submit payload must carry only the current formal task process snapshot')
assert.doesNotMatch(frontlinePanel, /selectedProcess:\s*\{\s*\.\.\.process\s*\}/, 'PQC submit payload must not duplicate every process task in each submission')
assert.match(frontlinePanel, /const filteredPqcProcessOptions = computed\(\(\) => \{[\s\S]*selectedPqcInspectionRuleKey\.value[\s\S]*hasExecutablePqcTaskForRule\(process, ruleKey\)/, 'PQC process navigation must filter formal processes by the selected inspection rule')
assert.match(frontlinePanel, /const processOptions = isPqcMode\.value\s*\?\s*filteredPqcProcessOptions\.value\s*:\s*switchableProcessOptions\.value/, 'PQC process picker must only show processes that support the currently selected inspection rule')
assert.match(frontlinePanel, /const selectedPqcProcessIndex = computed\(\(\) => \{[\s\S]*filteredPqcProcessOptions\.value\.findIndex/, 'PQC process arrows must resolve neighbors from the filtered process list')
assert.match(frontlinePanel, /showFrontlineError\(`当前工序没有可执行的\$\{PQC_INSPECTION_RULE_LABELS\[selectedRuleKey\]\}任务。`\)[\s\S]*return/, 'PQC process selection must reject a bypassed process that does not support the selected rule')
assert.doesNotMatch(frontlinePanel, /const nextRuleKey = findFirstPqcInspectionRuleKey\(\[process\]\)[\s\S]*selectedPqcInspectionRuleKey\.value = nextRuleKey/, 'PQC process selection must not rebase the inspection type to fit the target process')

assert.match(pqcLeaderPage, /data-pqc-leader-workbench-page/, 'PQC leader wrapper page missing data-pqc-leader-workbench-page')

for (const selector of [
  'data-pqc-leader-module-tab-management',
  'data-pqc-leader-work-order',
  'data-team-leader-review-event-id',
  'data-pqc-leader-pqc-task-id',
  'data-pqc-leader-submitted-event-ids',
  'data-team-leader-review-dialog',
  'data-team-leader-review-status',
  'data-team-leader-review-signature-password',
  'data-team-leader-review-submit'
]) {
  assert.match(teamLeaderPage, new RegExp(selector), `PQC leader review page missing ${selector}`)
}

assert.match(runner, /async function discoverPendingPqcTasksForOrder\(/)
assert.match(runner, /async function submitOnePqcInspectionRound\(/)
assert.match(runner, /async function reviewPqcInspectionSubmission\(/)
assert.match(runner, /\/mes\/pro\/feedback\/edhr-batch-pqc-fill/)
assert.match(runner, /data-pqc-order-option/)
assert.match(runner, /data-pqc-process-option/)
assert.match(runner, /data-pqc-inspection-rule-tab/)
assert.doesNotMatch(runner, /data-pqc-task-option/, 'PQC runner must use formal rule buttons instead of current-process task-id buttons')
assert.match(runner, /data-pqc-piece-number-input/)
assert.match(runner, /data-pqc-signature-password/)
assert.match(runner, /\/mes\/pro\/feedback\/frontline\/device-account\/pqc\/submit/)
assert.match(runner, /\/mes\/pro\/process-pool\/pqc-leader/)
assert.match(runner, /data-pqc-leader-work-order/)
assert.match(runner, /\/mes\/pro\/process-pool\/team-leader\/submission\/review/)
assert.doesNotMatch(runner, /data-team-leader-review-signature-password\] input/, 'PQC review signature locator must target the rendered input directly')
assert.match(runner, /async function submitOnePqcInspectionForProcess\(page, manifestOrder, step\)/, 'runner must be able to submit each PQC round in the interleaved chain')
assert.match(runner, /processExecution\.pqc/, 'runner must carry S03 output from the interleaved chain into the final report')
assert.match(runner, /const completion = await runWithStage\('S04',[\s\S]*completeActiveOrderAndApplyRelease\(page, mainOrder\)/, 'runner must continue from S03 into S04 instead of stopping after PQC review')
assert.match(runner, /failedStage:\s*null/, 'after S08 succeeds, runner must clear failedStage')
assert.match(runner, /stageResults\(null,\s*'PASS'\)/, 'S01-S08 PASS must be explicit')
assert.doesNotMatch(runner, /page\.request\.(post|get|put|delete)/)
assert.doesNotMatch(runner, /fetch\(/)

console.log('PASS: eDHR AI loop S03 PQC submission and review static contract')

assert.match(runner, /Promise.all\(step.tasks.map/, 'must collect every grouped task receipt')
assert.match(runner, /leader page reviews the formal group once/, 'PQC grouped tasks must share one formal leader review')
assert.match(runner, /groupedPqcTaskIds:\s*submissions\.map/, 'PQC review evidence must retain every grouped task id')
assert.doesNotMatch(runner, /for \(const submission of submissions\) \{[\s\S]*reviewPqcInspectionSubmission\(page, manifestOrder, submission\)/, 'PQC review must not repeat review per item inside one formal submit group')
assert.match(runner, /data\.sourceRevision/, 'PQC submit receipt must expose the formal source event used by the review list')
assert.match(runner, /当前工单下待复核PQC提交必须唯一/, 'PQC review must fail fast instead of guessing when multiple review rows exist')
assert.doesNotMatch(runner, /data-team-leader-review-event-id="\$\{requirePositiveIdString\(submission\.pqcEventId/, 'PQC review must not treat the submit source event as the review row id')
assert.match(teamLeaderPage, /formatSubmissionSourceEventIds\(row\)/, 'PQC review page must expose formal submit source event ids for exact E2E review targeting')
assert.match(teamLeaderPage, /row\.groupedEventIds/, 'PQC review source event ids must include grouped formal submit events')
assert.doesNotMatch(runner, /pqcSubmissionCount:\s*8|pqcReviewCount:\s*8/)
assert.match(runner, /formalIdentity/, 'PQC runtime submission coverage must use formal task identity')
assert.doesNotMatch(runner, /baseline\.pqcTasks\.find\(t => t\.pqcTaskId ===/, 'PQC runtime rediscovery must not depend on frozen database task ids')
assert.match(runner, /async function ensurePqcSession/, 'long-running PQC E2E must re-authenticate through the real login page after session expiry')
assert.match(runner, /async function waitForLoginFormShell/, 'login recovery must wait for the real login form shell before selecting tenant')
assert.match(runner, /async function waitForLoginOrAuthenticated/, 'login recovery must continue when refresh-token restores an authenticated page instead of rendering the login form')
assert.match(runner, /const loginState = await waitForLoginOrAuthenticated\(page\)[\s\S]*loginState === 'AUTHENTICATED'[\s\S]*return/, 'login recovery must not wait for tenant controls after the app already redirected to an authenticated page')
assert.match(runner, /waitForResponse\(\(r\) => r\.url\(\)\.includes\('\/admin-api\/system\/auth\/login'\)[\s\S]*\{ timeout: 60000 \}/, 'login response wait must be bounded so E2E cannot hang silently before S01')
assert.match(runner, /page\.setDefaultTimeout\(60000\)[\s\S]*page\.setDefaultNavigationTimeout\(60000\)/, 'full runner must bound all Playwright waits instead of hanging indefinitely')
assert.match(runner, /ensurePqcSession\(page, manifestOrder, step\.processKey\)/, 'PQC submission must restore the current order and process after re-authentication')
assert.match(runner, /async function submitOnePqcInspectionRound\(page, manifestOrder, step, sessionRecoveryAttempted = false\)/, 'PQC submit must have a bounded session recovery guard')
assert.match(runner, /Number\(body\.code\) === 401[\s\S]*submitOnePqcInspectionRound\(page, manifestOrder, step, true\)/, 'PQC submit must retry the same formal submit after real re-authentication')
assert.match(runner, /async function reviewPqcInspectionSubmission\(page, manifestOrder, submission, sessionRecoveryAttempted = false\)/, 'PQC review must have a bounded session recovery guard')
assert.match(runner, /Number\(body\.code\) === 401[\s\S]*reviewPqcInspectionSubmission\(page, manifestOrder, submission, true\)/, 'PQC review must retry the same formal event after real re-authentication')
