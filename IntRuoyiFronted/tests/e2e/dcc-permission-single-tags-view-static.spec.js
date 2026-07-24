const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')

const readSource = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const routerHelper = readSource('src/utils/routerHelper.ts')
const tagsViewStore = readSource('src/store/modules/tagsView.ts')
const categoriesPage = readSource('src/views/dcc/controlled-file/categories/index.vue')

assert.match(
  categoriesPage,
  /watch\(activeTab,[\s\S]*router\.replace\([\s\S]*tab[\s\S]*\)[\s\S]*\)/,
  '文控权限内部页签切换会同步到 route.query.tab，顶栏页签必须忽略 query 去重'
)

assert.match(
  tagsViewStore,
  /view\.meta\?\.tagsViewKeyMode === 'path'/,
  'TagsView 必须支持按 path 作为唯一身份，避免同一路由不同 query 打开多个顶栏页签'
)

assert.match(
  routerHelper,
  /DCC_PERMISSION_CATEGORIES_ROUTE_PATH\s*=\s*'controlled-file\/categories'/,
  '动态路由覆盖必须声明文控权限菜单 path'
)
assert.match(
  routerHelper,
  /DCC_PERMISSION_CATEGORIES_ROUTE_COMPONENT\s*=\s*'dcc\/controlled-file\/categories\/index'/,
  '动态路由覆盖必须声明文控权限菜单组件'
)
assert.match(
  routerHelper,
  /routePath === DCC_PERMISSION_CATEGORIES_ROUTE_PATH[\s\S]*componentPath === DCC_PERMISSION_CATEGORIES_ROUTE_COMPONENT[\s\S]*meta\.tagsViewKeyMode = 'path'/,
  '文控权限动态菜单路由必须设置 tagsViewKeyMode=path，使 tab query 不再产生 文控权限(2)/(3)'
)

const dccPermissionOverrideStart = routerHelper.indexOf('DCC_PERMISSION_CATEGORIES_ROUTE_COMPONENT')
const dccPermissionOverrideSnippet = routerHelper.slice(
  Math.max(0, dccPermissionOverrideStart),
  dccPermissionOverrideStart + 900
)

assert.doesNotMatch(
  dccPermissionOverrideSnippet,
  /mock|placeholder data|fallback|降级|吞异常/i,
  '文控权限顶栏页签去重不得引入 mock、placeholder、fallback、降级或吞异常'
)

console.log('PASS: DCC permission single tags view static contract')
