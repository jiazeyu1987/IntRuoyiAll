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

const linkMethodStart = pqcPersonnelService.indexOf('public Long linkFormalInspector')
const linkMethodEnd = pqcPersonnelService.indexOf('@Override', linkMethodStart + 1)
const linkMethod = pqcPersonnelService.slice(linkMethodStart, linkMethodEnd)

assert.match(
  controller,
  /@GetMapping\("\/pqc-personnel\/formal-candidates"\)[\s\S]*runtimeConfigService\.searchFormalUserCandidates\(SecurityFrameworkUtils\.getLoginUserId\(\),\s*keyword\)/,
  'PQC personnel candidate endpoint must reuse the formal user candidate search.'
)

assert.match(
  runtimeConfigService,
  /searchFormalUserCandidates[\s\S]*adminUserApi\.getUserListByNickname\(normalizedKeyword\)/,
  'Formal user candidate search must query all system users by nickname.'
)

assert.doesNotMatch(
  linkMethod,
  /getUserListBySubordinate|PRO_PROCESS_POOL_TEAM_SCOPE_DENIED/,
  'PQC formal inspector link must not restrict all-company candidates to current leader subordinates.'
)

assert.match(
  linkMethod,
  /adminUserApi\.validateUser\(reqBO\.getSystemUserId\(\)\)[\s\S]*scopeMapper\.selectPqcEmployeeScope\(reqBO\.getLeaderUserId\(\),\s*reqBO\.getSystemUserId\(\)\)/,
  'PQC formal inspector link must validate the selected system user and still reject duplicate PQC scopes before insert.'
)

assert.match(
  teamLeaderApi,
  /searchPqcFormalEmployeeCandidates[\s\S]*\/mes\/pro\/process-pool\/team-leader\/pqc-personnel\/formal-candidates/,
  'Frontend PQC add dialog must call the PQC formal candidate endpoint.'
)

assert.match(
  page,
  /data-pqc-personnel-add-dialog[\s\S]*remote-method="searchPqcFormalEmployeeCandidatesForSelect"/,
  'PQC add dialog must keep using remote search for company-wide candidates.'
)

console.log('PASS: PQC personnel company-wide candidate contract')
