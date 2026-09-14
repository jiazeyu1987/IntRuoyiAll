const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const read = (root, relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const component = read(frontendRoot, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const api = read(frontendRoot, 'src/api/mes/pro/route/flowconfig.ts')
const backendRoot = path.join(workspaceRoot, 'IntRuoyiBackend')
const backendBase = path.join(
  'yudao-module-mes',
  'src/main/java/cn/iocoder/yudao/module/mes'
)
const saveVO = read(
  backendRoot,
  path.join(backendBase, 'controller/admin/pro/route/vo/flowconfig/MesProRouteFlowFormBindingSaveReqVO.java')
)
const respVO = read(
  backendRoot,
  path.join(backendBase, 'controller/admin/pro/route/vo/flowconfig/MesProRouteFlowFormBindingRespVO.java')
)
const bindingDO = read(
  backendRoot,
  path.join(backendBase, 'dal/dataobject/pro/route/MesProRouteFlowProcessBatchRecordDO.java')
)
const configService = read(
  backendRoot,
  path.join(backendBase, 'service/pro/route/MesProRouteFlowConfigServiceImpl.java')
)
const projectionService = read(
  backendRoot,
  path.join(backendBase, 'service/pro/route/MesProRouteVersionPublishProjectionServiceImpl.java')
)
const routeService = read(
  backendRoot,
  path.join(backendBase, 'service/pro/route/MesProRouteServiceImpl.java')
)
const migration = read(backendRoot, 'sql/mysql/20260817_mes_route_form_global_sync_key.sql')

for (const source of [api, saveVO, respVO, bindingDO]) {
  assert.match(source, /globalSyncKey/, '前后端保存、响应与持久化模型必须完整携带 globalSyncKey。')
}

for (const expected of [
  'data-route-process-setting-field="global-form-binding-switch"',
  'handleRecordBindingGlobalSyncChange',
  'createGlobalFormBindingSyncKey',
  'syncGlobalRecordBindingGroupFromSource',
  'inheritGlobalRecordBindingsForRouteProcess',
  'confirmEnableGlobalRecordBinding',
  'confirmDisableGlobalRecordBinding',
  'removeGlobalRecordBindingGroup'
]) {
  assert.ok(component.includes(expected), `全局附加表单必须实现可见开关与联动行为: ${expected}`)
}

assert.match(
  component,
  /:disabled="recordBindingEditorDisabled \|\| !binding\.formTemplateId"[\s\S]*@change="\(value\) => handleRecordBindingGlobalSyncChange\(binding, Boolean\(value\)\)"/,
  '全局开关必须在非草稿、加载中或未选模板时禁用，并走专用二次确认处理器。'
)
assert.match(
  component,
  /confirmEnableGlobalRecordBinding[\s\S]*目标工序[\s\S]*新增[\s\S]*替换/,
  '开启确认必须说明目标工序、新增和同槽位替换数量。'
)
assert.match(
  component,
  /handleRouteProcessAdd[\s\S]*inheritGlobalRecordBindingsForRouteProcess\(routeProcessId\)/,
  '新增普通工序必须继承当前路线的全部全局附加表单。'
)
assert.doesNotMatch(
  component,
  /syncRouteWideRecordBindingProcessIndependent|syncRouteWideRecordBindingFillerByTemplate/,
  '同模板跨工序自动同步必须移除，只允许按 globalSyncKey 联动。'
)
assert.match(component, /globalSyncKey:\s*binding\.globalSyncKey \|\| null/, '草稿与保存快照必须携带全局组身份。')
assert.match(
  component,
  /buildFormBindingSaveRows[\s\S]*reportSort:\s*isRecordBindingGlobalSynced\(binding\)[\s\S]*binding\.reportSort \|\| index \+ 1/,
  '全局组保存时必须保留组内同步的排序值，不能被各工序本地列表位置重新编号。'
)
assert.match(
  component,
  /syncSelectedRecordBindingsToDraft\(false\)[\s\S]*synchronizedSourceBinding[\s\S]*cloneGlobalRecordBindingForProcess\([\s\S]*synchronizedSourceBinding/,
  '开启全局联动时必须使用当前工序重排后的源绑定复制，不能把源卡片的旧 reportSort 带到其他工序。'
)
assert.match(
  component,
  /const syncSelectedRecordBindingsToDraft = \([\s\S]*syncGlobalRecordBindingGroupsFromCurrentProcess\(draft\)/,
  '当前工序普通表单增删或复制导致排序变化时，必须同步刷新当前工序中的全部全局组。'
)
assert.match(
  component,
  /copySelectedProcessFormBindingsFromSource[\s\S]*currentGlobalBindings[\s\S]*filter\(\(binding\) => !isRecordBindingGlobalSynced\(binding\)\)/,
  '复制工序表单时必须保留当前工序的全局组，只替换非全局表单。'
)
assert.match(
  component,
  /handleSelectedRecordBindingTemplateChange[\s\S]*!templateId && isRecordBindingGlobalSynced\(binding\)[\s\S]*removeGlobalRecordBindingGroup\(binding\)/,
  '清空全局表单模板必须走全组删除二次确认，不能绕过删除确认。'
)

for (const source of [configService, projectionService, routeService]) {
  assert.match(source, /globalSyncKey|getGlobalSyncKey|setGlobalSyncKey/, '候选快照、发布投影与路线快照必须保留全局组身份。')
}
assert.match(configService, /validateGlobalFormBindingGroups/, '后端必须校验完整候选快照中的全局组。')
assert.match(configService, /GLOBAL_FORM_GROUP_INCOMPLETE/, '后端必须阻断未覆盖全部普通工序的全局组。')
assert.match(configService, /GLOBAL_FORM_GROUP_DUPLICATE/, '后端必须阻断同工序重复组成员。')
assert.match(configService, /GLOBAL_FORM_GROUP_INCONSISTENT/, '后端必须阻断同组可编辑配置不一致。')

assert.match(migration, /ADD COLUMN `global_sync_key` varchar\(128\) DEFAULT NULL/, '迁移必须增加可空全局组列。')
assert.match(migration, /idx_mes_route_flow_global_sync/, '迁移必须增加路线级全局组查询索引。')

console.log('mes-route-flow-global-form-binding-static PASS')
