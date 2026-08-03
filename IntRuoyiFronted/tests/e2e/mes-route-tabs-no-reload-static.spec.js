const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
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
const routePage = readSource('src/views/mes/pro/route/index.vue')
const batchRecordFormListPage = readSource('src/views/mes/pro/batchrecordformlist/index.vue')
const mesBaseSchema = readWorkspaceSource('IntRuoyiBackend/sql/mysql/ruoyi-vue-pro.sql')
const edhrVisibleTabsMigration = readWorkspaceSource(
  'IntRuoyiBackend/sql/mysql/20260702_mes_edhr_seven_visible_tabs.sql'
)
const edhrMenuMigration = readWorkspaceSource(
  'IntRuoyiBackend/sql/mysql/20260715_mes_edhr_template_config_menu_removal.sql'
)

const assertNoFullPathWatcher = (source, label) => {
  assert.doesNotMatch(
    source,
    /watch\(\s*\(\) => route\.fullPath/,
    `${label}不得用 route.fullPath watcher 在同状态切回时触发恢复加载。`
  )
}

assert.equal(
  packageJson.scripts?.['e2e:mes:route-tabs-no-reload:static'],
  'node tests/e2e/mes-route-tabs-no-reload-static.spec.js',
  'package.json must expose the MES route/batch-record tab no-reload static contract'
)

assert.ok(mesBaseSchema.includes("'工艺流程'"), 'menu schema must keep 工艺流程')
assert.ok(
  mesBaseSchema.includes("'mes/pro/route/index'") && mesBaseSchema.includes("'MesProRoute'"),
  'menu schema must map 工艺流程 to MesProRoute'
)
for (const source of [edhrVisibleTabsMigration, edhrMenuMigration]) {
  assert.ok(source.includes("'批记录表单'"), 'menu migrations must keep 批记录表单')
  assert.ok(
    source.includes("'mes/pro/batchrecordformlist/index'") &&
      source.includes("'MesProBatchRecordFormList'"),
    'menu migrations must map 批记录表单 to MesProBatchRecordFormList'
  )
}

assert.match(
  routePage,
  /defineOptions\(\{\s*name:\s*'MesProRoute'\s*\}\)/,
  '工艺流程组件名必须稳定匹配动态菜单 componentName，供 keep-alive include 命中。'
)
assert.match(
  batchRecordFormListPage,
  /defineOptions\(\{\s*name:\s*'MesProBatchRecordFormList'\s*\}\)/,
  '批记录表单组件名必须稳定匹配动态菜单 componentName，供 keep-alive include 命中。'
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
  /const FORCED_CACHED_TAGS_VIEW_ROUTE_NAMES = new Set\(\[[\s\S]*'MesProRoute'[\s\S]*'MesProBatchRecordFormList'[\s\S]*\]\)/,
  'TagsView store 必须按正式路由名强制缓存工艺流程和批记录表单。'
)
assert.match(
  tagsViewStore,
  /const FORCED_CACHED_TAGS_VIEW_ROUTE_PATHS = new Set\(\[[\s\S]*'mes\/pro\/route'[\s\S]*'mes\/pro\/batch-record-form-list'[\s\S]*\]\)/,
  'TagsView store 必须按正式 path 强制缓存工艺流程和批记录表单。'
)
assert.match(
  tagsViewStore,
  /const shouldForceCacheTagsView = \(item: RouteLocationNormalizedLoaded\) =>[\s\S]*FORCED_CACHED_TAGS_VIEW_ROUTE_NAMES\.has\(name\)[\s\S]*FORCED_CACHED_TAGS_VIEW_ROUTE_PATHS\.has\(normalizedPath\)/,
  'TagsView store 必须提供正式页签强制缓存判定。'
)
assert.match(
  tagsViewStore,
  /const needCache = !item\.meta\?\.noCache \|\| shouldForceCacheTagsView\(item\)[\s\S]*cacheMap\.add\(name\)/,
  'TagsView store 必须把正式强制缓存页签加入 keep-alive include。'
)

assert.match(
  routerHelper,
  /const MES_ROUTE_BATCH_RECORD_TAB_CACHE_ROUTE_COMPONENTS = new Set\(\[[\s\S]*MES_PRO_ROUTE_LIST_COMPONENT[\s\S]*MES_PRO_BATCH_RECORD_FORM_LIST_COMPONENT[\s\S]*\]\)/,
  '工艺流程和批记录表单必须共享正式缓存组件集合。'
)
assert.match(
  routerHelper,
  /const MES_ROUTE_BATCH_RECORD_TAB_CACHE_ROUTE_PATHS = new Set\(\[[\s\S]*MES_PRO_ROUTE_MENU_PATHS[\s\S]*MES_PRO_BATCH_RECORD_FORM_LIST_ROUTE_PATHS[\s\S]*\]\)/,
  '工艺流程和批记录表单必须共享正式缓存路径集合。'
)
assert.match(
  routerHelper,
  /MES_ROUTE_BATCH_RECORD_TAB_CACHE_ROUTE_PATHS\.has\(routePath\)[\s\S]*MES_ROUTE_BATCH_RECORD_TAB_CACHE_ROUTE_COMPONENTS\.has\(componentPath\)[\s\S]*meta\.tagsViewKeyMode = 'path'/,
  '工艺流程和批记录表单必须强制 path 页签身份，避免 query-only 变化生成新页签。'
)
assert.match(
  routerHelper,
  /MES_ROUTE_BATCH_RECORD_TAB_CACHE_ROUTE_PATHS\.has\(routePath\)[\s\S]*MES_ROUTE_BATCH_RECORD_TAB_CACHE_ROUTE_COMPONENTS\.has\(componentPath\)[\s\S]*meta\.noCache = false/,
  '工艺流程和批记录表单必须强制 noCache=false，避免切回页签重新挂载首屏。'
)

assertNoFullPathWatcher(routePage, '工艺流程页面')
assertNoFullPathWatcher(batchRecordFormListPage, '批记录表单页面')

assert.match(
  routePage,
  /const MES_PRO_ROUTE_LIST_PATH = '\/mes\/pro\/route'[\s\S]*const isMesProRouteListPath = \(\) => route\.path === MES_PRO_ROUTE_LIST_PATH/,
  '工艺流程页面必须声明正式列表路径守卫，避免 keep-alive 后台 watcher 响应其它页签路由。'
)
assert.match(
  routePage,
  /const buildMesProRouteListStateKey = \(\) =>[\s\S]*code: typeof route\.query\.code === 'string'[\s\S]*name: typeof route\.query\.name === 'string'[\s\S]*openId: typeof route\.query\.openId === 'string'/,
  '工艺流程页面必须把有效 query 状态归一化成稳定 key，避免同状态页签切回被数组 watcher 误判。'
)
assert.match(
  routePage,
  /let mesProRouteListLastLoadedStateKey = ''[\s\S]*const shouldKeepMesProRouteListLoadedState = \(targetStateKey: string\) =>[\s\S]*mesProRouteListHasLoadedRouteState\.value[\s\S]*mesProRouteListLastLoadedStateKey === targetStateKey[\s\S]*!loading\.value/,
  '工艺流程页面必须记录最后成功加载的路由状态，并在列表未加载中时复用 keep-alive 状态。'
)
assert.match(
  routePage,
  /const loadListFromRoute = async \(\) => \{[\s\S]*const targetStateKey = buildMesProRouteListStateKey\(\)[\s\S]*await getList\(\)[\s\S]*mesProRouteListLastLoadedStateKey = targetStateKey[\s\S]*mesProRouteListHasLoadedRouteState\.value = true/,
  '工艺流程页面必须只在列表请求成功后标记当前路由状态已加载。'
)
assert.match(
  routePage,
  /watch\(\s*\(\) => \[route\.query\.code, route\.query\.name, route\.query\.openId\][\s\S]*if \(!isMesProRouteListPath\(\)\) \{[\s\S]*return[\s\S]*\}[\s\S]*const targetStateKey = buildMesProRouteListStateKey\(\)[\s\S]*if \(shouldKeepMesProRouteListLoadedState\(targetStateKey\)\) \{[\s\S]*return[\s\S]*\}[\s\S]*await loadListFromRoute\(\)/,
  '工艺流程页面 query watcher 必须先确认正式列表路径和有效状态变化，再触发列表加载。'
)

assert.match(
  batchRecordFormListPage,
  /const BATCH_RECORD_FORM_LIST_PATH = '\/mes\/pro\/batch-record-form-list'[\s\S]*const isBatchRecordFormListPath = \(\) => route\.path === BATCH_RECORD_FORM_LIST_PATH/,
  '批记录表单页面必须声明正式列表路径守卫，避免 keep-alive 后台 watcher 响应其它页签路由。'
)
assert.match(
  batchRecordFormListPage,
  /const buildBatchRecordFormListRouteStateKey = \(\) =>[\s\S]*reportId: normalizeRouteQueryText\(route\.query\.reportId\)[\s\S]*action: normalizeRouteQueryText\(route\.query\.action\)[\s\S]*mode: normalizeRouteQueryText\(route\.query\.mode\)/,
  '批记录表单页面必须把 reportId/action/mode 归一化成稳定 key，避免同状态页签切回误刷新。'
)
assert.match(
  batchRecordFormListPage,
  /let batchRecordFormListLastLoadedRouteStateKey = ''[\s\S]*const shouldKeepBatchRecordFormListLoadedState = \(targetStateKey: string\) =>[\s\S]*batchRecordFormListHasLoadedRouteState\.value[\s\S]*batchRecordFormListLastLoadedRouteStateKey === targetStateKey[\s\S]*!listLoading\.value/,
  '批记录表单页面必须记录最后成功加载的路由状态，并在列表未加载中时复用 keep-alive 状态。'
)
assert.match(
  batchRecordFormListPage,
  /const getList = async \(\) => \{[\s\S]*const targetRouteStateKey = buildBatchRecordFormListRouteStateKey\(\)[\s\S]*deferRecordFormSecondaryLoad\(nextList, nextSelected, requestSerial\)[\s\S]*batchRecordFormListLastLoadedRouteStateKey = targetRouteStateKey[\s\S]*batchRecordFormListHasLoadedRouteState\.value = true/,
  '批记录表单页面必须只在非过期列表请求成功后标记当前路由状态已加载。'
)
assert.match(
  batchRecordFormListPage,
  /watch\(\s*\(\) => \[route\.query\.reportId, route\.query\.action, route\.query\.mode\] as const,[\s\S]*if \(!isBatchRecordFormListPath\(\)\) \{[\s\S]*return[\s\S]*\}[\s\S]*if \(isDesignerMode\.value\)[\s\S]*const targetRouteStateKey = buildBatchRecordFormListRouteStateKey\(\)[\s\S]*if \(shouldKeepBatchRecordFormListLoadedState\(targetRouteStateKey\)\) \{[\s\S]*return[\s\S]*\}[\s\S]*await getList\(\)/,
  '批记录表单页面 query watcher 必须先确认正式列表路径、designer mode 和有效状态变化，再触发列表加载。'
)

const cacheOverrideStart = routerHelper.indexOf('MES_ROUTE_BATCH_RECORD_TAB_CACHE_ROUTE_PATHS')
const cacheOverrideSnippet = routerHelper.slice(
  Math.max(0, cacheOverrideStart),
  cacheOverrideStart + 1200
)
assert.doesNotMatch(
  cacheOverrideSnippet,
  /setTimeout|localStorage|sessionStorage|fallback|mock|placeholder|吞异常|降级/i,
  '页签切回不刷新修复不得使用计时器、存储结果兜底、mock、placeholder、吞异常或降级。'
)

console.log('PASS: MES route/batch-record tab no-reload static contract')
