const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)
const settingsColumns = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/route/routeProcessSettingsColumns.ts'),
  'utf8'
)

assert.ok(
  settingsColumns.includes("| 'batchRecordFormNames'") &&
    settingsColumns.includes("{ key: 'batchRecordFormNames', label: '批记录表单'"),
  '批记录表单必须保留在共享工序设置列定义中。'
)

const defaultKeysMatch = component.match(
  /const DEFAULT_PROCESS_DETAIL_FIELD_KEYS: ProcessDetailFieldKey\[\] = \[([\s\S]*?)\]\.filter/
)
assert.ok(defaultKeysMatch, '流转关系图必须声明默认左侧配置项列表。')
assert.ok(
  defaultKeysMatch[1].includes("'batchRecordFormNames'"),
  '批记录表单必须作为默认左侧配置项显示。'
)

assert.match(
  component,
  /const REQUIRED_PROCESS_DETAIL_FIELD_KEYS[\s\S]*'batchRecordFormNames'/,
  '读取用户已保存左侧字段配置时也必须强制保留批记录表单。'
)
assert.match(
  component,
  /const resolveSavedProcessDetailFieldKeys[\s\S]*mergeRequiredProcessDetailFieldKeys/,
  '用户配置存在时不得把批记录表单过滤隐藏。'
)

assert.ok(
  component.includes("key: 'batchRecordFormNames'") &&
    component.includes("label: getRouteProcessSettingColumnLabel('batchRecordFormNames', '批记录表单')") &&
    component.includes('value: buildBatchRecordFormValue()') &&
    component.includes('links: buildBatchRecordFormLinks()'),
  '左侧批记录表单字段必须使用正式批记录表单专用值和跳转链接。'
)

assert.match(
  component,
  /fieldKey === 'batchRecordFormNames'[\s\S]*isRouteNodeBatchRecordFormConfigured\(node\)/,
  '点击批记录表单字段时必须按正式批记录表单绑定状态标记节点。'
)

console.log('PASS: MES route flow batch record panel visible static contract')
