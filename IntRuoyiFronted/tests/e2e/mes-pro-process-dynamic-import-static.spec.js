const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const processFormSource = readText('src/views/mes/pro/process/ProProcessForm.vue')
const routeEditPageSource = readText('src/views/mes/pro/route/RouteEditPage.vue')
const routeFlowGraphSource = readText('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

assert.doesNotMatch(
  processFormSource,
  /RouteFlowConfigPanel/,
  '工序弹窗不得继续导入已下线的 RouteFlowConfigPanel，否则工序页面动态导入会请求不存在的模块。'
)
assert.match(
  processFormSource,
  /const router = useRouter\(\)/,
  '工序弹窗必须持有 router，用于跳转到正式的工艺路线流转关系图配置入口。'
)
assert.match(
  processFormSource,
  /const openAssociatedRouteConfig = \(\) => \{[\s\S]*router\.push\(\{[\s\S]*name: 'MesProRouteEdit'[\s\S]*params: \{ id: selectedRouteId\.value \}[\s\S]*query: \{[\s\S]*tab: 'flow'[\s\S]*routeProcessId: String\(selectedRouteProcessId\.value\)[\s\S]*\}[\s\S]*\}\)/,
  '工序弹窗应通过 MesProRouteEdit + flow tab + routeProcessId 深链到当前路线工序设置。'
)
assert.match(
  processFormSource,
  /@click="openAssociatedRouteConfig"/,
  '工序弹窗必须提供可点击入口打开当前路线工序设置。'
)
assert.match(
  processFormSource,
  /getRouteProcessByRouteAndProcess/,
  '工序弹窗仍需先解析 routeId + processId 对应的 routeProcessId，不能只按工序 ID 猜测。'
)
assert.match(
  routeEditPageSource,
  /route\.query\.routeProcessId/,
  '编辑工艺路线页面必须接收 routeProcessId 深链参数。'
)
assert.match(
  routeFlowGraphSource,
  /props\.targetRouteProcessId[\s\S]*selectedRouteProcessId\.value = restoredRouteProcessId/,
  '流转关系图必须用 targetRouteProcessId 恢复并选中目标路线工序。'
)

console.log('PASS: MES process dynamic import static contract is satisfied')
