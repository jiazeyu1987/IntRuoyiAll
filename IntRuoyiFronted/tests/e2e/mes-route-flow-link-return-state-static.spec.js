const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const routeGraphPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'route',
  'RouteFlowGraphDesigner.vue'
)

const routeGraph = fs.readFileSync(routeGraphPath, 'utf8')

function assertIncludes(source, expected, label) {
  assert(source.includes(expected), `${label}: expected ${JSON.stringify(expected)}`)
}

assertIncludes(routeGraph, 'const route = useRoute()', 'route query access for return state')
assertIncludes(routeGraph, 'resolveRouteFlowReturnState', 'return state resolver')
assertIncludes(routeGraph, 'persistRouteFlowReturnState', 'return state persister')
assertIncludes(routeGraph, 'router.replace({ query: nextQuery })', 'route query persistence without new history entry')
assertIncludes(routeGraph, 'route.query.routeProcessId', 'selected route process query source')
assertIncludes(routeGraph, 'routeProcessId: explicitRouteProcessId', 'selected node restored from explicit query route process')
assertIncludes(routeGraph, "source: 'explicit'", 'explicit return state source wins before local memory')
assertIncludes(routeGraph, 'await persistRouteFlowReturnState()', 'link navigation flushes state before leaving')
assertIncludes(routeGraph, "tab: 'flow'", 'return state keeps flow tab active')
assert(!routeGraph.includes('PROCESS_DETAIL_FIELD_QUERY_KEY'), 'visible item config must not use URL query key')
assert(!routeGraph.includes('selectedProcessDetailFieldKeys.value = restoredFieldKeys'), 'visible item config must not be restored from URL query')
assert(!routeGraph.includes('nextQuery[PROCESS_DETAIL_FIELD_QUERY_KEY]'), 'visible item config must not be written to URL query')

console.log('PASS route flow link return state static contract')
