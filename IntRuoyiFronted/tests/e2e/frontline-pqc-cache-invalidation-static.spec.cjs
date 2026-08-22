const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const source = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts'),
  'utf8'
)

assert.match(
  source,
  /export const invalidateFrontlinePqcProcessCacheForActiveOrder = [\s\S]{0,420}activeOrderKey[\s\S]{0,260}pqcProcessOptionsCache\.delete\(cacheKey\)/,
  'PQC submit flow must invalidate the selected active-order process cache.'
)
assert.match(
  source,
  /pqcProcessOptionsRequests\.delete\(requestKey\)/,
  'PQC submit flow must invalidate in-flight process requests for the selected active order.'
)
assert.match(
  source,
  /if \(\(state\.pqcProcessCacheInvalidationVersionByOrder\[activeOrderKey\] \|\| 0\) === cacheVersion\) \{[\s\S]{0,180}pqcProcessOptionsCache\.set\(cacheKey, processes\)/,
  'An invalidated stale process request must not repopulate the process cache.'
)
assert.match(
  source,
  /buildFrontlinePqcActiveOrderProcessCacheKey/,
  'PQC process cache must be scoped by actual employee.'
)
assert.ok(
  source.includes('actualEmployeeId?: number'),
  'PQC process cache key must include actualEmployeeId.'
)
assert.match(
  source,
  /ProFeedbackApi\.getPqcProcesses\(activeOrder\.activeOrderId, actualEmployeeId\)/,
  'PQC process loading must pass actualEmployeeId to the backend.'
)

console.log('PASS: frontline PQC active-order process cache invalidation contract')
