const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const designer = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)

const defaultKeysMatch = designer.match(/const DEFAULT_PROCESS_DETAIL_FIELD_KEYS:[\s\S]*?\.filter\(/)
assert.ok(defaultKeysMatch, 'Route flow designer must define default process detail fields.')
assert.match(
  defaultKeysMatch[0],
  /'deviceParameters'/,
  '设备参数必须进入工序详情默认字段，首次进入流转关系图就能看到入口。'
)

const requiredKeysMatch = designer.match(/const REQUIRED_PROCESS_DETAIL_FIELD_KEYS:[\s\S]*?\.filter\(/)
assert.ok(requiredKeysMatch, 'Route flow designer must define required process detail fields.')
assert.match(
  requiredKeysMatch[0],
  /'deviceParameters'/,
  '设备参数必须进入工序详情必显字段，避免历史个人字段偏好把入口隐藏。'
)

assert.match(
  designer,
  /if \(fieldKey === 'deviceParameters'\) \{[\s\S]*loadSelectedRouteProcessDeviceParameterConfig\(\)/,
  '点击设备参数字段时必须加载路线侧设备参数配置。'
)

console.log('PASS: route flow device parameter panel visibility static contract')
