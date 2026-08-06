const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const backendRoot = path.join(workspaceRoot, 'IntRuoyiBackend')
const packageJsonPath = path.join(frontendRoot, 'package.json')
const realFlowPath = path.join(frontendRoot, 'tests/e2e/role-requirement-matrix-real-flow.e2e.js')
const remainingRouterPath = path.join(frontendRoot, 'src/router/modules/remaining.ts')
const teamLeaderWorkbenchPath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
)
const frontlineFixedTemplatePanelPath = path.join(
  frontendRoot,
  'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const teamLeaderApiPath = path.join(frontendRoot, 'src/api/mes/pro/processpool/teamLeader.ts')
const processPoolApiPath = path.join(frontendRoot, 'src/api/mes/pro/processpool/index.ts')
const timelineFilterTestPath = path.join(
  backendRoot,
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/ProcessPoolTimelineFilterTest.java'
)
const activeOrderServiceTestPath = path.join(
  backendRoot,
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceTest.java'
)
const activeOrderTransferTraceServiceTestPath = path.join(
  backendRoot,
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesActiveOrderTransferTraceServiceTest.java'
)
const teamLeaderOrderProcessCompletionServiceTestPath = path.join(
  backendRoot,
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderOrderProcessCompletionServiceTest.java'
)
const teamLeaderBatchRecordBackfillServiceTestPath = path.join(
  backendRoot,
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderBatchRecordBackfillServiceTest.java'
)
const edhrReleaseServiceTestPath = path.join(
  backendRoot,
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImplTest.java'
)
const frontlinePqcContextServiceTestPath = path.join(
  backendRoot,
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceTest.java'
)
const activeOrderAuthorityMigrationPath = path.join(
  backendRoot,
  'sql/mysql/20260802_mes_process_pool_active_order_authority.sql'
)
const activeOrderProcessSnapshotMigrationPath = path.join(
  backendRoot,
  'sql/mysql/20260802_mes_process_pool_active_order_process_snapshot.sql'
)
const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'))
const scripts = packageJson.scripts || {}
const plannedStaticScripts = {
  'e2e:role-matrix-qa-regulation:static': 'tests/e2e/role-matrix-qa-regulation-static.spec.cjs',
  'e2e:role-matrix-pqc-dynamic-form:static': 'tests/e2e/role-matrix-pqc-dynamic-form-static.spec.cjs',
  'e2e:role-matrix-transfer-start-check:static': 'tests/e2e/role-matrix-transfer-start-check-static.spec.cjs',
  'e2e:role-matrix-daily-close-scope:static': 'tests/e2e/role-matrix-daily-close-scope-static.spec.cjs',
  'e2e:role-matrix-migration-preflight:static': 'tests/e2e/role-matrix-migration-preflight-static.spec.cjs',
  'e2e:role-matrix-pqc-d32-fixture:static': 'tests/e2e/role-matrix-pqc-d32-fixture-static.spec.cjs',
  'e2e:role-matrix-pqc-rerun-fixture:static': 'tests/e2e/role-matrix-pqc-rerun-fixture-static.spec.cjs'
}

assert.equal(
  scripts['e2e:role-requirement-matrix:preflight:static'],
  'node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs',
  'package.json must expose the role matrix preflight static contract.'
)
assert.equal(
  scripts['e2e:role-requirement-matrix:real:check'],
  'node tests/e2e/role-requirement-matrix-real-flow.e2e.js --check',
  'package.json must expose the real E2E preflight command, not a static or API-only substitute.'
)
assert.equal(
  scripts['e2e:role-requirement-matrix:real'],
  'node tests/e2e/role-requirement-matrix-real-flow.e2e.js',
  'package.json must expose the real Playwright E2E command.'
)

assert.ok(fs.existsSync(realFlowPath), 'role-requirement-matrix real Playwright E2E script must exist.')
assert.ok(fs.existsSync(remainingRouterPath), 'remaining.ts must exist for hidden real-page routes.')
assert.ok(fs.existsSync(teamLeaderWorkbenchPath), 'PQC team leader workbench page must exist.')
assert.ok(fs.existsSync(teamLeaderApiPath), 'team-leader API wrapper must exist.')
assert.ok(fs.existsSync(processPoolApiPath), 'process-pool timeline API types must exist.')
assert.ok(
  fs.existsSync(activeOrderAuthorityMigrationPath),
  'M1 active-order authority migration must exist before the real source blocker check can clear RRM-BLK-006.'
)
assert.ok(
  fs.existsSync(activeOrderProcessSnapshotMigrationPath),
  'M2 active-order process snapshot migration must exist before the production coefficient blockers can clear.'
)
assert.ok(
  fs.existsSync(activeOrderTransferTraceServiceTestPath),
  'AC-M07 must have a runnable transfer-trace service test, not only schema/static proof.'
)
assert.ok(
  fs.existsSync(edhrReleaseServiceTestPath),
  'AC-M23 must have a runnable eDHR release service test, not only real-flow catalog text.'
)
for (const [scriptName, relativeFile] of Object.entries(plannedStaticScripts)) {
  assert.equal(
    scripts[scriptName],
    `node ${relativeFile}`,
    `package.json must expose ${scriptName}.`
  )
  assert.ok(
    fs.existsSync(path.join(frontendRoot, relativeFile)),
    `${relativeFile} must exist so future milestones fail with business RED, not missing scripts.`
  )
}

const source = fs.readFileSync(realFlowPath, 'utf8')
const remainingRouterSource = fs.readFileSync(remainingRouterPath, 'utf8')
const teamLeaderSource = fs.readFileSync(teamLeaderWorkbenchPath, 'utf8')
const frontlineFixedTemplatePanelSource = fs.readFileSync(frontlineFixedTemplatePanelPath, 'utf8')
const teamLeaderApiSource = fs.readFileSync(teamLeaderApiPath, 'utf8')
const processPoolApiSource = fs.readFileSync(processPoolApiPath, 'utf8')
const timelineFilterTestSource = fs.readFileSync(timelineFilterTestPath, 'utf8')
const activeOrderServiceTestSource = fs.readFileSync(activeOrderServiceTestPath, 'utf8')
const activeOrderTransferTraceServiceTestSource = fs.readFileSync(activeOrderTransferTraceServiceTestPath, 'utf8')
const teamLeaderOrderProcessCompletionServiceTestSource = fs.readFileSync(teamLeaderOrderProcessCompletionServiceTestPath, 'utf8')
const teamLeaderBatchRecordBackfillServiceTestSource = fs.readFileSync(teamLeaderBatchRecordBackfillServiceTestPath, 'utf8')
const edhrReleaseServiceTestSource = fs.readFileSync(edhrReleaseServiceTestPath, 'utf8')
const frontlinePqcContextServiceTestSource = fs.readFileSync(frontlinePqcContextServiceTestPath, 'utf8')

for (const token of [
  'RRM_FRONTEND_URL',
  'RRM_BACKEND_URL',
  'RRM_TENANT',
  'RRM_LOCAL_BASELINE_TENANT_AUTHORIZATION',
  'RRM_PRODUCTION_EMPLOYEE_USERNAME',
  'RRM_PRODUCTION_LEADER_USERNAME',
  'RRM_QA_USERNAME',
  'RRM_PQC_INSPECTOR_USERNAME',
  'RRM_PQC_LEADER_USERNAME',
  'RRM_RELEASE_OWNER_USERNAME',
  'RRM_UNAUTHORIZED_USERNAME',
  'RRM_UNAUTHORIZED_PASSWORD',
  'RRM_SIGNATURE_IDS_JSON',
  'RRM_PRODUCTION_ORDER_ID',
  'RRM_TRANSFER_IDS'
]) {
  assert.match(source, new RegExp(token), `real E2E preflight must require ${token}.`)
}

assert.match(
  source,
  /function readText\(filePath\)[\s\S]*?replace\(\s*\/\\r\\n\/g,\s*['"]\\n['"]\s*\)/,
  'real E2E source checks must normalize CRLF to LF before regex parsing on Windows.'
)

assert.match(
  source,
  /async function waitForPostLoginNavigationSettled\(page,\s*roleKey\)[\s\S]*?waitForURL\(/,
  'real E2E login must wait for the post-login SPA redirect before later page.goto calls.'
)
assert.match(
  source,
  /async function login\(page,\s*config,\s*roleKey,\s*role\)[\s\S]*?waitForPostLoginNavigationSettled\(page,\s*roleKey\)/,
  'login() must settle post-login navigation so final cleanup page.goto cannot be aborted by a pending redirect.'
)
assert.match(
  source,
  /async function runFinalActiveOrderCleanup\(browser,\s*config,\s*actionEvidence\)[\s\S]*?return await verifyActiveOrderCleanupTraceability\(page,\s*config,\s*joinEvidence\)/,
  'runFinalActiveOrderCleanup() must await cleanup verification before closing the Playwright context.'
)
const activeOrderCleanupSource = source.match(
  /async function verifyActiveOrderCleanupTraceability\(page,\s*config,\s*joinEvidence\)[\s\S]*?(?=\nasync function runFinalActiveOrderCleanup)/
)
assert.ok(activeOrderCleanupSource, 'Active-order cleanup function must exist.')
assert.match(
  activeOrderCleanupSource[0],
  /\/mes\/pro\/process-pool\/production-leader[\s\S]*await selectRealFlowTab\(page,\s*'活跃订单池'\)[\s\S]*data-team-leader-active-order-config/,
  'Active-order cleanup must reopen the formal production-leader page and switch to 活跃订单池 before locating the cleanup surface.'
)
assert.match(
  source,
  /async function findVisibleActiveOrderRowAcrossPages[\s\S]*button\.btn-next[\s\S]*isDisabled\(\)[\s\S]*waitForFunction/,
  'Active-order cleanup must page through the real visible table when the task row is not on the first page.'
)
assert.match(
  activeOrderCleanupSource[0],
  /findVisibleActiveOrderRowAcrossPages\(\s*section,\s*joinEvidence\.activeOrderId,\s*rows\.length\s*\)/,
  'Active-order cleanup must use the pagination-aware visible-row locator.'
)
assert.doesNotMatch(
  activeOrderCleanupSource[0],
  /const activeOrderRow = section\.locator\('tbody tr'\)[\s\S]*activeOrderRow\.waitFor\(\{\s*state:\s*'visible'/,
  'Active-order cleanup must not assume the task row is already visible on the current page.'
)

for (const token of [
  'collectSourceBlockers',
  'collectErpRelationBlockers',
  'collectQaRegulationBlockers',
  'collectPqcSubmissionBlockers',
  'collectPqcFrontendBlockers',
  'collectProductionCoefficientBlockers',
  'collectBatchRecordBindingBlockers',
  'ACTIVE_ORDER_AUTHORITY_SQL',
  '20260802_mes_process_pool_active_order_authority.sql',
  'ACTIVE_ORDER_PROCESS_SNAPSHOT_SQL',
  '20260802_mes_process_pool_active_order_process_snapshot.sql',
  'USER_APPROVED_YUDAO_SOURCE_20260802',
  'processPoolMapper.selectActiveList',
  'selectActiveByWorkOrderRouteProcess',
  'buildSourceNotIntegratedItem',
  'mes_pro_process_pool_active_order',
  'mes_kingdee_production_material_list',
  'MesProRouteFlowProcessConfigDO',
  'MesProScheduleOrderProcessDO',
  'productionQuantityFactor',
  'MesProRouteFlowProcessBatchRecordDO',
  'batchRecordFormNames',
  'formBindings',
  'normalizeRecordBindingSlotType',
  'mes_wm_transfer',
  'qaRegulationOwnership',
  'pqcTaskModel',
  'FRONTLINE_FIXED_TEMPLATE_PANEL',
  'hardcodedPqcInspectionItems',
  'defaultPqcInspectionType',
  'defaultPqcInspectionQuantity',
  'defaultPqcScrapQuantity',
  'PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH',
  'chromium.launch',
  '/login',
  'loadAcceptanceMatrix',
  'M6_REAL_FLOW_PHASES',
  'buildAcceptanceCoverage',
  'assertAcceptanceCoverage',
  'acceptanceCoverage',
  'phaseEvidence',
  'performActiveOrderJoin',
  'verifyActiveOrderConflictRouteFailure',
  'activeOrderConflictRouteRejected',
  'verifyPqcActiveOrderReadOnly',
  'activeOrderCrossRoleReadOnly',
  'buildPqcProcessSourceBlocker',
  'E2E_PQC_TASK_SOURCE',
  'verifyPqcRegulationItemsRendered',
  'pqcRegulationItemsRendered',
  'verifyQaRegulationPublishedVersionReadOnly',
  'qaRegulationPublishedVersionReadOnly',
  'E2E_QA_REGULATION_PAGE',
  'verifyPqcPieceDetailQuantityPrepared',
  'pqcPieceDetailQuantityPrepared',
  'verifyPqcFormalSubmissionCreatesEvent',
  'pqcFormalSubmissionCreated',
  'resolveUnusedPqcSignatureId',
  'collectConfiguredSignatureIds',
  'isPqcSignaturePoolRole',
  'E2E_PQC_SIGNATURE_POOL',
  'verifyPqcLeaderSubmissionFilterPaginationConsistency',
  'pqcLeaderSubmissionFilterPaginationConsistent',
  'verifyPqcLeaderSubmissionDetailTraceability',
  'pqcLeaderSubmissionDetailTraceable',
  'verifyPqcLeaderSubmissionDetailUnauthorizedBlocked',
  'pqcLeaderSubmissionDetailUnauthorizedBlocked',
  'E2E_PQC_DETAIL_PERMISSION',
  'verifyPqcLeaderReviewApprovalAggregatesProcessInspection',
  'pqcLeaderReviewApprovedAndAggregated',
  'verifyPqcLeaderDuplicateTerminalReviewBlocked',
  'pqcLeaderDuplicateTerminalReviewBlocked',
  'E2E_PQC_REVIEW_TERMINAL',
  'verifyPqcLeaderSelfReviewBlocked',
  'pqcLeaderSelfReviewBlocked',
  'PRO_PROCESS_POOL_SUBMISSION_REVIEW_SELF_FORBIDDEN',
  'E2E_PQC_REVIEW_SELF',
  'verifyPqcLeaderRejectedCorrectionChain',
  'pqcLeaderRejectedCorrectionChain',
  'preparePqcRejectedCorrectionCandidate',
  'E2E_PQC_REJECT_CORRECTION',
  '/mes/pro/process-pool/event-revision/update-original',
  'revisionSignatureId',
  'modificationHistorySummary',
  'processInspectionAggregationStatus',
  'processInspectionReviewId',
  'processInspectionAggregatedAt',
  'resolveRoleUserId',
  'reviewerUserId',
  'excludedReviewerUserIds',
  'E2E_PQC_PERSONNEL_REVIEWER',
  'verifyPqcActualEmployeeSwitch',
  'pqcActualEmployeeSelected',
  'verifyActiveOrderUnauthorizedMutationBlocked',
  'activeOrderUnauthorizedMutationBlocked',
  'unauthorizedActor',
  'verifyActiveOrderCleanupTraceability',
  'runFinalActiveOrderCleanup',
  'activeOrderCleanupCompleted',
  'verifyEdhrReleaseTraceabilityReadOnly',
  'edhrReleaseTraceabilityReadOnly',
  'prepareEdhrReleaseBatchExecutionViaRealPage',
  '/mes/pro/feedback/edhr-batch-execution?prefillWorkOrderCode=',
  '/mes/pro/edhr-batch-execution/open-or-create',
  '/mes/pro/edhr-release/precheck',
  '/mes/pro/edhr-release/page',
  '/mes/pro/edhr-release/check-item/page',
  '/mes/pro/edhr-release/event/page',
  'buildM6ConcurrencyPerformanceGateEvidence',
  'collectM6ConcurrencyProofs',
  'hasCompleteM6ConcurrencyGateEvidence',
  'concurrencyProofs',
  'observedConcurrencyAcceptanceIds',
  'provedConcurrencyAcceptanceIds',
  'missingConcurrencyAcceptanceIds',
  'buildGateBlockers',
  'm6ConcurrencyGateDeferred',
  'm6ConcurrencyGateVerified',
  'm6PerformanceGateDeferred',
  'E2E_CONCURRENCY',
  'E2E_PERFORMANCE',
  'AC-M23',
  'AC-D12',
  'AC-D38',
  'gateEvidence',
  '/system/auth/get-permission-info',
  'actionEvidence',
  'probeActiveOrderListRuntime',
  'activeOrderListRuntime',
  '/system/tenant/get-id-by-name',
  '/system/auth/login',
  'loginResponseTimeout',
  '/mes/pro/process-pool/team-leader/active-order/list',
  'tenant-id',
  'Authorization',
  '/mes/pro/process-pool/team-leader/active-order/add',
  '/mes/pro/feedback/edhr-batch-production-fill',
  '/mes/pro/feedback/frontline/submit',
  '/mes/pro/feedback/edhr-batch-pqc-fill',
  '/mes/pro/feedback/frontline/device-account/pqc/active-orders',
  '/mes/pro/feedback/frontline/device-account/pqc/active-order/processes',
  '/mes/pro/feedback/frontline/device-account/pqc/personnel',
  '/mes/pro/feedback/frontline/device-account/pqc/switch-employee',
  '/mes/pro/feedback/frontline/device-account/pqc/submit',
  '/mes/pro/process-pool/team-leader/submission/review',
  'writeEvidence',
  'failFast'
]) {
  assert.match(source, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `real E2E script must include ${token}.`)
}

assert.match(
  source,
  /async function verifyPqcLeaderSubmissionDetailTraceability[\s\S]*\/mes\/pro\/process-pool\/team-leader\/submission\/detail[\s\S]*originalPayloadJson[\s\S]*pqcPieceValues[\s\S]*data-team-leader-structured-detail[\s\S]*data-pqc-submission-log[\s\S]*signatureId/,
  'real E2E must prove AC-D33 with the real PQC leader detail drawer, team-leader detail API, original payload, piece details, and signature snapshot.'
)
assert.match(
  source,
  /async function verifyPqcLeaderSubmissionDetailTraceability[\s\S]*detailButtonVisible[\s\S]*detailButton[\s\S]*\.waitFor\(\{ state: 'visible'[\s\S]*\.catch\(\(error\) => \(\{ detailButtonError: error \}\)\)[\s\S]*E2E_PQC_DETAIL_PAGE/,
  'AC-D33 detail button visibility failures must become structured E2E_PQC_DETAIL_PAGE blockers instead of unstructured locator timeouts.'
)
assert.match(
  source,
  /async function verifyPqcLeaderSubmissionDetailUnauthorizedBlocked[\s\S]*unauthorizedActor[\s\S]*\/mes\/pro\/process-pool\/team-leader\/submission\/detail[\s\S]*!result\.ok \|\| !isBusinessSuccess\(result\.body\)[\s\S]*E2E_PQC_DETAIL_PERMISSION/,
  'real E2E must prove AC-D33 detail permission isolation with a dedicated unauthorized actor and the formal team-leader detail endpoint.'
)
assert.match(
  source,
  /async function completePqcPieceDetailsForSubmission[\s\S]*getByRole\('button',\s*\{\s*name:\s*\/\^全部合格\$\/\s*\}\)/,
  'real E2E must click the visible 全部合格 bulk button so every choice inspection item receives non-empty pqcPieceValues.'
)
assert.match(
  source,
  /async function selectFirstFormalPqcOption[\s\S]*option:not\(\[value=""\]\)[\s\S]*selectOption/,
  'real E2E must select a non-placeholder formal option for project-level PQC equipment fields.'
)
assert.match(
  source,
  /async function completePqcPieceDetailsForSubmission[\s\S]*selectFirstFormalPqcOption\([\s\S]*data-pqc-equipment-select[\s\S]*selectFirstFormalPqcOption\([\s\S]*data-pqc-equipment-number-select/,
  'real E2E must select formal equipment and equipment number for every PQC inspection item.'
)
assert.match(
  source,
  /async function findPqcLeaderSubmissionRowByTaskAndSignature[\s\S]*Number\(item\.pqcTaskId\)\s*===\s*Number\(submittedTaskId\)[\s\S]*Number\(item\.electronicSignatureId\)\s*===\s*Number\(signatureId\)/,
  'PQC formal-submission E2E must identify the newly-created event by both submitted taskId and configured signatureId, not by reused taskId alone.'
)
assert.match(
  source,
  /key:\s*'releaseOwnerEntry'[\s\S]*actionKey:\s*'verifyEdhrReleaseTraceabilityReadOnly'[\s\S]*acceptanceIds:\s*\['AC-M22',\s*'AC-M23'\]/,
  'M6 release owner phase must run a dedicated eDHR release traceability action for AC-M22/AC-M23 instead of borrowing another role action.'
)
assert.doesNotMatch(
  source,
  /key:\s*'releaseOwnerEntry'[\s\S]*actionKey:\s*'verifyActiveOrderUnauthorizedMutationBlocked'/,
  'M6 release owner phase must not reuse the active-order unauthorized mutation action as AC-M22/AC-M23 evidence.'
)
assert.match(
  source,
  /async function verifyEdhrReleaseTraceabilityReadOnly[\s\S]*loadEdhrReleasePage[\s\S]*loadEdhrReleaseCheckItemPage[\s\S]*loadEdhrReleaseEventPage/,
  'M6 release traceability action must prove the real release page plus release list, check-item, and event read models.'
)
assert.match(
  source,
  /async function prepareEdhrReleaseBatchExecutionViaRealPage[\s\S]*const dialog = leaderPage\.locator\('\.el-dialog:visible'\)\.filter\(\{ hasText: '打开或创建 eDHR 批次执行' \}\)\.first\(\)[\s\S]*const autoOpenedDialog = await dialog\.waitFor\(\{ state: 'visible', timeout: 5000 \}\)\.then\(\(\) => true\)\.catch\(\(\) => false\)[\s\S]*if \(!autoOpenedDialog\)[\s\S]*getByRole\('button', \{ name: '打开\/创建' \}\)\.first\(\)[\s\S]*await dialog\.waitFor\(\{ state: 'visible', timeout: 30000 \}\)/,
  'M6 release preparation must reuse the prefill auto-open dialog before clicking the underlying 打开/创建 button, otherwise Element Plus overlay intercepts the real click.'
)
assert.match(
  remainingRouterSource,
  /path:\s*'pro\/feedback\/edhr-release'[\s\S]*component:\s*\(\)\s*=>\s*import\('@\/views\/mes\/pro\/edhr-release\/ReleasePage\.vue'\)[\s\S]*name:\s*'MesProEdhrReleasePage'[\s\S]*permission:\s*\['mes:pro-edhr-release:query'\]/,
  'M6 release traceability real-page action requires a hidden remaining route for /mes/pro/feedback/edhr-release using the formal ReleasePage component and query permission.'
)
assert.match(
  source,
  /if \(phase\.actionKey === 'verifyEdhrReleaseTraceabilityReadOnly'\)[\s\S]*verifyEdhrReleaseTraceabilityReadOnly\(page,\s*config/,
  'M6 runPhaseAction must dispatch the release owner phase to the dedicated eDHR release action.'
)
assert.match(
  source,
  /if \(phase\.actionKey === 'joinActiveOrder'\)[\s\S]*verifyScheduleOrderErpCandidateAdmission\(page,\s*config\)[\s\S]*verifyActiveOrderTransferTraceReadOnly\(page,\s*config,\s*joinEvidence\)[\s\S]*return \[admissionEvidence,\s*joinEvidence,\s*conflictRouteEvidence,\s*transferTraceEvidence,\s*dailyCloseEvidence\]/,
  'Production leader phase must prove AC-M01 schedule-order admission before joining the active order, then read transfer/shipment/replenishment/return trace before daily-close evidence.'
)
assert.match(
  source,
  /key:\s*'productionLeaderWorkbench'[\s\S]*targetPath:\s*'\/mes\/pro\/process-pool\/production-leader'[\s\S]*selectorGroups:\s*\[[\s\S]*tabText:\s*'报工管理'[\s\S]*data-team-leader-report-workbench[\s\S]*tabText:\s*'看板'[\s\S]*data-role-matrix-daily-close[\s\S]*tabText:\s*'活跃订单池'[\s\S]*data-team-leader-active-order-config[\s\S]*tabText:\s*'班组配置'[\s\S]*data-team-leader-config-center/,
  'Production leader real-flow phase must verify report, dashboard, active-order, and config surfaces through their formal module tabs.'
)
assert.match(
  source,
  /key:\s*'pqcLeaderWorkbench'[\s\S]*selectorGroups:\s*\[[\s\S]*tabText:\s*'PQC管理'[\s\S]*data-team-leader-report-workbench[\s\S]*tabText:\s*'看板'[\s\S]*data-role-matrix-daily-close/,
  'PQC leader real-flow phase must verify management and dashboard surfaces through their actual module tabs.'
)
assert.match(
  source,
  /async function selectRealFlowTab\(page,\s*tabText\)[\s\S]*locator\('\.el-tabs__item'\)\.filter\(\{\s*hasText:\s*tabText\s*\}\)[\s\S]*await tab\.waitFor\(\{\s*state:\s*'visible',\s*timeout:\s*60000\s*\}\)[\s\S]*classList\.contains\('is-active'\)[\s\S]*await tab\.click\(\)/,
  'Full real E2E must wait for asynchronously mounted visible Element Plus module tabs before switching them.'
)
assert.doesNotMatch(
  source,
  /async function selectRealFlowTab\(page,\s*tabText\)[\s\S]*await tab\.count\(\)[\s\S]*===\s*0\)\s*return/,
  'Full real E2E must not skip a formal module tab after an immediate zero count.'
)
assert.match(
  source,
  /async function verifyRealFlowPhase[\s\S]*const selectorGroups = phase\.selectorGroups[\s\S]*for \(const group of selectorGroups\)[\s\S]*await selectRealFlowTab\(page,\s*group\.tabText\)[\s\S]*for \(const selector of group\.selectors\)/,
  'Phase verification must iterate tab-scoped selector groups so hidden module panels do not become raw Playwright timeouts.'
)
assert.match(
  source,
  /if \(phase\.actionKey === 'joinActiveOrder'\)[\s\S]*await selectRealFlowTab\(page,\s*'活跃订单池'\)[\s\S]*data-team-leader-active-order-config[\s\S]*await selectRealFlowTab\(page,\s*'看板'\)[\s\S]*verifyDailyClosePerformanceReadOnly/,
  'Production leader active-order action must join from 活跃订单池 and read daily-close evidence from 看板.'
)
assert.match(
  source,
  /if \(phase\.actionKey === 'verifyPqcLeaderSubmissionFilterPaginationConsistency'\)[\s\S]*await selectRealFlowTab\(page,\s*'PQC管理'\)[\s\S]*verifyPqcLeaderSubmissionFilterPaginationConsistency/,
  'PQC leader action must return to the PQC管理 module before operating the submission workbench.'
)
const activeOrderTransferTraceActionSource = source.match(
  /async function verifyActiveOrderTransferTraceReadOnly[\s\S]*?async function verifyPqcActiveOrderReadOnly/
)
assert.ok(activeOrderTransferTraceActionSource, 'AC-M07 transfer trace real E2E action must exist.')
for (const token of [
  'activeOrderTransferTraceReadOnly',
  'data-team-leader-active-order-transfer-trace',
  '/mes/pro/process-pool/team-leader/active-order/transfer-trace',
  'sourceType',
  'sourceObjectCode',
  'sourceStatus',
  'quantity',
  'materialStockId',
  'batchId',
  'idempotencyKey',
  'mutationRequestCount'
]) {
  assert.match(
    activeOrderTransferTraceActionSource[0],
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `AC-M07 transfer trace real E2E action must prove ${token}.`
  )
}
assert.match(
  activeOrderTransferTraceActionSource[0],
  /const acceptanceIds = \['AC-M02',\s*'AC-M05',\s*'AC-M07',\s*'AC-M08'\]/,
  'active-order transfer trace read-only action must map to ERP transfer/shipment/replenishment/return ACs.'
)
assert.match(
  source,
  /function parsePositiveIntegerEnvList[\s\S]*transferIds:\s*parsePositiveIntegerEnvList\(envValue\('RRM_TRANSFER_IDS'\),\s*'RRM_TRANSFER_IDS'\)/,
  'real E2E must parse RRM_TRANSFER_IDS into formal transferIds before joining the active order.'
)
assert.match(
  source,
  /performActiveOrderJoin[\s\S]*getByRole\('button',\s*\{\s*name:\s*'新增活跃订单'\s*\}\)\.click\(\)[\s\S]*data-team-leader-active-order-add-dialog[\s\S]*fillFormItem\(dialog,\s*'调拨单ID列表',\s*config\.transferIds\.join\(','\)\)/,
  'real E2E must fill the active-order join fields directly within the visible dialog form.'
)
const activeOrderDialogActionsSource = source.match(
  /async function performActiveOrderJoin[\s\S]*?(?=async function verifyActiveOrderTransferTraceReadOnly)/
)
assert.ok(activeOrderDialogActionsSource, 'Active-order dialog action functions must exist.')
assert.doesNotMatch(
  activeOrderDialogActionsSource[0],
  /fillFormItemForAction\(dialog,\s*'加入活跃订单'/,
  'Active-order dialog actions must not infer an el-form ancestor from the footer action button.'
)
assert.match(
  activeOrderDialogActionsSource[0],
  /verifyActiveOrderConflictRouteFailure[\s\S]*fillFormItem\(dialog,\s*'生产订单ID',\s*config\.workOrderId\)[\s\S]*fillFormItem\(dialog,\s*'路线ID',\s*conflictRouteId\)/,
  'Conflict-route verification must fill fields directly within the active-order dialog.'
)
assert.match(
  activeOrderDialogActionsSource[0],
  /verifyActiveOrderConflictRouteFailure[\s\S]*\.el-message,\s*\.el-notification[\s\S]*dialog\.getByRole\('button',\s*\{\s*name:\s*'取消'\s*\}\)\.click\(\)[\s\S]*dialog\.waitFor\(\{\s*state:\s*'hidden'/,
  'Conflict-route verification must close the expected-failure dialog before continuing to other module tabs.'
)
assert.match(
  source,
  /async function performActiveOrderJoin\(page,\s*config\)[\s\S]*const refreshedRows = Array\.isArray\(listBody\.data\) \? listBody\.data : \[\][\s\S]*const rows = refreshedRows\.some\([\s\S]*\?\s*refreshedRows\s*:\s*await reloadActiveOrderRows\(page\)[\s\S]*rows\.some\(\(row\) => Number\(row\.id\) === Number\(activeOrderId\) && Number\(row\.workOrderId\) === Number\(config\.workOrderId\)\)/,
  'real E2E active-order join must re-read the final active-order list before asserting the returned activeOrderId.'
)
assert.match(
  source,
  /verifyPqcRegulationItemsRendered[\s\S]*visibleStandardTexts[\s\S]*data-pqc-standard-button[\s\S]*visibleMethodTexts[\s\S]*data-pqc-method-button[\s\S]*visibleMethodText\.includes\(method\)[\s\S]*visibleStandardText\.includes\(standard\)[\s\S]*visibleMetaText\.includes\(resultTypeLabel\)/,
  'real E2E must verify PQC method, standard, and result type from their actual visible page surfaces.'
)
assert.match(
  frontlineFixedTemplatePanelSource,
  /data-pqc-piece-open-button[\s\S]*openPqcPieceInspection\(activePqcTabItem\.key\)/,
  'PQC page must expose a stable visible button for opening piece-detail inspection.'
)
assert.match(
  source,
  /verifyPqcPieceDetailQuantityPrepared[\s\S]*page\.locator\('\[data-pqc-piece-open-button\]'\)[\s\S]*completePqcPieceDetailsForSubmission[\s\S]*data-pqc-inspection-tab[\s\S]*activePanel\.locator\('\[data-pqc-piece-open-button\]'\)/,
  'real E2E must open PQC piece-detail modals through the visible piece-detail button across QA item tabs.'
)
assert.match(
  source,
  /async function waitForPqcInspectionQuantityHydrated\(page,\s*quantityInput,\s*plannedQuantities[\s\S]*plannedQuantities\.includes\(uiQuantity\)[\s\S]*async function verifyQaRegulationPublishedVersionReadOnly/,
  'real E2E must wait for PQC inspection quantity hydration against formal plannedInspectionQuantity before judging piece-detail quantity.'
)
assert.match(
  source,
  /verifyPqcPieceDetailQuantityPrepared[\s\S]*waitForPqcInspectionQuantityHydrated\(page,\s*quantityInput,\s*plannedQuantities\)[\s\S]*category:\s*'E2E_PQC_PIECE_DETAIL'[\s\S]*hydrationTimeoutMs[\s\S]*requestBudget/,
  'PQC quantity hydration misses must become structured E2E_PQC_PIECE_DETAIL blockers instead of uncaught assertions.'
)

for (const token of [
  'processInspectionAggregationStatus',
  'processInspectionReviewId',
  'processInspectionAggregatedAt'
]) {
  assert.match(processPoolApiSource, new RegExp(token), `process-pool API type must expose ${token}.`)
}

assert.match(
  teamLeaderSource,
  /data-team-leader-review-event-id/,
  'PQC review buttons must expose a stable event-id selector for real-page review verification.'
)
assert.match(
  teamLeaderSource,
  /data-team-leader-detail-event-id/,
  'PQC detail buttons must expose a stable event-id selector for real-page detail traceability verification.'
)
assert.match(
  teamLeaderSource,
  /data-team-leader-correction-event-id/,
  'PQC correction buttons must expose a stable event-id selector for AC-D30 rejected correction verification.'
)
assert.match(
  teamLeaderSource,
  /data-pqc-submission-signature-id/,
  'PQC detail drawer must visibly render the submitted signature id for AC-D33 traceability.'
)
assert.match(
  teamLeaderSource,
  /data-pqc-process-inspection-aggregation/,
  'PQC team leader page must render process-inspection aggregation status visibly.'
)
const teamLeaderTransferTraceApiSource = teamLeaderApiSource.match(
  /export interface TeamLeaderActiveOrderTransferTraceRespVO[\s\S]*?export interface TeamLeaderReportAllocationLine/
)
assert.ok(
  teamLeaderTransferTraceApiSource,
  'team-leader API wrapper must type the formal active-order transfer trace response.'
)
assert.match(
  teamLeaderApiSource,
  /export interface TeamLeaderActiveOrderAddReqVO[\s\S]*transferIds\?: number\[\]/,
  'team-leader active-order add API payload must carry optional formal transferIds.'
)
for (const token of [
  'sourceType',
  'sourceObjectCode',
  'sourceStatus',
  'quantity',
  'materialStockId',
  'batchId',
  'idempotencyKey'
]) {
  assert.match(
    teamLeaderTransferTraceApiSource[0],
    new RegExp(token),
    `team-leader API wrapper must type active-order transfer trace ${token}.`
  )
}
assert.match(
  teamLeaderApiSource,
  /getTeamLeaderActiveOrderTransferTrace[\s\S]*\/mes\/pro\/process-pool\/team-leader\/active-order\/transfer-trace[\s\S]*activeOrderId/,
  'team-leader API wrapper must call the formal read-only active-order transfer trace endpoint.'
)
assert.match(
  teamLeaderSource,
  /<el-form-item label="调拨单ID列表" data-team-leader-active-order-transfer-ids>[\s\S]*v-model="activeOrderForm\.transferIdsText"/,
  'team leader workbench must expose a visible formal transferIds field.'
)
assert.match(
  teamLeaderSource,
  /const parsePositiveIntegerList = \(value: string, label: string\)[\s\S]*Number\.isInteger\(parsed\)[\s\S]*throw new Error\(`\$\{label\}只能包含大于 0 的整数 ID`\)/,
  'team leader workbench must validate transferIds as positive integer IDs.'
)
assert.match(
  teamLeaderSource,
  /transferIds:\s*parsePositiveIntegerList\(activeOrderForm\.transferIdsText,\s*'调拨单ID列表'\)/,
  'team leader workbench must submit visible transferIds as validated transferIds.'
)
const teamLeaderTransferTraceViewSource = teamLeaderSource.match(
  /data-team-leader-active-order-transfer-trace[\s\S]*?<\/el-table>/
)
assert.ok(
  teamLeaderTransferTraceViewSource,
  'team leader workbench must visibly render a read-only active-order transfer trace table.'
)
for (const token of [
  'activeOrderTransferTraceRows',
  'sourceType',
  'sourceObjectCode',
  'sourceStatus',
  'quantity',
  'materialStockId',
  'batchId',
  'idempotencyKey'
]) {
  assert.match(
    teamLeaderTransferTraceViewSource[0],
    new RegExp(token),
    `team leader workbench transfer trace table must render ${token}.`
  )
}
assert.match(
  teamLeaderSource,
  /getTeamLeaderActiveOrderTransferTrace[\s\S]*Promise\.all[\s\S]*activeOrderOptions\.value\.map/,
  'team leader workbench must load trace rows from active-order ids returned by the formal active-order list.'
)

const forbiddenSuccessPattern = new RegExp(['mo' + 'ck', 'placeholder suc' + 'cess', 'default suc' + 'cess'].join('|'), 'i')
assert.doesNotMatch(source, forbiddenSuccessPattern, 'real E2E must not contain fake success paths.')
assert.doesNotMatch(source, /catch\s*\(\s*[^)]*\s*\)\s*\{\s*\}/, 'real E2E must not swallow exceptions.')
assert.doesNotMatch(
  source,
  /M6 全链路真实 E2E 尚未实现/,
  'M6 real E2E must report structured AC coverage blockers instead of a generic not-implemented placeholder.'
)
assert.match(
  source,
  /const\s+listResponsePromise\s*=\s*page\.waitForResponse[\s\S]{0,300}\.catch\(/,
  'active-order join must catch the list refresh wait so a failed write or closed page cannot become an unstructured rejection.'
)
assert.match(
  source,
  /activeOrderListResponseError/,
  'active-order join must convert list refresh wait failures into structured E2E evidence.'
)
assert.match(
  source,
  /pqcLeader[\s\S]*'\/mes\/pro\/process-pool\/pqc-leader'[\s\S]*async function searchPqcLeaderSubmissionsOnPage[\s\S]*table-multi-filter\[data-table-key="mes\.processPool\.teamLeader\.submissions"\]/,
  'PQC leader submission filter E2E must open the standalone route and operate the formal standard multi-filter.'
)
assert.doesNotMatch(
  source,
  /searchPqcLeaderSubmissionsOnPage[\s\S]*getByRole\('tab',\s*\{\s*name:\s*'PQC 组长'\s*\}\)\.click\(\)/,
  'PQC leader submission filter E2E must not depend on the removed internal PQC leader tab.'
)
assert.match(
  source,
  /async function ensurePqcLeaderSubmissionFilterCondition[\s\S]*data-filter-key[\s\S]*新增筛选条件[\s\S]*table-multi-filter__field-select/,
  'PQC leader submission filter E2E must add or activate each formal standard multi-filter condition.'
)
assert.match(
  source,
  /async function searchPqcLeaderSubmissionsOnPage[\s\S]*setPqcLeaderSubmissionFilter\(page,\s*multiFilter,\s*'submitDate',\s*'提交日期',\s*filters\.submitDate\)[\s\S]*setPqcLeaderSubmissionFilter\(page,\s*multiFilter,\s*'workOrderCode',\s*'生产工单',\s*filters\.workOrderCode\)[\s\S]*getByRole\('button',\s*\{\s*name:\s*'查询'\s*\}\)\.click\(\)/,
  'PQC leader submission filter E2E must set the event date and business filters through the standard multi-filter before querying.'
)
assert.match(
  source,
  /async function setPqcLeaderSubmissionFilter[\s\S]*table-multi-filter-field__value[\s\S]*input:not\(\[readonly\]\)/,
  'PQC leader submission filter E2E must target the value control instead of the read-only operator select.'
)
assert.match(
  source,
  /async function setPqcLeaderSubmissionFilter[\s\S]*table-multi-filter__tabs[\s\S]*filterLabel/,
  'PQC leader submission filter E2E must wait for each updated condition to render in its Tab label.'
)
assert.match(
  source,
  /filterKey\s*===\s*'submitDate'[\s\S]*input\.press\(['"]Tab['"]\)/,
  'PQC leader submission filter E2E must commit the Element Plus date value through a real blur.'
)
assert.match(
  source,
  /async function resetPqcLeaderSubmissionFilters[\s\S]*getByRole\('button',\s*\{\s*name:\s*'重置'\s*\}\)[\s\S]*table-multi-filter__tabs-empty[\s\S]*async function searchPqcLeaderSubmissionsOnPage[\s\S]*resetPqcLeaderSubmissionFilters\(multiFilter\)[\s\S]*setPqcLeaderSubmissionFilter/,
  'each PQC leader query must reset the standard multi-filter before rebuilding the target conditions.'
)
assert.doesNotMatch(
  source,
  /data-pqc-leader-filter-(?:product|inspection-type|round|review-status)|fillFormItem\(section,\s*'提交日期'/,
  'PQC leader submission filter E2E must not depend on the removed legacy form controls.'
)
assert.match(
  source,
  /async function verifyPqcLeaderSubmissionFilterPaginationConsistency[\s\S]*candidateWithStablePagination[\s\S]*const inspectCandidate = async[\s\S]*candidatePage\.total >= 2[\s\S]*for \(const candidate of candidates\)[\s\S]*inspectCandidate\(candidate,\s*'base'\)[\s\S]*searchPqcLeaderSubmissionsOnPage/,
  'PQC leader pagination E2E must choose a candidate whose read-model filter has at least two rows before proving page 1/2 stability.'
)
assert.match(
  source,
  /async function preparePqcPaginationCandidate[\s\S]*verifyPqcFormalSubmissionCreatesEvent[\s\S]*pqcPaginationCandidatePrepared[\s\S]*async function verifyPqcLeaderSubmissionFilterPaginationConsistency[\s\S]*preparePqcPaginationCandidate\(page,\s*config,\s*actionEvidence\)/,
  'PQC leader pagination E2E must use the real PQC inspector page to create an additional same-filter submission before declaring D32 data blocked.'
)
assert.match(
  source,
  /async function verifyPqcLeaderReviewApprovalAggregatesProcessInspection[\s\S]*data-team-leader-review-event-id[\s\S]*\/mes\/pro\/process-pool\/team-leader\/submission\/review[\s\S]*processInspectionAggregationStatus[\s\S]*AGGREGATED/,
  'PQC leader review E2E must approve a real row from the page and verify process-inspection aggregation in the read model.'
)
assert.match(
  source,
  /async function fillPqcLeaderReviewSignature[\s\S]*requireSignatureId\(config,\s*'pqcLeader'\)[\s\S]*复核签名ID[\s\S]*签名员工ID[\s\S]*签名快照[\s\S]*async function verifyPqcLeaderReviewApprovalAggregatesProcessInspection[\s\S]*fillPqcLeaderReviewSignature[\s\S]*waitFor\(\{ state: 'hidden'/,
  'PQC leader page review must fill the formal leader signature context and wait for the review dialog to close.'
)
assert.match(
  source,
  /async function verifyPqcLeaderReviewApprovalAggregatesProcessInspection[\s\S]*reviewButtonVisible[\s\S]*reviewButton[\s\S]*\.waitFor\(\{ state: 'visible'[\s\S]*\.catch\(\(error\) => \(\{ reviewButtonError: error \}\)\)[\s\S]*E2E_PQC_REVIEW_PAGE/,
  'PQC leader approval review button visibility failures must become structured E2E_PQC_REVIEW_PAGE blockers.'
)
assert.match(
  source,
  /async function verifyPqcLeaderDuplicateTerminalReviewBlocked[\s\S]*pqcLeaderReviewApprovedAndAggregated[\s\S]*PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS[\s\S]*expectedErrorCode\s*=\s*1040760329[\s\S]*\/mes\/pro\/process-pool\/team-leader\/submission\/review[\s\S]*reviewSignatureId[\s\S]*reviewSignatureEmployeeUserId[\s\S]*reviewSignatureSnapshotJson[\s\S]*isBusinessSuccess\(result\.body\)[\s\S]*E2E_PQC_REVIEW_TERMINAL/,
  'PQC leader review E2E must attempt a second terminal review for the same approved event and prove the backend rejects it.'
)
assert.match(
  source,
  /async function verifyPqcLeaderSelfReviewBlocked[\s\S]*resolveRoleUserId\(page,\s*config,\s*'pqcLeader'\)[\s\S]*searchPqcLeaderSubmissionsOnPage[\s\S]*PRO_PROCESS_POOL_SUBMISSION_REVIEW_SELF_FORBIDDEN[\s\S]*expectedErrorCode\s*=\s*1040760330[\s\S]*\/mes\/pro\/process-pool\/team-leader\/submission\/review[\s\S]*reviewSignatureId[\s\S]*reviewSignatureEmployeeUserId[\s\S]*reviewSignatureSnapshotJson[\s\S]*E2E_PQC_REVIEW_SELF/,
  'PQC leader review E2E must observe a pending self-review row in the real page and prove the formal review endpoint rejects it.'
)
assert.match(
  source,
  /async function preparePqcSelfReviewCandidate[\s\S]*reviewerUserId[\s\S]*verifyPqcFormalSubmissionCreatesEvent[\s\S]*pqcSelfReviewCandidatePrepared[\s\S]*async function verifyPqcLeaderSelfReviewBlocked[\s\S]*preparePqcSelfReviewCandidate\(page,\s*config,\s*actionEvidence,\s*reviewerUserId\)/,
  'PQC self-review E2E must prepare the missing self-review candidate through the real PQC inspector page path with actualEmployeeUserId equal to the reviewer.'
)
assert.match(
  source,
  /async function verifyPqcLeaderRejectedCorrectionChain[\s\S]*data-team-leader-correction-event-id[\s\S]*reviewStatus:\s*'REJECTED'[\s\S]*fillPqcLeaderReviewSignature[\s\S]*\/mes\/pro\/process-pool\/team-leader\/submission\/review[\s\S]*waitFor\(\{ state: 'hidden'[\s\S]*revisionSignatureId[\s\S]*\/mes\/pro\/process-pool\/event-revision\/update-original[\s\S]*modificationHistorySummary[\s\S]*E2E_PQC_REJECT_CORRECTION/,
  'PQC rejected-correction E2E must reject a real pending submission, submit the page correction through update-original with a new signature, and prove modification history in the read model.'
)
assert.match(
  source,
  /async function preparePqcRejectedCorrectionCandidate[\s\S]*login\(pqcPage,\s*config,\s*'pqcInspector'[\s\S]*verifyPqcActiveOrderReadOnly[\s\S]*verifyPqcRegulationItemsRendered[\s\S]*verifyPqcPieceDetailQuantityPrepared[\s\S]*verifyPqcActualEmployeeSwitch[\s\S]*verifyPqcFormalSubmissionCreatesEvent/,
  'PQC rejected-correction E2E must prepare a missing candidate through the real PQC inspector page path before the leader rejects it.'
)
assert.match(
  source,
  /async function verifyPqcLeaderRejectedCorrectionChain[\s\S]*preparePqcRejectedCorrectionCandidate\(page,\s*config,\s*actionEvidence\)[\s\S]*loadPqcLeaderSubmissionPage\(page,\s*\{[\s\S]*submissionReviewStatus:\s*'PENDING'/,
  'PQC rejected-correction E2E must retry the real pending read model after preparing its own PQC submission candidate.'
)
assert.match(
  source,
  /async function verifyPqcLeaderRejectedCorrectionChain[\s\S]*resolveUnusedPqcSignatureId\(page,\s*config,\s*'pqcInspector'\)[\s\S]*revisionSignatureId\s*=\s*signatureResolution\.signatureId/,
  'PQC rejected-correction E2E must use the configured unused signature pool for revisionSignatureId, not a generated timestamp.'
)
assert.match(
  source,
  /async function verifyPqcLeaderRejectedCorrectionChain[\s\S]*reviewButtonVisible[\s\S]*reviewButton[\s\S]*\.waitFor\(\{ state: 'visible'[\s\S]*\.catch\(\(error\) => \(\{ reviewButtonError: error \}\)\)[\s\S]*E2E_PQC_REJECT_CORRECTION/,
  'AC-D30 rejected-correction review button visibility failures must become structured E2E_PQC_REJECT_CORRECTION blockers.'
)
assert.match(
  source,
  /async function verifyPqcLeaderRejectedCorrectionChain[\s\S]*correctionButtonVisible[\s\S]*correctionButton[\s\S]*\.waitFor\(\{ state: 'visible'[\s\S]*\.catch\(\(error\) => \(\{ correctionButtonError: error \}\)\)[\s\S]*E2E_PQC_REJECT_CORRECTION/,
  'AC-D30 correction button visibility failures must become structured E2E_PQC_REJECT_CORRECTION blockers.'
)
assert.doesNotMatch(
  source,
  /const\s+revisionSignatureId\s*=\s*Date\.now\(\)/,
  'PQC rejected-correction E2E must not generate revisionSignatureId from Date.now().'
)
assert.match(
  source,
  /async function verifyPqcProcessInspectionAggregationReadOnly[\s\S]*pqcLeaderReviewApprovedAndAggregated[\s\S]*pqcLeaderSelfReviewBlocked[\s\S]*loadPqcLeaderSubmissionPage[\s\S]*processInspectionAggregationStatus[\s\S]*AGGREGATED[\s\S]*notEqual[\s\S]*AGGREGATED[\s\S]*E2E_PQC_AGGREGATION_READONLY/,
  'PQC process-inspection aggregation E2E must add a read-only proof that approved events are aggregated while pending/self-review-blocked events are not aggregated.'
)
assert.match(
  source,
  /key:\s*'qaRegulationEntry'[\s\S]*actionKey:\s*'verifyQaRegulationPublishedVersionReadOnly'[\s\S]*acceptanceIds:\s*\['AC-M09'[\s\S]*'AC-D23'\]/,
  'QA regulation entry must run a real action instead of only proving the old page shell loads.'
)
const qaRegulationActionSource = source.match(/async function verifyQaRegulationPublishedVersionReadOnly[\s\S]*?async function verifyPqcPieceDetailQuantityPrepared/)
assert.ok(
  qaRegulationActionSource,
  'QA regulation real E2E must keep verifyQaRegulationPublishedVersionReadOnly before the next PQC action for static inspection.'
)
assert.match(
  qaRegulationActionSource[0],
  /qaRegulationPublishedVersionReadOnly/,
  'QA regulation real E2E action evidence must use a stable qaRegulationPublishedVersionReadOnly key.'
)
assert.match(
  qaRegulationActionSource[0],
  /AC-M09[\s\S]*AC-D23/,
  'QA regulation real E2E action evidence must map the full BDD-07 acceptance range.'
)
assert.match(
  qaRegulationActionSource[0],
  /formalSelectorEvidence[\s\S]*E2E_QA_REGULATION_PAGE/,
  'QA regulation real E2E must prove a formal published-version page surface or emit a structured E2E_QA_REGULATION_PAGE blocker.'
)
assert.match(
  qaRegulationActionSource[0],
  /waitForSelector\('\[data-qa-regulation-section\]'[\s\S]*waitForResponse[\s\S]*inspection-regulation\/published-version/,
  'QA regulation real E2E must wait for the formal QA section and published-version API response before judging the page surface.'
)
assert.match(
  qaRegulationActionSource[0],
  /apiStatus[\s\S]*apiCode[\s\S]*apiMessage[\s\S]*qaRegulationApiEvidence/,
  'QA regulation real E2E blockers must include published-version API status/code/message evidence.'
)
assert.match(
  source,
  /if \(phase\.actionKey === 'verifyQaRegulationPublishedVersionReadOnly'\)[\s\S]*verifyQaRegulationPublishedVersionReadOnly\(page,\s*config\)/,
  'QA regulation phase action must be wired into runPhaseAction.'
)
assert.match(
  source,
  /function buildM6ConcurrencyPerformanceGateEvidence[\s\S]*observedConcurrencyAcceptanceIds[\s\S]*observedActionKeys[\s\S]*observedConcurrencyAcceptanceIds/,
  'M6 concurrency gate evidence must list observed CONC acceptance ids instead of reporting only AC-M04 after later CONC actions have passed.'
)
assert.match(
  source,
  /function collectM6ConcurrencyProofs[\s\S]*AC-M04[\s\S]*shouldReturnExistingActiveOrderWhenConcurrentInsertHitsUniqueKey[\s\S]*AC-D29[\s\S]*shouldRejectPqcInspectionWhenPendingTaskWasConsumedConcurrently[\s\S]*AC-D34[\s\S]*shouldRejectDuplicateTerminalReviewForSameSubmission[\s\S]*AC-D37[\s\S]*shouldRejectConcurrentDuplicateAggregationWhenPendingWasConsumed/,
  'M6 concurrency gate must collect per-AC service-level proof instead of relying only on observed real actions.'
)
assert.match(
  source,
  /ACTIVE_ORDER_TRANSFER_TRACE_SERVICE_TEST[\s\S]*MesActiveOrderTransferTraceServiceTest\.java[\s\S]*function collectM6ConcurrencyProofs[\s\S]*transferTraceServiceTestSource[\s\S]*AC-M07[\s\S]*shouldReturnExistingTransferTraceWhenConcurrentInsertHitsUniqueKey[\s\S]*DuplicateKeyException[\s\S]*selectByIdempotencyKey/,
  'M6 AC-M07 concurrency proof must read the runnable transfer-trace service test and prove duplicate/concurrent idempotency.'
)
assert.match(
  activeOrderTransferTraceServiceTestSource,
  /shouldReturnExistingTransferTraceWhenConcurrentInsertHitsUniqueKey[\s\S]*DuplicateKeyException[\s\S]*uk_mes_pp_active_order_transfer_trace[\s\S]*selectByIdempotencyKey/,
  'AC-M07 transfer-trace service test must prove unique-key race handling reloads the existing trace by idempotency key.'
)
assert.match(
  teamLeaderOrderProcessCompletionServiceTestSource,
  /shouldPreventOverTargetProgressWhenConcurrentAllocationAlreadyConsumedRemainingQuantity[\s\S]*never\(\)\)\.backfillCompletedProcess/,
  'AC-M18 order-process completion service test must prove over-target concurrent progress never reaches batch-record backfill.'
)
assert.match(
  source,
  /AC-M18[\s\S]*never\\\(\\\)\\\)\\\.backfillCompletedProcess[\s\S]*shouldPreventOverTargetProgress/,
  'M6 AC-M18 concurrency proof must recognize Mockito verify(..., never()).backfillCompletedProcess syntax.'
)
assert.match(
  source,
  /TEAM_LEADER_BATCH_RECORD_BACKFILL_SERVICE_TEST[\s\S]*MesTeamLeaderBatchRecordBackfillServiceTest\.java[\s\S]*function collectM6ConcurrencyProofs[\s\S]*batchRecordBackfillServiceTestSource[\s\S]*AC-M19[\s\S]*shouldBackfillCompletedProcessOnlyOnceWhenConcurrentAuditAlreadyApplied/,
  'M6 AC-M19 concurrency proof must read the runnable batch-record backfill service test instead of the order-process completion proxy test.'
)
assert.match(
  teamLeaderBatchRecordBackfillServiceTestSource,
  /shouldBackfillCompletedProcessOnlyOnceWhenConcurrentAuditAlreadyApplied[\s\S]*PROCESS_POOL_REPORT_BACKFILL_AGG:9001:5001:6001:agg-single-1001-7101[\s\S]*times\(2\)[\s\S]*saveSystemCellLinkChanges/,
  'AC-M19 batch-record backfill service test must prove repeated/concurrent backfill uses the same idempotency key and delegates duplicate suppression to field audit.'
)
assert.match(
  source,
  /EDHR_RELEASE_SERVICE_TEST[\s\S]*MesProEdhrReleaseServiceImplTest\.java[\s\S]*function collectM6ConcurrencyProofs[\s\S]*releaseServiceTestSource[\s\S]*AC-M23[\s\S]*shouldRejectConcurrentReleaseTerminalWhenPrecheckWasConsumedUnderForUpdateLock/,
  'M6 AC-M23 concurrency proof must read the runnable eDHR release service test and prove terminal release state is guarded under row lock.'
)
assert.match(
  edhrReleaseServiceTestSource,
  /shouldRejectConcurrentReleaseTerminalWhenPrecheckWasConsumedUnderForUpdateLock[\s\S]*STATUS_PRECHECK_PASSED[\s\S]*STATUS_RELEASED[\s\S]*selectByIdForUpdate[\s\S]*PRO_EDHR_RELEASE_PRECHECK_REQUIRED[\s\S]*assertEquals\(1,\s*batchSignatureMapper\.selectListByBatchExecutionId/,
  'AC-M23 eDHR release service test must prove a consumed precheck cannot create another terminal release signature or event after the locked reread.'
)
assert.match(
  source,
  /function hasCompleteM6ConcurrencyGateEvidence[\s\S]*missingAcceptanceIds[\s\S]*complete/,
  'M6 concurrency gate must fail closed until every CONC AC has explicit proof and expose the exact missing list.'
)
assert.match(
  source,
  /const concurrencyGateStatus = hasCompleteM6ConcurrencyGateEvidence[\s\S]*concurrencyGateStatus \? 'm6ConcurrencyGateVerified' : 'm6ConcurrencyGateDeferred'/,
  'M6 concurrency gate must switch to PASS only when all required per-AC concurrency proofs are complete.'
)
assert.match(
  source,
  /missingConcurrencyAcceptanceIds[\s\S]*provedConcurrencyAcceptanceIds[\s\S]*concurrencyProofs/,
  'M6 concurrency gate evidence must include proved and missing CONC AC lists plus the raw proof flags.'
)
assert.match(
  source,
  /function collectM6PerformanceProofs[\s\S]*shouldUseCountAndPageQueriesWithoutDetailLookupsForDailyCloseSubmissionSummary[\s\S]*shouldPreparePqcPieceDetailContextWithBulkQueriesOnly[\s\S]*idx_mes_pp_event_timeline_acd32/,
  'M6 performance gate must collect backend query-count and index proof instead of relying only on page observations.'
)
assert.match(
  source,
  /function hasCompleteM6PerformanceGateEvidence[\s\S]*dailyClosePerformanceReadOnly[\s\S]*pqcPieceDetailQuantityPrepared[\s\S]*pqcLeaderSubmissionFilterPaginationConsistent/,
  'M6 performance gate must require all four PERF AC actions before it can pass.'
)
assert.match(
  source,
  /const performanceGateStatus = hasCompleteM6PerformanceGateEvidence[\s\S]*performanceGateStatus \? 'm6PerformanceGateVerified' : 'm6PerformanceGateDeferred'/,
  'M6 performance gate must switch to PASS when request-budget, query-count, paging, and index proof are complete.'
)
const dailyClosePerformanceActionSource = source.match(/async function verifyDailyClosePerformanceReadOnly[\s\S]*?async function verifyRealFlowPhase/)
assert.ok(dailyClosePerformanceActionSource, 'M6 daily-close performance evidence action must exist before phase verification.')
assert.match(dailyClosePerformanceActionSource[0], /data-role-matrix-daily-close-card/, 'M6 daily-close performance evidence must read the real daily-close cards.')
assert.match(dailyClosePerformanceActionSource[0], /AC-D12/, 'M6 daily-close performance evidence must map to AC-D12.')
assert.match(dailyClosePerformanceActionSource[0], /AC-D38/, 'M6 daily-close performance evidence must map to AC-D38.')
assert.match(dailyClosePerformanceActionSource[0], /dailyClosePerformanceReadOnly/, 'M6 daily-close performance evidence must use a stable action key.')
assert.match(dailyClosePerformanceActionSource[0], /createDailyCloseRequestBudgetTracker/, 'M6 daily-close performance evidence must start a request budget tracker.')
assert.match(dailyClosePerformanceActionSource[0], /submissionDetailRequests[\s\S]*0/, 'M6 daily-close performance evidence must prove the cards do not trigger per-row submission detail requests.')
assert.match(dailyClosePerformanceActionSource[0], /requestBudget/, 'M6 daily-close performance evidence must write request budget evidence into the action result.')
assert.match(
  source,
  /if \(phase\.actionKey === 'joinActiveOrder'\)[\s\S]*verifyActiveOrderTransferTraceReadOnly\(page,\s*config,\s*joinEvidence\)[\s\S]*verifyDailyClosePerformanceReadOnly\(page,\s*config,\s*\[[\s\S]*joinEvidence,[\s\S]*conflictRouteEvidence,[\s\S]*transferTraceEvidence[\s\S]*\]\)/,
  'Production leader phase must collect daily-close performance evidence before final cleanup.'
)
assert.doesNotMatch(
  source,
  /if \(phase\.actionKey === 'joinActiveOrder'\)[\s\S]*verifyActiveOrderCleanupTraceability\(page,\s*config,\s*joinEvidence\)[\s\S]*return \[joinEvidence,\s*conflictRouteEvidence,\s*cleanupEvidence/,
  'Active-order cleanup must not run in the production leader phase before downstream PQC/release/day-close actions finish.'
)
assert.match(
  source,
  /await runFinalActiveOrderCleanup\(browser,\s*config,\s*actionEvidence\)[\s\S]*const acceptanceCoverage = buildAcceptanceCoverage/,
  'Full real E2E must run final active-order cleanup after all role actions and before building coverage/blocker evidence.'
)
const pqcPieceDetailPerformanceActionSource = source.match(/async function verifyPqcPieceDetailQuantityPrepared[\s\S]*?async function fillVisiblePqcPieceModalValues/)
assert.ok(pqcPieceDetailPerformanceActionSource, 'M6 PQC piece-detail performance evidence action must exist before modal helper functions.')
const pqcPieceModalFillSource = source.match(
  /async function fillVisiblePqcPieceModalValues\(modal\)[\s\S]*?(?=\nasync function completePqcPieceDetailsForSubmission)/
)
assert.ok(pqcPieceModalFillSource, 'PQC piece modal fill helper must exist.')
assert.match(
  pqcPieceModalFillSource[0],
  /\.frontline-pqc-piece-row button\.pass/,
  'PQC piece modal helper must click only the explicit pass button for an all-pass fixture.'
)
assert.doesNotMatch(
  pqcPieceModalFillSource[0],
  /hasText:\s*'合格'/,
  'PQC piece modal helper must not use substring text matching that also selects 不合格.'
)
assert.match(
  pqcPieceDetailPerformanceActionSource[0],
  /createPqcPieceDetailRequestBudgetTracker/,
  'M6 PQC piece-detail evidence must start a request budget tracker before opening and completing piece-detail modals.'
)
assert.match(
  pqcPieceDetailPerformanceActionSource[0],
  /pieceDetailRequests[\s\S]*0/,
  'M6 PQC piece-detail evidence must prove modal completion does not trigger per-item piece-detail requests.'
)
assert.match(
  pqcPieceDetailPerformanceActionSource[0],
  /requestBudget/,
  'M6 PQC piece-detail evidence must write request budget evidence into the AC-D27 action result.'
)
assert.match(
  timelineFilterTestSource,
  /shouldUseCountAndPageQueriesWithoutDetailLookupsForDailyCloseSubmissionSummary[\s\S]*assertEquals\(1,\s*mapper\.getCountQueryCalls\(\)[\s\S]*assertEquals\(1,\s*mapper\.getPageQueryCalls\(\)[\s\S]*assertEquals\(0,\s*mapper\.getDetailQueryCalls\(\)/,
  'M6 daily-close performance gate must have backend query-count proof for submission summary page reads.'
)
assert.match(
  activeOrderServiceTestSource,
  /shouldListActiveOrdersWithSingleActiveOrderQueryForDailyClosePerformance[\s\S]*selectActiveList\(\)[\s\S]*never\(\)\)\.selectListByScheduleOrderId[\s\S]*never\(\)\)\.insertBatch/,
  'M6 daily-close performance gate must prove active-order card reads do not load or rebuild per-process snapshots.'
)
assert.match(
  frontlinePqcContextServiceTestSource,
  /shouldPreparePqcPieceDetailContextWithBulkQueriesOnly[\s\S]*selectListByRouteId\(ROUTE_ID\)[\s\S]*selectListByActiveOrderId\(ACTIVE_ORDER_ID\)[\s\S]*selectListByVersionId\(REGULATION_VERSION_ID\)[\s\S]*never\(\)\)\.selectPendingByActiveOrderProcess[\s\S]*never\(\)\)\.selectById/,
  'M6 AC-D27 performance gate must prove PQC piece-detail context preparation uses bulk route/task/regulation reads and no per-process or per-piece task detail queries.'
)
const pqcLeaderSubmissionPerformanceActionSource = source.match(/async function verifyPqcLeaderSubmissionFilterPaginationConsistency[\s\S]*?function parseOriginalPayloadJson/)
assert.ok(pqcLeaderSubmissionPerformanceActionSource, 'M6 PQC leader submission performance evidence action must exist before detail helpers.')
assert.match(
  pqcLeaderSubmissionPerformanceActionSource[0],
  /createPqcLeaderSubmissionListRequestBudgetTracker/,
  'M6 PQC leader submission pagination evidence must start a request budget tracker around the real list filter and page reads.'
)
assert.match(
  pqcLeaderSubmissionPerformanceActionSource[0],
  /submissionDetailRequests[\s\S]*0/,
  'M6 PQC leader submission pagination evidence must prove list paging does not trigger per-row detail requests.'
)
assert.match(
  pqcLeaderSubmissionPerformanceActionSource[0],
  /requestBudget/,
  'M6 PQC leader submission pagination evidence must write request budget evidence into the AC-D32 action result.'
)
const pqcSignaturePoolScanner = source.match(/async function resolveUnusedPqcSignatureId[\s\S]*?async function verifyPqcFormalSubmissionCreatesEvent/)
assert.ok(pqcSignaturePoolScanner, 'real E2E script must keep resolveUnusedPqcSignatureId before verifyPqcFormalSubmissionCreatesEvent for static inspection.')
assert.match(
  source,
  /function isPqcSignaturePoolRole\(roleKey,\s*preferredRoleKey\)[\s\S]*roleKey === preferredRoleKey[\s\S]*\^pqcExtra\\d\+\$/,
  'PQC signature-pool candidates must be limited to the preferred PQC role plus pqcExtra* task-owned IDs.'
)
assert.match(
  source,
  /function collectConfiguredSignatureIds\(config,\s*preferredRoleKey\)[\s\S]*isPqcSignaturePoolRole\(roleKey,\s*preferredRoleKey\)[\s\S]*pushCandidate\(signatureId\)/,
  'PQC signature-pool collection must not fall through to unrelated role signature IDs after the preferred PQC ID is consumed.'
)
assert.match(
  pqcSignaturePoolScanner[0],
  /const\s+submitDate\s*=\s*localDateString\(\)[\s\S]*loadPqcLeaderSubmissionPage\(leaderPage,\s*\{[\s\S]*submitDate/,
  'PQC signature-pool scan must pass submitDate into the submission page query because the backend timeline requires a formal submit-date window.'
)
const pqcFormalSubmissionSource = source.match(/async function verifyPqcFormalSubmissionCreatesEvent[\s\S]*?function localDateString/)
assert.ok(
  pqcFormalSubmissionSource,
  'real E2E script must keep verifyPqcFormalSubmissionCreatesEvent before localDateString for static inspection.'
)
assert.match(
  source,
  /routeProcessIds:\s*\[[\s\S]*RRM_ROUTE_PROCESS_ID_1[\s\S]*RRM_ROUTE_PROCESS_ID_2[\s\S]*\][\s\S]*primaryRouteProcessId:\s*Number\(envValue\('RRM_ROUTE_PROCESS_ID_1'\)\)/,
  'RRM config must retain both route process IDs and a primary route process ID so PQC prerequisite production submit is tied to the same formal route context.'
)
assert.match(
  source,
  /const\s+PRODUCTION_FILL_ROUTE\s*=\s*'\/mes\/pro\/feedback\/edhr-batch-production-fill'[\s\S]*const\s+FRONTLINE_SUBMIT_ENDPOINT\s*=\s*'\/mes\/pro\/feedback\/frontline\/submit'/,
  'RRM real E2E must define the formal production fill route and production submit endpoint before PQC submission.'
)
assert.match(
  source,
  /function buildProductionFillUrl[\s\S]*feedbackCode[\s\S]*routeProcessId[\s\S]*signatureEmployeeId[\s\S]*outputQuantity[\s\S]*idempotencyKey/,
  'RRM real E2E must build a production fill URL with formal route process, signature, quantity, and idempotency context.'
)
assert.match(
  frontlineFixedTemplatePanelSource,
  /const\s+findInitialProcess[\s\S]*context\.routeProcessId[\s\S]*context\.processId[\s\S]*handleSelectProcess\(initialProcess\)/,
  'frontline production/PQC fixed template page must initialize the selected process from route query before falling back to the first process.'
)
assert.match(
  frontlineFixedTemplatePanelSource,
  /const\s+findInitialEmployee[\s\S]*context\.actualEmployeeId[\s\S]*deviceState\.employeeOptions\.find[\s\S]*handleSelectEmployee\(initialEmployee\)/,
  'frontline fixed template page must initialize the selected employee from actualEmployeeId query before falling back to the first employee.'
)
assert.match(
  source,
  /async function readFrontlineProductionSubmitState[\s\S]*submitButton\.isDisabled[\s\S]*statusText[\s\S]*async function waitForFrontlineProductionSubmitReady[\s\S]*submitFrontlineProductionForPqcPrereq[\s\S]*waitForFrontlineProductionSubmitReady/,
  'PQC prerequisite production source event E2E must wait for the production page to finish process/employee/template hydration before declaring the submit button disabled.'
)
assert.match(
  source,
  /async function preparePqcFormalSubmissionContext[\s\S]*buildProductionFillUrl[\s\S]*submitFrontlineProductionForPqcPrereq[\s\S]*processPoolEventId/,
  'PQC formal submission must create and capture a fresh processPoolEventId through the real production submit path.'
)
assert.match(
  source,
  /function buildPqcFillUrl[\s\S]*productionSubmitEventId[\s\S]*processPoolEventId[\s\S]*routeProcessId[\s\S]*processId/,
  'PQC fill URL must carry the fresh production event and the exact route-process identity instead of relying on historical or default process state.'
)
assert.match(
  source,
  /async function clickPqcEmployeeOptionAndWaitForSwitch[\s\S]*for \(let attempt = 1; attempt <= 3; attempt \+= 1\)[\s\S]*targetOption\.click\(\{ timeout: 10000 \}\)[\s\S]*pqcSwitchEmployeeResponseError/,
  'PQC employee picker clicks must retry real visible options and convert unstable detached DOM into structured personnel blockers.'
)
assert.match(
  source,
  /async function clickPqcEmployeeOptionAndWaitForSwitch[\s\S]*try \{[\s\S]*targetOption\.waitFor\(\{ state: 'visible', timeout: 30000 \}\)[\s\S]*\} catch \(error\) \{[\s\S]*lastClickError = error[\s\S]*pqcSwitchEmployeeResponseError/,
  'PQC employee picker helper must convert missing candidate waits into structured personnel blockers instead of raw Playwright timeouts.'
)
assert.match(
  source,
  /async function verifyPqcActualEmployeeSwitch[\s\S]*clickPqcEmployeeOptionAndWaitForSwitch/,
  'Initial PQC actual-employee selection must use the stable employee-picker helper.'
)
assert.match(
  source,
  /async function switchPqcActualEmployeeToUser[\s\S]*isPqcEmployeeCardAlreadySelected[\s\S]*clickPqcEmployeeOptionAndWaitForSwitch/,
  'PQC actual-employee restoration must accept route-hydrated selection and otherwise use the stable employee-picker helper.'
)
assert.match(
  source,
  /function buildPqcFillUrl\(config,\s*context,\s*employeeEvidence,\s*signatureId\)[\s\S]*appendQueryValue\(query,\s*'signatureId',\s*signatureId\)/,
  'PQC fill URL must carry the fresh unused signatureId through route query so the real page hydrates pqcSignatureId without a hidden test-only input.'
)
assert.match(
  pqcFormalSubmissionSource[0],
  /preparePqcFormalSubmissionContext[\s\S]*buildPqcFillUrl[\s\S]*page\.goto/,
  'verifyPqcFormalSubmissionCreatesEvent must prepare the formal production submit context and reopen PQC with productionSubmitEventId before clicking submit.'
)
assert.match(
  pqcFormalSubmissionSource[0],
  /resolveUnusedPqcSignatureId[\s\S]*const\s+signatureId\s*=\s*signatureResolution\.signatureId[\s\S]*buildPqcFillUrl\(config,\s*formalSubmissionContext,\s*employeeEvidence,\s*signatureId\)[\s\S]*waitForPqcSubmitReady/,
  'PQC formal submission must resolve the unused signature before navigation, pass it through URL query, and wait for submit readiness instead of editing a removed control.'
)
assert.match(
  pqcFormalSubmissionSource[0],
  /validationResponsePromise[\s\S]*frontline-template\/payload\/validate[\s\S]*pqcSubmitResponseError[\s\S]*E2E_PQC_TEMPLATE_VALIDATION[\s\S]*validationResponseCode[\s\S]*postClickState[\s\S]*postClickMessages/,
  'PQC formal submission must capture template payload validation when submit is not emitted, instead of reporting an opaque missing submit response.'
)
assert.doesNotMatch(
  pqcFormalSubmissionSource[0],
  /#frontlinePqcSignatureId/,
  'PQC formal submission E2E must not wait for the removed #frontlinePqcSignatureId input; signature context is route-query driven.'
)
assert.match(
  frontlineFixedTemplatePanelSource,
  /pqcSignatureId\.value\s*=\s*firstRouteQueryNumber\(\['signatureId'\]\)\s*\?\?\s*pqcSignatureId\.value/,
  'frontline PQC page must hydrate pqcSignatureId from the signatureId route query.'
)
assert.doesNotMatch(
  source,
  /RRM_PROCESS_POOL_EVENT_ID|RRM_PRODUCTION_SUBMIT_EVENT_ID/,
  'RRM full real E2E must not accept historical production event IDs from env; it must create a fresh event in the same run.'
)
assert.match(
  pqcFormalSubmissionSource[0],
  /findPqcLeaderSubmissionRowByTaskAndSignature[\s\S]*submittedTaskId[\s\S]*signatureId/,
  'PQC formal submission E2E must scan the leader submission pages by pqcTaskId and signatureId; the backend timeline is ascending and new events may not be on page 1.'
)
assert.doesNotMatch(
  pqcFormalSubmissionSource[0],
  /pageSize:\s*20[\s\S]*submittedRow\s*=\s*submissionPage\.list\.find/,
  'PQC formal submission E2E must not prove event creation by checking only the first 20 leader submission rows.'
)
const pqcProcessesLoader = source.match(/async function loadPqcProcessesViaAuth[\s\S]*?function buildPqcProcessSourceBlocker/)
assert.ok(pqcProcessesLoader, 'real E2E script must keep buildPqcProcessSourceBlocker directly after the PQC process loader for static inspection.')
assert.doesNotMatch(
  pqcProcessesLoader[0],
  /async function loadPqcProcessesViaAuth[\s\S]*assert\.equal\(result\.body\?\.code,\s*0/,
  'PQC process source API business failures must become structured E2E_PQC_TASK_SOURCE blockers so later M6 gates can continue.'
)

console.log('PASS role-requirement-matrix preflight static contract')
