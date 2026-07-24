const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)

const assertIncludes = (expected, message) => {
  assert.ok(component.includes(expected), `${message}: expected ${JSON.stringify(expected)}`)
}

const assertMatch = (pattern, message) => {
  assert.match(component, pattern, message)
}

assertIncludes(
  "'is-binding-bound': getRouteNodeBindingStatus(data.routeNode) === 'bound'",
  '节点模板必须在当前配置项已绑定时添加绿色边框状态类'
)
assertIncludes(
  "'is-binding-missing': getRouteNodeBindingStatus(data.routeNode) === 'missing'",
  '节点模板必须在当前配置项未绑定时添加红色边框状态类'
)

assertMatch(
  /const ROUTE_NODE_BINDING_STATUS_FIELD_KEYS = new Set<RouteProcessSettingColumnKey>\(\[[\s\S]*FORM_SLOT_AGGREGATE_FIELD_KEY[\s\S]*'batchRecordFormNames'[\s\S]*'keyFlag'[\s\S]*'checkFlag'[\s\S]*'workstation'[\s\S]*\]\)/,
  '只允许用户要求的配置项触发关系图节点红绿边框'
)
assertMatch(
  /const getRouteNodeBindingStatus = \(node: RouteFlowNodeVO\): RouteNodeBindingStatus => \{[\s\S]*selectedProcessDetailFieldKey\.value[\s\S]*isRouteNodeFormSlotConfigured\(node\)[\s\S]*isRouteNodeRecordBindingConfigured\(node, 'MAIN'\)[\s\S]*isRouteNodeWorkstationBound\(node\)[\s\S]*Boolean\(node\.keyFlag\)[\s\S]*Boolean\(node\.checkFlag\)/,
  '节点红绿状态必须按当前选择的配置项分别判断表单槽位、批记录表单、工作站、关键工序和质检确认'
)
assertMatch(
  /const getRouteNodeBatchRecordBindings = \(node: RouteFlowNodeVO\): RouteFlowRecordBinding\[\] => \{[\s\S]*selectedProcessAttributeDrafts\[node\.routeProcessId\]\?\.recordBindings[\s\S]*selectedProcessRouteConfigCache\.value\?\.batchConfigs[\s\S]*buildRecordBindings\(batchConfig\)/,
  '批记录和表单槽位节点状态必须复用当前草稿与批记录配置缓存'
)
assertMatch(
  /\.route-flow-graph-designer__node\.is-binding-bound[\s\S]*border-color: #67c23a[\s\S]*\.route-flow-graph-designer__node\.is-binding-missing[\s\S]*border-color: #f56c6c/,
  '节点绑定状态样式必须提供绿色已绑定和红色未绑定边框'
)

console.log('PASS: MES route flow binding border static contract')
