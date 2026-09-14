const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')

assert.equal(
  packageJson.scripts?.['e2e:dcc:browser-tab-return-no-reload:static'],
  'node tests/e2e/dcc-browser-tab-return-no-reload-static.spec.js',
  'package.json must expose the DCC browser tab return no-reload static contract'
)

assert.match(
  browserPage,
  /watch\(\s*\(\) => route\.fullPath[\s\S]*restoreBrowserDirectoryTreeAndList\(\)/,
  '受控浏览仍需监听 route.fullPath，以便真正的目录、筛选、分页状态变化能够恢复页面。'
)

assert.match(
  browserPage,
  /let browserDirectoriesLoaded = false/,
  '受控浏览必须显式记录目录树是否已由正式 loadDirectories 成功加载。'
)

assert.match(
  browserPage,
  /let browserLastLoadedRouteStateKey: string \| undefined/,
  '受控浏览必须记录列表最后一次成功加载对应的正式路由状态 key。'
)

assert.match(
  browserPage,
  /const buildBrowserRouteStateKey = \([\s\S]*JSON\.stringify\(query\)/,
  '受控浏览必须用正式 route query 构造可比较的有效状态 key。'
)

assert.match(
  browserPage,
  /const buildBrowserRouteRestoreStateKey = \(\) =>[\s\S]*buildBrowserRouteStateKey\(buildBrowserRouteRestoreQuery\(\)\)/,
  '受控浏览必须按 restoreBrowserInitialRouteState 的同一口径计算切回目标状态 key。'
)

assert.match(
  browserPage,
  /const shouldKeepBrowserLoadedStateOnRouteReturn = \(targetRouteStateKey: string\) =>[\s\S]*browserDirectoriesLoaded[\s\S]*browserLastLoadedRouteStateKey === targetRouteStateKey[\s\S]*!directoryLoading\.value[\s\S]*!loading\.value/,
  '受控浏览必须只在目录树和列表已按同一状态成功加载且当前无加载中请求时跳过恢复加载。'
)

const restoreStart = browserPage.indexOf('const restoreBrowserDirectoryTreeAndList = async () => {')
const restoreEnd = browserPage.indexOf('const buildBrowserReturnPath =', restoreStart)
assert.notEqual(restoreStart, -1, 'browser page must define restoreBrowserDirectoryTreeAndList')
assert.notEqual(restoreEnd, -1, 'browser page must keep buildBrowserReturnPath after restoreBrowserDirectoryTreeAndList')
const restoreBlock = browserPage.slice(restoreStart, restoreEnd)

assert.match(
  restoreBlock,
  /const targetRouteStateKey = buildBrowserRouteRestoreStateKey\(\)/,
  'route restore must compute the target state key before applying restored state.'
)
assert.match(
  restoreBlock,
  /if \(shouldKeepBrowserLoadedStateOnRouteReturn\(targetRouteStateKey\)\) \{[\s\S]*return[\s\S]*\}/,
  'same-state tab return must return before directory/list reload.'
)
assert.ok(
  restoreBlock.indexOf('shouldKeepBrowserLoadedStateOnRouteReturn') <
    restoreBlock.indexOf('await loadDirectories()'),
  'same-state return guard must run before loadDirectories().'
)
assert.ok(
  restoreBlock.indexOf('shouldKeepBrowserLoadedStateOnRouteReturn') <
    restoreBlock.indexOf('await getList()'),
  'same-state return guard must run before getList().'
)

const getListStart = browserPage.indexOf('const getList = async () => {')
const getListEnd = browserPage.indexOf('const dccBrowserQuickFilter =', getListStart)
assert.notEqual(getListStart, -1, 'browser page must define getList')
assert.notEqual(getListEnd, -1, 'browser page must keep quick filter after getList')
const getListBlock = browserPage.slice(getListStart, getListEnd)
assert.match(
  getListBlock,
  /const requestRouteStateKey = buildBrowserRouteStateKey\(\)/,
  'getList must snapshot the effective state it is loading.'
)
assert.match(
  getListBlock,
  /markBrowserListLoadedForState\(requestRouteStateKey\)/,
  'getList must mark successful empty and non-empty loads with the request state key.'
)

const loadDirectoriesStart = browserPage.indexOf('const loadDirectories = async () => {')
const loadDirectoriesEnd = browserPage.indexOf('const loadCategories = async () => {', loadDirectoriesStart)
assert.notEqual(loadDirectoriesStart, -1, 'browser page must define loadDirectories')
assert.notEqual(loadDirectoriesEnd, -1, 'browser page must keep loadCategories after loadDirectories')
const loadDirectoriesBlock = browserPage.slice(loadDirectoriesStart, loadDirectoriesEnd)
assert.match(
  loadDirectoriesBlock,
  /browserDirectoriesLoaded = false[\s\S]*getDirectoryTree\(\)[\s\S]*browserDirectoriesLoaded = true/,
  'loadDirectories must only mark the directory tree loaded after the formal directory tree request succeeds.'
)

const noReloadImplementationStart = browserPage.indexOf('let browserDirectoriesLoaded = false')
const noReloadImplementationSnippet = browserPage.slice(
  Math.max(0, noReloadImplementationStart),
  noReloadImplementationStart + 1800
)
assert.doesNotMatch(
  noReloadImplementationSnippet,
  /setTimeout|localStorage|sessionStorage|fallback|mock|placeholder|吞异常|降级/i,
  'same-state return fix must not use timers, storage result fallback, mock data, placeholder data, downgrade, or swallowed errors.'
)

console.log('PASS: DCC browser tab return no-reload static contract')
