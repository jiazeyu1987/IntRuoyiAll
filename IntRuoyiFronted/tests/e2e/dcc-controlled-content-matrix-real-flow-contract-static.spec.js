const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')

function read(relativePath) {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

const packageJson = JSON.parse(read('package.json'))
const flowScript = read('tests/e2e/dcc-controlled-content-matrix-real-flow.e2e.js')
const normalizedFlowScript = flowScript.replace(/\\/g, '/')
const buildScenarioArtifactBody = flowScript.match(
  /function buildScenarioArtifact\(scenario, preflight\) \{[\s\S]*?\n\}\n\nasync function main/
)?.[0]
const loginBody = flowScript.match(/async function login\(page, actor = \{}, targetPath = ['"`]\/dcc\/controlled-file\/upload['"`]\) \{[\s\S]*?\n\}\n\nasync function readBrowserCache/)?.[0]
const openDetailPageBody = flowScript.match(/async function openDetailPage\(page, controlledFileId\) \{[\s\S]*?\n\}/)?.[0]
const selectDccProjectThroughUiBody = flowScript.match(/async function selectDccProjectThroughUi\(page, scenario\) \{[\s\S]*?\n\}/)?.[0]
const selectFileTypeTaxonomyThroughUiBody = flowScript.match(/async function selectFileTypeTaxonomyThroughUi\(page, scenario\) \{[\s\S]*?\n\}/)?.[0]
const selectProductMasterThroughUiBody = flowScript.match(/async function selectProductMasterThroughUi\(page, scenario\) \{[\s\S]*?\n\}/)?.[0]
const submitControlledFileThroughUiBody = flowScript.match(/async function submitControlledFileThroughUi\(page, scenario, runId, options = \{}\) \{[\s\S]*?\n\}/)?.[0]
const createReleasedControlledFileThroughUiBody = flowScript.match(/async function createReleasedControlledFileThroughUi\(browser, scenario, preflight, runId, suffix\) \{[\s\S]*?\n\}/)?.[0]
const approveCurrentStageThroughUiBody = flowScript.match(/async function approveCurrentStageThroughUi\(browser, scenario, controlledFileId, actor, stageIndex, stageCount, options = \{}\) \{[\s\S]*?\n\}/)?.[0]
const approveAllStagesThroughUiBody = flowScript.match(/async function approveAllStagesThroughUi\(browser, page, scenario, controlledFileId, activeRoute\) \{[\s\S]*?\n\}/)?.[0]
const runObsoleteFullFlowBody = flowScript.match(/async function runObsoleteFullFlow\(browser, scenario, preflight\) \{[\s\S]*?\n\}\n\nfunction assertObsoleteTerminalState/)?.[0]
const browserActiveCountBody = flowScript.match(/async function browserActiveCount\(page, scenario, fileNumber = null\) \{[\s\S]*?\n\}/)?.[0]

assert.ok(buildScenarioArtifactBody, 'buildScenarioArtifact must remain inspectable by the DCC matrix static contract.')
assert.ok(loginBody, 'login helper must remain inspectable by the DCC matrix static contract.')
assert.ok(openDetailPageBody, 'openDetailPage must remain inspectable by the DCC matrix static contract.')
assert.ok(selectDccProjectThroughUiBody, 'selectDccProjectThroughUi must remain inspectable by the DCC matrix static contract.')
assert.ok(selectFileTypeTaxonomyThroughUiBody, 'selectFileTypeTaxonomyThroughUi must remain inspectable by the DCC matrix static contract.')
assert.ok(selectProductMasterThroughUiBody, 'selectProductMasterThroughUi must remain inspectable by the DCC matrix static contract.')
assert.ok(submitControlledFileThroughUiBody, 'submitControlledFileThroughUi must remain inspectable by the DCC matrix static contract.')
assert.ok(createReleasedControlledFileThroughUiBody, 'createReleasedControlledFileThroughUi must remain inspectable by the DCC matrix static contract.')
assert.ok(approveCurrentStageThroughUiBody, 'approveCurrentStageThroughUi must remain inspectable by the DCC matrix static contract.')
assert.ok(approveAllStagesThroughUiBody, 'approveAllStagesThroughUi must remain inspectable by the DCC matrix static contract.')
assert.ok(runObsoleteFullFlowBody, 'runObsoleteFullFlow must remain inspectable by the DCC matrix static contract.')
assert.ok(browserActiveCountBody, 'browserActiveCount must remain inspectable by the DCC matrix static contract.')

assert.equal(
  packageJson.scripts['e2e:controlled-content:dcc-matrix:real'],
  'node tests/e2e/dcc-controlled-content-matrix-real-flow.e2e.js',
  'DCC controlled-content matrix real flow must have a stable package entry.'
)

assert.match(
  flowScript,
  /baseUrl:\s*\(process\.env\.DCC_CONTROLLED_CONTENT_E2E_BASE_URL\s*\|\|\s*['"`]http:\/\/localhost:8081['"`]\)/,
  'DCC matrix real flow must default to the documented local frontend entry http://localhost:8081.'
)

for (const marker of [
  'controlled-content-dcc-sop-release-real.json',
  'controlled-content-dcc-work-instruction-review-readonly-real.json',
  'controlled-content-dcc-inspection-withdraw-draft-real.json',
  'controlled-content-dcc-drawing-obsolete-real.json',
  'SOP',
  'WORK_INSTRUCTION',
  'INSPECTION_PROCEDURE',
  'DRAWING',
  'executionMode',
  'playwright-ui',
  'writeChannel',
  'frontend-ui',
  'directApiWrites',
  'sqlBusinessDataWritePerformed',
  'mockDataUsed',
  'writeRequests',
  'finalAssertions',
  'blockers'
]) {
  assert.match(flowScript, new RegExp(escapeRegExp(marker)), `${marker} must be written into DCC release evidence.`)
}

for (const marker of [
  '--preflight-only',
  'DCC_CONTROLLED_CONTENT_E2E_ALLOW_WRITES',
  'DCC_CONTROLLED_CONTENT_E2E_APPROVAL_USERS_JSON',
  'DCC_CONTROLLED_CONTENT_E2E_APPROVAL_USERS_JSON is required for DCC write matrix approval',
  'approvalUsersJsonParsed',
  'apiPostReadOnly',
  'readOnlyPostPaths',
  '/dcc/controlled-files/route-preview',
  '/system/user/profile/get',
  'categoryUploadPermission',
  'categoryActionPermissionEvidence',
  'permissionRuleMatchesSubmitter',
  'categoryObsoletePermission',
  'DCC submitter lacks category UPLOAD permission',
  'DCC submitter lacks category OBSOLETE permission',
  'obsoleteRuleCount',
  'directObsoleteRuleMatchCount',
  'officialRoutePreview',
  'DCC official route preview',
  'validateApprovalUsersForRoute',
  'resolveApprovalCandidatesForRoute',
  'approvalCandidateResolution',
  '/dcc/approval-positions',
  '/system/user/simple-list',
  'POSITION',
  'assignmentType',
  'systemPostId',
  'activeAssignmentCount',
  'resolvedCandidateUserIds',
  'DCC approval account missing',
  'DCC approval position missing',
  'DCC approval position has no active assignment',
  'DCC approval position resolves to no active user',
  'configured approver userId is not in USER candidateSourceIds',
  'configured approver userId is not in resolved candidate users',
  'uniqueMessages',
  'DCC 审核矩阵',
  'routePreview',
  'categoryId',
  'openCandidateCount',
  '20260720-controlled-state-machine-implementation/e2e-artifacts',
  'fullFlowPlan',
  'runScenarioFullFlow',
  'runPendingReadonlyFullFlow',
  'runWithdrawResubmitFullFlow',
  'runObsoleteFullFlow',
  'assertObsoleteTerminalState',
  'DCC obsolete final assertions failed',
  'scenarioRequiresFinalApproval',
  'submitControlledFileThroughUi',
  'selectUploadLeafDirectoryThroughUi',
  '请选择绑定目录下的最后一层子目录',
  '最终提交路径',
  'approveCurrentStageThroughUi',
  'DCC_CONTROLLED_CONTENT_E2E_SOURCE_FILE',
  'DCC_CONTROLLED_CONTENT_E2E_STAMPED_PDF',
  'collectUploadSizePolicyEvidence',
  '/dcc/protection/upload-size-policies/effective',
  'DCC upload size policy preflight failed',
  'collectDistributionDepartmentEvidence',
  '/system/dept/simple-list',
  'DCC distribution department label not found',
  'DCC_CONTROLLED_CONTENT_E2E_DISTRIBUTION_DEPARTMENT_LABEL',
  'DCC_CONTROLLED_CONTENT_E2E_PROJECT_KEYWORD',
  'selectDccProjectThroughUi',
  'selectFileTypeTaxonomyThroughUi',
  'DCC_CONTROLLED_CONTENT_E2E_SCENARIOS',
  'selectedScenarioMatrix',
  'selectedScenarioKeys',
  'DCC matrix selected scenario not found',
  '/dcc/project-codes/page',
  '/dcc/file-type-taxonomies',
  'DCC project code option is required',
  'DCC file type taxonomy requires an active leaf with at least 3 levels',
  'selectProductMasterThroughUi',
  'selectRevisionTargetThroughUi',
  'DCC_CONTROLLED_CONTENT_E2E_PRODUCT_KEYWORD',
  'waitForProductMasterOptionThroughUi',
  '/dcc/controlled-files/upload-revision-candidates',
  'revisionTargetControlledFileId',
  'DCC revision target option did not appear',
  'DCC product master option did not appear',
  'filter({ hasText: config.productKeyword })',
  'parseApiResponsePayload',
  'collectVisibleSubmitFeedback',
  'collectImmediateSubmitFeedback',
  'collectProductMasterDiagnostics',
  'waitForProductMasterSelectionThroughUi',
  'DCC product master selection did not stabilize',
  'submitButtonState',
  'waitForUploadPreviewReadyThroughUi',
  '预览文件：',
  'dcc-upload-current-version-panel',
  'clickVisibleDropdownOptionByPoint',
  'getBoundingClientRect()',
  'DCC submit did not reach /submit',
  'DCC submit button is disabled before click',
  'DCC submit click did not complete before a /submit request was observed',
  'DCC submit returned non-success payload',
  'submitResponseBody',
  'afterSubmitFailureFeedback',
  'beforeClickFeedback',
  'clickErrorFeedback',
  'submitRequestObserved',
  'collectDetailPageDiagnostics',
  'collectApprovalActionDiagnostics',
  'DCC approval action button is not visible',
  'collectStampedPdfUploadDiagnostics',
  'uploadStampedPdfWithDiagnostics',
  'DCC stamped PDF upload did not reach /upload-preview',
  "filter({ hasText: '盖章 PDF' })",
  'visibleButtons',
  'actorUserId',
  'waitForDetailPageReady',
  'DCC detail page did not expose handling summary or detail key text',
  '审批阶段进度',
  'createTraceableRunId',
  'crypto.randomBytes',
  'MAX_DCC_FILE_NUMBER_LENGTH',
  'DCC generated fileNumber exceeds database limit',
  'buildDccE2eFileNumber',
  'getCurrentPendingApprovalAccount',
  'DCC approval current task did not match configured approval accounts',
  'approveCurrentPendingTaskThroughUi',
  'uniqueApprovalAccountsForRoute',
  'accountKey',
  'scenarioWriteStartIndex',
  'scenarioWriteRequests'
]) {
  assert.match(flowScript, new RegExp(escapeRegExp(marker)), `${marker} must be covered by the DCC matrix flow.`)
}

assert.doesNotMatch(
  flowScript,
  /return accounts\[0\]/,
  'DCC approval runner must not collapse multi-candidate stage accounts to the first configured user.'
)

for (const marker of [
  'nodeWithIndex',
  'nodeIndex',
  'resolveApprovalAccountReference',
  'findApprovalStageAccount',
  'Array.isArray(scope.stages)',
  'stage.node',
  'approvalUsers?.users?.[stageAccount.username]',
  'node:${nodeIndex}',
  '${scenario.key}:node:${nodeIndex}',
  '${scenario.contentType}:node:${nodeIndex}',
  'approvalRouteForExecution',
  'normalizedOfficialRoutePreview',
  'approveAllStagesThroughUi(browser, page, scenario,',
  'approvalRouteForExecution(preflight)'
]) {
  assert.match(flowScript, new RegExp(escapeRegExp(marker)), `${marker} must be covered so repeated approval stages can map distinct real approver accounts.`)
}

assert.doesNotMatch(
  normalizedFlowScript,
  /20260719-controlled-content-full-objective-completion-audit\/e2e-artifacts/,
  'DCC matrix real flow default artifactDir must not point to an old task artifact directory.'
)

assert.doesNotMatch(
  normalizedFlowScript,
  /20260719-dcc-controlled-content-state-machine-implementation\/e2e-artifacts/,
  'DCC matrix real flow default artifactDir must point to the current implementation task, not the previous DCC implementation task.'
)

assert.doesNotMatch(
  buildScenarioArtifactBody,
  /writeRequests\s*:\s*\[\]/,
  'DCC scenario artifacts must not hard-code top-level writeRequests to []; blocked and future write flows must expose tracked writes.'
)

assert.doesNotMatch(
  flowScript,
  /scrollIntoViewIfNeeded\(\)/,
  'DCC matrix real flow must not rely on Playwright scrollIntoViewIfNeeded for Element Plus dropdown options; it can wait forever while the overlay is animating.'
)

assert.match(
  buildScenarioArtifactBody,
  /writeRequests\s*:\s*scenarioWriteRequests\(preflight\)/,
  'DCC scenario artifacts must mirror the scenario-scoped tracked write requests at the top level.'
)

assert.match(
  runObsoleteFullFlowBody,
  /browserActiveCount\(page,\s*scenario,\s*source\.fileNumber\)/,
  'DCC obsolete final assertions must count ACTIVE rows for the generated fileNumber, not the whole category.'
)

assert.match(
  browserActiveCountBody,
  /params\.set\(['"`]keyword['"`],\s*fileNumber\)/,
  'DCC browserActiveCount must support fileNumber narrowing for master-scoped obsolete assertions.'
)

assert.match(
  flowScript,
  /detail\.status\s*===\s*['"`]PENDING_DOC_CONTROL_APPROVAL['"`]/,
  'DCC approval runner must treat the real DCC pending doc-control status as final approval when BPM task metadata is incomplete.'
)

assert.match(
  approveAllStagesThroughUiBody,
  /Math\.max\(activeRoute\?\.nodes\?\.length\s*\|\|\s*0,\s*accounts\.length\)\s*\+\s*2/,
  'DCC approval runner must bound the loop by route node count, not unique approver count, because the same user can approve multiple stages.'
)

assert.doesNotMatch(
  approveCurrentStageThroughUiBody,
  /stageIndex\s*===\s*stageCount/,
  'DCC stamped PDF upload must only be triggered by the real current doc-control task/status, not by a stage-count heuristic.'
)

assert.doesNotMatch(
  flowScript,
  /full-flow requires a leaf upload directory binding before UI submit/,
  'DCC full-flow must select a real upload leaf directory through the UI when the category binding is not already a leaf.'
)

assert.doesNotMatch(
  flowScript,
  /UI write automation is not complete yet/,
  'DCC full-flow gate must not be permanently blocked by an unfinished UI automation placeholder.'
)

assert.doesNotMatch(
  flowScript,
  /full-flow plan .* is not implemented yet/,
  'DCC matrix full-flow must implement all planned release, pending-readonly, withdraw-resubmit, and obsolete scenario runners.'
)

assert.match(
  flowScript,
  /runScenarioFullFlow\(browser,\s*scenario,\s*preflight\)/,
  'DCC matrix full-flow must call a scenario runner when preflight prerequisites are satisfied.'
)

assert.match(
  flowScript,
  /const beforeClickFeedback = await collectVisibleSubmitFeedback\(page,\s*submitButton\)/,
  'DCC submit wait must capture button state and page feedback before clicking.'
)

assert.match(
  flowScript,
  /if \(beforeClickFeedback\.submitButtonState\?\.disabled\) \{[\s\S]*DCC submit button is disabled before click/,
  'DCC submit wait must fail fast with diagnostics when the submit button is disabled.'
)

assert.match(
  flowScript,
  /let submitRequestObserved\s*=\s*false[\s\S]*page\.on\(['"`]request['"`],\s*observeSubmitRequest\)[\s\S]*submitRequestObserved=\$\{submitRequestObserved\}/,
  'DCC submit wait must explicitly diagnose the no-submit-request path.'
)

assert.match(
  openDetailPageBody,
  /waitForDetailPageReady\(page,\s*controlledFileId\)/,
  'openDetailPage must delegate to a readiness helper that checks real detail content and errors.'
)

assert.match(
  flowScript,
  /function createTraceableRunId\(\) \{[\s\S]*crypto\.randomBytes\(\d+\)[\s\S]*\}/,
  'DCC runId must include a random segment in addition to timestamp precision.'
)

assert.doesNotMatch(
  flowScript,
  /new Date\(\)\.toISOString\(\)\.replace\(\s*\/\[-:\.TZ\]\/g,\s*['"`]['"`]\s*\)\.slice\(0,\s*14\)/,
  'DCC runId must not use second-level precision that can collide in repeated full-flow runs.'
)

assert.match(
  flowScript,
  /const MAX_DCC_FILE_NUMBER_LENGTH\s*=\s*64/,
  'DCC E2E file numbers must encode the database file_number varchar(64) limit explicitly.'
)

assert.match(
  submitControlledFileThroughUiBody,
  /selectDccProjectThroughUi\(page,\s*scenario\)[\s\S]*selectFileTypeTaxonomyThroughUi\(page,\s*scenario\)[\s\S]*selectOptionByFormLabel\(page,\s*['"`]文件类别['"`],\s*scenario\.categoryName\)/,
  'DCC submit helper must select DCC project, file type taxonomy, and file category before submitting.'
)

assert.match(
  selectDccProjectThroughUiBody,
  /status:\s*['"`]ENABLE['"`][\s\S]*clickVisibleDropdownOptionByPoint\(page,\s*option,\s*['"`]DCC项目['"`]\)/,
  'DCC project selection must use enabled real project-code options through the visible UI select.'
)

assert.match(
  selectFileTypeTaxonomyThroughUiBody,
  /buildFileTypeTaxonomyPath\(rows\)[\s\S]*locator\(['"`]\.el-cascader-node:visible['"`]\)[\s\S]*pathRows\.length/,
  'DCC file type taxonomy selection must use a real active 3-level cascader path through the visible UI.'
)

assert.match(
  submitControlledFileThroughUiBody,
  /buildDccE2eFileNumber\(scenario,\s*runId,\s*options\.fileNumberSuffix\)/,
  'DCC submit helper must build traceable file numbers through the length-checked helper.'
)

assert.match(
  createReleasedControlledFileThroughUiBody,
  /fileNumberSuffix:\s*suffix/,
  'DCC source creation must pass suffixes to the length-checked file number helper instead of concatenating long content type names.'
)

assert.doesNotMatch(
  flowScript,
  /fileNumber:\s*`CODEX-DCC-\$\{scenario\.contentType\}-\$\{suffix\}-\$\{runId\}`/,
  'DCC E2E must not concatenate scenario.contentType + suffix + runId directly because INSPECTION_PROCEDURE can exceed varchar(64).'
)

assert.doesNotMatch(
  selectProductMasterThroughUiBody,
  /DCC 产品编号：/,
  'DCC product selection must not wait on the optional helper text; selection is valid when the select value stabilizes.'
)

assert.match(
  selectProductMasterThroughUiBody,
  /waitForProductMasterSelectionThroughUi\(page,\s*item,\s*scenario,\s*optionLabel\)/,
  'DCC product selection must wait for selected option stability through a dedicated diagnostic helper.'
)

assert.match(
  approveAllStagesThroughUiBody,
  /getCurrentPendingApprovalAccount\(page,\s*controlledFileId,\s*accounts\)/,
  'DCC approval runner must choose the configured account that matches the current running BPM task before opening the actor page.'
)

assert.match(
  approveAllStagesThroughUiBody,
  /pendingAfterMaxApprovalSteps/,
  'DCC approval runner must re-check current BPM tasks after the last expected approval before reporting maxApprovalSteps blocked.'
)

assert.match(
  loginBody,
  /loginUrl\.searchParams\.set\(['"`]redirect['"`],\s*['"`]\/index['"`]\)/,
  'DCC matrix login must match official login-preflight by logging in through /index before navigating to the target page.'
)

assert.match(
  loginBody,
  /tenantOption\.waitFor\(\{\s*state:\s*['"`]visible['"`]/,
  'DCC matrix login must wait for the tenant dropdown option before clicking it, matching official login-preflight behavior.'
)

assert.doesNotMatch(
  loginBody,
  /await settle\(page\)[\s\S]*form\.waitFor/,
  'DCC matrix login must not wait for networkidle before filling the login form; the official login preflight starts interacting after commit.'
)

assert.doesNotMatch(
  loginBody,
  /option\.isVisible\(\)\.catch\(\(\)\s*=>\s*false\)/,
  'DCC matrix login must not silently press Enter when the tenant option has not appeared yet.'
)

assert.doesNotMatch(
  flowScript,
  /request\.(post|put|delete)|api(Post|Put|Patch|Delete)(?!ReadOnly)|fetch\([^)]*method:\s*['"`](PUT|PATCH|DELETE)/,
  'DCC matrix real flow must not use direct API writes; write paths must be UI-only except declared read-only preview POSTs.'
)

assert.doesNotMatch(
  flowScript,
  /\(response\)\s*=>\s*\(response\)\s*=>/,
  'DCC matrix approval response wait predicate must inspect the actual response, not return a nested function.'
)

assert.doesNotMatch(
  flowScript,
  /const stampedResponsePromise = page\.waitForResponse/,
  'DCC stamped PDF upload wait must be bound inside Promise.all so timeout rejections are caught and written into scenario artifacts.'
)

assert.equal(
  (flowScript.match(/async function confirmElementMessageBox/g) || []).length,
  1,
  'DCC matrix flow must define confirmElementMessageBox exactly once.'
)

assert.match(
  flowScript,
  /readOnlyPostPaths\s*=\s*new Set\([^)]*['"`]\/dcc\/controlled-files\/route-preview['"`]/s,
  'DCC route preview POST must be explicitly classified as read-only so P5 write tracking remains honest.'
)

assert.match(
  flowScript,
  /apiPostReadOnly[\s\S]*readOnlyPostPaths\.has\(apiPath\)/,
  'DCC matrix script must guard read-only POST helpers with an allow-list.'
)

console.log('dcc-controlled-content-matrix-real-flow-contract-static PASS')
