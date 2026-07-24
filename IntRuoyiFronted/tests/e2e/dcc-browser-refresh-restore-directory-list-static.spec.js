const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(
  path.resolve(process.cwd(), 'src/views/dcc/controlled-file/browser/index.vue'),
  'utf8'
)

assert.doesNotMatch(
  source,
  /async\s+function\s+withBrowserRouteSyncGuard\s*<|const\s+withBrowserRouteSyncGuard\s*=\s*async\s*<\w+>/,
  'Vue SFC 内的路由同步保护器不得使用 async 泛型声明，否则 Vite eslint transform 会在真实刷新时返回 500。'
)
assert.match(
  source,
  /async function withBrowserRouteSyncGuard\(action: \(\) => Promise<unknown>\)/,
  '路由同步保护器应使用非泛型函数声明，保证 Vite 运行态可解析。'
)

const syncQueryFromRouteMatch = source.match(/const syncQueryFromRoute = \(\) => \{[\s\S]*?\n\}/)
assert.ok(syncQueryFromRouteMatch, '文件查阅必须保留路由状态同步函数。')
assert.match(
  syncQueryFromRouteMatch[0],
  /clearSelectedDirectory\(\)[\s\S]*applyBrowserRememberedState\(buildBrowserRememberedStateFromRoute\(\)\)/,
  '路由同步会先清空目录选择，再按当前 URL query 应用目录状态。'
)

const restoreInitialStart = source.indexOf('const restoreBrowserInitialRouteState = async () => {')
assert.notEqual(restoreInitialStart, -1, '文件查阅必须保留初始化恢复逻辑。')
const restoreInitialEnd = source.indexOf('\nconst persistBrowserRememberedState', restoreInitialStart)
assert.notEqual(restoreInitialEnd, -1, '初始化恢复逻辑后必须继续声明状态持久化函数。')
const restoreInitialSource = source.slice(restoreInitialStart, restoreInitialEnd)

const rememberedNoQueryBranchStart = restoreInitialSource.indexOf(
  'if (await restoreBrowserRouteFromRememberedState(rememberedState)) {'
)
assert.notEqual(
  rememberedNoQueryBranchStart,
  -1,
  '无 URL query 入口必须从上次查看目录恢复路由和页面状态。'
)
const noRememberedStateFallbackStart = restoreInitialSource.indexOf(
  'syncQueryFromRoute()',
  rememberedNoQueryBranchStart
)
assert.notEqual(noRememberedStateFallbackStart, -1, '无缓存默认入口必须继续同步当前路由状态。')
const rememberedNoQueryBranch = restoreInitialSource.slice(
  rememberedNoQueryBranchStart,
  noRememberedStateFallbackStart
)

assert.doesNotMatch(
  restoreInitialSource,
  /if \(await restoreBrowserRouteFromRememberedState\(rememberedState\)\) \{\s*syncQueryFromRoute\(\)/,
  '从缓存恢复上次目录的分支不能立即调用 syncQueryFromRoute()，否则会用刷新前的空 route query 清掉目录选择。'
)
assert.match(
  source,
  /const restoreBrowserRouteFromRememberedState = async \([\s\S]*withBrowserRouteSyncGuard\(\(\) =>[\s\S]*router\.replace\([\s\S]*applyBrowserRememberedState\(restoredState\)/,
  '从缓存恢复上次目录时必须通过 route guard 暂时抑制 route watcher，避免 router.replace 触发后又把 restored state 清空。'
)
assert.match(
  restoreInitialSource,
  /if \(hasBrowserRouteQuery\(\)\) \{[\s\S]*withBrowserRouteSyncGuard\(\(\) =>[\s\S]*router\.replace\([\s\S]*applyBrowserRememberedState\(mergedRouteState\)/,
  '带 URL query 恢复目录时也必须通过 route guard 暂时抑制 route watcher，避免 merged route replace 后再次覆盖目录选择。'
)
assert.doesNotMatch(
  rememberedNoQueryBranch,
  /syncQueryFromRoute\(\)/,
  '从缓存恢复上次目录后，不得再用尚未稳定的 route query 覆盖 selectedDirectoryId，否则刷新后列表会显示“请选择目录”。'
)
assert.match(
  rememberedNoQueryBranch,
  /persistBrowserRememberedState\(\)[\s\S]*return true/,
  '从缓存恢复上次目录后，应直接保留已应用的恢复状态，并让 mounted 阶段用该目录加载列表。'
)

const mountedMatch = source.match(/onMounted\(async \(\) => \{[\s\S]*?\n\}\)/)
assert.ok(mountedMatch, '文件查阅必须保留 mounted 初始化流程。')
assert.match(
  mountedMatch[0],
  /await Promise\.all\(\[loadCategories\(\), loadDirectories\(\)\]\)[\s\S]*await getList\(\)/,
  'mounted 阶段必须先恢复目录树和选中目录，再加载列表。'
)

const loadDirectoriesMatch = source.match(/const loadDirectories = async \(\) => \{[\s\S]*?\n\}/)
assert.ok(loadDirectoriesMatch, '文件查阅必须保留目录树加载逻辑。')
assert.match(
  loadDirectoriesMatch[0],
  /await openRememberedDirectoryInTree\(\)/,
  '目录树加载完成后必须重新打开并高亮上次目录，让右侧列表能使用 selectedDirectoryId。'
)

console.log('PASS: dcc browser refresh restores directory list static contract')
