const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(
  path.resolve(process.cwd(), 'src/views/dcc/controlled-file/browser/index.vue'),
  'utf8'
)

assert.match(
  source,
  /const DCC_BROWSER_ROUTE_PATH = '\/dcc\/controlled-file\/browser'/,
  '文件查阅应声明自身路由常量，避免离开到文件提交页时按其它路由 query 同步并清空目录。'
)

const watcherStart = source.indexOf('watch(\n  () => route.fullPath')
assert.notEqual(watcherStart, -1, '文件查阅必须监听路由变化以支持返回页面恢复状态。')
const watcherEnd = source.indexOf('\nconst handleQuery', watcherStart)
assert.notEqual(watcherEnd, -1, '路由 watcher 后应继续声明查询处理函数。')
const watcherSource = source.slice(watcherStart, watcherEnd)

assert.match(
  watcherSource,
  /if \(route\.path !== DCC_BROWSER_ROUTE_PATH\) \{\s*return\s*\}/,
  'route watcher 必须在当前路由不是文件查阅页时直接返回，不能在离开页面时清空并持久化 selectedDirectoryId。'
)

assert.match(
  watcherSource,
  /await restoreBrowserDirectoryTreeAndList\(\)/,
  '返回文件查阅页时必须调用统一恢复流程，重新从 URL 或缓存恢复目录状态、打开目录树并加载右侧列表。'
)

assert.doesNotMatch(
  watcherSource,
  /syncQueryFromRoute\(\)[\s\S]*persistBrowserRememberedState\(\)/,
  'route watcher 不得继续用裸 syncQueryFromRoute 处理所有路由变化，否则切到文件提交页会把目录状态清空写回缓存。'
)

const restoreTreeAndListStart = source.indexOf(
  'const restoreBrowserDirectoryTreeAndList = async () => {'
)
assert.notEqual(
  restoreTreeAndListStart,
  -1,
  '文件查阅必须提供返回页面时复用的目录树与列表恢复流程。'
)
const restoreTreeAndListEnd = source.indexOf('\nconst buildBrowserReturnPath', restoreTreeAndListStart)
assert.notEqual(restoreTreeAndListEnd, -1, '恢复流程后应继续声明返回路径函数。')
const restoreTreeAndListSource = source.slice(restoreTreeAndListStart, restoreTreeAndListEnd)

assert.match(
  restoreTreeAndListSource,
  /const restoredFromQueryOrCache = await restoreBrowserInitialRouteState\(\)[\s\S]*await loadDirectories\(\)[\s\S]*await getList\(\)/,
  '返回文件查阅页时必须重新从 URL 或缓存恢复目录状态、打开目录树并加载右侧列表。'
)

assert.match(
  restoreTreeAndListSource,
  /if \(restoredFromQueryOrCache\) \{\s*persistBrowserRememberedState\(\)\s*\}/,
  '返回文件查阅页完成恢复后，应只持久化已恢复的目录状态。'
)

console.log('PASS: dcc browser route return restores selected directory list static contract')
