const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const packageJsonPath = path.join(frontendRoot, 'package.json')
const realFlowPath = path.join(frontendRoot, 'tests/e2e/role-requirement-matrix-real-flow.e2e.js')

const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'))
const scripts = packageJson.scripts || {}
const plannedStaticScripts = {
  'e2e:role-matrix-qa-regulation:static': 'tests/e2e/role-matrix-qa-regulation-static.spec.cjs',
  'e2e:role-matrix-pqc-dynamic-form:static': 'tests/e2e/role-matrix-pqc-dynamic-form-static.spec.cjs',
  'e2e:role-matrix-transfer-start-check:static': 'tests/e2e/role-matrix-transfer-start-check-static.spec.cjs',
  'e2e:role-matrix-daily-close-scope:static': 'tests/e2e/role-matrix-daily-close-scope-static.spec.cjs'
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
  'writeEvidence',
  'failFast'
]) {
  assert.match(source, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `real E2E script must include ${token}.`)
}

const forbiddenSuccessPattern = new RegExp(['mo' + 'ck', 'placeholder suc' + 'cess', 'default suc' + 'cess'].join('|'), 'i')
assert.doesNotMatch(source, forbiddenSuccessPattern, 'real E2E must not contain fake success paths.')
assert.doesNotMatch(source, /catch\s*\(\s*[^)]*\s*\)\s*\{\s*\}/, 'real E2E must not swallow exceptions.')

console.log('PASS role-requirement-matrix preflight static contract')
