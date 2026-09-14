const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(read('package.json'))
const routeFormContent = read('src/views/mes/pro/route/RouteFormContent.vue')
const routeEditPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const graphComponent = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

assert.equal(
  packageJson.scripts?.['e2e:mes:route-flow-graph-only:static'],
  'node tests/e2e/mes-route-flow-graph-only-static.spec.js',
  'package.json must expose the flow-graph-only route static contract'
)

assert.match(
  routeFormContent,
  /label="流转关系图" name="flow" lazy/,
  'route form must keep the flow graph tab as the route process maintenance entry'
)
assert.match(
  routeFormContent,
  /<RouteFlowGraphDesigner[\s\S]*:route-version-edit-context="routeVersionEditContext"/,
  'flow graph must still receive route version edit context'
)
assert.doesNotMatch(
  routeFormContent,
  /label="组成工序" name="process" lazy/,
  'route form must remove the old process settings tab after moving configuration into the flow graph'
)
assert.doesNotMatch(
  routeFormContent,
  /<RouteProcessList/,
  'route form must not mount the old process settings tab component'
)
const initialTabType = /type RouteFormInitialTab =([\s\S]*?)const activeTab/.exec(routeFormContent)?.[1] || ''
assert.ok(
  !initialTabType.includes("'process'"),
  'route form initial tab type must not accept the removed process settings tab'
)

assert.ok(
  !routeEditPage.includes("['basic', 'flow', 'process', 'product'].includes(tab)"),
  'route edit page must route old tab=process links back to the flow graph default'
)
assert.match(
  routeEditPage,
  /return 'flow'/,
  'route edit page must default existing route editing to the flow graph'
)

assert.match(
  graphComponent,
  /formatRouteProcessScheduleStrategySummary/,
  'flow graph must summarize process settings instead of reintroducing a separate inline settings editor'
)
for (const selector of [
  'data-flow-action="add-route-process"',
  'data-flow-action="connect-route-process"',
  'data-flow-action="delete-route-process"',
  'data-flow-action="save-route-flow"',
  'data-flow-action="add-process-config-item"'
]) {
  assert.match(graphComponent, new RegExp(selector), `flow graph must keep ${selector}`)
}

console.log('mes-route-flow-graph-only-static PASS')
