const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const tagsViewStore = read('src/store/modules/tagsView.ts')
const routeEditPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const routeConfig = read('src/router/modules/remaining.ts')

assert.match(
  routeConfig,
  /name: 'MesProRouteEdit'[\s\S]*?noTagsView:\s*true[\s\S]*?activeMenu:\s*'\/mes\/pro\/route'/,
  '编辑页必须继续使用隐藏路由并保留工艺流程菜单归属'
)
assert.match(
  tagsViewStore,
  /replaceActiveMenuView\(view:\s*RouteLocationNormalizedLoaded\)/,
  'tags view store must expose hidden route activation by activeMenu'
)
assert.match(
  tagsViewStore,
  /restoreActiveMenuView\([\s\S]*?snapshot/,
  'tags view store must restore the original activeMenu view on return to the list'
)
assert.match(
  routeEditPage,
  /const tagsViewStore = useTagsViewStore\(\)/,
  'route edit page must coordinate its hidden route with the top-level tag'
)
assert.match(
  routeEditPage,
  /tagsViewStore\.replaceActiveMenuView\(route\)/,
  'route edit page must make the existing 工艺流程 tag target the editor route'
)
assert.match(
  routeEditPage,
  /tagsViewStore\.restoreActiveMenuView\([\s\S]*?routeEditTabSnapshot/,
  'route edit page must restore the list tag before leaving to the route list'
)
assert.match(
  routeEditPage,
  /onBeforeRouteLeave\(async \(to\)/,
  'route edit page must restore the tag for direct navigation back to the list'
)

console.log('mes-route-flow-tab-return-state-static PASS')
