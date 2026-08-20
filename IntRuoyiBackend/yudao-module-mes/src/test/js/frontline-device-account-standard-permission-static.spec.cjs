const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const servicePath = path.resolve(
  __dirname,
  '../../main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineDeviceAccountContextServiceImpl.java'
)
const source = fs.readFileSync(servicePath, 'utf8').replace(/\r\n/g, '\n')

const start = source.indexOf('private Set<Long> resolveRouteStartProductionLeaderAuthorizedRouteIds')
assert.ok(start >= 0, 'Route-start authorization method must exist.')
const end = source.indexOf('private List<RouteStartProductionLeaderSnapshot> parseRouteStartProductionLeaderSnapshots', start)
assert.ok(end > start, 'Route-start authorization method boundary must be readable.')
const method = source.slice(start, end)

const hasAnyPermissionsIndex = method.indexOf('permissionApi.hasAnyPermissions(')
const roleIdIndex = method.indexOf('permissionApi.getUserRoleIdListByUserId(')
const pressurePumpRouteCheckIndex = method.indexOf('isPressurePumpRoute(')

assert.ok(
  hasAnyPermissionsIndex >= 0,
  'Route-start device-account authorization must use standard PermissionApi.hasAnyPermissions(loginUserId, permission).'
)
assert.ok(
  method.includes('PRESSURE_PUMP_ALL_PROCESS_PERMISSION'),
  'Route-start device-account authorization must use the pressure-pump all-process permission constant.'
)
assert.ok(
  roleIdIndex < 0 || hasAnyPermissionsIndex < roleIdIndex,
  'Standard permission authorization must be checked before falling back to explicit route-start role IDs.'
)
assert.ok(
  pressurePumpRouteCheckIndex > hasAnyPermissionsIndex,
  'Pressure-pump all-process permission must be limited to pressure-pump route-start routes.'
)

console.log('PASS: frontline device-account route-start authorization uses standard permission parsing within pressure-pump scope')
