const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const backendRoot = path.join(workspaceRoot, 'IntRuoyiBackend')
const packageJsonPath = path.join(frontendRoot, 'package.json')
const realFlowPath = path.join(frontendRoot, 'tests/e2e/role-requirement-matrix-real-flow.e2e.js')
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
  'e2e:role-matrix-migration-preflight:static': 'tests/e2e/role-matrix-migration-preflight-static.spec.cjs'
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
assert.ok(
  fs.existsSync(activeOrderAuthorityMigrationPath),
  'M1 active-order authority migration must exist before the real source blocker check can clear RRM-BLK-006.'
)
assert.ok(
  fs.existsSync(activeOrderProcessSnapshotMigrationPath),
  'M2 active-order process snapshot migration must exist before the production coefficient blockers can clear.'
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
  'verifyPqcPieceDetailQuantityPrepared',
  'pqcPieceDetailQuantityPrepared',
  'verifyPqcFormalSubmissionCreatesEvent',
  'pqcFormalSubmissionCreated',
  'resolveUnusedPqcSignatureId',
  'collectConfiguredSignatureIds',
  'E2E_PQC_SIGNATURE_POOL',
  'verifyPqcLeaderSubmissionFilterPaginationConsistency',
  'pqcLeaderSubmissionFilterPaginationConsistent',
  'verifyPqcActualEmployeeSwitch',
  'pqcActualEmployeeSelected',
  'verifyActiveOrderUnauthorizedMutationBlocked',
  'activeOrderUnauthorizedMutationBlocked',
  'unauthorizedActor',
  'verifyActiveOrderCleanupTraceability',
  'activeOrderCleanupDeferred',
  'buildM6ConcurrencyPerformanceGateEvidence',
  'buildGateBlockers',
  'm6ConcurrencyGateDeferred',
  'm6PerformanceGateDeferred',
  'E2E_CONCURRENCY',
  'E2E_PERFORMANCE',
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
  '/mes/pro/feedback/edhr-batch-pqc-fill',
  '/mes/pro/feedback/frontline/device-account/pqc/active-orders',
  '/mes/pro/feedback/frontline/device-account/pqc/active-order/processes',
  '/mes/pro/feedback/frontline/device-account/pqc/personnel',
  '/mes/pro/feedback/frontline/device-account/pqc/switch-employee',
  '/mes/pro/feedback/frontline/device-account/pqc/submit',
  'writeEvidence',
  'failFast'
]) {
  assert.match(source, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `real E2E script must include ${token}.`)
}

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
  /async function verifyPqcLeaderSubmissionFilterPaginationConsistency[\s\S]*getByRole\('tab',\s*\{\s*name:\s*'PQC 组长'\s*\}\)\.click\(\)[\s\S]*data-pqc-leader-filter-product/,
  'PQC leader submission filter E2E must switch to the PQC leader tab before locating PQC-only filters.'
)
assert.match(
  source,
  /function\s+fillElementPlusInput[\s\S]*input\$\{selector\},\s*\$\{selector\} input/,
  'PQC leader submission filter E2E must fill Element Plus inputs whether data-* is on the wrapper or the native input.'
)
const pqcProcessesLoader = source.match(/async function loadPqcProcessesViaAuth[\s\S]*?\n}\n\nfunction buildPqcProcessSourceBlocker/)
assert.ok(pqcProcessesLoader, 'real E2E script must keep buildPqcProcessSourceBlocker directly after the PQC process loader for static inspection.')
assert.doesNotMatch(
  pqcProcessesLoader[0],
  /async function loadPqcProcessesViaAuth[\s\S]*assert\.equal\(result\.body\?\.code,\s*0/,
  'PQC process source API business failures must become structured E2E_PQC_TASK_SOURCE blockers so later M6 gates can continue.'
)

console.log('PASS role-requirement-matrix preflight static contract')
