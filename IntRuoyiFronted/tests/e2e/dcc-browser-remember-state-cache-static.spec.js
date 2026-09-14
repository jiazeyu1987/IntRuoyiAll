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
const quickFilterHook = readSource('src/hooks/web/useTableQuickFilter.ts')

for (const required of [
  /DCC_BROWSER_STATE_CACHE_SCHEMA_VERSION/,
  /DCC_BROWSER_STATE_STORAGE_PREFIX/,
  /DCC_BROWSER_METADATA_STORAGE_PREFIX/,
  /export interface DccBrowserRememberedState/,
  /export interface DccBrowserMetadataDirectoryNode/,
  /scope\?: BrowserSearchScopeValue/,
  /directoryId\?: number/,
  /lastOpenedDirectoryId\?: number/,
  /pageNo\?: number/,
  /pageSize\?: number/,
  /categoryId\?: number/,
  /status\?: string/,
  /keyword\?: string/,
  /export interface DccBrowserMetadataCache/,
  /categories\?: ControlledFileCategoryVO\[\]/,
  /directoryChildrenByParentKey\?: Record<string, DccBrowserMetadataDirectoryNode\[\]>/,
  /expandedDirectoryIds\?: number\[\]/,
  /buildDccBrowserCacheContext/,
  /readDccBrowserRememberedState/,
  /writeDccBrowserRememberedState/,
  /clearDccBrowserRememberedState/,
  /readDccBrowserMetadataCache/,
  /writeDccBrowserMetadataCache/
]) {
  assert.match(stateCache, required, `state cache helper missing required contract: ${required}`)
}

