const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.resolve(__dirname, '../..')

const readUtf8 = (root, relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readUtf8(frontendRoot, 'package.json'))
const frontlineApi = readUtf8(frontendRoot, 'src/api/mes/pro/feedback/index.ts')
const processPoolApi = readUtf8(frontendRoot, 'src/api/mes/pro/processpool/index.ts')
const teamLeaderApi = readUtf8(frontendRoot, 'src/api/mes/pro/processpool/teamLeader.ts')
const frontlinePanel = readUtf8(frontendRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const timelinePage = readUtf8(frontendRoot, 'src/views/mes/pro/processpool/TimelinePage.vue')
const teamLeaderPage = readUtf8(frontendRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const realE2e = readUtf8(frontendRoot, 'tests/e2e/p0-production-execution-loop-real.e2e.js')
const scopeContract = readUtf8(workspaceRoot, 'docs/acceptance/production-execution-main-loop/scope-contract.md')
const tddPlan = readUtf8(workspaceRoot, 'docs/acceptance/production-execution-main-loop/tdd-plan.md')
const pqcPayloadBuilderMatch = frontlinePanel.match(
  /const\s+buildPqcInspectionSubmitPayload[\s\S]*?\r?\n}\r?\n\r?\nconst\s+formatLocalDateTime/
)
assert.ok(pqcPayloadBuilderMatch, 'PQC payload builder must be locatable for focused contract checks.')
const pqcPayloadBuilder = pqcPayloadBuilderMatch[0]
const assertPqcPayloadField = (fieldName) => {
  assert.match(
    pqcPayloadBuilder,
    new RegExp(`\\b${fieldName}\\b(?:\\s*:\\s*\\b${fieldName}\\b)?\\s*,`),
    `PQC payload builder must submit ${fieldName}.`
  )
}

assert.equal(
  packageJson.scripts['e2e:p0-production-execution-loop:static'],
  'node tests/e2e/p0-production-execution-loop-static.spec.cjs',
  'package.json must expose the P0 static contract script.'
)
assert.equal(
  packageJson.scripts['e2e:p0-production-execution-loop:real'],
  'node tests/e2e/p0-production-execution-loop-real.e2e.js',
  'package.json must expose the P0 real E2E script.'
)

for (const requiredDocToken of [
  'processPoolEventId',
  'complete=false',
  'PQC',
  'FIFO',
  '电子签名',
  '班组长复核',
  '批记录追溯'
]) {
  assert.match(scopeContract, new RegExp(requiredDocToken), `P0 scope contract must mention ${requiredDocToken}.`)
}

assert.match(tddPlan, /P0-T00[\s\S]*P0-T12/, 'P0 TDD plan must keep the full RED/GREEN sequence.')
assert.match(tddPlan, /e2e:p0-production-execution-loop:static/, 'P0 TDD plan must point to the static script.')
assert.match(tddPlan, /e2e:p0-production-execution-loop:real/, 'P0 TDD plan must point to the real E2E script.')

assert.match(
  processPoolApi,
  /processPoolEventId\??:\s*number/,
  'Process pool frontend DTO must expose processPoolEventId as the canonical trace root, not only a generic id.'
)
assert.match(
  processPoolApi,
  /interface\s+ProductionExecutionTraceVO[\s\S]*submit[\s\S]*quality[\s\S]*review[\s\S]*allocation[\s\S]*completion[\s\S]*batchRecord/,
  'Frontend API must model the unified P0 trace sections: submit, quality, review, allocation, completion, batchRecord.'
)
assert.match(
  processPoolApi,
  /interface\s+ProductionExecutionClosureEvidenceVO[\s\S]*answers:\s*Record<string,\s*ProductionExecutionEvidenceAnswerVO>[\s\S]*sameSourceChecks:\s*ProductionExecutionSameSourceCheckVO\[][\s\S]*blockers:\s*ProductionExecutionTraceBlockerVO\[]/,
  'Frontend API must model the P0 closureEvidence packet returned by backend trace.'
)
for (const evidenceType of [
  'ProductionExecutionEvidenceAnswerVO',
  'ProductionExecutionSameSourceCheckVO',
  'ProductionExecutionReadOnlyVerificationEntryVO'
]) {
  assert.match(
    processPoolApi,
    new RegExp(`interface\\s+${evidenceType}`),
    `Frontend API must expose ${evidenceType} for closureEvidence.`
  )
}
assert.match(
  processPoolApi,
  /interface\s+ProductionExecutionTraceVO[\s\S]*closureEvidence\??:\s*ProductionExecutionClosureEvidenceVO/,
  'Unified P0 trace DTO must include closureEvidence, not only the six section summaries.'
)
assert.match(
  processPoolApi,
  /getProductionExecutionTrace\s*=\s*async[\s\S]*\/mes\/pro\/process-pool\/team-leader\/production-execution\/trace/,
  'Frontend API must provide the formal team-leader processPoolEventId-based P0 trace endpoint.'
)

assert.match(
  frontlineApi,
  /interface\s+FrontlinePqcInspectionSubmitReqVO[\s\S]*productionSubmitEventId:\s*number[\s\S]*deviceAccountId:\s*number[\s\S]*deviceId:\s*number[\s\S]*workstationId:\s*number[\s\S]*pqcSubmissionIdempotencyKey:\s*string/,
  'PQC submit request must carry productionSubmitEventId, deviceAccountId, deviceId, workstationId, and pqcSubmissionIdempotencyKey.'
)
for (const fieldName of ['productionSubmitEventId', 'deviceAccountId', 'deviceId', 'workstationId', 'pqcSubmissionIdempotencyKey']) {
  assertPqcPayloadField(fieldName)
}
assert.match(
  pqcPayloadBuilder,
  /缺少PQC正式提交上下文[\s\S]*productionSubmitEventId[\s\S]*deviceAccountId[\s\S]*deviceId[\s\S]*workstationId/,
  'PQC payload builder must fail fast when formal production submit, device, and workstation context is missing.'
)

assert.match(
  teamLeaderApi,
  /interface\s+TeamLeaderSubmissionReviewReqVO[\s\S]*reviewSignatureId:\s*number/,
  'Team leader review request must require a structured electronic signature id.'
)
assert.match(
  teamLeaderApi,
  /reviewSignatureEmployeeUserId:\s*number/,
  'Team leader review request must carry the signature employee user id.'
)
assert.match(
  teamLeaderApi,
  /reviewSignatureSnapshotJson\??:\s*string/,
  'Team leader review request must carry or reference a signature snapshot.'
)
assert.match(
  teamLeaderApi,
  /confirmTeamLeaderReportAllocation[\s\S]*reviewSignatureId/,
  'FIFO allocation confirm must not bypass the signed team-leader review payload.'
)

assert.match(
  timelinePage,
  /data-p0-production-execution-trace/,
  'Timeline page must expose a P0 trace surface anchored by processPoolEventId.'
)
assert.match(
  timelinePage,
  /complete\s*===\s*false|!.*complete/,
  'Trace UI must surface incomplete trace status instead of hiding missing sections.'
)
assert.match(
  timelinePage,
  /data-p0-closure-evidence/,
  'Trace UI must expose the closureEvidence packet as a dedicated evidence surface.'
)
for (const answerKey of [
  'who',
  'device',
  'process',
  'quantity',
  'quality',
  'signature',
  'workOrder',
  'review',
  'batchRecord'
]) {
  assert.match(
    timelinePage,
    new RegExp(`['"]${answerKey}['"]`),
    `Trace UI must explicitly render closureEvidence answer ${answerKey}.`
  )
}
assert.match(
  timelinePage,
  /readOnlyVerificationEntries/,
  'Trace UI must render closureEvidence read-only verification entries.'
)
assert.match(
  timelinePage,
  /sameSourceChecks/,
  'Trace UI must render closureEvidence same-source checks.'
)
assert.match(
  timelinePage,
  /trace\.complete\s*===\s*true[\s\S]*closureEvidence/,
  'Trace UI must fail visibly when complete=true lacks closureEvidence.'
)
assert.match(
  teamLeaderPage,
  /data-team-leader-review-signature/,
  'Team leader page must expose a review signature control before approval/allocation.'
)
assert.match(
  teamLeaderPage,
  /reviewSignatureId/,
  'Team leader page must submit the structured reviewSignatureId field.'
)

for (const envKey of [
  'P0_FRONTEND_URL',
  'P0_BACKEND_URL',
  'P0_RUN_ID',
  'P0_TENANT',
  'P0_USERNAME',
  'P0_PASSWORD',
  'P0_WORK_ORDER_ID',
  'P0_DEVICE_ID',
  'P0_DEVICE_ACCOUNT_ID',
  'P0_SIGNATURE_ID',
  'P0_SIGNATURE_EMPLOYEE_ID',
  'P0_SUBMIT_IDEMPOTENCY_KEY',
  'P0_SUBMIT_QUANTITY',
  'P0_CONFIRM_QUANTITY',
  'P0_PQC_TASK_ID',
  'P0_QA_REGULATION_VERSION_ID',
  'P0_PQC_SIGNATURE_ID',
  'P0_PQC_SIGNATURE_EMPLOYEE_ID',
  'P0_PQC_IDEMPOTENCY_KEY',
  'P0_PQC_INSPECTION_QUANTITY',
  'P0_PQC_QUALIFIED_QUANTITY',
  'P0_PQC_ALLOCATABLE_QUANTITY',
  'P0_PQC_REVIEW_SIGNATURE_ID',
  'P0_PQC_REVIEW_SIGNATURE_EMPLOYEE_ID',
  'P0_REVIEW_SIGNATURE_ID',
  'P0_REVIEW_SIGNATURE_EMPLOYEE_ID',
  'P0_RUNTIME_DB_HOST',
  'P0_RUNTIME_DB_PORT',
  'P0_RUNTIME_DB_NAME',
  'P0_RUNTIME_DB_USER',
  'P0_RUNTIME_DB_PASSWORD',
  'P0_BATCH_RECORD_DEFINITION_ID',
  'P0_BATCH_RECORD_VERSION_ID',
  'P0_SCHEMA_MIGRATION_ID',
  'P0_MIGRATION_POLICY_EVIDENCE'
]) {
  assert.match(realE2e, new RegExp(envKey), `P0 real E2E must fail fast on missing ${envKey}.`)
}

assert.match(
  realE2e,
  /appendQueryValue\(query,\s*['"]actualEmployeeId['"],\s*config\.signatureEmployeeId\)/,
  'P0 real E2E must pass the production actualEmployeeId into the real production fill page.'
)
assert.match(
  realE2e,
  /appendQueryValue\(query,\s*['"]outputQuantity['"],\s*config\.submitQuantity\)/,
  'P0 real E2E must pass the production submit quantity into the real production fill page.'
)
assert.match(
  realE2e,
  /appendQueryValue\(query,\s*['"]idempotencyKey['"],\s*config\.submitIdempotencyKey\)/,
  'P0 real E2E must use P0_SUBMIT_IDEMPOTENCY_KEY for the real production submit page.'
)
assert.match(
  realE2e,
  /SYSTEM_AUTH_GET_PERMISSION_INFO_ENDPOINT/,
  'P0 real E2E must read the formal current-user endpoint before trusting deviceAccountId.'
)
assert.match(
  realE2e,
  /deviceAccountId/,
  'P0 real E2E must require and verify P0_DEVICE_ACCOUNT_ID instead of relying on an implicit login user.'
)
assert.match(
  realE2e,
  /appendQueryValue\(query,\s*['"]signatureId['"],\s*config\.pqcSignatureId\)/,
  'P0 real E2E must use the PQC signature id for the PQC fill page, not the production signature id.'
)
assert.match(
  realE2e,
  /appendQueryValue\(query,\s*['"]actualEmployeeId['"],\s*config\.pqcSignatureEmployeeId\)/,
  'P0 real E2E must pass the PQC actualEmployeeId into the real PQC fill page.'
)
assert.match(
  realE2e,
  /appendQueryValue\(query,\s*['"]pqcSubmissionIdempotencyKey['"],\s*config\.pqcIdempotencyKey\)/,
  'P0 real E2E must use P0_PQC_IDEMPOTENCY_KEY for the real PQC submit page.'
)
for (const pqcQueryField of ['pqcTaskId', 'regulationVersionId', 'pqcInspectionQuantity']) {
  assert.match(
    realE2e,
    new RegExp(`appendQueryValue\\(query,\\s*['"]${pqcQueryField}['"]`),
    `P0 real E2E must pass ${pqcQueryField} into the real PQC fill page.`
  )
}
assert.match(
  frontlinePanel,
  /productionDraft\.outputQuantity\s*=\s*firstRouteQueryNumber\(\[['"]outputQuantity['"],\s*['"]submitQuantity['"]\]\)/,
  'Production fill page must hydrate outputQuantity from route query for deterministic real E2E.'
)
assert.doesNotMatch(
  realE2e,
  /envValue\(['"]P0_PROCESS_POOL_EVENT_ID['"]\)|numberEnv\(['"]P0_PROCESS_POOL_EVENT_ID['"]\)/,
  'P0 real E2E must not accept a historical P0_PROCESS_POOL_EVENT_ID as input.'
)
assert.match(
  realE2e,
  /confirmIdempotencyKey/,
  'P0 real E2E must carry P0_CONFIRM_IDEMPOTENCY_KEY in evidence even when backend duplicate protection is source-event unique.'
)
assert.match(
  realE2e,
  /pqcReviewSignatureId/,
  'P0 real E2E must keep PQC review signature separate from production FIFO confirmation signature.'
)
assert.match(
  realE2e,
  /batchRecordDefinitionId[\s\S]*batchRecordVersionId/,
  'P0 real E2E must require formal batch-record definition and version IDs, not only a report id.'
)
assert.match(
  realE2e,
  /schemaMigrationId[\s\S]*migrationPolicyEvidence/,
  'P0 real E2E must carry schema migration and release policy evidence before browser writes.'
)
assert.match(
  realE2e,
  /function\s+validateMigrationPolicyEvidence\s*\(/,
  'P0 real E2E must validate migration policy evidence content before browser writes.'
)
assert.match(
  realE2e,
  /P0_MIGRATION_POLICY_EVIDENCE_NOT_PASS/,
  'P0 real E2E must block migration policy evidence without explicit PASS or with BLOCKED/FAIL markers.'
)

assert.match(
  realE2e,
  /const\s+RUNTIME_MIGRATION_VERIFIER_SCRIPT\s*=/,
  'P0 real E2E must declare the runtime migration verifier script path.'
)
assert.match(
  realE2e,
  /require\(['"]node:child_process['"]\)/,
  'P0 real E2E must use a child process to invoke the read-only runtime migration verifier.'
)
assert.match(
  realE2e,
  /async\s+function\s+runRuntimeMigrationVerifier\s*\(/,
  'P0 real E2E must implement a runtime migration verifier preflight.'
)
assert.match(
  realE2e,
  /verify_p0_runtime_migration\.py/,
  'P0 real E2E must call verify_p0_runtime_migration.py before browser writes.'
)
assert.match(
  realE2e,
  /P0_RUNTIME_SCHEMA_BLOCKED/,
  'P0 real E2E must preserve runtime schema blockers instead of continuing to browser writes.'
)
assert.match(
  realE2e,
  /runtimeMigration/,
  'P0 real E2E evidence must include runtimeMigration verification results.'
)
for (const evidenceLine of [
  'Duplicate Production Submit Verified',
  'Duplicate PQC Submit Verified',
  'Duplicate FIFO Confirm Rejected'
]) {
  assert.match(
    realE2e,
    new RegExp(evidenceLine.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `P0 real E2E evidence must write ${evidenceLine} instead of only executing the duplicate action.`
  )
}

assert.match(realE2e, /processPoolEventId/, 'P0 real E2E must use processPoolEventId as the trace root.')
assert.match(realE2e, /require\(['"]playwright['"]\)/, 'P0 real E2E must load Playwright for a real browser path.')
assert.match(realE2e, /chromium\.launch/, 'P0 real E2E must launch Chromium instead of using API-only preflight.')
assert.match(realE2e, /newContext\(/, 'P0 real E2E must create an isolated browser context.')
assert.match(realE2e, /page\.goto/, 'P0 real E2E must navigate through a real frontend page.')
assert.match(
  realE2e,
  /const\s+TEAM_LEADER_ROUTE\s*=\s*['"]\/mes\/pro\/process-pool\/team-leader['"]/,
  'P0 real E2E must lock the team-leader workbench route as a real UI step.'
)
assert.match(
  realE2e,
  /const\s+PRODUCTION_FILL_ROUTE\s*=\s*['"]\/mes\/pro\/feedback\/edhr-batch-production-fill['"]/,
  'P0 real E2E must lock the production fill route as the frontline submit UI step.'
)
assert.match(
  realE2e,
  /const\s+PQC_FILL_ROUTE\s*=\s*['"]\/mes\/pro\/feedback\/edhr-batch-pqc-fill['"]/,
  'P0 real E2E must lock the PQC fill route as the quality submit UI step.'
)
assert.match(
  realE2e,
  /const\s+TIMELINE_ROUTE\s*=\s*['"]\/mes\/pro\/process-pool\/timeline['"]/,
  'P0 real E2E must lock the process-pool timeline route for trace verification.'
)
for (const functionName of [
  'login',
  'openTeamLeaderWorkbench',
  'openProductionFill',
  'openPqcFill',
  'openProductionExecutionTrace'
]) {
  assert.match(
    realE2e,
    new RegExp(`async\\s+function\\s+${functionName}\\s*\\(`),
    `P0 real E2E must implement ${functionName} as an explicit real-page step.`
  )
}
assert.match(
  realE2e,
  /\/system\/auth\/login/,
  'P0 real E2E login must wait for the real backend login request.'
)
assert.doesNotMatch(realE2e, /console\.(?:log|error|warn)\([^)]*password/i, 'P0 real E2E must never log the password.')
for (const endpointName of [
  'FRONTLINE_SUBMIT_ENDPOINT',
  'PQC_SUBMIT_ENDPOINT',
  'TEAM_LEADER_REVIEW_ENDPOINT',
  'TEAM_LEADER_ALLOCATION_CONFIRM_ENDPOINT',
  'PRODUCTION_EXECUTION_TRACE_ENDPOINT'
]) {
  assert.match(
    realE2e,
    new RegExp(`const\\s+${endpointName}\\s*=`),
    `P0 real E2E must track ${endpointName} as a target request boundary.`
  )
}
for (const functionName of [
  'submitFrontlineProduction',
  'duplicateFrontlineProduction',
  'submitPqcInspection',
  'duplicatePqcInspection',
  'reviewTeamLeaderSubmission',
  'confirmTeamLeaderAllocation',
  'duplicateTeamLeaderAllocationConfirm',
  'fetchProductionExecutionTrace',
  'validateClosureEvidence'
]) {
  assert.match(
    realE2e,
    new RegExp(`async\\s+function\\s+${functionName}\\s*\\(`),
    `P0 real E2E must implement ${functionName} as an explicit action-level step.`
  )
}
for (const endpointName of [
  'FRONTLINE_SUBMIT_ENDPOINT',
  'PQC_SUBMIT_ENDPOINT',
  'TEAM_LEADER_REVIEW_ENDPOINT',
  'TEAM_LEADER_ALLOCATION_CONFIRM_ENDPOINT',
  'PRODUCTION_EXECUTION_TRACE_ENDPOINT'
]) {
  assert.match(
    realE2e,
    new RegExp(`waitForEndpointResponse\\s*\\([\\s\\S]*${endpointName}`),
    `P0 real E2E must wait for the real ${endpointName} response before continuing.`
  )
}
assert.match(
  realE2e,
  /const\s+TARGET_REQUEST_BOUNDARIES\s*=\s*\[/,
  'P0 real E2E must keep labeled target request boundaries for completion evidence.'
)
assert.match(
  realE2e,
  /function\s+resolveTargetRequestBoundary\s*\(/,
  'P0 real E2E must resolve responses through an explicit target request boundary helper.'
)
assert.match(
  realE2e,
  /label:\s*boundary\.label/,
  'P0 real E2E result.json targetRequests must retain the labeled target request boundary.'
)
assert.match(
  realE2e,
  /function\s+hasSameTargetRequestEvidence\s*\(/,
  'P0 real E2E must normalize duplicate target request responses before writing result.json evidence.'
)
assert.match(
  realE2e,
  /function\s+buildTargetRequestEvidenceLines\s*\(/,
  'P0 real E2E must write target request hit evidence lines.'
)
assert.match(
  realE2e,
  /function\s+buildTargetResponseIdentityEvidenceLines\s*\(/,
  'P0 real E2E must write target response identity evidence lines.'
)
assert.match(
  realE2e,
  /function\s+buildTargetResponseIdentityEvidence\s*\(/,
  'P0 real E2E must normalize target response identities for result.json.'
)
assert.match(
  realE2e,
  /const\s+generatedAt\s*=\s*new Date\(\)\.toISOString\(\)/,
  'P0 real E2E evidence must create one generated-at ISO timestamp for completion-gate freshness.'
)
assert.match(
  realE2e,
  /const\s+evidenceResult\s*=\s*{[\s\S]*\.\.\.result,[\s\S]*generatedAt[\s\S]*}/,
  'P0 real E2E result.json must include the same generatedAt used in Markdown evidence.'
)
assert.match(
  realE2e,
  /targetResponseIdentities:\s*buildTargetResponseIdentityEvidence\(result\)/,
  'P0 real E2E result.json must include normalized target response identity evidence.'
)
assert.match(
  realE2e,
  /field:\s*identity\.field/,
  'P0 real E2E result.json targetResponseIdentities must include each response identity field name.'
)
assert.match(
  realE2e,
  /value:\s*identity\.value\s*\|\|\s*null/,
  'P0 real E2E result.json targetResponseIdentities must include each response identity value slot.'
)
assert.match(
  realE2e,
  /sourceRequestLabel:\s*identity\.label/,
  'P0 real E2E result.json targetResponseIdentities must bind each identity back to its canonical target request label.'
)
assert.match(
  realE2e,
  /targetRequestEvidenceFlushed:\s*true/,
  'P0 real E2E PASS result.json must prove target request business-code parsing was flushed before evidence write.'
)
assert.match(
  realE2e,
  /runtimeMigration:\s*result\.runtimeMigration/,
  'P0 real E2E result.json must include runtime migration verifier evidence.'
)
assert.match(
  realE2e,
  /function\s+normalizeBrowserDiagnostics\s*\(/,
  'P0 real E2E must normalize browser diagnostics for Markdown and result.json evidence.'
)
assert.match(
  realE2e,
  /browserDiagnostics:\s*normalizeBrowserDiagnostics\(result\)/,
  'P0 real E2E result.json must include normalized browser diagnostics evidence.'
)
for (const duplicateField of [
  'duplicateProductionSubmitVerified',
  'duplicatePqcSubmitVerified',
  'duplicateConfirmRejected'
]) {
  assert.match(
    realE2e,
    new RegExp(`${duplicateField}:\\s*browserPreflight\\.${duplicateField}`),
    `P0 real E2E PASS result.json must retain ${duplicateField} from the browser run.`
  )
}
assert.match(
  realE2e,
  /Generated At:[^\n]*generatedAt/,
  'P0 real E2E Markdown evidence must use the shared generatedAt timestamp.'
)
for (const endpointName of [
  'FRONTLINE_SUBMIT_ENDPOINT',
  'PQC_SUBMIT_ENDPOINT',
  'TEAM_LEADER_REVIEW_ENDPOINT',
  'TEAM_LEADER_ALLOCATION_CONFIRM_ENDPOINT',
  'PRODUCTION_EXECUTION_TRACE_ENDPOINT'
]) {
  assert.match(
    realE2e,
    new RegExp(`label:\\s*['"]${endpointName}['"][\\s\\S]*endpoint:\\s*${endpointName}`),
    `P0 real E2E must label target request boundary ${endpointName}.`
  )
  assert.match(
    realE2e,
    new RegExp(`Target Request \\$\\{boundary\\.label\\} Hit`),
    `P0 real E2E evidence must emit target request hit line for ${endpointName}.`
  )
  assert.match(
    realE2e,
    new RegExp(`Target Request \\$\\{boundary\\.label\\} URL`),
    `P0 real E2E evidence must emit target request URL line for ${endpointName}.`
  )
  assert.match(
    realE2e,
    new RegExp(`Target Request \\$\\{boundary\\.label\\} Method`),
    `P0 real E2E evidence must emit target request HTTP method line for ${endpointName}.`
  )
  assert.match(
    realE2e,
    new RegExp(`Target Request \\$\\{boundary\\.label\\} HTTP Status`),
    `P0 real E2E evidence must emit target request HTTP status line for ${endpointName}.`
  )
  assert.match(
    realE2e,
    new RegExp(`Target Request \\$\\{boundary\\.label\\} Business Code`),
    `P0 real E2E evidence must emit target request business code line for ${endpointName}.`
  )
  assert.ok(
    realE2e.includes('Target Response ${label} ${identity.field}'),
    `P0 real E2E evidence must emit target response identity line for ${endpointName}.`
  )
}
assert.match(
  realE2e,
  /function\s+buildBrowserDiagnosticEvidenceLines\s*\(/,
  'P0 real E2E must write browser diagnostic evidence lines.'
)
for (const diagnosticLine of [
  'Browser Page Errors',
  'Browser Console Errors',
  'Target Request Failures'
]) {
  assert.match(
    realE2e,
    new RegExp(diagnosticLine.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `P0 real E2E evidence must emit ${diagnosticLine}.`
  )
}
for (const eventName of ['pageerror', 'console', 'requestfailed']) {
  assert.match(
    realE2e,
    new RegExp(`page\\.on\\(['"]${eventName}['"]`),
    `P0 real E2E must track browser ${eventName} diagnostics.`
  )
}
assert.match(
  realE2e,
  /extractProcessPoolEventIdFromFrontlineResponse/,
  'P0 real E2E must extract processPoolEventId from the frontline submit response.'
)
assert.match(
  realE2e,
  /(?:const\s+processPoolEventId\s*=\s*await\s+submitFrontlineProduction|const\s+\{[\s\S]*processPoolEventId[\s\S]*\}\s*=\s*await\s+submitFrontlineProduction)/,
  'P0 real E2E must dynamically capture a fresh processPoolEventId from the frontline action.'
)
assert.match(
  realE2e,
  /const\s+executionConfig\s*=\s*\{\s*\.\.\.config,\s*processPoolEventId\s*\}/,
  'P0 real E2E must pass the captured processPoolEventId into downstream PQC, review, allocation, and trace steps.'
)
assert.match(
  realE2e,
  /duplicateFrontlineProduction\(\s*page,\s*config,\s*routeSteps,\s*productionFillUrl,\s*processPoolEventId\s*\)/,
  'P0 real E2E must repeat the real production submit page with the same idempotency key before downstream steps.'
)
assert.match(
  realE2e,
  /duplicateProcessPoolEventId[\s\S]*processPoolEventId/,
  'P0 real E2E duplicate production submit must assert the second response returns the original processPoolEventId.'
)
assert.match(
  realE2e,
  /duplicatePqcInspection\(\s*page,\s*executionConfig,\s*routeSteps,\s*pqcFillUrl,\s*pqcEventId\s*\)/,
  'P0 real E2E must repeat the real PQC submit page with the same idempotency key before review and FIFO steps.'
)
assert.match(
  realE2e,
  /duplicatePqcEventId[\s\S]*pqcEventId/,
  'P0 real E2E duplicate PQC submit must assert the second response returns the original PQC result instead of creating duplicate details or events.'
)
assert.match(
  realE2e,
  /duplicateTeamLeaderAllocationConfirm\(\s*page,\s*executionConfig,\s*routeSteps,\s*allocationResponse\s*\)/,
  'P0 real E2E must repeat the real team-leader FIFO confirm page before trace verification.'
)
assert.match(
  realE2e,
  /PRO_PROCESS_POOL_REPORT_ALLOCATION_DUPLICATE/,
  'P0 real E2E duplicate FIFO confirm must require an explicit backend duplicate rejection instead of relying on button disablement.'
)
assert.match(
  realE2e,
  /duplicateConfirmResponse[\s\S]*allocationResponse/,
  'P0 real E2E duplicate FIFO confirm must retain the first allocation response and the duplicate response for traceable evidence.'
)
assert.doesNotMatch(
  realE2e,
  /P0_PROCESS_POOL_EVENT_ID/,
  'P0 real E2E must not accept a historical processPoolEventId from env for the write-path PASS.'
)
assert.match(
  realE2e,
  /buildPqcFillUrl\(\s*executionConfig\s*\)/,
  'P0 real E2E PQC route must use the captured production submit processPoolEventId.'
)
assert.match(
  realE2e,
  /validateClosureEvidence\(\s*trace\.closureEvidence\s*,\s*processPoolEventId\s*\)/,
  'P0 real E2E must validate closureEvidence for the freshly captured processPoolEventId.'
)
assert.match(
  realE2e,
  /closureEvidence\.complete\s*!==\s*true/,
  'P0 real E2E must reject trace closureEvidence when complete is not explicitly true.'
)
for (const answerKey of [
  'who',
  'device',
  'process',
  'quantity',
  'quality',
  'signature',
  'workOrder',
  'review',
  'batchRecord'
]) {
  assert.match(
    realE2e,
    new RegExp(`CLOSURE_EVIDENCE_REQUIRED_ANSWERS[\\s\\S]*['"]${answerKey}['"]`),
    `P0 real E2E must validate closureEvidence answer ${answerKey}.`
  )
}
assert.match(realE2e, /closureEvidence/, 'P0 real E2E evidence must include the backend closureEvidence packet.')
assert.match(realE2e, /readOnlyVerificationEntries/, 'P0 real E2E evidence must retain read-only verification entries.')
assert.match(realE2e, /sameSourceChecks/, 'P0 real E2E evidence must retain same-source check results.')
assert.match(realE2e, /CLOSURE_EVIDENCE_MISSING_SOURCE/, 'P0 real E2E must treat missing closure evidence source as non-PASS.')
assert.match(
  realE2e,
  /for\s*\(\s*const\s+blocker\s+of\s+answer\.blockers\s*\|\|\s*\[\]\s*\)/,
  'P0 real E2E must fail when an individual closureEvidence answer retains blockers.'
)
assert.match(realE2e, /status:\s*'BLOCKED'/, 'P0 real E2E must report BLOCKED for missing real prerequisites.')
assert.doesNotMatch(realE2e, /status:\s*'PASS'[\s\S]*missing/i, 'P0 real E2E must not report PASS when prerequisites are missing.')

console.log('PASS: P0 production execution loop static contract is wired')
