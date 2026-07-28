const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)

const defaultKeysMatch = component.match(
  /const DEFAULT_PROCESS_DETAIL_FIELD_KEYS: ProcessDetailFieldKey\[\] = \[([\s\S]*?)\]\.filter/
)
assert.ok(defaultKeysMatch, '流转关系图必须声明默认左侧配置 item 列表')
assert.ok(
  defaultKeysMatch[1].includes("'batchRecordFormNames'"),
  '批记录表单必须作为默认左侧配置 item 可见，不能只停留在添加配置项下拉里'
)

assert.ok(
  component.includes("key: 'batchRecordFormNames'") &&
    component.includes("label: getRouteProcessSettingColumnLabel('batchRecordFormNames', '批记录表单')"),
  '批记录表单必须保留独立字段定义'
)
assert.ok(
  component.includes("key: 'formSlots'") &&
    component.includes("label: getRouteProcessSettingColumnLabel('formSlots', '表单槽位')"),
  '表单槽位必须继续保留为独立字段，不能替代批记录表单'
)

const bindingStatusKeysMatch = component.match(
  /const ROUTE_NODE_BINDING_STATUS_FIELD_KEYS = new Set<RouteProcessSettingColumnKey>\(\[([\s\S]*?)\]\)/
)
assert.ok(bindingStatusKeysMatch, '流转关系图必须声明触发红绿边框的配置项集合')
assert.ok(
  bindingStatusKeysMatch[1].includes("'batchRecordFormNames'"),
  '点击批记录表单 item 时必须触发关系图节点红绿边框'
)
assert.match(
  component,
  /fieldKey === 'batchRecordFormNames'[\s\S]*isRouteNodeBatchRecordConfigured\(node\)/,
  '批记录表单红绿边框必须只判断正式批记录报表绑定状态'
)

console.log('PASS: MES route flow batch record item restore static contract')
