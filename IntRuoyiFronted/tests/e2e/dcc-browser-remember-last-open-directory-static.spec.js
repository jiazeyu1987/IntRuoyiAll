const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')
const stateCache = readSource('src/views/dcc/controlled-file/browser/state-cache.ts')

assert.match(
  stateCache,
  /lastOpenedDirectoryId\?: number/,
  'remembered browser state must keep the last opened directory separately from the current route selection'
)
assert.match(
  stateCache,
  /lastOpenedDirectoryId: normalizePositiveNumber\(\s*source\.lastOpenedDirectoryId,\s*'lastOpenedDirectoryId'\s*\)/,
  'remembered browser state must validate the last opened directory id before restoring it'
)
assert.match(
  browserPage,
  /lastOpenedDirectoryId: selectedDirectoryId\.value/,
  'browser page must persist the selected directory as the last opened directory'
)
assert.match(
  browserPage,
  /const resolveRememberedDirectoryId = \([\s\S]*rememberedState\?\.lastOpenedDirectoryId[\s\S]*rememberedState\?\.directoryId/,
  'initial route restore must resolve the remembered directory from lastOpenedDirectoryId first, then legacy directoryId'
)
assert.match(
  browserPage,
  /const mergeBrowserRouteStateWithRememberedDirectory = \([\s\S]*const rememberedDirectoryId = resolveRememberedDirectoryId\(rememberedState\)[\s\S]*rememberedDirectoryId[\s\S]*routeState\.directoryId = rememberedDirectoryId/,
  'route query without an explicit directory must merge the remembered last opened directory'
)
assert.match(
  browserPage,
  /const restoreBrowserRouteFromRememberedState = async \([\s\S]*const rememberedDirectoryId = resolveRememberedDirectoryId\(rememberedState\)[\s\S]*directoryId: rememberedDirectoryId/,
  'no-query entry must restore a route state that opens the remembered last directory'
)
assert.match(
  browserPage,
  /const applyBrowserRememberedState = \(state: DccBrowserRememberedState\) => \{[\s\S]*selectedDirectoryId\.value = state\.directoryId[\s\S]*queryParams\.pageNo = state\.pageNo \|\| 1[\s\S]*searchScope\.value = state\.scope \|\| BROWSER_SEARCH_SCOPE_CURRENT/,
  'restored browser state must be applied directly to the query model before list loading'
)
assert.match(
  browserPage,
  /const restoreBrowserRouteFromRememberedState = async \([\s\S]*const restoredState: DccBrowserRememberedState = \{[\s\S]*directoryId: rememberedDirectoryId[\s\S]*lastOpenedDirectoryId: rememberedDirectoryId[\s\S]*withBrowserRouteSyncGuard\(\(\) =>[\s\S]*router\.replace\([\s\S]*applyBrowserRememberedState\(restoredState\)/,
  'no-query remembered directory restore must not rely on async route reactivity before loading the file list'
)
assert.match(
  browserPage,
  /const restoreBrowserInitialRouteState = async \(\) => \{[\s\S]*const mergedRouteState = mergeBrowserRouteStateWithRememberedDirectory\(rememberedState\)[\s\S]*withBrowserRouteSyncGuard\(\(\) =>[\s\S]*router\.replace\([\s\S]*applyBrowserRememberedState\(mergedRouteState\)[\s\S]*persistBrowserRememberedState\(\)/,
  'route-query restore must apply the merged directory state directly before the mounted list request'
)
assert.match(
  browserPage,
  /if \(await restoreBrowserRouteFromRememberedState\(rememberedState\)\) \{\s*persistBrowserRememberedState\(\)\s*return true\s*\}/,
  'no-query remembered directory restore must persist the directly applied restored state before returning'
)

assert.doesNotMatch(
  browserPage,
  /if \(await restoreBrowserRouteFromRememberedState\(rememberedState\)\) \{\s*syncQueryFromRoute\(\)/,
  'no-query remembered directory restore must not overwrite the directly applied restored state with stale route data'
)
assert.match(
  browserPage,
  /const openRememberedDirectoryInTree = async \(\) => \{[\s\S]*resolveSelectedDirectory\(\)[\s\S]*rememberDirectoryAncestorChain\(selectedDirectoryId\.value\)[\s\S]*syncDirectoryTreeExpandedState\(\)[\s\S]*directoryTreeRef\.value\?\.setCurrentKey\(selectedDirectoryId\.value\)/,
  'after directories load, the remembered directory must be resolved, ancestor-expanded, and highlighted in the tree'
)
assert.match(
  browserPage,
  /await openRememberedDirectoryInTree\(\)/,
  'directory loading must call the shared remembered-directory opener'
)

console.log('PASS: DCC browser remembers and opens the last directory static contract')
