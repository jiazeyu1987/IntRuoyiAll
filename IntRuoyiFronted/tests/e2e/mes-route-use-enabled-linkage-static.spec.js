const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const routeFormPath = path.resolve(process.cwd(), 'src/views/mes/pro/route/RouteFormContent.vue')
const routeFlowConfigPanelPath = path.resolve(process.cwd(), 'src/views/mes/pro/route/RouteFlowConfigPanel.vue')
const routeApiPath = path.resolve(process.cwd(), 'src/api/mes/pro/route/index.ts')
const flowConfigApiPath = path.resolve(process.cwd(), 'src/api/mes/pro/route/flowconfig.ts')

for (const requiredPath of [routeFormPath, routeFlowConfigPanelPath, routeApiPath, flowConfigApiPath]) {
  assert(fs.existsSync(requiredPath), `required file missing: ${requiredPath}`)
}

const routeForm = fs.readFileSync(routeFormPath, 'utf8')
const routeFlowConfigPanel = fs.readFileSync(routeFlowConfigPanelPath, 'utf8')
const routeApi = fs.readFileSync(routeApiPath, 'utf8')
const flowConfigApi = fs.readFileSync(flowConfigApiPath, 'utf8')

assert.match(routeForm, /label="排产配置" name="schedule-config"[\s\S]*<RouteFlowConfigPanel[\s\S]*config-type="SCHEDULE"/, '工艺流程详情必须以内嵌页签承载排产配置。')
assert.match(routeForm, /label="批记录配置" name="batch-record-config"[\s\S]*<RouteFlowConfigPanel[\s\S]*config-type="BATCH"/, '工艺流程详情必须以内嵌页签承载批记录配置。')

assert.match(routeApi, /scheduleRouteEnabled\?: boolean/, '路线分页 VO 必须包含排产用途启用字段 scheduleRouteEnabled。')
assert.match(routeApi, /batchRouteEnabled\?: boolean/, '路线分页 VO 必须包含批记录用途启用字段 batchRouteEnabled。')
assert.match(flowConfigApi, /updateEnabled:\s*async\s*\(/, '前端工艺流程配置 API 必须提供 updateEnabled。')
assert.match(flowConfigApi, /\/mes\/pro\/route\/flow-config\/enabled/, '配置启停必须调用正式后端 enabled 接口。')

assert.match(routeFlowConfigPanel, /<el-switch[\s\S]*v-model="routeConfigEnabled"[\s\S]*@change="handleEnabledChange"/, '排产/批记录配置页签必须提供配置启停开关。')
assert.match(routeFlowConfigPanel, /const updatePermission = computed\(\(\) =>[\s\S]*'mes:pro-route:schedule-config:update'[\s\S]*'mes:pro-route:batch-record-config:update'/, '配置启停与保存必须使用对应 update 权限。')
assert.match(routeFlowConfigPanel, /:disabled="readonly \|\| !canUpdate"/, '配置启停开关必须按 update 权限禁用。')
assert.match(routeFlowConfigPanel, /import \{ checkPermi \} from '@\/utils\/permission'/, '用途启用开关必须使用 checkPermi 判断权限，不再使用文本按钮隐藏样式。')
assert.doesNotMatch(routeFlowConfigPanel, /updateRouteUseEnabled|buildRouteUseEnabled|handleUpdateRouteUseEnabled|useType\.value/, '工艺流程配置页签不得保留旧用途列表启停实现。')
assert.match(routeFlowConfigPanel, /await ProRouteFlowConfigApi\.updateEnabled\(\{[\s\S]*routeId: props\.routeId,[\s\S]*useType: props\.configType,[\s\S]*enabled: Boolean\(enabled\)[\s\S]*\}\)/, '配置启停必须传 routeId、useType、enabled 调用新工艺流程配置接口。')

console.log('PASS: MES route flow enabled linkage static contract')
