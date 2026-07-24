const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routeFormContent = read('src/views/mes/pro/route/RouteFormContent.vue')
const routeEditPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const flowGraph = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const flowConfigApi = read('src/api/mes/pro/route/flowconfig.ts')
const staleRouteProcessListPath = path.join(root, 'src/views/mes/pro/route/RouteProcessList.vue')

for (const staleEntry of [
  'label="组成工序"',
  "name=\"process\"",
  '<RouteProcessList',
  "import RouteProcessList from './RouteProcessList.vue'"
]) {
  assert.ok(
    !routeFormContent.includes(staleEntry),
    `迁移到流转关系图后工艺路线表单不应继续暴露旧组成工序入口: ${staleEntry}`
  )
}

assert.ok(
  !routeEditPage.includes("'process'"),
  '工艺路线编辑页不应继续把 tab=process 当作可打开页签'
)
assert.match(routeEditPage, /return 'flow'/, '历史 tab 参数应回到流转关系图入口')
assert.ok(
  !fs.existsSync(staleRouteProcessListPath),
  '旧组成工序内容组件必须删除，避免动态路由或旧入口继续打开 RouteProcessList'
)

for (const expected of [
  '添加配置项',
  'data-flow-action="add-process-config-item"',
  'data-flow-field="process-config-item-select"',
  ':disabled="field.disabled"',
  "key: 'formSlots'",
  '表单槽位',
  "key: 'relationList'",
  '关系清单',
  "label: getRouteProcessSettingColumnLabel('relationList', '关系清单')",
  'buildRouteProcessRelationListSummary()',
  'visibleBoundaryRelationEdges',
  'visibleRouteRelationEdges',
  '（已添加）',
  'DEFAULT_PROCESS_DETAIL_FIELD_KEYS',
  'selectedProcessDetailFieldToAdd',
  'availableProcessDetailFieldOptions',
  'processDetailFieldSelectOptions',
  'handleAddProcessDetailField'
]) {
  assert.ok(flowGraph.includes(expected), `流转关系图缺少配置项下拉契约: ${expected}`)
}

for (const removedGroupTitle of [
  '<el-option-group',
  '工序设置列（表单槽位）',
  '工序设置列（基础字段）',
  'processDetailFieldSelectOptionGroups',
  'ProcessDetailFieldSelectOptionGroup'
]) {
  assert.ok(
    !flowGraph.includes(removedGroupTitle),
    `添加配置项下拉不应继续渲染分组标题或分组结构: ${removedGroupTitle}`
  )
}

assert.ok(
  flowGraph.includes("if (!columns) return mergeRequiredProcessDetailFieldKeys([...DEFAULT_PROCESS_DETAIL_FIELD_KEYS])"),
  '未保存关注配置时，流转关系图必须只展示默认字段与必显字段，不能默认铺开全部迁移配置项'
)
assert.match(
  flowGraph,
  /const REQUIRED_PROCESS_DETAIL_FIELD_KEYS[\s\S]*'batchRecordFormNames'[\s\S]*\.filter/,
  '批记录表单是左侧必显字段，不能被旧用户关注配置隐藏'
)
assert.ok(
  !flowGraph.includes("if (!columns) return [...PROCESS_DETAIL_FIELD_KEYS]"),
  '未保存关注配置时不得默认把全部配置项预添加，避免下拉列表为空'
)

for (const slot of ['MAIN', 'LOSS_REPORT', 'PROCESS_INSPECTION', 'PARAMETER_RECORD']) {
  assert.match(
    flowGraph,
    new RegExp(`const RECORD_BINDING_SLOT_TYPES[\\s\\S]*'${slot}'[\\s\\S]*\\]`),
    `添加配置项下拉或卡片缺少现有槽位: ${slot}`
  )
}
assert.ok(flowGraph.includes('关系清单'), '添加配置项下拉或卡片缺少关系清单')
assert.ok(
  flowGraph.includes("label: getRouteProcessSettingColumnLabel('formSlots', '表单槽位')"),
  '添加配置项下拉必须来自工序设置里的通用“表单槽位”列'
)
assert.ok(
  flowGraph.includes("label: getRouteProcessSettingColumnLabel('relationList', '关系清单')"),
  '添加配置项下拉必须来自工序设置里的“关系清单”列'
)
assert.match(
  flowGraph,
  /processDetailFieldSelectOptions[\s\S]*selectedFieldKeySet[\s\S]*disabled[\s\S]*已添加/,
  '已添加配置项仍必须留在下拉列表中并显示“已添加”，不得让用户误以为新增项不存在'
)
assert.match(
  flowGraph,
  /processDetailFieldSelectOptions[\s\S]*slotOptions[\s\S]*basicOptions[\s\S]*return \[\.\.\.slotOptions, \.\.\.basicOptions\]/,
  '添加配置项下拉必须仍把表单槽位类选项置顶，但不再显示分组标题'
)
assert.ok(
  !flowGraph.includes("{ label: '表单槽位', options: slotOptions }"),
  '表单槽位不得作为脱离工序设置列的独立顶层分组'
)
assert.ok(
  !flowGraph.includes('selectedProcessDetailFieldToAdd.value = firstAvailableFieldKey'),
  '添加配置项选择器不得自动选中第一个普通字段；未选择前必须保留“添加配置项”占位提示'
)

for (const expected of [
  'getTemplatePool',
  'data-route-process-setting-field="form-template"',
  'data-route-process-setting-field="candidate-source-type"',
  'data-route-process-setting-field="candidate-source-id"',
  'SHARED_FORM_FILLABLE_SCOPE_JSON',
  'buildSharedRecordBindingKey',
  'saveBatchRecordConfig'
]) {
  assert.ok(flowGraph.includes(expected), `配置卡片必须复用现有路线表单绑定契约: ${expected}`)
}

for (const removedSharedFormControl of [
  'data-route-process-setting-field="shared-form-instance-scope"',
  'data-route-process-setting-field="required-policy"',
  'data-route-process-setting-field="shared-form-key"',
  'data-route-process-setting-field="fillable-scope-json"'
]) {
  assert.ok(
    !flowGraph.includes(removedSharedFormControl),
    `选择表单后即共享，不应继续渲染红框配置控件: ${removedSharedFormControl}`
  )
}

for (const expected of [
  'formSlotType?: ProRouteFlowFormSlotType | null',
  'instanceScope?:',
  'sharedFormKey?:',
  'fillableScopeJson?:',
  'requiredPolicy?:'
]) {
  assert.ok(flowConfigApi.includes(expected), `现有保存契约缺少共享表单字段: ${expected}`)
}

assert.ok(
  flowGraph.includes('remote') && flowGraph.includes('loadFormTemplateOptions'),
  '具体表单应通过卡片内表单中心模板搜索绑定，而不是恢复旧组成工序 tab'
)
assert.ok(
  !flowGraph.includes('mock') && !flowGraph.includes('伪成功'),
  '流转关系图配置项实现不得通过 mock 或伪成功口径掩盖保存失败'
)

console.log('mes-route-flowgraph-dropdown-config-static PASS')
