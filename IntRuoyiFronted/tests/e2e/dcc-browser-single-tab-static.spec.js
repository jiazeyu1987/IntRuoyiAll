const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const routerHelper = readSource('src/utils/routerHelper.ts')
const tagsViewStore = readSource('src/store/modules/tagsView.ts')
const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')

assert.match(
  browserPage,
  /const DCC_BROWSER_ROUTE_PATH = '\/dcc\/controlled-file\/browser'/,
  '文件查阅页面必须声明自身稳定路由路径。'
)

assert.match(
  browserPage,
  /watch\(\s*\(\) => route\.fullPath/,
  '文件查阅页面仍需监听 fullPath 变化以恢复目录和筛选状态。'
)

assert.match(
  browserPage,
  /router\.replace\(\{\s*path: route\.path,\s*query: browserRouteQuery\s*\}\)/,
  '文件查阅页面仍需把目录和筛选状态同步到 query，支持刷新恢复。'
)

assert.match(
  tagsViewStore,
  /view\.meta\?\.tagsViewKeyMode\s*===\s*'path'/,
  '标签页 store 必须支持按 path 作为标签身份，忽略 query 变化。'
)

assert.match(
  routerHelper,
  /DCC_BROWSER_ROUTE_COMPONENT\s*=\s*'dcc\/controlled-file\/browser\/index'/,
  '动态路由生成必须识别文件查阅组件。'
)

assert.match(
  routerHelper,
  /DCC_BROWSER_ROUTE_PATH\s*=\s*'controlled-file\/browser'/,
  '动态路由生成必须识别文件查阅菜单路径。'
)

assert.match(
  routerHelper,
  /tagsViewKeyMode\s*=\s*'path'/,
  '文件查阅动态路由必须设置 path 标签身份，避免 query-only 变化新增同名 tab。'
)

process.stdout.write('PASS: DCC browser single-tab static contract\n')
