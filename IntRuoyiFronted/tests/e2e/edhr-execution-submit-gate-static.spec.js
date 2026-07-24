const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/ExecutionPage.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/feedback/index.ts')
const realE2ePath = path.resolve(process.cwd(), 'tests/e2e/edhr-batch-execution-submit-review-policy-real.e2e.js')
const source = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')
const realE2eSource = fs.readFileSync(realE2ePath, 'utf8')

assert(
  source.includes('const formSubmitGateError = computed('),
  'ExecutionPage must separate submit/review evidence gate from editable form render gate.'
)

assert(
  source.includes(':disabled="hasSlotContextBlockers || hasPendingFieldChanges"'),
  'Submit button must stay clickable for explicit formSubmitGateError feedback while still blocking slot-context blockers and unsaved field changes.'
)

assert(
  /const formReviewSignGateError = computed\([\s\S]*formSubmitGateError\.value[\s\S]*\)/.test(source),
  'Form review signature gate must use formSubmitGateError.'
)

assert(
  /const openSubmitDialog = \(\) => \{[\s\S]*if \(formSubmitGateError\.value\)/.test(source),
  'Submit dialog must check formSubmitGateError instead of formRenderError.'
)

assert(
  apiSource.includes('reviewAssigneeSelections?: ProFeedbackEdhrReviewAssigneeSelectionVO[]'),
  'Submit API payload must carry selected review/approval assignees when backend returns reviewAssigneeOptions.'
)

assert(
  apiSource.includes('export interface ProFeedbackEdhrReviewAssigneeSelectionVO'),
  'Feedback API must define the review assignee selection payload contract.'
)

assert(
  source.includes('submitReviewAssigneeSelections'),
  'Execution submit dialog must keep selected review/approval assignees in reactive state.'
)

assert(
  source.includes('edhr-page-shell__submit-select'),
  'Execution submit dialog must render reviewer selection controls for reviewAssigneeOptions.'
)

assert(
  /reviewAssigneeSelections:\s*buildSubmitReviewAssigneeSelections\(\)/.test(source),
  'Execution submit request must send selected review/approval assignees to the submit API.'
)

assert(
  realE2eSource.includes('/mes/pro/feedback/edhr-execution/form?id='),
  'Batch execution submit review real E2E must use the current formal execution form route.'
)

assert(
  /JOIN\s+mes_pro_edhr_batch_execution\s+be\s+ON\s+be\.id=wt\.batch_execution_id/.test(realE2eSource),
  'Batch execution submit review real E2E fixture must copy from a source with a real batch execution.'
)

assert(
  /JOIN\s+mes_pro_edhr_batch_execution_task\s+bt\s+ON\s+bt\.id=wt\.batch_task_id/.test(realE2eSource),
  'Batch execution submit review real E2E fixture must copy from a source with a real batch task.'
)

assert(
  realE2eSource.includes('INSERT INTO mes_pro_edhr_batch_execution ('),
  'Batch execution submit review real E2E fixture must create a real batch execution row.'
)

assert(
  realE2eSource.includes('INSERT INTO mes_pro_edhr_batch_execution_task ('),
  'Batch execution submit review real E2E fixture must create a real batch task row.'
)

assert(
  realE2eSource.includes('SET @batch_execution_id := LAST_INSERT_ID();') &&
    realE2eSource.includes('SET @batch_task_id := LAST_INSERT_ID();'),
  'Batch execution submit review real E2E fixture must link execution and work task through created batch identifiers.'
)

assert(
  realE2eSource.includes('INSERT INTO mes_pro_edhr_work_task_assignment_rule (') &&
    realE2eSource.includes("'REVIEW'") &&
    realE2eSource.includes('due_minutes'),
  'Batch execution submit review real E2E fixture must prepare the real REVIEW work-task assignment rule.'
)

assert(
  !realE2eSource.includes("getByText('eDHR 执行详情')"),
  'Batch execution submit review real E2E must not wait for the removed legacy detail title.'
)

assert(
  realE2eSource.includes('EDHR_EXEC_SUBMIT_REVIEW_COMPLETE_APPROVAL') &&
    realE2eSource.includes('/admin-api/approval-center/tasks/review'),
  'Batch execution submit review BPM_REQUIRED real E2E must complete the terminal approval through unified approval center.'
)

assert(
  realE2eSource.includes('findApprovalRowByTask') &&
    /getByText\('审核通过',\s*\{\s*exact:\s*true\s*\}\)/.test(realE2eSource),
  'Batch execution submit review real E2E must locate the exact approval row and click the exact approve option.'
)

assert(
  /redactSensitiveRequestPayload[\s\S]*signaturePassword:\s*'\[REDACTED\]'/.test(realE2eSource),
  'Batch execution submit review E2E artifacts must redact the approval signature password.'
)

console.log('PASS: eDHR execution submit gate static contract')
