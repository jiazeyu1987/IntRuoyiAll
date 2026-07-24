const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const workflowApi = readSource('src/api/dcc/controlledFile/workflow.ts')
const formInstanceApi = readSource('src/api/form-center/instance.ts')
const processInstanceApi = readSource('src/api/bpm/processInstance/index.ts')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const policySetupE2e = readSource('tests/e2e/dcc-obsolete-policy-ui-setup.e2e.cjs')
const realSubmitE2e = readSource('tests/e2e/dcc-obsolete-form-center-real-submit.e2e.cjs')

assert.match(
  formInstanceApi,
  /findActiveBusinessAction/,
  'form-center API must expose active business action lookup for official pages.'
)
assert.match(
  formInstanceApi,
  /url:\s*'\/form-center\/actions\/active-instance'/,
  'active business action lookup must use the backend active-instance route.'
)
assert.match(
  detailPage,
  /resolveBusinessAction/,
  'DCC obsolete dialog must resolve the form-center action policy before submission.'
)
assert.match(
  detailPage,
  /getProcessDefinition\(undefined,\s*resolution\.bpmProcessKey\)/,
  'DCC obsolete dialog must resolve the BPM process definition from the form-center policy key.'
)
assert.match(
  detailPage,
  /getApprovalDetail/,
  'DCC obsolete dialog must load BPM approval detail before submission.'
)
assert.match(
  detailPage,
  /CandidateStrategy\.START_USER_SELECT/,
  'DCC obsolete dialog must detect BPM starter-selected approval nodes.'
)
assert.match(
  detailPage,
  /NodeId\.START_USER_NODE_ID/,
  'DCC obsolete dialog must query start-node approval detail for starter-selected assignees.'
)

assert.match(
  workflowApi,
  /import type \{ FormInstanceVO \} from '@\/api\/form-center\/instance'/,
  'DCC obsolete API must reuse the platform FormInstanceVO response contract.'
)
assert.match(
  workflowApi,
  /idempotencyKey:\s*string/,
  'DCC obsolete request must carry an idempotency key.'
)
assert.match(
  workflowApi,
  /startUserSelectAssignees\?:\s*Record<string,\s*number\[\]>/,
  'DCC obsolete request must support optional starter-selected BPM assignees.'
)
assert.match(
  workflowApi,
  /obsoleteControlledFile[\s\S]*Promise<FormInstanceVO>/,
  'DCC obsolete API must return a form-center instance instead of direct boolean success.'
)

