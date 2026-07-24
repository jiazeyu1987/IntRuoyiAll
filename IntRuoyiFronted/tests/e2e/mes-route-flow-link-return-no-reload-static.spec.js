const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const routeConfigPath = path.join(repoRoot, 'src', 'router', 'modules', 'remaining.ts')
const appViewPath = path.join(repoRoot, 'src', 'layout', 'components', 'AppView.vue')
const routeFormContentPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'route',
  'RouteFormContent.vue'
)
const routeEditPagePath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'route',
  'RouteEditPage.vue'
)

const routeConfig = fs.readFileSync(routeConfigPath, 'utf8')
const appView = fs.readFileSync(appViewPath, 'utf8')
const routeFormContent = fs.readFileSync(routeFormContentPath, 'utf8')
const routeEditPage = fs.readFileSync(routeEditPagePath, 'utf8')

function assertIncludes(source, expected, label) {
  assert(source.includes(expected), `${label}: expected ${JSON.stringify(expected)}`)
}

const routeNameIndex = routeConfig.indexOf("name: 'MesProRouteEdit'")
assert(routeNameIndex >= 0, 'route edit page route must exist')
const routeEditBlock = routeConfig.slice(Math.max(0, routeNameIndex - 300), routeNameIndex + 700)

assertIncludes(routeEditBlock, 'noCache: false', 'route edit page must participate in keep-alive')
assertIncludes(
  appView,
  'const currentRoute = useRoute()',
  'app view must read the active route for immediate cache eligibility'
)
assertIncludes(
  appView,
  'const caches = new Set(tagsViewStore.getCachedViews)',
  'app view must retain tags view cache entries'
)
assertIncludes(
  appView,
  "if (currentRoute.name && currentRoute.meta?.noCache !== true)",
  'current cacheable route must be included before tags view store catches up'
)
assertIncludes(
  appView,
  'caches.add(String(currentRoute.name))',
  'active route component must be in keep-alive include immediately'
)
assert(
  !routeFormContent.includes('label="组成工序"'),
  'removed process-composition tab must not be required for no-reload contract'
)
assertIncludes(
  routeFormContent,
  'label="流转关系图" name="flow" lazy',
  'flow tab must remain lazy-capable under keep-alive'
)
assertIncludes(
  routeFormContent,
  'label="关联产品" name="product" lazy',
  'inactive product tab must not load while returning to flow tab'
)
assert(!routeFormContent.includes('name="schedule-config"'), 'removed schedule config tab must not be required for no-reload contract')
assert(!routeFormContent.includes('name="batch-record-config"'), 'removed batch config tab must not be required for no-reload contract')
assertIncludes(
  routeEditPage,
  "const isCurrentRouteEditPage = computed(() => route.name === 'MesProRouteEdit')",
  'route loader must ignore unrelated routes while cached'
)
assertIncludes(
  routeEditPage,
  'const loadedRouteRequestKey = ref<string>()',
  'route edit page must remember the loaded route request'
)
assertIncludes(routeEditPage, 'const buildRouteRequestKey = () =>', 'stable load request key')
assertIncludes(
  routeEditPage,
  'if (loadedRouteRequestKey.value === nextRouteRequestKey)',
  'same route return must skip reload'
)
assertIncludes(
  routeEditPage,
  'loadedRouteRequestKey.value = nextRouteRequestKey',
  'load key updates only after open succeeds'
)
assertIncludes(
  routeEditPage,
  '() => [isCurrentRouteEditPage.value, routeId.value, initialTab.value',
  'watcher must include current route guard'
)
assertIncludes(
  routeEditPage,
  'routeVersionEditContextKey.value',
  'watcher must include route version context in the load key'
)

console.log('PASS route flow link return no reload static contract')
