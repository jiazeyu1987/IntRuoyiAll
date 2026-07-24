const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const sharedColumnsPath = 'src/views/mes/pro/route/routeProcessSettingsColumns.ts'
assert.ok(exists(sharedColumnsPath), '工序设置列定义必须抽取到共享文件')

const sharedColumns = read(sharedColumnsPath)
const flowGraph = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const packageJson = JSON.parse(read('package.json'))
const staleProcessListPath = 'src/views/mes/pro/route/RouteProcessList.vue'

function assertIncludes(source, expected, label) {
  assert.ok(source.includes(expected), `${label}: expected ${JSON.stringify(expected)}`)
}

function assertNotIncludes(source, unexpected, label) {
  assert.ok(!source.includes(unexpected), `${label}: must not include ${JSON.stringify(unexpected)}`)
}

assert.strictEqual(
  packageJson.scripts?.['e2e:mes:route-process-settings-column-sync:static'],
  'node tests/e2e/mes-route-process-settings-column-sync-static.spec.js',
  'package.json 必须暴露红框列同步静态契约'
)
assert.ok(!exists(staleProcessListPath), '组成工序旧列表组件必须下线，迁移内容统一从流转关系图配置')

assertIncludes(
  sharedColumns,
  "export const ROUTE_PROCESS_SETTINGS_TABLE_KEY = 'mes.pro.route.process.settings'",
  '共享列配置必须使用稳定 tableKey'
)
assertIncludes(
  sharedColumns,
  'ROUTE_PROCESS_SETTINGS_COLUMN_CONFIG_CHANGED_EVENT',
  '共享列配置必须提供跨组件刷新事件'
)
assertIncludes(
  sharedColumns,
  'routeProcessSettingsDefaultColumns',
  '共享列配置必须提供工序设置默认列'
)
assertIncludes(
  sharedColumns,
  'isRouteProcessSettingsDetailColumnKey',
  '共享列配置必须提供红框可渲染列过滤函数'
)

for (const key of [
  'sort',
  'processCode',
  'processName',
  'capacitySource',
  'standardResource',
  'standardShiftCapacity',
  'productionQuantityFactor',
  'shiftCapacity',
  'formSlots',
  'batchRecordFormNames',
  'lossReportFormNames',
  'processInspectionFormNames',
  'parameterRecordFormNames',
  'resourceStatus',
  'predecessor',
  'successors',
  'relationList',
  'keyFlag',
  'checkFlag',
  'workstation'
]) {
  assertIncludes(sharedColumns, `key: '${key}'`, `共享列定义缺少 ${key}`)
}
assertIncludes(sharedColumns, "label: '表单槽位'", '工序设置显示字段必须提供通用表单槽位列')
assertIncludes(sharedColumns, "label: '关系清单'", '工序设置显示字段必须提供关系清单列')
assertIncludes(flowGraph, "key: 'formSlots'", '流转图必须把通用表单槽位列作为可添加配置项')
assertIncludes(flowGraph, 'buildFormSlotSummaryValue()', '通用表单槽位列必须汇总现有槽位绑定状态')
assertIncludes(flowGraph, 'isFormSlotAggregateDetailField', '通用表单槽位列必须复用现有槽位配置卡片')
assertIncludes(flowGraph, "key: 'relationList'", '流转图必须把关系清单列作为可添加配置项')
assertIncludes(
  flowGraph,
  "label: getRouteProcessSettingColumnLabel('relationList', '关系清单')",
  '关系清单列必须复用工序设置共享列标签'
)
assertIncludes(flowGraph, 'buildRouteProcessRelationListSummary()', '关系清单列必须展示现有关系摘要')
assertIncludes(flowGraph, 'visibleBoundaryRelationEdges', '关系清单列必须复用现有边界关系数据源')
assertIncludes(flowGraph, 'visibleRouteRelationEdges', '关系清单列必须复用现有工序关系数据源')

