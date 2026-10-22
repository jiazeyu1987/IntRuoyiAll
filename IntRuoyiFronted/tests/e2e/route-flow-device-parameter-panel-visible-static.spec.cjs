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

// 工艺身份只能来自当前版本的显式绑定，不能按设备或参数内容推断。
const vm = require('node:vm')
const summaryMatch = designer.match(
  /const buildRouteProcessDeviceParameterSummaryValue = (\(\) => \{[\s\S]*?\n\})/
)
assert.ok(summaryMatch, '设备参数字段必须提供工艺绑定摘要。')
const summary = (config) => vm.runInNewContext(`(${summaryMatch[1]})()`, {
  routeProcessDeviceParameterConfig: { value: config }
})
assert.equal(summary(undefined), '点击查看工艺')
assert.equal(summary({ devices: [] }), '未配置设备参数')
assert.equal(summary({ devices: [{ deviceId: 980020, parameters: [] }] }), '未配置设备参数')
assert.equal(summary({ devices: [{ deviceId: 980020, parameters: [{ parameterCode: 'P1' }] }] }), '已配置参数（未关联工艺库）')
assert.equal(summary({ craftId: 8, craftBindings: [{ craftId: 8, craftCode: 'GY-0008' }], devices: [] }), 'GY-0008')
assert.equal(summary({ craftId: 8, craftBindings: [{ craftId: 8, craftCode: '' }] }), '已绑定工艺，但工艺编号缺失')
assert.equal(summary({ craftId: 8, craftBindings: [{ craftId: 8, craftCode: 'GY-0008' }, { craftId: 9, craftCode: 'GY-0009' }] }), 'GY-0008、GY-0009')
assert.equal(summary({ craftId: 8 }), '已绑定工艺，但关联明细缺失')
assert.match(designer, /data-flow-panel="route-process-craft-unbound"/, '未绑定工艺必须明示绑定入口所在版本。')
assert.match(designer, /<el-collapse[\s\S]*?data-flow-panel="published-route-process-devices"[\s\S]*?<el-collapse-item/, '原设备参数详情必须保留为可展开内容。')

console.log('PASS: route flow device parameter panel visibility static contract')
