const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const listPage = readSource('src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const formInstanceApi = readSource('src/api/form-center/instance.ts')
const businessActionApi = readSource('src/api/form-center/businessAction.ts')
const policySetupE2e = readSource('tests/e2e/edhr-batch-void-policy-ui-setup.e2e.cjs')
const realSubmitE2e = readSource('tests/e2e/edhr-batch-void-form-center-real-submit.e2e.cjs')

assert.match(
  listPage,
  /buildVoidBusinessActionContext[\s\S]*objectType:\s*'EDHR_BATCH_EXECUTION'[\s\S]*actionCode:\s*'VOID'[\s\S]*objectState:\s*'CLOSED'/,
  'eDHR batch void must resolve the exact form-center business action context.'
)

assert.match(
  listPage,
  /createFormInstance[\s\S]*submitFormInstance/,
  'eDHR batch void must submit through form-center instance runtime, not legacy direct void endpoints.'
)

assert.doesNotMatch(
  listPage,
  /requestVoidBatchExecution\(/,
  'eDHR batch void list page must not call the legacy direct request API.'
)

assert.match(
  formInstanceApi,
  /url:\s*'\/form-center\/instances'/,
  'form-center instance API must expose the official create instance endpoint.'
)

assert.match(
  businessActionApi,
  /url:\s*'\/form-center\/actions\/resolve'/,
  'form-center business action API must expose the official resolve endpoint.'
)

assert.match(
  policySetupE2e,
  /EDHR_BATCH_VOID_E2E_TARGET_APPROVAL_MODE/,
  'eDHR batch void policy setup E2E must support explicit DIRECT/BPM_REQUIRED target mode.'
)

assert.match(
  policySetupE2e,
  /objectType\s*===\s*'EDHR_BATCH_EXECUTION'[\s\S]*actionCode\s*===\s*'VOID'[\s\S]*objectState\s*===\s*'CLOSED'/,
  'eDHR batch void policy setup E2E must target only MES EDHR_BATCH_EXECUTION VOID CLOSED policy.'
)

assert.match(
  policySetupE2e,
  /switch-approval-mode/,
  'eDHR batch void policy setup E2E must use the official form-center approval-mode switch endpoint through the page.'
)

assert.match(
  realSubmitE2e,
  /EDHR_BATCH_VOID_E2E_APPROVAL_MODE/,
  'eDHR batch void submit E2E must support DIRECT and BPM_REQUIRED assertions.'
)

assert.match(
  realSubmitE2e,
  /\/mes\/pro\/feedback\/edhr-batch-execution/,
  'eDHR batch void submit E2E must operate the real batch execution list page.'
)

assert.match(
  realSubmitE2e,
  /assertDirectVoidEvidence/,
  'eDHR batch void submit E2E must assert DIRECT terminal state and no BPM process.'
)

assert.match(
  realSubmitE2e,
  /assertBpmRequiredVoidEvidence/,
  'eDHR batch void submit E2E must assert BPM_REQUIRED pending state and BPM side effects.'
)

assert.match(
  realSubmitE2e,
  /completeVoidApprovalFromTodo/,
  'eDHR batch void submit E2E must be able to complete the approval through the unified approval center.'
)

assert.match(
  realSubmitE2e,
  /EDHR_BATCH_VOID_E2E_RESUME_BATCH_EXECUTION_ID/,
  'eDHR batch void submit E2E must support resuming a real pending BPM process after a selector or approver mismatch.'
)

assert.match(
  realSubmitE2e,
  /redactSensitiveRequestPayload[\s\S]*signaturePassword:\s*'\[REDACTED\]'/,
  'eDHR batch void E2E artifacts must redact the approval signature password.'
)

console.log('PASS: eDHR batch void form-center real E2E static contract')
