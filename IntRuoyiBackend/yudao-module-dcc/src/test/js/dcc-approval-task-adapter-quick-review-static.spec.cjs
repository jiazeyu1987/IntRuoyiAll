const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const adapterPath = path.join(
  moduleRoot,
  'src/main/java/cn/iocoder/yudao/module/dcc/approval/DccApprovalTaskAdapter.java'
)
const testPath = path.join(
  moduleRoot,
  'src/test/java/cn/iocoder/yudao/module/dcc/approval/DccApprovalTaskAdapterTest.java'
)

const adapter = fs.readFileSync(adapterPath, 'utf8')
const test = fs.readFileSync(testPath, 'utf8')

const extract = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.notEqual(end, -1, `${label} missing end marker`)
  return source.slice(start, end)
}

assert.match(
  adapter,
  /private static final Set<String> QUICK_REVIEW_ACTIONS = Set\.of\("APPROVE", "REJECT", "PROCESS_IN_MODULE"\)/,
  'DCC adapter must formally expose unified approve/reject plus module handling for quick-review-capable nodes.'
)
assert.match(
  adapter,
  /\.availableActions\(resolveTodoAvailableActions\(task\.getTaskDefinitionKey\(\), file\.getStatus\(\)\)\)/,
  'DCC TODO summaries must derive available actions from the current stage/status.'
)
const actionResolver = extract(
  adapter,
  'private static Set<String> resolveTodoAvailableActions',
  'private List<String> buildDccBusinessContextTags',
  'DCC TODO available action resolver'
)
assert.match(actionResolver, /isDocControlFinalApprovalTask\(taskDefinitionKey, fileStatus\)[\s\S]*?return PROCESS_IN_MODULE_ACTIONS;/,
  'Final doc-control approval must stay in module handling because it requires stamped PDF, directory, and distribution data.')
assert.match(actionResolver, /isQuickReviewTask\(taskDefinitionKey, fileStatus\)[\s\S]*?return QUICK_REVIEW_ACTIONS;/,
  'Review-capable DCC stages must expose unified quick review actions.')
assert.match(actionResolver, /DccControlledFileStageCodeEnum\.DOC_CONTROL_APPROVAL/, 'Final doc-control stage must be explicitly identified.')
assert.match(actionResolver, /DccControlledFileStatusEnum\.PENDING_DOC_CONTROL_APPROVAL/, 'Final doc-control status must be explicitly identified.')
assert.match(
  adapter,
  /public void review\(ApprovalTaskReviewContext context\)[\s\S]*?workflowService\.approveTask\(context\.getLoginUserId\(\), fileId, reqVO\)/,
  'Unified DCC APPROVE review must delegate to the formal controlled-file workflow service.'
)
assert.match(
  adapter,
  /workflowService\.rejectTask\(context\.getLoginUserId\(\), fileId, reqVO\)/,
  'Unified DCC REJECT review must delegate to the formal controlled-file workflow service.'
)
assert.match(
  adapter,
  /setPassword\(password\)[\s\S]*?setReason\(context\.getReason\(\)\)/,
  'DCC approve must carry the signature password and review reason to the workflow service.'
)
assert.match(
  adapter,
  /APPROVAL_REJECT_REASON_REQUIRED: DCC reject requires reason/,
  'DCC reject must fail fast when the unified review command is missing a reason.'
)
assert.match(
  test,
  /pageTodoKeepsDocControlApprovalInModuleBecauseQuickApproveRequiresArtifacts/,
  'JUnit coverage must document that final doc-control approval remains module-only.'
)
assert.match(
  test,
  /reviewApproveDelegatesToControlledFileWorkflow[\s\S]*?reviewRejectDelegatesToControlledFileWorkflow/,
  'JUnit coverage must exercise both DCC approve and reject delegation paths.'
)

console.log('dcc approval task adapter quick review static contract passed')
