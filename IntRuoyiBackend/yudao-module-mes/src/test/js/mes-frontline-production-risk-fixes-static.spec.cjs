const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

function sliceFrom(source, startNeedle, label) {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `${label} missing start marker`)
  return source.slice(start)
}

function sliceBetween(source, startNeedle, endNeedle, label) {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `${label} missing start marker`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.ok(end > start, `${label} missing end marker`)
  return source.slice(start, end)
}

const signatureService = read(
  'main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionSignatureService.java'
)
const parameterValidator = read(
  'main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesFrontlineDeviceParameterValidatorImpl.java'
)
const teamRuntimeConfig = read(
  'main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderRuntimeConfigServiceImpl.java'
)
const scopeMapper = read(
  'main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolTeamLeaderScopeMapper.java'
)

const productionSignatureBlock = sliceBetween(
  signatureService,
  'public Long recordProductionSubmitSignature(Long actorId, String password, String comment)',
  '@Transactional(rollbackFor = Exception.class)\n    public Long recordSubmitSignature',
  'production submit signature'
)
assert.match(
  productionSignatureBlock,
  /AdminUserDO user = adminUserService\.getUser\(actorId\);[\s\S]*if \(user == null\) \{[\s\S]*recordProductionSubmitSignatureForEmployeeProfile\(actorId, password, comment\)/,
  'Production submit must use the selected actor and route non-system employee profiles to profile password validation.'
)
assert.match(
  productionSignatureBlock,
  /authorizationService\.isElectronicSignatureEnabled\(actorId\)/,
  'System-user production submit must still enforce unified electronic-signature authorization.'
)
assert.doesNotMatch(
  productionSignatureBlock,
  /SecurityFrameworkUtils\.getLoginUserId\(\)/,
  'Production submit must not sign as the current team-leader login user.'
)

const profileSignatureBlock = sliceBetween(
  signatureService,
  'private Long recordProductionSubmitSignatureForEmployeeProfile(Long actorId, String password, String comment)',
  'private String resolveEmployeeProfileDisplayName',
  'employee profile production signature'
)
assert.match(
  profileSignatureBlock,
  /employeeProfileMapper\.selectById\(actorId\)/,
  'Temporary employee signature must load the selected personnel profile by actorId.'
)
assert.match(
  profileSignatureBlock,
  /!Boolean\.TRUE\.equals\(profile\.getEnabled\(\)\)/,
  'Temporary employee signature must reject disabled personnel profiles.'
)
assert.match(
  profileSignatureBlock,
  /profile\.getSystemUserId\(\) != null/,
  'The personnel-profile password path must be limited to employees without a system account.'
)
assert.match(
  profileSignatureBlock,
  /passwordEncoder\.matches\(password, profile\.getSignaturePasswordHash\(\)\)/,
  'Temporary employee signature must verify the selected employee profile signature hash.'
)
assert.match(
  profileSignatureBlock,
  /\.actorId\(actorId\)[\s\S]*\.actorUsernameSnapshot\(employeeCode\)[\s\S]*\.actorNicknameSnapshot\(employeeName\)/,
  'Temporary employee signature snapshot must record the selected employee identity, not the team leader.'
)
assert.match(
  profileSignatureBlock,
  /AUTHORIZATION_BASIS_EMPLOYEE_PROFILE/,
  'Temporary employee signature must use an explicit personnel-profile authorization basis.'
)
assert.doesNotMatch(
  profileSignatureBlock,
  /adminUserService\.isPasswordMatch/,
  'Temporary employee signature must not validate against the current login/system-user password store.'
)

const routeProcessMatchBlock = sliceFrom(
  parameterValidator,
  'private static boolean routeProcessMatches',
  'route process parameter match'
)
assert.match(
  routeProcessMatchBlock,
  /return configuredRouteProcessId != null && Objects\.equals\(configuredRouteProcessId, routeProcessId\);/,
  'Submit-time parameter validation must match the runtime routeProcessId exactly.'
)
assert.doesNotMatch(
  routeProcessMatchBlock,
  /configuredRouteProcessId == null \|\| Objects\.equals/,
  'Submit-time parameter validation must not require legacy/global rules hidden from runtime cards.'
)

assert.match(
  scopeMapper,
  /selectProductionEmployeeScope\(Long leaderUserId, Long employeeUserId\)[\s\S]*LEADER_TYPE_PRODUCTION[\s\S]*SCOPE_TYPE_EMPLOYEE[\s\S]*getEmployeeUserId, employeeUserId/,
  'Team leader scope mapper must expose production employee scope lookup.'
)

for (const methodName of [
  'createTemporaryEmployee',
  'linkFormalEmployee',
  'createEmployee'
]) {
  const block = sliceBetween(
    teamRuntimeConfig,
    `public Long ${methodName}`,
    methodName === 'createEmployee'
      ? 'private void syncProductionEmployeeScope'
      : '@Override',
    methodName
  )
  assert.match(
    block,
    /syncProductionEmployeeScope\(profile\);/,
    `${methodName} must synchronize the production leader employee scope used by report visibility.`
  )
}

assert.match(
  teamRuntimeConfig,
  /private Long resolveProductionEmployeeUserId\(MesProcessPoolTeamEmployeeProfileDO profile\)[\s\S]*profile\.getSystemUserId\(\) != null \? profile\.getSystemUserId\(\) : profile\.getId\(\)/,
  'Scope synchronization must use systemUserId for formal employees and profile id for temporary employees.'
)
assert.match(
  teamRuntimeConfig,
  /private void syncProductionEmployeeScopeEnabled[\s\S]*scopeMapper\.updateById[\s\S]*\.enabled\(enabled\)/,
  'Disabling or enabling a personnel profile must keep the production employee scope status aligned.'
)

console.log('PASS: frontline production risk fixes backend static contract')
