const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '../..')
const servicePath = path.join(
  moduleRoot,
  'main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteServiceImpl.java'
)
const source = fs.readFileSync(servicePath, 'utf8')

const deleteStart = source.indexOf('public void deleteRoute(Long id)')
const nextMethod = source.indexOf('private void cancelOpenCandidateBeforeDelete', deleteStart)
assert.ok(deleteStart >= 0 && nextMethod > deleteStart, 'deleteRoute must exist')

const deleteMethod = source.slice(deleteStart, nextMethod)
const cancelCall = deleteMethod.indexOf('cancelOpenCandidateBeforeDelete(id);')
const routeDelete = deleteMethod.indexOf('routeMapper.deleteById(id);')
assert.ok(cancelCall >= 0, 'route deletion must close the open candidate')
assert.ok(routeDelete > cancelCall, 'candidate lifecycle must close before the route is deleted')

const helperEnd = source.indexOf('@Override', nextMethod)
const helper = source.slice(nextMethod, helperEnd)
assert.match(helper, /selectOpenCandidateByRouteId\(routeId\)/)
assert.match(helper, /STATUS_DRAFT\.equals\(candidate\.getLifecycleStatus\(\)\)/)
assert.match(helper, /STATUS_READY_TO_PUBLISH\.equals\(candidate\.getLifecycleStatus\(\)\)/)
assert.match(helper, /PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE/)
assert.match(helper, /setLifecycleStatus\(ROUTE_VERSION_STATUS_CANCELLED\)/)
assert.match(helper, /platformAdapter\.recordCancelled\(candidate, SecurityFrameworkUtils\.getLoginUserId\(\)\)/)

console.log('mes-route-delete-candidate-lifecycle-static contract PASS')
