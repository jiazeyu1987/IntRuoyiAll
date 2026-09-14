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
const workflowApi = readSource('src/api/dcc/controlledFile/workflow.ts')

assert.equal(
  packageJson.scripts['e2e:dcc:browser-search-usability:static'],
  'node tests/e2e/dcc-browser-search-usability-static.spec.js',
  'package.json must expose the DCC browser search usability static contract'
)

assert.match(
  workflowApi,
  /interface ControlledFilePageReqVO[\s\S]*keyword\?: string/,
  'controlled file page request type must expose keyword for backend pagination search'
)

for (const required of [
  /const BROWSER_SEARCH_SCOPE_CURRENT = 'current'/,
  /const BROWSER_SEARCH_SCOPE_GLOBAL = 'global'/,
  /const searchScope = ref<BrowserSearchScope>\(BROWSER_SEARCH_SCOPE_CURRENT\)/,
  /<UnifiedListTemplate[\s\S]*:filter-definitions="dccBrowserQuickFilterDefinitions"[\s\S]*@quick-filter-query="dccBrowserQuickFilter\.applyQuickFilter"/,
  /key: 'keyword'[\s\S]*queryParamKey: 'keyword'[\s\S]*placeholder: '请输入文件名称\/编号'/,
  /key: 'status'[\s\S]*queryParamKey: 'status'[\s\S]*options: BROWSER_STATUS_FILTER_OPTIONS/,
  /key: 'categoryId'[\s\S]*queryParamKey: 'categoryId'/,
  /<el-segmented[\s\S]*v-model="searchScope"[\s\S]*:options="browserSearchScopeOptions"/,
  /label: '当前目录'/,
  /label: '全域'/
]) {
  assert.match(browserPage, required, `browser page missing required search UI contract: ${required}`)
}

for (const required of [
  /buildBrowserRouteQuery/,
  /syncRouteFromBrowserState/,
  /router\.replace/,
  /directoryId/,
  /pageNo/,
  /pageSize/,
  /categoryId/,
  /status/,
  /keyword/,
  /scope/,
  /buildBrowserReturnPath/,
  /buildControlledFileViewerPath\(normalizedId, 'browser', buildBrowserReturnPath\(\)\)/
]) {
  assert.match(browserPage, required, `browser page must persist and reuse list state: ${required}`)
}

assert.match(
  browserPage,
  /isCurrentDirectorySearch[\s\S]*directoryId: selectedDirectoryId\.value/,
  'current-directory search must pass directoryId'
)
assert.doesNotMatch(
  browserPage,
  /isCurrentDirectorySearch[\s\S]*includeDescendantDirectories:\s*true/,
  'current-directory search must not force descendant-directory recursion'
)
assert.match(
  browserPage,
  /onMounted\(async \(\) => \{[\s\S]*restoreBrowserInitialRouteState\(\)[\s\S]*loadCategories\(\)[\s\S]*loadDirectories\(\)[\s\S]*await getList\(\)/,
  'browser page must load the server list after restoring URL or remembered state on mount'
)
assert.match(
  browserPage,
  /browserSearchScopeOptions[\s\S]*label: '全域'[\s\S]*value: BROWSER_SEARCH_SCOPE_GLOBAL/,
  'global search must remain an explicit user-selectable scope'
)
assert.match(
  browserPage,
  /if \(isGlobalSearch\.value\) \{[\s\S]*return '全域受控浏览'/,
  'global search must be explicit in UI state and copy'
)

assert.match(browserPage, /<el-table-column label="文件名称"/, 'title column must be renamed to 文件名称')
assert.doesNotMatch(browserPage, /<el-table-column label="标题"/, 'browser list must not keep the old 标题 column label')
assert.match(browserPage, /class="browser-file-name/, 'file name cell must use dedicated readable styling')
assert.match(browserPage, /<el-tooltip[\s\S]*getBrowserFileNameTooltip/, 'file name cell must show full content in tooltip')
assert.match(browserPage, /-webkit-line-clamp: 2/, 'file name style must clamp to two visible lines')

assert.match(browserPage, /useClipboard/, 'file number copy must use the browser clipboard composable')
assert.match(browserPage, /const copyFileNumber = async/, 'browser page must expose an async file number copy action')
assert.match(
  browserPage,
  /data-testid="dcc-browser-file-number-copy"/,
  'file number column must expose a stable copy button test id'
)
assert.match(browserPage, /message\.success\('文件编号已复制'\)/, 'copy success must show an explicit success message')
assert.match(
  browserPage,
  /message\.error\('文件编号复制失败，请检查浏览器剪贴板权限或浏览器限制。'\)/,
  'copy failure must show an explicit clipboard/browser permission error'
)
assert.match(browserPage, /throw error/, 'copy failure must surface the real browser error')
const searchFilterStart = browserPage.indexOf('const dccBrowserQuickFilterDefinitions =')
const searchFilterEnd = browserPage.indexOf('const canEditMetadata =', searchFilterStart)
const searchReloadStart = browserPage.indexOf('const reloadBrowserListAndCommitState =')
const searchReloadEnd = browserPage.indexOf('const dccBrowserQuickFilter =', searchReloadStart)
assert.notEqual(searchFilterStart, -1, 'browser page must define unified search filters')
assert.notEqual(searchFilterEnd, -1, 'browser page must close unified search filters before permissions')
assert.notEqual(searchReloadStart, -1, 'browser page must define the state-committing search reload')
assert.notEqual(searchReloadEnd, -1, 'browser page must close search reload before initializing the hook')
const browserSearchImplementation = [
  browserPage.slice(searchFilterStart, searchFilterEnd),
  browserPage.slice(searchReloadStart, searchReloadEnd)
].join('\n')
assert.doesNotMatch(
  browserSearchImplementation,
  /mock|placeholder data|默认成功|静默|吞异常|fallback|降级/i,
  'browser search usability change must not introduce mock, fallback, downgrade, silent errors, or default success'
)

console.log('PASS: DCC browser search usability static contract')