assert.match(
  detailPage,
  /findActiveBusinessAction/,
  'DCC detail page must query the backend active action before showing controlled action state.'
)
assert.match(
  detailPage,
  /DCC_OBSOLETE_ACTION_CODE\s*=\s*'OBSOLETE'/,
  'DCC detail page must define the obsolete action code for the platform context.'
)
assert.match(
  detailPage,
  /generateUUID/,
  'DCC obsolete submit must generate a project idempotency key.'
)
assert.match(
  detailPage,
  /:idempotency-key="obsoleteDialog\.idempotencyKey"/,
  'DCC obsolete form-center panel must reuse the dialog idempotency key for the request.'
)
assert.match(
  detailPage,
  /startUserSelectAssignees/,
  'DCC obsolete submit must preserve the optional starter-selected assignee contract.'
)
assert.match(
  detailPage,
  /startUserSelectTasks/,
  'DCC obsolete dialog must track starter-selected BPM nodes explicitly.'
)
assert.match(
  detailPage,
  /UserSelectV2[\s\S]*v-model="obsoleteDialog\.startUserSelectAssignees\[task\.id\]"/,
  'DCC obsolete dialog must render a visible user selector for each starter-selected BPM node.'
)
assert.match(
  detailPage,
  /请选择\$\{task\.name\}审批人/,
  'DCC obsolete submit must fail visibly when a required starter-selected approver is missing.'
)
assert.match(
  detailPage,
  /data-testid="dcc-obsolete-form-center-panel"[\s\S]*<ActionFormPanel/,
  'DCC obsolete dialog must delegate approval submission to the embedded form-center action panel.'
)
assert.match(
  detailPage,
  /:form-data="dccObsoleteFormCenterFormData"/,
  'DCC obsolete form-center panel must submit the official obsolete business form snapshot.'
)
assert.match(
  detailPage,
  /:context="dccObsoleteFormCenterContext"/,
  'DCC obsolete form-center panel must bind the official controlled-file business context.'
)
assert.doesNotMatch(
  detailPage,
  /smokeappr1|smokeplan1|91451\d/,
  'DCC obsolete frontend must not hard-code E2E approver usernames or user ids.'
)
assert.doesNotMatch(
  detailPage,
  /当前版本已作废/,
  'DCC obsolete submit must not show immediate obsolete terminal success before BPM approval.'
)
assert.match(
  detailPage,
  /dcc-obsolete-action-lock/,
  'DCC detail page must render visible pending action lock information.'
)
assert.match(
  detailPage,
  /activeObsoleteAction/,
  'DCC detail page must store the active form-center action instance.'
)
assert.match(
  detailPage,
  /activeObsoleteActionError/,
  'DCC detail page must expose active action lookup errors visibly.'
)
assert.match(
  detailPage,
  /obsoleteActionLocked/,
  'DCC detail page must gate obsolete action with backend active action state.'
)
assert.match(
  processInstanceApi,
  /cancelProcessInstanceByStartUser\s*=\s*async\s*\(\s*id:\s*(number\s*\|\s*string|string\s*\|\s*number)/,
  'BPM start-user cancellation API must accept Flowable string process instance ids.'
)
assert.match(
  detailPage,
  /撤回作废申请/,
  'DCC active obsolete lock must expose an official applicant withdraw action.'
)
const cancelHandlerStart = detailPage.indexOf('const cancelActiveObsoleteAction')
assert.notEqual(
  cancelHandlerStart,
  -1,
  'DCC detail page must implement a dedicated active obsolete action cancel handler.'
)
const cancelHandlerEnd = detailPage.indexOf('\nconst ', cancelHandlerStart + 1)
const cancelHandler = detailPage.slice(
  cancelHandlerStart,
  cancelHandlerEnd === -1 ? detailPage.length : cancelHandlerEnd
)
assert.match(
  cancelHandler,
  /ProcessInstanceApi\.cancelProcessInstanceByStartUser[\s\S]*bpmProcessInstanceId/,
  'DCC obsolete withdraw must cancel the bound BPM process instance.'
)
assert.doesNotMatch(
  cancelHandler,
  /withdrawControlledFile/,
  'DCC obsolete withdraw must not call the DCC file-flow withdraw endpoint.'
)

assert.match(
  policySetupE2e,
  /DCC_OBSOLETE_E2E_TARGET_APPROVAL_MODE/,
  'DCC obsolete policy setup E2E must support explicitly switching approval mode.'
)
assert.match(
  policySetupE2e,
  /switch-approval-mode/,
  'DCC obsolete policy setup E2E must use the official form-center switch-approval-mode endpoint through the page.'
)
assert.match(
  realSubmitE2e,
  /DCC_OBSOLETE_E2E_APPROVAL_MODE/,
  'DCC obsolete submit E2E must support DIRECT and BPM_REQUIRED assertions.'
)
assert.match(
  realSubmitE2e,
  /assertDirectResult/,
  'DCC obsolete submit E2E must assert direct-effect terminal state when approval is disabled.'
)
assert.doesNotMatch(
  realSubmitE2e,
  /projection\.actionLocked\s*===\s*true/,
  'DCC obsolete DIRECT assertion must not confuse terminal non-active lock with pending approval lock.'
)
assert.match(
  realSubmitE2e,
  /projection\.actionLockReason[\s\S]*OBSOLETE_APPROVAL_PENDING/,
  'DCC obsolete DIRECT assertion must explicitly reject pending approval lock reason.'
)
assert.match(
  realSubmitE2e,
  /projection\.pendingRequestId\s*==\s*null/,
  'DCC obsolete DIRECT assertion must prove no pending form-center request remains.'
)
assert.match(
  realSubmitE2e,
  /Controlled file version is terminal/,
  'DCC obsolete DIRECT assertion must allow the expected terminal file lock reason.'
)
assert.match(
  realSubmitE2e,
  /assertBpmRequiredResult/,
  'DCC obsolete submit E2E must keep BPM_REQUIRED pending-lock assertions when approval is enabled.'
)

for (const file of [
  'src/api/dcc/controlledFile/workflow.ts',
  'src/api/form-center/instance.ts',
  'src/views/dcc/controlled-file/detail/index.vue'
]) {
  const content = readSource(file)
  if (/catch\s*\([^)]*\)\s*\{\s*\}/.test(content) || /catch\s*\{\s*\}/.test(content)) {
    throw new Error(`Empty catch is not allowed in ${file}`)
  }
}

console.log('PASS: DCC obsolete form-center frontend static contract')
