const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const read = (relativePath) => fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8')

const controller = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'
)
const pqcPersonnelService = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesPqcLeaderPersonnelServiceImpl.java'
)
const runtimeConfigService = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderRuntimeConfigServiceImpl.java'
)
const teamLeaderApi = read('IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts')
const page = read('IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const candidateRespVO = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamFormalUserCandidateRespVO.java'
)
const candidateBO = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamFormalUserCandidateBO.java'
)
const scopeMapper = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolTeamLeaderScopeMapper.java'
)

const linkMethodStart = pqcPersonnelService.indexOf('public Long linkFormalInspector')
const linkMethodEnd = pqcPersonnelService.indexOf('@Override', linkMethodStart + 1)
const linkMethod = pqcPersonnelService.slice(linkMethodStart, linkMethodEnd)
const candidateMethodStart = pqcPersonnelService.indexOf('public List<MesTeamFormalUserCandidateBO> searchFormalInspectorCandidates')
const candidateMethodEnd = pqcPersonnelService.indexOf('@Override', candidateMethodStart + 1)
const candidateMethod = pqcPersonnelService.slice(candidateMethodStart, candidateMethodEnd)
const candidateEndpointStart = controller.indexOf(
  'public CommonResult<List<MesTeamFormalUserCandidateRespVO>> searchPqcFormalEmployeeCandidates'
)
const candidateEndpointEnd = controller.indexOf('@PostMapping("/pqc-personnel/formal/link")', candidateEndpointStart)
const candidateEndpoint = controller.slice(candidateEndpointStart, candidateEndpointEnd)

assert.match(
  controller,
  /@GetMapping\("\/pqc-personnel\/formal-candidates"\)[\s\S]*public CommonResult<List<MesTeamFormalUserCandidateRespVO>> searchPqcFormalEmployeeCandidates/,
  'PQC personnel candidate endpoint route must remain available.'
)

assert.match(
  candidateEndpoint,
  /@RequestParam\(value = "keyword", required = false\)\s+String keyword/,
  'PQC personnel candidate endpoint must accept a missing keyword so empty dropdown loading does not fail binding.'
)

assert.match(
  candidateEndpoint,
  /pqcPersonnelService\.searchFormalInspectorCandidates\(SecurityFrameworkUtils\.getLoginUserId\(\),\s*keyword\)/,
  'PQC personnel candidate endpoint method body must use the PQC permission candidate service.'
)

assert.doesNotMatch(
  candidateEndpoint,
  /runtimeConfigService\.searchFormalUserCandidates/,
  'PQC personnel candidate endpoint must not reuse the production all-company formal user search directly.'
)

assert.match(
  pqcPersonnelService,
  /PQC_PERMISSION_ROLE_CODE\s*=\s*"pqc_permission"/,
  'PQC personnel candidate filtering must use the formal PQC permission role code.'
)

assert.match(
  candidateMethod,
  /Long pqcRoleId = requirePqcPermissionRoleId\(\)[\s\S]*permissionApi\.getUserRoleIdListByRoleIds\(Set\.of\(pqcRoleId\)\)[\s\S]*adminUserApi\.getUserList\(userIds\)/,
  'PQC personnel candidate search must start from the PQC role assignment pool and then load those users.'
)

assert.match(
  pqcPersonnelService,
  /private Long requirePqcPermissionRoleId\(\)[\s\S]*roleApi\.getRoleByCode\(PQC_PERMISSION_ROLE_CODE\)/,
  'PQC personnel candidate search must resolve the role id from the formal PQC role code.'
)

assert.doesNotMatch(
  candidateMethod,
  /getUserListByNickname/,
  'PQC personnel candidate search must not scan all company users by nickname before filtering.'
)

assert.doesNotMatch(
  pqcPersonnelService,
  /CANDIDATE_LIMIT|\.limit\(CANDIDATE_LIMIT\)/,
  'PQC personnel candidate search must not truncate the 30-user PQC permission pool.'
)

assert.match(
  pqcPersonnelService,
  /private boolean hasPqcPersonnelPermission\(Long userId\)[\s\S]*permissionApi\.hasAnyRoles\(userId,\s*PQC_PERMISSION_ROLE_CODE\)/,
  'PQC personnel permission checks must go through the system permission API role resolver.'
)

assert.doesNotMatch(
  linkMethod,
  /getUserListBySubordinate|PRO_PROCESS_POOL_TEAM_SCOPE_DENIED/,
  'PQC formal inspector link must not restrict all-company candidates to current leader subordinates.'
)

assert.match(
  linkMethod,
  /adminUserApi\.validateUser\(reqBO\.getSystemUserId\(\)\)[\s\S]*assertPqcInspectorPermission\(reqBO\.getSystemUserId\(\)\)[\s\S]*scopeMapper\.selectPqcEmployeeScope\(reqBO\.getLeaderUserId\(\),\s*reqBO\.getSystemUserId\(\)\)/,
  'PQC formal inspector link must validate the selected system user, enforce PQC permission, and still reject duplicate PQC scopes before insert.'
)

