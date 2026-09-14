const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const api = fs.readFileSync(path.join(root, 'src/api/mes/pro/route/flowconfig.ts'), 'utf8')
const page = fs.readFileSync(path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'), 'utf8')

assert.match(api, /getRouteProcessDeviceParameterConfig[\s\S]*routeVersionId[\s\S]*routeProcessId/)
assert.match(api, /deleteRouteProcessDeviceParameterRule/)
assert.match(page, /routeProcessDeviceParameterRequestId/)
assert.match(page, /expectedRouteSnapshotSha256/)
assert.match(page, /deleteRouteProcessDeviceParameterRule/)
assert.match(page, /data-flow-action="delete-route-process-device-parameter-rule"/)

console.log('PASS: route device parameter editor uses candidate version and stale-request guard')