assert.match(
  stateCache,
  /tenantId[\s\S]*visitTenantId[\s\S]*userId[\s\S]*schemaVersion/,
  'state cache key must be isolated by tenant, visit tenant, user, and schema version'
)
assert.doesNotMatch(stateCache, /exp\s*:/, 'browser remembered state must be long-lived')
assert.doesNotMatch(
  stateCache,
  /viewerToken|previewBlob|Blob|downloadResult|canPreview|canDownload|fileRows|listSnapshot/i,
  'browser cache must not persist controlled file content, tickets, file rows, or permission booleans'
)
assert.doesNotMatch(stateCache, /catch\s*\(\s*\)\s*\{/, 'state cache helper must not swallow storage errors')

for (const required of [
  /from '.\/state-cache'/,
  /buildDccBrowserCacheContext/,
  /readDccBrowserRememberedState/,
  /writeDccBrowserRememberedState/,
  /clearDccBrowserRememberedState/,
  /readDccBrowserMetadataCache/,
  /writeDccBrowserMetadataCache/,
  /hasBrowserRouteQuery/,
  /isDefaultEmptyBrowserRememberedState/,
  /toDirectoryCacheNode/,
  /normalizeDirectoryChildrenCacheRecord/,
  /buildDirectoryTreeFromCacheRecord/,
  /restoreBrowserRouteFromRememberedState/,
  /persistBrowserRememberedState/,
  /restoreBrowserMetadataCache/,
  /persistBrowserMetadataCache/,
  /message\.error\('DCC 受控浏览本地缓存读取失败/,
  /message\.error\('DCC 受控浏览本地缓存写入失败/
]) {
  assert.match(browserPage, required, `browser page missing required state-cache integration: ${required}`)
}

const restoreInitialMatch = browserPage.match(
  /const restoreBrowserInitialRouteState = async \(\) => \{[\s\S]*?\n\}/
)
assert.ok(restoreInitialMatch, 'browser page must keep initial route restore logic')
const restoreInitialSource = restoreInitialMatch[0]
assert.match(
  restoreInitialSource,
  /if \(hasBrowserRouteQuery\(\)\)/,
  'URL query must win over remembered state'
)
assert.match(
  restoreInitialSource,
  /mergeBrowserRouteStateWithRememberedDirectory\(rememberedState\)/,
  'URL query restore must merge remembered directory when directoryId is absent'
)
assert.match(
  restoreInitialSource,
  /restoreBrowserRouteFromRememberedState\(rememberedState\)/,
  'no-query entry must restore remembered state'
)
assert.match(
  browserPage,
  /const restoredFromQueryOrCache = await restoreBrowserInitialRouteState\(\)[\s\S]*if \(restoredFromQueryOrCache\) \{[\s\S]*persistBrowserRememberedState\(\)[\s\S]*\}/,
  'no-query entry without remembered state must gate default-state persistence behind a query/cache restore'
)
assert.match(
  browserPage,
  /if \(isDefaultEmptyBrowserRememberedState\(rememberedState\)\) \{[\s\S]*clearDccBrowserRememberedState\(getBrowserCacheContext\(\)\)[\s\S]*return[\s\S]*\}/,
  'default empty browser state must clear remembered state instead of being persisted'
)
assert.match(
  quickFilterHook,
  /const resetQuickFilter = async \(\) => \{[\s\S]*clearQuickFilterParams\(\)[\s\S]*queryParams\.pageNo = 1[\s\S]*await reload\(\)[\s\S]*\}/,
  'unified filter reset must clear its formal query parameters and reload the first page'
)
assert.match(
  browserPage,
  /const reloadBrowserListAndCommitState = async \(\) => \{[\s\S]*await getList\(\)[\s\S]*await syncRouteFromBrowserState\(\)[\s\S]*persistBrowserRememberedState\(\)/,
  'unified filter reset must reload first, then synchronize and persist the resulting formal browser state'
)
assert.match(
  browserPage,
  /if \(isDefaultEmptyBrowserRememberedState\(rememberedState\)\) \{[\s\S]*clearDccBrowserRememberedState\(getBrowserCacheContext\(\)\)/,
  'persisting a fully reset browser state must remove the remembered-state entry'
)
assert.match(
  browserPage,
  /const loadCategories = async \(\) => \{[\s\S]*restoreBrowserMetadataCache\(\)[\s\S]*getFileCategoryList\(\)[\s\S]*persistBrowserMetadataCache\(\)/,
  'category loading must use local metadata cache for startup and then refresh from the backend'
)
assert.match(
  browserPage,
  /const restoreBrowserMetadataCache = \(\) => \{[\s\S]*normalizeDirectoryChildrenCacheRecord\([\s\S]*buildDirectoryTreeFromCacheRecord\(/,
  'metadata cache restore must rebuild the directory tree from the cached parent-child record'
)
assert.match(
  browserPage,
  /const loadDirectories = async \(\) => \{[\s\S]*restoreBrowserMetadataCache\(\)[\s\S]*getDirectoryTree\(\)[\s\S]*applyDirectoryTree\(rootDirectories\)[\s\S]*persistBrowserMetadataCache\(\)/,
  'directory loading must restore local metadata cache for startup and then persist refreshed backend tree results'
)
assert.match(
  browserPage,
  /directoryChildrenByParentKey: buildDirectoryChildrenCacheRecord\(\),[\s\S]*expandedDirectoryIds: buildExpandedDirectoryIdsCacheRecord\(\)/,
  'browser metadata cache must persist expanded directory ids together with directory children'
)
assert.match(
  browserPage,
  /const expandCollapsedDirectoryNode = \(node: any\) => \{[\s\S]*rememberExpandedDirectoryId\(node\.data\?\.id\)[\s\S]*node\.expand\(\)/,
  'directory name clicks must remember the clicked directory as expanded before selection sync'
)
assert.match(
  browserPage,
  /const handleDirectoryClick = async \([\s\S]*if \(isDirectoryExpandIconClick\(event\)\)[\s\S]*return[\s\S]*expandCollapsedDirectoryNode\(node\)[\s\S]*await selectDirectoryAndLoad\(data\)/,
  'directory name clicks must still select the clicked directory while expand-icon clicks remain the only collapse toggle'
)
assert.match(
  browserPage,
  /@node-expand="handleDirectoryNodeExpand"[\s\S]*@node-collapse="handleDirectoryNodeCollapse"/,
  'browser tree must publish node expand/collapse events for metadata cache updates'
)

console.log('PASS: DCC browser remembered state and metadata cache static contract')
