const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (file) => fs.readFileSync(path.join(repoRoot, file), 'utf8')

const routerHelper = read('src/utils/routerHelper.ts')
const routeIndexPath = path.join(repoRoot, 'src/views/mes/pro/route/index.vue')
const routeFormContentPath = path.join(repoRoot, 'src/views/mes/pro/route/RouteFormContent.vue')

assert.ok(fs.existsSync(routeIndexPath), '工艺流程列表页组件必须存在。')
assert.ok(fs.existsSync(routeFormContentPath), '本用例需要确认同目录还存在表单组件，避免模糊命中。')

assert.ok(
  !routerHelper.includes('findIndex((item) => item.includes(componentPath))'),
  '动态路由组件解析不得使用 componentPath 模糊 includes，否则 mes/pro/route 可能命中表单页。'
)
assert.ok(
  !routerHelper.includes("findIndex((item) => item.includes(fallbackPath || ''))"),
  '动态路由组件解析不得使用 fallbackPath 模糊 includes，否则目录级菜单可能命中错误组件。'
)
assert.match(
  routerHelper,
  /\`\$\{target\}\/index\`/,
  '目录级组件路径必须优先解析到 index.vue，例如 mes/pro/route -> mes/pro/route/index.vue。'
)
assert.match(
  routerHelper,
  /normalizeViewModuleKey/,
  '动态路由组件解析必须先把 import.meta.glob 的 key 归一化后做确定性匹配。'
)

console.log('mes-pro-route-menu-component-resolution-static PASS')