assert.match(
  linkMethod,
  /assertInspectorNotOccupiedByOtherPqcLeader\(reqBO\.getLeaderUserId\(\),\s*reqBO\.getSystemUserId\(\)\)/,
  'PQC formal inspector link must reject users already linked by another active PQC leader before insert.'
)

assert.match(
  scopeMapper,
  /selectActivePqcEmployeeScopesByEmployeeUserId\(Long employeeUserId\)[\s\S]*LEADER_TYPE_PQC[\s\S]*SCOPE_TYPE_EMPLOYEE[\s\S]*Boolean\.TRUE/,
  'PQC scope mapper must expose an enabled employee-scope lookup by employee user id for cross-leader occupancy checks.'
)

assert.match(
  candidateMethod,
  /selectActiveScopesByLeaderType\(MesProcessPoolTeamLeaderScopeDO\.LEADER_TYPE_PQC\)[\s\S]*SCOPE_TYPE_EMPLOYEE[\s\S]*occupiedByOtherPqcLeader/,
  'PQC candidate search must mark candidates occupied by another active PQC leader.'
)

for (const [sourceName, source] of [
  ['BO', candidateBO],
  ['Response VO', candidateRespVO],
  ['Frontend API type', teamLeaderApi]
]) {
  assert.match(source, /disabled/, `PQC candidate ${sourceName} must expose disabled state.`)
  assert.match(
    source,
    /disabledReason/,
    `PQC candidate ${sourceName} must expose a disabled reason for occupied candidates.`
  )
  assert.match(
    source,
    /occupiedByOtherPqcLeader/,
    `PQC candidate ${sourceName} must expose whether another PQC leader already selected the user.`
  )
}

assert.match(
  controller,
  /toFormalUserCandidateRespVO[\s\S]*setDisabled\(candidate\.getDisabled\(\)\)[\s\S]*setDisabledReason\(candidate\.getDisabledReason\(\)\)[\s\S]*setOccupiedByOtherPqcLeader\(candidate\.getOccupiedByOtherPqcLeader\(\)\)/,
  'Controller must pass PQC candidate disabled/occupied fields through to the frontend.'
)

assert.match(
  teamLeaderApi,
  /searchPqcFormalEmployeeCandidates[\s\S]*\/mes\/pro\/process-pool\/team-leader\/pqc-personnel\/formal-candidates/,
  'Frontend PQC add dialog must call the PQC formal candidate endpoint.'
)

assert.match(
  page,
  /data-pqc-personnel-add-dialog[\s\S]*remote-method="searchPqcFormalEmployeeCandidatesForSelect"/,
  'PQC add dialog must keep using remote search for backend-filtered PQC permission candidates.'
)

const pqcDialogStart = page.indexOf('data-pqc-personnel-add-dialog')
const pqcDialogEnd = page.indexOf('<template #footer>', pqcDialogStart)
const pqcDialog = page.slice(pqcDialogStart, pqcDialogEnd)
const pqcSearchStart = page.indexOf('const searchPqcFormalEmployeeCandidatesForSelect')
const pqcSearchEnd = page.indexOf('const submitLinkPqcFormalEmployee', pqcSearchStart)
const pqcSearchMethod = page.slice(pqcSearchStart, pqcSearchEnd)

assert.match(
  pqcDialog,
  /automatic-dropdown[\s\S]*remote-show-suffix[\s\S]*@focus="loadPqcFormalEmployeeCandidatesForSelect"[\s\S]*@visible-change="handlePqcCandidateDropdownVisibleChange"/,
  'PQC add select must open with an empty keyword and automatically load candidates on click/focus.'
)

assert.doesNotMatch(
  pqcSearchMethod,
  /if\s*\(!searchText\)\s*\{\s*pqcCandidateOptions\.value\s*=\s*\[\]/,
  'PQC empty candidate search must not clear options and return; it must call the backend with a blank keyword.'
)

assert.match(
  pqcSearchMethod,
  /searchPqcFormalEmployeeCandidates\(searchText\)/,
  'PQC candidate search method must call the backend even when searchText is blank.'
)

assert.match(
  pqcDialog,
  /:disabled="candidate\.disabled"[\s\S]*team-leader-workbench__pqc-candidate-option--occupied[\s\S]*已被其他PQC组长选择/,
  'PQC occupied candidates must be visible, red-styled, and disabled in the add-person dropdown.'
)

assert.match(
  page,
  /team-leader-workbench__pqc-candidate-option--occupied[\s\S]*#f56c6c/,
  'PQC occupied candidate style must use the existing red danger color.'
)

console.log('PASS: PQC personnel permission candidate contract')
