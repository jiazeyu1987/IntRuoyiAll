const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractBetween = (source, start, end) => {
  const startIndex = source.indexOf(start)
  assert.notEqual(startIndex, -1, `missing source marker: ${start}`)
  const endIndex = source.indexOf(end, startIndex + start.length)
  assert.notEqual(endIndex, -1, `missing source marker: ${end}`)
  return source.slice(startIndex, endIndex)
}

const assertBefore = (source, left, right, message) => {
  const leftIndex = source.indexOf(left)
  const rightIndex = source.indexOf(right)
  assert.notEqual(leftIndex, -1, `missing left marker: ${left}`)
  assert.notEqual(rightIndex, -1, `missing right marker: ${right}`)
  assert.ok(leftIndex < rightIndex, message)
}

const packageJson = JSON.parse(readSource('package.json'))
const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')
const quickFilterHook = readSource('src/hooks/web/useTableQuickFilter.ts')
const pagination = readSource('src/components/Pagination/index.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:browser-state-consistency:static'],
  'node tests/e2e/dcc-browser-state-consistency-static.spec.js',
  'package.json must expose the DCC browser state consistency static contract'
)

assert.match(
  browserPage,
  /const browserListErrorMessage = ref<string>\(''\)/,
  'browser page must keep an explicit list error state instead of showing stale rows as valid results.'
)

const getListBlock = extractBetween(
  browserPage,
  'const getList = async () => {',
  'const dccBrowserQuickFilter = useTableQuickFilter'
)
assert.match(
  getListBlock,
  /browserListErrorMessage\.value = ''[\s\S]*await getControlledFileBrowserPage/,
  'getList must clear the previous list error only when starting a new formal load.'
)
assert.match(
  getListBlock,
  /catch \(error\) \{[\s\S]*list\.value = \[\][\s\S]*total\.value = 0[\s\S]*clearBrowserLoadedListState\(\)[\s\S]*browserListErrorMessage\.value = resolveBrowserErrorMessage\(error, '受控浏览列表加载失败，请重新登录或刷新后重试。'\)[\s\S]*throw error[\s\S]*\}/,
  'getList must clear stale rows, mark the list state invalid, expose the real error, and rethrow on failed loads.'
)

const tableEmptyTextBlock = extractBetween(
  browserPage,
  'const tableEmptyText = computed(() => {',
  'const selectedDirectoryPath = computed'
)
assert.match(
  tableEmptyTextBlock,
  /browserListErrorMessage\.value/,
  'table empty title must show the explicit failed-load/stale-data state.'
)
const tableEmptyHintBlock = extractBetween(
  browserPage,
  'const tableEmptyHint = computed(() => {',
  'const batchRecognitionScopeLabel = computed'
)
assert.match(
  tableEmptyHintBlock,
  /browserListErrorMessage\.value/,
  'table empty hint must explain that data was cleared because the last query failed.'
)

const reloadBrowserListAndCommitStateBlock = extractBetween(
  browserPage,
  'const reloadBrowserListAndCommitState = async',
  'const dccBrowserQuickFilter = useTableQuickFilter'
)
assertBefore(
  reloadBrowserListAndCommitStateBlock,
  'await getList()',
  'await syncRouteFromBrowserState()',
  'unified quick filtering must only sync URL and remembered state after the list request succeeds.'
)
assert.match(
  browserPage,
  /@quick-filter-query="dccBrowserQuickFilter\.applyQuickFilter"/,
  'the unified browser filter must apply through the state-consistent quick-filter hook.'
)

const handleSearchScopeChangeBlock = extractBetween(
  browserPage,
  'const handleSearchScopeChange = async',
  'const refreshDirectories = async'
)
assertBefore(
  handleSearchScopeChangeBlock,
  'await getList()',
  'await syncRouteFromBrowserState()',
  'scope changes must only commit URL state after the list request succeeds.'
)

