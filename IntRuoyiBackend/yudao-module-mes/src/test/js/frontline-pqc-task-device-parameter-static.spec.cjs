const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../../..')
const source = fs.readFileSync(
  path.join(root, 'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java'),
  'utf8'
)

assert.doesNotMatch(
  source,
  /identities\.size\(\) != 1/,
  'PQC device parameters must not require one production process identity for an entire QA process.'
)
assert.match(
  source,
  /ProductionProcessIdentity[\s\S]*routeDevicesByProcessIdentity[\s\S]*task\.getRouteProcessId\(\)[\s\S]*task\.getProcessId\(\)/,
  'PQC route devices and parameters must be selected by each task routeProcessId and processId.'
)
assert.match(
  source,
  /toPqcTaskOptionRespVO[\s\S]*routeDevicesByProcessIdentity[\s\S]*PqcRouteDeviceContext routeDeviceContext/,
  'Each PQC task option must receive the route device context for its own frozen production process.'
)

console.log('PASS: frontline PQC task-scoped device parameter contract')
