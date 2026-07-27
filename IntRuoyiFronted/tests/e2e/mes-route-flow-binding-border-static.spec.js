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
  /const getRouteNodeBindingStatus = \(node: RouteFlowNodeVO\): RouteNodeBindingStatus => \{[\s\S]*selectedProcessDetailFieldKey\.value[\s\S]*return getRouteNodeAdditionalFormCount\(node\) > 0 \? 'bound' : 'none'[\s\S]*isRouteNodeBatchRecordFormConfigured\(node\)[\s\S]*isRouteNodeWorkstationBound\(node\)[\s\S]*Boolean\(node\.keyFlag\)[\s\S]*Boolean\(node\.checkFlag\)/,
  '节点状态必须按当前选择的配置项判断；表单槽位零附加表单隐藏，其它配置项保持红绿边框'
)
assertMatch(
  /const getRouteNodeBatchRecordBindings = \(node: RouteFlowNodeVO\): RouteFlowRecordBinding\[\] => \{[\s\S]*selectedProcessAttributeDrafts\[node\.routeProcessId\]\?\.recordBindings[\s\S]*selectedProcessRouteConfigCache\.value\?\.batchConfigs[\s\S]*buildRecordBindings\(batchConfig\)/,
  '表单槽位节点状态必须复用当前草稿与批记录配置缓存'
)
assertMatch(
  /const getRouteNodeLegacyBatchRecords = \(node: RouteFlowNodeVO\): RouteFlowLegacyBatchRecord\[\] => \{[\s\S]*selectedProcessAttributeDrafts\[node\.routeProcessId\]\?\.legacyBatchRecords[\s\S]*selectedProcessRouteConfigCache\.value\?\.batchConfigs[\s\S]*buildLegacyBatchRecords\(batchConfig\?\.batchRecordReports\)/,
  '正式批记录表单节点状态必须按 routeProcessId 复用当前草稿与批记录配置缓存'
)
assertMatch(
  /\.route-flow-graph-designer__node\.is-binding-bound[\s\S]*border-color: #67c23a[\s\S]*\.route-flow-graph-designer__node\.is-binding-missing[\s\S]*border-color: #f56c6c/,
  '节点绑定状态样式必须提供绿色已绑定和红色未绑定边框'
)
assertMatch(
  /\.route-flow-graph-designer__node\.is-binding-missing[\s\S]*border-color: #f56c6c[\s\S]*\.route-flow-graph-designer__node\.is-selected \{[\s\S]*border-color: #7c3aed[\s\S]*0 0 0 2px rgb\(124 58 237 \/ 22%\)/,
  '选中工序节点必须在红绿绑定状态后用紫色边框覆盖'
)

console.log('PASS: MES route flow binding border static contract')
