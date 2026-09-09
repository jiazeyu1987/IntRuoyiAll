const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const flowApi = read('src/api/mes/pro/route/flowconfig.ts')
const designer = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const columns = read('src/views/mes/pro/route/routeProcessSettingsColumns.ts')

assert.match(
  flowApi,
  /getRouteProcessDeviceParameterConfig/,
  'Route flow API must expose a route-owned device parameter config query.'
)
assert.match(
  flowApi,
  /saveRouteProcessDeviceParameterRule/,
  'Route flow API must expose a route-owned device parameter rule save endpoint.'
)
assert.match(
  columns,
  /'deviceParameters'/,
  'Route process detail field keys must include deviceParameters.'
)
assert.match(
  designer,
  /key:\s*'deviceParameters'[\s\S]*label:[\s\S]*设备参数/,
  'Route flow selected process detail options must include 设备参数.'
)
assert.match(
  designer,
  /data-flow-panel="route-process-device-parameter-config"/,
  'Route flow detail panel must render the route process device parameter config panel.'
)
assert.match(
  designer,
  /saveRouteProcessDeviceParameterRule/,
  'Route flow device parameter editor must save through the route-owned API.'
)
assert.doesNotMatch(
  designer,
  /saveTeamProcessConfigDeviceParameterRule/,
  'Route flow device parameter editor must not save through the production-leader API.'
)

console.log('PASS: route flow device parameter config static contract')
