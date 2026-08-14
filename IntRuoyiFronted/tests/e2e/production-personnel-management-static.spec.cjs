const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const page = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const api = readUtf8('src/api/mes/pro/processpool/teamLeader.ts')
const route = readUtf8('src/router/modules/remaining.ts')
const frontlineContext = readUtf8('src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts')
const packageJson = JSON.parse(readUtf8('package.json'))
const realE2ePath = path.join(repoRoot, 'tests/e2e/production-personnel-management-real.e2e.js')

const requirePage = (pattern, message) => assert.match(page, pattern, message)
const requireApi = (functionName, endpoint, message) => {
  assert.match(api, new RegExp(`${functionName}\\s*=\\s*async`), `${message}: missing function`)
  assert.match(api, new RegExp(endpoint.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `${message}: missing endpoint`)
}

requirePage(/import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index\.vue'/,
  'employee management tab must use the standard UnifiedListTemplate.')
requirePage(/data-team-leader-production-personnel-tab/,
  'team leader page must expose a standalone production personnel tab.')
requirePage(/table-key="mes\.processPool\.teamLeader\.productionPersonnel"/,
  'production personnel list must use a stable standard-list table key.')
requirePage(/data-team-leader-production-personnel-list/,
  'production personnel rows must render in the scoped personnel list.')
requirePage(/data-team-leader-formal-employee-select/,
  'formal employee add flow must use a remote searchable input dropdown.')
requirePage(/remote-method="searchFormalEmployeeCandidatesForSelect"/,
  'formal employee dropdown must search backend candidates instead of loading all users.')
requirePage(/data-team-leader-temporary-employee-form/,
  'temporary employee add flow must use a dedicated display-name and signature-password form.')
requirePage(/v-model="temporaryEmployeeForm\.displayName"/,
  'temporary worker creation must capture display name.')
requirePage(/v-model="temporaryEmployeeForm\.signaturePassword"/,
  'temporary worker creation must capture signature password.')
assert.doesNotMatch(page, /data-team-leader-personnel-audit-list/,
  'employee management tab must not render a standalone operation trace table.')
assert.doesNotMatch(page, /<el-divider>\s*操作追溯\s*<\/el-divider>/,
  'employee management tab must not render a standalone operation trace title.')
assert.doesNotMatch(page, /employeeAuditRows|employeeAuditLoading|loadEmployeeAuditRecords/,
  'employee management tab must not keep local audit-table state.')
requirePage(/resetTemporarySignaturePassword/,
  'temporary worker signature password reset action must be wired.')
requirePage(/updateEmployeeStatus/,
  'enable and disable actions must call backend status update.')
requirePage(/updateEmployeeDisplayName/,
  'rename action must call backend display-name update.')

assert.doesNotMatch(page, /label="系统用户ID"/,
  'formal workers must not be added by hand-entering a system user id.')
assert.doesNotMatch(api, /\/system\/user\/page/,
  'team leader employee management must not call the full system user list.')

requireApi('getProductionPersonnelList', '/employee-profile/list', 'scoped production personnel list API')
requireApi('searchTeamFormalEmployeeCandidates', '/employee-profile/formal-candidates',
  'formal employee backend-scoped search API')
requireApi('createTemporaryTeamEmployee', '/employee-profile/temporary/create',
  'temporary employee create API')
requireApi('linkFormalTeamEmployee', '/employee-profile/formal/link',
  'formal employee link API')
requireApi('updateTeamEmployeeDisplayName', '/employee-profile/display-name/update',
  'employee display-name update API')
requireApi('updateTeamEmployeeStatus', '/employee-profile/status/update',
  'employee enable/disable API')
requireApi('resetTemporaryTeamEmployeeSignaturePassword', '/employee-profile/temp-signature-password/reset',
  'temporary signature password reset API')
assert.match(route, /title:\s*'表单日志'/,
  'traceable operation records must remain available through the existing form log route.')

assert.match(frontlineContext, /ProFeedbackApi\.getFrontlineRuntimeConfig/,
  'production filling employee cards must be sourced from runtime config.')
assert.match(frontlineContext, /userId:\s*employee\.systemUserId\s*\|\|\s*employee\.employeeProfileId/,
  'temporary workers must be selectable without a system user account.')
assert.match(frontlineContext, /nickname:\s*employee\.displayName\s*\|\|\s*employee\.employeeName/,
  'employee cards must prefer production personnel displayName snapshots.')

assert.equal(
  packageJson.scripts['e2e:production-personnel-management:real:check'],
  'node --check tests/e2e/production-personnel-management-real.e2e.js',
  'package scripts must expose a syntax check for the production personnel real E2E.'
)
assert.equal(
  packageJson.scripts['e2e:production-personnel-management:real'],
  'node tests/e2e/production-personnel-management-real.e2e.js',
  'package scripts must expose the production personnel real E2E.'
)
assert.ok(fs.existsSync(realE2ePath), 'production personnel real E2E script must exist.')

const realE2e = fs.readFileSync(realE2ePath, 'utf8')

assert.match(realE2e, /PPM_FRONTEND_URL/, 'real E2E must accept explicit frontend URL.')
assert.match(realE2e, /PPM_BACKEND_URL/, 'real E2E must accept explicit backend URL.')
assert.match(realE2e, /PPM_TENANT/, 'real E2E must require an explicit test tenant.')
assert.match(realE2e, /PPM_USERNAME/, 'real E2E must require an explicit test username.')
assert.match(realE2e, /PPM_PASSWORD/, 'real E2E must require password injection through env.')
assert.match(realE2e, /isAllowedIntMainRuntimePair/, 'real E2E must validate paired int_main slot URLs.')
assert.match(realE2e, /createTemporaryEmployeeViaPage/, 'real E2E must create a temporary worker through the page.')
assert.match(realE2e, /assertDuplicateTemporaryWorkerRejected/, 'real E2E must prove duplicate display names are rejected.')
assert.match(realE2e, /resetTemporarySignaturePasswordViaPage/,
  'real E2E must reset temporary worker signature password through the page.')
assert.match(realE2e, /disableTemporaryWorkerViaPage/, 'real E2E must disable the worker through the page.')
assert.match(realE2e, /assertNoStandaloneAuditList/,
  'real E2E must verify the personnel page no longer renders a standalone audit list.')
assert.match(realE2e, /assertRuntimeConfigCandidateScope/,
  'real E2E must verify production filling candidates through runtime config.')
assert.doesNotMatch(realE2e, /admin123|111111|DEFAULT_PASSWORD/,
  'real E2E must not hardcode local passwords or fallback credentials.')

console.log('PASS: production personnel management static contract is wired')
