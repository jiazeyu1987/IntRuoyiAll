const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const parameterValidator = read(
  'main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesFrontlineDeviceParameterValidatorImpl.java'
)
const teamRuntimeConfig = read(
  'main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderRuntimeConfigServiceImpl.java'
)

const routeProcessMatchStart = parameterValidator.indexOf('private static boolean routeProcessMatches')
assert.ok(routeProcessMatchStart >= 0, 'device parameter validator must keep a routeProcessMatches helper.')
const routeProcessMatchBlock = parameterValidator.slice(routeProcessMatchStart)
assert.match(
  routeProcessMatchBlock,
  /return configuredRouteProcessId != null && Objects\.equals\(configuredRouteProcessId, routeProcessId\);/,
  'Submit-time device parameter validation must use the same exact routeProcessId rule as runtime config.'
)
assert.doesNotMatch(
  routeProcessMatchBlock,
  /configuredRouteProcessId == null \|\| Objects\.equals/,
  'Submit-time validation must not require hidden routeProcessId-null parameters that the page did not display.'
)

assert.match(
  teamRuntimeConfig,
  /private final MesProcessPoolTeamLeaderScopeMapper scopeMapper;/,
  'Production employee maintenance must have access to team leader scope persistence.'
)

const createEmployeeStart = teamRuntimeConfig.indexOf('public Long createEmployee(MesTeamEmployeeProfileSaveReqBO reqBO)')
assert.ok(createEmployeeStart >= 0, 'createEmployee must exist.')
const createEmployeeEnd = teamRuntimeConfig.indexOf('@Override', createEmployeeStart + 1)
assert.ok(createEmployeeEnd > createEmployeeStart, 'createEmployee block must close before the next override.')
const createEmployeeBlock = teamRuntimeConfig.slice(createEmployeeStart, createEmployeeEnd)

assert.match(
  createEmployeeBlock,
  /syncProductionEmployeeScope\(profile\);/,
  'Creating a production employee profile must synchronise the team leader employee scope used by submission pages.'
)
assert.match(
  teamRuntimeConfig,
  /private Long resolveProductionEmployeeUserId\(MesProcessPoolTeamEmployeeProfileDO profile\)[\s\S]*profile\.getSystemUserId\(\) != null \? profile\.getSystemUserId\(\) : profile\.getId\(\)/,
  'Synchronized scope must use the formal employee system user id and only use the profile id for temporary employees.'
)
assert.match(
  teamRuntimeConfig,
  /MesProcessPoolTeamLeaderScopeDO\.SCOPE_TYPE_EMPLOYEE[\s\S]*\.employeeUserId\(employeeUserId\)/,
  'Synchronized scope must write the resolved production employee identity used by submission pages.'
)

console.log('PASS: frontline production extra restrictions removed backend static contract')
