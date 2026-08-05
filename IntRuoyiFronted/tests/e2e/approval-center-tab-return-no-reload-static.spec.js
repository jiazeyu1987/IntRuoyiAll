const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const routes = readSource('src/router/modules/remaining.ts')
const routerTypes = readSource('types/router.d.ts')
const appView = readSource('src/layout/components/AppView.vue')
const tagsViewStore = readSource('src/store/modules/tagsView.ts')
const approvalCenter = readSource('src/views/approval-center/index.vue')

assert.equal(
  packageJson.scripts?.['e2e:approval-center:tab-return-no-reload:static'],
  'node tests/e2e/approval-center-tab-return-no-reload-static.spec.js',
  'package.json must expose the approval center tab return no-reload contract'
)

assert.match(
  approvalCenter,
  /defineOptions\(\{\s*name:\s*'ApprovalCenterWorkbench'\s*\}\)/,
  'approval center must keep a stable component name for keep-alive'
)

for (const routeName of [
  'ApprovalCenterTodo',
  'ApprovalCenterDone',
  'ApprovalCenterMyInitiated',
  'ApprovalCenterCc'
]) {
  const routeStart = routes.indexOf(`name: '${routeName}'`)
  assert.notEqual(routeStart, -1, `missing approval center route: ${routeName}`)
  const routeBlock = routes.slice(routeStart, routeStart + 420)
  assert.match(routeBlock, /noCache:\s*false/, `${routeName} must enable keep-alive caching`)
  assert.match(
    routeBlock,
    /keepAliveName:\s*'ApprovalCenterWorkbench'/,
    `${routeName} must cache by the shared approval center component name`
  )
}

assert.match(
  routerTypes,
  /keepAliveName\?:\s*string/,
  'RouteMeta must declare the explicit keep-alive component identity'
)
assert.match(
  appView,
  /const resolveKeepAliveName = \(route:[\s\S]*route\.meta\?\.keepAliveName \|\| route\.name/,
  'AppView must resolve keep-alive include names from route metadata before route names'
)
assert.match(
  appView,
  /resolveKeepAliveName\(currentRoute\)[\s\S]*caches\.add\(keepAliveName\)/,
  'AppView must include the current route component cache identity'
)
assert.match(
  tagsViewStore,
  /const resolveCachedViewName = \(view:[\s\S]*view\.meta\?\.keepAliveName \|\| view\.name/,
  'TagsView must resolve cached component names from explicit route metadata'
)
assert.match(
  tagsViewStore,
  /const name = resolveCachedViewName\(item\)[\s\S]*cacheMap\.add\(name\)/,
  'TagsView cache collection must retain the approval center component name after switching tabs'
)
assert.match(
  tagsViewStore,
  /const cachedViewName = resolveCachedViewName\(route\)[\s\S]*v === cachedViewName/,
  'explicit TagsView refresh must remove the same component cache identity'
)

assert.match(
  approvalCenter,
  /const approvalCenterRouteInstanceName = String\(route\.name \|\| ''\)/,
  'each cached approval center route instance must remember its own route name'
)
assert.match(
  approvalCenter,
  /let approvalModulesLoaded = false/,
  'approval center must only skip a return load after modules were successfully loaded'
)
assert.match(
  approvalCenter,
  /let approvalLastLoadedRouteStateKey: string \| undefined/,
  'approval center must remember the last successfully loaded semantic route state'
)
assert.match(
  approvalCenter,
  /const buildApprovalCenterRouteStateKey = \(\) =>[\s\S]*JSON\.stringify\(/,
  'approval center must compare route state using an explicit semantic key'
)
assert.match(
  approvalCenter,
  /const shouldKeepApprovalCenterLoadedStateOnRouteReturn = \(targetRouteStateKey: string\) =>[\s\S]*approvalModulesLoaded[\s\S]*approvalLastLoadedRouteStateKey === targetRouteStateKey[\s\S]*!loading\.value/,
  'same-state tab return must only keep a completed successful load'
)

const routeLoadStart = approvalCenter.indexOf('const applyRouteQueryAndLoad = async () => {')
const routeLoadEnd = approvalCenter.indexOf('const handleQuery =', routeLoadStart)
assert.notEqual(routeLoadStart, -1, 'approval center must keep applyRouteQueryAndLoad')
assert.notEqual(routeLoadEnd, -1, 'approval center must keep handleQuery after route loading')
const routeLoadBlock = approvalCenter.slice(routeLoadStart, routeLoadEnd)
assert.match(
  routeLoadBlock,
  /const targetRouteStateKey = buildApprovalCenterRouteStateKey\(\)/
)
assert.match(
  routeLoadBlock,
  /if \(!targetRouteStateKey\) \{[\s\S]*return[\s\S]*\}/,
  'inactive cached approval center instances must ignore unrelated route changes'
)
assert.match(
  routeLoadBlock,
  /if \(shouldKeepApprovalCenterLoadedStateOnRouteReturn\(targetRouteStateKey\)\) \{[\s\S]*return[\s\S]*\}/,
  'same-state tab return must stop before applying route state or requesting the list'
)
assert.ok(
  routeLoadBlock.indexOf('shouldKeepApprovalCenterLoadedStateOnRouteReturn') <
    routeLoadBlock.indexOf('await getList()'),
  'same-state return guard must run before getList()'
)

const getListStart = approvalCenter.indexOf('const getList = async () => {')
const getListEnd = approvalCenter.indexOf('const handlePagination =', getListStart)
assert.notEqual(getListStart, -1, 'approval center must keep getList')
assert.notEqual(getListEnd, -1, 'approval center must keep pagination after getList')
const getListBlock = approvalCenter.slice(getListStart, getListEnd)
assert.match(
  getListBlock,
  /const requestRouteStateKey = buildApprovalCenterRouteStateKey\(\)/,
  'list loading must snapshot the semantic route state before requesting'
)
assert.match(
  getListBlock,
  /approvalLastLoadedRouteStateKey = requestRouteStateKey/,
  'only a successful list request may mark the route state as loaded'
)

const cacheImplementationStart = approvalCenter.indexOf(
  'const buildApprovalCenterRouteStateKey = () =>'
)
const cacheImplementationSnippet = approvalCenter.slice(
  Math.max(0, cacheImplementationStart),
  cacheImplementationStart + 1500
)
assert.doesNotMatch(
  cacheImplementationSnippet,
  /setTimeout|localStorage|sessionStorage|fallback|mock|placeholder|吞异常|降级/i,
  'approval center cache behavior must not use timers, storage fallback, mocks, placeholders, or swallowed errors'
)

console.log('PASS: approval center tab return no-reload static contract')
