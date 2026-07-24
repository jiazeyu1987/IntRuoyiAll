const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (file) => fs.readFileSync(path.join(repoRoot, file), 'utf8')

const routerHelper = read('src/utils/routerHelper.ts')
const remainingRouter = read('src/router/modules/remaining.ts')
const routeIndexPath = path.join(repoRoot, 'src/views/mes/pro/route/index.vue')
const routeFormContentPath = path.join(repoRoot, 'src/views/mes/pro/route/RouteFormContent.vue')

assert.ok(fs.existsSync(routeIndexPath), '工艺流程列表页 index.vue 必须存在。')
assert.ok(fs.existsSync(routeFormContentPath), '本契约需要确认同目录表单组件仍存在但不能作为菜单入口。')

assert.match(
  routerHelper,
  /MES_PRO_ROUTE_LIST_COMPONENT\s*=\s*'mes\/pro\/route\/index'/,
  '工艺流程动态菜单必须有唯一列表组件常量，避免刷新后进入同目录表单组件。'
)
assert.match(
  routerHelper,
  /normalizeMesProRouteMenuComponent/,
  '动态菜单生成前必须规范化工艺流程菜单组件，兼容已登录会话或权限响应里的旧组件值。'
)
assert.match(
  routerHelper,
  /route\.component\s*=\s*MES_PRO_ROUTE_LIST_COMPONENT/,
  '命中工艺流程菜单时必须强制使用列表 index.vue，而不是 RouteFormContent/RouteEditPage。'
)
assert.match(
  routerHelper,
  /componentPath\.startsWith\('mes\/pro\/route\/'\)/,
  '工艺流程菜单缓存若残留同目录表单/编辑组件，也必须被识别并纠正到列表入口。'
)
assert.match(
  remainingRouter,
  /name:\s*'MesProRouteEdit'[\s\S]{0,420}activeMenu:\s*'\/mes\/pro\/route'/,
  '隐藏编辑页仍应高亮工艺流程菜单，但菜单入口本身必须回到列表页。'
)

console.log('mes-pro-route-refresh-menu-component-static PASS')
