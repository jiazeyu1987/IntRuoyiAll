const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const readFrontend = (relativePath) => fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')
const readWorkspace = (relativePath) => fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8')

const frontlineContext = readFrontend('src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts')
const teamLeaderPage = readFrontend('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const teamLeaderApi = readFrontend('src/api/mes/pro/processpool/teamLeader.ts')
const runtimeConfigService = readWorkspace(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRuntimeConfigServiceImpl.java'
)

assert.match(
  teamLeaderApi,
  /getProductionPersonnelList\s*=\s*async[\s\S]*\/mes\/pro\/process-pool\/team-leader\/employee-profile\/list/,
  'Production leader personnel management must expose the formal employee-profile list API.'
)
assert.match(
  teamLeaderPage,
  /productionPersonnelRows\.value\s*=\s*await\s+getProductionPersonnelList\(\)/,
  'Production leader personnel management list must load the complete linked personnel list.'
)
assert.match(
  frontlineContext,
  /ProFeedbackApi\.getFrontlineRuntimeConfig/,
  'Frontline production employee popup must load the formal runtime config instead of the legacy workstation candidate API.'
)
assert.match(
  frontlineContext,
  /state\.employeeOptions\s*=\s*runtimeConfig\.employees\.map\(toEmployeeCandidate\)/,
  'Frontline production employee popup must render employees returned by runtime config.'
)
assert.doesNotMatch(
  frontlineContext,
  /getFrontlineEmployeeCandidates/,
  'Frontline production employee popup must not use the legacy device employee-candidates endpoint.'
)
assert.match(
  runtimeConfigService,
  /toEmployeeOptions\(loginUserId\)/,
  'Runtime config must derive employee popup options from the current login production leader personnel list.'
)
assert.match(
  runtimeConfigService,
  /employeeProfileMapper\.selectList\(\s*new LambdaQueryWrapperX<MesProcessPoolTeamEmployeeProfileDO>\(\)[\s\S]*\.eq\(MesProcessPoolTeamEmployeeProfileDO::getLeaderUserId,\s*leaderUserId\)[\s\S]*\.eq\(MesProcessPoolTeamEmployeeProfileDO::getEnabled,\s*Boolean\.TRUE\)/,
  'Runtime config employees must come from the same enabled production personnel profiles used by personnel management.'
)
assert.doesNotMatch(
  runtimeConfigService,
  /private\s+List<MesFrontlineTeamEmployeeOption>\s+toEmployeeOptions\(\s*List<MesProcessPoolTeamEmployeeBindingDO>/,
  'Runtime config employee popup options must not be limited to process employee bindings.'
)
assert.doesNotMatch(
  runtimeConfigService,
  /employeeProfileMapper\.selectBatchIds/,
  'Runtime config must not rebuild employee popup options from process-bound profile ids.'
)

console.log('PASS: frontline production employee popup uses production leader personnel list scope')
