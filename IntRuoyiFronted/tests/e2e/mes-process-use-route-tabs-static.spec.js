const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.join(workspaceRoot, 'yudao-ui-admin-vue3')
const backendRoot = path.join(workspaceRoot, 'ruoyi-vue-pro')

const scheduleRoutePath = path.join(
  frontendRoot,
  'src/views/mes/pro',
  `schedule${'-'}route`,
  'index.vue'
)
const batchRoutePath = path.join(
  frontendRoot,
  'src/views/mes/pro',
  `edhr${'-'}batch${'-'}route`,
  'index.vue'
)
const routeUsePagePath = path.join(frontendRoot, 'src/views/mes/pro', `route${'-'}use`, 'RouteUsePage.vue')
const routeUseApiPath = path.join(frontendRoot, 'src/api/mes/pro/route', `use${'config'}.ts`)
const routeFormContentPath = path.join(frontendRoot, 'src/views/mes/pro/route/RouteFormContent.vue')
const routeEditPagePath = path.join(frontendRoot, 'src/views/mes/pro/route/RouteEditPage.vue')
const routeFlowConfigPanelPath = path.join(frontendRoot, 'src/views/mes/pro/route/RouteFlowConfigPanel.vue')
const flowConfigApiPath = path.join(frontendRoot, 'src/api/mes/pro/route/flowconfig.ts')
const migrationPath = path.join(backendRoot, 'sql/mysql/20260709_mes_route_flow_config_unification.sql')

for (const deletedPath of [scheduleRoutePath, batchRoutePath, routeUsePagePath, routeUseApiPath]) {
  assert(!fs.existsSync(deletedPath), `legacy route-use entry must be deleted: ${deletedPath}`)
}
for (const requiredPath of [routeFormContentPath, routeEditPagePath, routeFlowConfigPanelPath, flowConfigApiPath, migrationPath]) {
  assert(fs.existsSync(requiredPath), `required file missing: ${requiredPath}`)
}

const routeFormContent = fs.readFileSync(routeFormContentPath, 'utf8')
const routeEditPage = fs.readFileSync(routeEditPagePath, 'utf8')
const routeFlowConfigPanel = fs.readFileSync(routeFlowConfigPanelPath, 'utf8')
const flowConfigApi = fs.readFileSync(flowConfigApiPath, 'utf8')
const migrationSql = fs.readFileSync(migrationPath, 'utf8')

assert.match(routeFormContent, /label="排产配置" name="schedule-config"/, '工艺排产配置必须并入工艺流程编辑页 Tab。')
assert.match(routeFormContent, /label="批记录配置" name="batch-record-config"/, '工艺批记录配置必须并入工艺流程编辑页 Tab。')
assert.match(routeFormContent, /<RouteFlowConfigPanel/, '工艺流程编辑页必须复用路线级配置面板。')
assert.match(routeEditPage, /route\.query\.routeProcessId/, '工艺流程编辑页必须支持按工序定位。')
assert.match(routeFlowConfigPanel, /highlight-current-row/, '路线级配置面板必须高亮目标工序。')
assert.match(routeFlowConfigPanel, /setCurrentRow/, '路线级配置面板必须选中目标工序。')
assert.match(routeFlowConfigPanel, /scrollIntoView/, '路线级配置面板必须滚动到目标工序。')
assert.match(flowConfigApi, /\/mes\/pro\/route\/flow-config\/schedule\/save/, '排产配置必须走路线级 flow-config 保存接口。')
assert.match(flowConfigApi, /\/mes\/pro\/route\/flow-config\/batch-record\/save/, '批记录配置必须走路线级 flow-config 保存接口。')
assert(!flowConfigApi.includes('route-use-config'), '前端不得继续调用旧用途路线接口。')

for (const forbidden of [
  'ProRouteApi.createRoute',
  'ProRouteApi.updateRoute',
  'ProRouteApi.deleteRoute',
  'ProRouteProcessApi.createRouteProcess',
  'ProRouteProcessApi.updateRouteProcess',
  'ProRouteProcessApi.deleteRouteProcess',
  '新增工艺路线',
  '编辑工艺路线',
  '删除工艺路线',
  '添加工序',
  '删除工序'
]) {
  assert(!routeFlowConfigPanel.includes(forbidden), `路线级配置面板不得包含原始路线/工序写入口：${forbidden}`)
}

assert.match(migrationSql, /UPDATE `system_menu`[\s\S]*`deleted` = b'1'[\s\S]*`visible` = b'0'[\s\S]*WHERE `id` IN \(900121, 900122, 900221, 900222\)/, '菜单迁移必须软删除并隐藏旧工艺排产/批记录路线入口。')
assert.match(migrationSql, /old process route menus must be deleted after route flow migration/, '菜单迁移必须校验旧入口已删除。')
assert.match(migrationSql, /mes:pro-route:schedule-config:query/, '菜单迁移必须补齐排产配置查询权限。')
assert.match(migrationSql, /mes:pro-route:schedule-config:update/, '菜单迁移必须补齐排产配置更新权限。')
assert.match(migrationSql, /mes:pro-route:batch-record-config:query/, '菜单迁移必须补齐批记录配置查询权限。')
assert.match(migrationSql, /mes:pro-route:batch-record-config:update/, '菜单迁移必须补齐批记录配置更新权限。')

console.log('PASS: MES process route flow config unified entry static contract')
