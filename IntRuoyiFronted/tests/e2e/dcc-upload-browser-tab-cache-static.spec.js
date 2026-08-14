const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(repoRoot, '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const readWorkspaceSource = (relativePath) => {
  const absolutePath = path.join(workspaceRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const routerHelper = readSource('src/utils/routerHelper.ts')
const tagsViewStore = readSource('src/store/modules/tagsView.ts')
const appView = readSource('src/layout/components/AppView.vue')
const uploadPage = readSource('src/views/dcc/controlled-file/upload/index.vue')
const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')
const menuSchema = readWorkspaceSource('IntRuoyiBackend/sql/mysql/20260513_dcc_base_schema.sql')

assert.equal(
  packageJson.scripts?.['e2e:dcc:upload-browser-tab-cache:static'],
  'node tests/e2e/dcc-upload-browser-tab-cache-static.spec.js',
  'package.json must expose the DCC upload/browser tab cache static contract'
)

for (const [label, componentName, componentPath, menuPath] of [
  ['文件上传', 'DccControlledFileUpload', 'dcc/controlled-file/upload/index', 'controlled-file/upload'],
  ['受控浏览', 'DccControlledFileBrowser', 'dcc/controlled-file/browser/index', 'controlled-file/browser']
]) {
  assert.ok(menuSchema.includes(`'${label}'`), `menu schema must keep ${label}`)
  assert.ok(menuSchema.includes(`'${componentPath}'`), `menu schema must map ${label} component`)
  assert.ok(menuSchema.includes(`'${componentName}'`), `menu schema must keep ${label} componentName`)
  assert.ok(menuSchema.includes(`'${menuPath}'`), `menu schema must keep ${label} path`)
}

assert.match(
  uploadPage,
  /defineOptions\(\{\s*name:\s*'DccControlledFileUpload'\s*\}\)/,
  '文件上传组件名必须稳定匹配动态菜单 componentName，供 keep-alive include 命中。'
)
assert.match(
  browserPage,
  /defineOptions\(\{\s*name:\s*'DccControlledFileBrowser'\s*\}\)/,
  '受控浏览组件名必须稳定匹配动态菜单 componentName，供 keep-alive include 命中。'
)

assert.match(
  appView,
  /if \(currentRoute\.name && currentRoute\.meta\?\.noCache !== true\)[\s\S]*caches\.add\(String\(currentRoute\.name\)\)/,
  'AppView 必须把 noCache=false 的当前路由加入 keep-alive include。'
)
assert.match(
  appView,
  /<keep-alive :include="getCaches">[\s\S]*<component :is="Component" :key="resolveRouteViewKey\(route\)" \/>[\s\S]*<\/keep-alive>/,
  'AppView 必须用 keep-alive 包裹路由组件。'
)
assert.match(
  appView,
  /const resolveRouteViewKey = \(route: \{ path: string \}\) => route\.path/,
  '路由组件 key 必须按 path 稳定，切换页签时不得按 fullPath 重新创建实例。'
)

assert.match(
  tagsViewStore,
  /const needCache = !item\.meta\?\.noCache[\s\S]*cacheMap\.add\(name\)/,
  'TagsView store 必须用 meta.noCache 控制缓存集合。'
)

assert.match(
  routerHelper,
  /const DCC_UPLOAD_ROUTE_COMPONENT = 'dcc\/controlled-file\/upload\/index'/,
  '动态路由覆盖必须识别文件上传组件。'
)
assert.match(
  routerHelper,
  /const DCC_UPLOAD_ROUTE_PATH = 'controlled-file\/upload'/,
  '动态路由覆盖必须识别文件上传菜单路径。'
)
assert.match(
  routerHelper,
  /const DCC_UPLOAD_BROWSER_CACHE_ROUTE_PATHS = new Set\(\[[\s\S]*DCC_UPLOAD_ROUTE_PATH[\s\S]*DCC_BROWSER_ROUTE_PATH[\s\S]*\]\)/,
  '文件上传和受控浏览必须共享正式缓存路径集合。'
)
assert.match(
  routerHelper,
  /const DCC_UPLOAD_BROWSER_CACHE_ROUTE_COMPONENTS = new Set\(\[[\s\S]*DCC_UPLOAD_ROUTE_COMPONENT[\s\S]*DCC_BROWSER_ROUTE_COMPONENT[\s\S]*\]\)/,
  '文件上传和受控浏览必须共享正式缓存组件集合。'
)
assert.match(
  routerHelper,
  /DCC_UPLOAD_BROWSER_CACHE_ROUTE_PATHS\.has\(routePath\)[\s\S]*DCC_UPLOAD_BROWSER_CACHE_ROUTE_COMPONENTS\.has\(componentPath\)[\s\S]*meta\.tagsViewKeyMode = 'path'[\s\S]*meta\.noCache = false/,
  '文件上传和受控浏览动态菜单路由必须强制 path 标签身份且 noCache=false，避免切回重复加载。'
)

const cacheOverrideStart = routerHelper.indexOf('DCC_UPLOAD_BROWSER_CACHE_ROUTE_PATHS')
const cacheOverrideSnippet = routerHelper.slice(
  Math.max(0, cacheOverrideStart),
  cacheOverrideStart + 1000
)

assert.doesNotMatch(
  cacheOverrideSnippet,
  /mock|placeholder data|fallback|降级|吞异常/i,
  '页签缓存修复不得引入 mock、placeholder、fallback、降级或吞异常。'
)

console.log('PASS: DCC upload/browser tab cache static contract')