assertIncludes(flowGraph, "from '@/hooks/web/useUserTableColumns'", '流转图必须接入同一个列配置 hook')
assertIncludes(flowGraph, "from '@/api/system/userTableColumnConfig'", '流转图必须保存用户关注列配置')
assertIncludes(flowGraph, 'routeProcessSettingsDefaultColumns', '流转图必须复用工序设置列定义')
assertIncludes(flowGraph, 'useUserTableColumns(ROUTE_PROCESS_SETTINGS_TABLE_KEY, routeProcessSettingsDefaultColumns)', '流转图必须读取同一个 tableKey')
assertIncludes(flowGraph, "const PROCESS_DETAIL_FIELD_CONFIG_TABLE_KEY = 'mes.pro.route.flow.detailFields'", '红框必须使用历史用户关注列 tableKey')
assertIncludes(flowGraph, 'selectedProcessDetailFieldKeys', '红框必须维护用户关注列 key 列表')
assertIncludes(flowGraph, 'v-for="field in selectedProcessDetailFields"', '字段按钮必须仍由用户关注列渲染')
assertIncludes(flowGraph, 'data-flow-action="select-process-detail-field"', '字段按钮必须复用红框字段卡片')
assertIncludes(flowGraph, 'selectedProcessDetailField = computed(() =>', '右侧字段明细必须从现有字段映射派生')
assertIncludes(flowGraph, 'selectedProcessDetailFieldToAdd', '红框必须提供待添加关注列选择')
assertIncludes(flowGraph, 'processDetailFieldSelectOptions', '红框下拉必须展示全部可配置项')
assertIncludes(flowGraph, 'availableProcessDetailFieldOptions', '红框必须只把未添加项作为可新增项')
assertIncludes(flowGraph, '（已添加）', '红框下拉必须标识已添加项，避免用户误以为新增项不存在')
assertIncludes(flowGraph, 'selectedProcessDetailFields = computed(() =>', '红框按钮必须来自已添加关注列')
assertIncludes(flowGraph, 'selectedProcessDetailField = computed', '右侧字段明细必须复用当前关注列投射结果')
assertIncludes(flowGraph, 'processDetailFieldOptionMap.value.get', '右侧字段明细必须复用现有字段 option map')
assertIncludes(flowGraph, 'handleAddProcessDetailField', '红框必须支持添加关注列')
assertIncludes(flowGraph, 'handleRemoveProcessDetailField', '红框必须支持删除关注列')
assertIncludes(flowGraph, 'handleSelectProcessDetailField', '红框必须支持点击已添加字段查看明细')
assertIncludes(flowGraph, 'saveUserTableColumnConfig', '红框必须保存用户关注列')
assertIncludes(flowGraph, 'getUserTableColumnConfig', '红框必须加载用户关注列')
assertIncludes(flowGraph, 'buildProcessDetailFieldConfigColumns', '红框必须把关注列保存为用户列配置')
assertIncludes(flowGraph, 'routeProcessSettingsDefaultColumns.map', '红框可选字段必须来自工序设置列定义')
assertIncludes(flowGraph, 'isRouteProcessSettingsDetailColumnKey', '红框字段必须过滤工序设置允许渲染列')
assertIncludes(flowGraph, 'ROUTE_PROCESS_SETTINGS_COLUMN_CONFIG_CHANGED_EVENT', '流转图必须监听工序设置列配置变化')
assertIncludes(flowGraph, 'loadRouteProcessSettingColumnConfig', '流转图必须能刷新共享列配置')
assertIncludes(flowGraph, 'ProRouteProcessApi.getRouteProcessListByRoute(props.routeId)', '流转图必须加载工序设置行数据供红框渲染')
assertIncludes(flowGraph, ':data-flow-detail-field="field.key"', '红框字段行必须保留稳定选择器')
assertIncludes(flowGraph, 'data-flow-action="select-process-detail-field"', '红框已添加字段必须作为按钮选择右侧明细')
assertIncludes(flowGraph, 'data-flow-panel="selected-field-detail"', '红框右侧必须提供字段明细面板')
assertIncludes(flowGraph, 'route-flow-graph-designer__process-detail-field-picker', '红框必须显示关注列选择器')
assertIncludes(flowGraph, 'data-flow-action="add-process-config-item"', '红框必须提供添加配置项按钮')
assertIncludes(flowGraph, 'data-flow-action="remove-process-detail-field"', '红框必须提供删除关注列按钮')
assertIncludes(flowGraph, ':data-flow-field-editor="selectedProcessDetailField.key"', '红框右侧可编辑字段必须提供稳定编辑器选择器')
assertIncludes(flowGraph, 'PROCESS_DETAIL_EDITABLE_FIELD_KEYS', '红框必须通过白名单控制可编辑字段')
assertNotIncludes(flowGraph, 'RouteScheduleStrategyEditor', '红框不得把排产策略列内部编辑器整块渲染出来')
assertIncludes(flowGraph, '@change="handleKeyProcessToggle"', '关键工序红框字段必须复用候选草稿保存开关')
assertNotIncludes(flowGraph, "editor: 'record-form'", '红框不得把表单列渲染成独立下拉编辑器')
assertNotIncludes(flowGraph, 'selectedProcessDetailFieldItems', '不得新增第二套字段明细数组')
assertNotIncludes(flowGraph, 'selectedFieldDetailItems', '不得新增第二套字段明细数组')
assertNotIncludes(flowGraph, 'getProcessDetailFieldDetail', '红框字段明细不得新增第二套后端查询')
assertNotIncludes(flowGraph, 'selectedProcessDetailValueMap', '红框字段明细不得新增第二套字段值存储')

for (const removed of [
  'processDetailFieldConfigLoading',
  'processDetailFieldConfigSaving',
  '点击加号添加关注字段'
]) {
  assertNotIncludes(flowGraph, removed, `红框必须移除独立字段维护逻辑 ${removed}`)
}

console.log('mes-route-process-settings-column-sync-static PASS')