const handlePaginationBlock = extractBetween(
  browserPage,
  'const handlePagination = async',
  'const openPreview ='
)
assert.match(
  handlePaginationBlock,
  /payload\?: BrowserPaginationPayload/,
  'browser pagination handler must receive the intended target page/limit from the Pagination event.'
)
assert.match(
  handlePaginationBlock,
  /const previousRouteState = buildBrowserRememberedStateFromRoute\(\)[\s\S]*const previousPageNo = previousRouteState\.pageNo \|\| 1[\s\S]*const previousPageSize = resolveBrowserPageSize\(previousRouteState\.pageSize\)/,
  'browser pagination must snapshot the last successfully committed route page before applying a jumper target.'
)
assert.match(
  handlePaginationBlock,
  /catch \(error\) \{[\s\S]*queryParams\.pageNo = previousPageNo[\s\S]*queryParams\.pageSize = previousPageSize[\s\S]*message\.error\('分页跳转失败，已恢复当前页码，请重新登录或刷新后重试。'\)[\s\S]*throw error[\s\S]*\}/,
  'browser pagination must roll back the input page and surface the failure when the target page load fails.'
)

const openPreviewBlock = extractBetween(
  browserPage,
  'const openPreview =',
  'const openDetail ='
)
assert.match(
  openPreviewBlock,
  /const previewWindow = window\.open/,
  'preview must keep the opened window handle so popup-blocker failures can be detected.'
)
assert.match(
  openPreviewBlock,
  /if \(!previewWindow\) \{[\s\S]*message\.error\('预览窗口打开失败，请检查浏览器弹窗拦截设置。'\)[\s\S]*return[\s\S]*\}/,
  'preview must show an explicit popup-blocker failure instead of silently doing nothing.'
)
assert.doesNotMatch(
  openPreviewBlock,
  /catch\s*\([^)]*\)\s*\{\s*\}/,
  'preview must not swallow failures silently.'
)

const applyConditionTabsFilterBlock = extractBetween(
  quickFilterHook,
  'const applyConditionTabsFilter = async () => {',
  'const applyQuickFilter = async () => {'
)
assert.match(
  applyConditionTabsFilterBlock,
  /const previousConditions = cloneMultiFilterConditions\(state\.appliedConditions\)/,
  'condition-tab quick filter must snapshot the last successfully applied labels before reload.'
)
assert.match(
  applyConditionTabsFilterBlock,
  /if \(!reloadSucceeded\) \{[\s\S]*restoreQuickFilterParams\(previousQueryParams\)[\s\S]*state\.conditions = cloneMultiFilterConditions\(previousConditions\)[\s\S]*state\.activeConditionId = previousActiveConditionId[\s\S]*\}/,
  'condition-tab quick filter must roll back draft labels when reload fails.'
)

assert.match(
  pagination,
  /@keydown\.enter\.capture="handleJumperEnter"/,
  'Pagination must explicitly handle jumper Enter instead of relying only on Element Plus current-change.'
)
assert.match(
  pagination,
  /const handleJumperEnter = \(event: KeyboardEvent\) =>/,
  'Pagination must implement a typed jumper Enter handler.'
)
assert.match(
  pagination,
  /closest\('\.el-pagination__jump'\)/,
  'Pagination Enter handler must only react to the jumper input.'
)
assert.match(
  pagination,
  /message\.warning\('请输入有效页码'\)/,
  'Pagination must restore invalid jumper input and tell the user what happened.'
)
assert.match(
  pagination,
  /emit\('pagination', \{ page: nextPage, limit: pageSize\.value \}\)/,
  'Pagination jumper Enter must emit the target page and current page size for parent rollback/commit.'
)

const stateConsistencyImplementation = [
  getListBlock,
  reloadBrowserListAndCommitStateBlock,
  handleSearchScopeChangeBlock,
  handlePaginationBlock,
  openPreviewBlock,
  applyConditionTabsFilterBlock,
  pagination
].join('\n')

assert.doesNotMatch(
  stateConsistencyImplementation,
  /mock|placeholder data|默认成功|静默|吞异常|fallback|降级/i,
  'state consistency fix must not introduce mock, fallback, downgrade, silent errors, or default success'
)

console.log('PASS: DCC browser state consistency static contract')
