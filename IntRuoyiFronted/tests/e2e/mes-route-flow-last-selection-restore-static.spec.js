const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(read('package.json'))
const graphComponent = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const routeFormContent = read('src/views/mes/pro/route/RouteFormContent.vue')

assert.equal(
  packageJson.scripts?.['e2e:mes:route-flow-last-selection-restore:static'],
  'node tests/e2e/mes-route-flow-last-selection-restore-static.spec.js',
  'package.json must expose the route flow last selection restore static contract'
)

for (const token of [
  "import { useCache } from '@/hooks/web/useCache'",
  "import { useUserStoreWithOut } from '@/store/modules/user'",
  "const ROUTE_FLOW_LAST_SELECTION_CACHE_PREFIX = 'mes.pro.route.flow.lastSelection'",
  'type RouteFlowLastSelectionState = {',
  'routeProcessId: number',
  'detailFieldKey?: ProcessDetailFieldKey',
  'const buildRouteFlowLastSelectionCacheKey = () =>',
  'props.routeVersionEditContext?.routeVersionId',
  'userStore.getUser?.id',
  'const readRouteFlowLastSelection = ()',
  'const persistRouteFlowLastSelection =',
  'const removeRouteFlowLastSelection = ()',
  'const restoreRouteFlowSelection =',
  'const selectRouteProcessNode =',
  'const selectProcessDetailField =',
  'const clearRouteFlowLastSelectionDetailField =',
  'const resolveRouteFlowGraphReadRouteVersionId ='
]) {
  assert.ok(graphComponent.includes(token), `flow graph must include ${token}`)
}

assert.match(
  graphComponent,
  /wsCache\.set\(buildRouteFlowLastSelectionCacheKey\(\),\s*nextState\)/,
  'manual node/field selection must persist the current route process and detail field'
)
assert.match(
  graphComponent,
  /wsCache\.delete\(buildRouteFlowLastSelectionCacheKey\(\)\)/,
  'invalid or removed selection must clear only the scoped route flow last-selection key'
)
const versionCachePartBlock =
  graphComponent.match(/const buildRouteFlowVersionCachePart = \(\) => \{[\s\S]*?\n\}/)?.[0] || ''
assert.match(
  versionCachePartBlock,
  /props\.routeVersionEditContext\?\.routeVersionId[\s\S]*throw new Error\('流转关系图选择记忆失败：缺少路线版本编号。'\)/,
  'last-selection memory must fail fast when the exact route version id is unavailable'
)
assert.doesNotMatch(
  versionCachePartBlock,
  /activeRouteVersionNo|return ['"`]active/,
  'last-selection memory must not downgrade the version scope to version number or a generic active key'
)
assert.match(
  routeFormContent,
  /const routeFlowVersionEditContext = computed<RouteVersionEditContext \| undefined>\(\(\) => \{[\s\S]*routeVersionEditContext\.value[\s\S]*formData\.value\.activeRouteVersionId[\s\S]*lifecycleStatus: 'ACTIVE'/,
  'active-version entry must provide the exact active route version as routeVersionEditContext'
)
assert.match(
  routeFormContent,
  /:route-version-edit-context="routeFlowVersionEditContext"/,
  'the flow graph must receive the resolved exact route version context'
)
assert.match(
  graphComponent,
  /props\.routeVersionEditContext\?\.lifecycleStatus === 'ACTIVE'[\s\S]*\? undefined[\s\S]*: props\.routeVersionEditContext\?\.routeVersionId/,
  'active route version memory context must not change the existing active flow graph read API contract'
)
assert.match(
  graphComponent,
  /ProRouteApi\.getRouteProcessFlowGraph\(props\.routeId,\s*resolveRouteFlowGraphReadRouteVersionId\(\)\)/,
  'flow graph loading must use the read API route-version resolver'
)

const restoreStateBlock =
  graphComponent.match(/const restoreRouteFlowReturnState = \(\) => \{[\s\S]*?\n\}/)?.[0] || ''
assert.match(
  restoreStateBlock,
  /const explicitRouteProcessId = resolveExplicitRouteFlowRouteProcessId\(\)[\s\S]*restoreRouteFlowSelection\(\{[\s\S]*source: 'explicit'[\s\S]*return[\s\S]*const cachedSelection = readRouteFlowLastSelection\(\)[\s\S]*source: 'cache'/,
  'restore must prefer explicit URL/props routeProcessId before scoped local last selection'
)

assert.match(
  graphComponent,
  /const handleNodeClick = \(event: NodeMouseEvent\) => \{[\s\S]*selectRouteProcessNode\(routeProcessId,\s*\{ persist: true \}\)/,
  'clicking a process node must update and persist the last selected route process'
)
assert.match(
  graphComponent,
  /const handleSelectProcessDetailField = \(fieldKey: ProcessDetailFieldKey\) => \{[\s\S]*selectProcessDetailField\(fieldKey,\s*\{ persist: true \}\)/,
  'clicking a left panel detail item must update and persist the selected field'
)
assert.match(
  graphComponent,
  /const handleRemoveProcessDetailField = async \(fieldKey: ProcessDetailFieldKey\) => \{[\s\S]*clearRouteFlowLastSelectionDetailField\(fieldKey\)/,
  'removing the remembered left panel item must clear the remembered field'
)
assert.match(
  graphComponent,
  /watch\(selectedProcessDetailFieldKeys,[\s\S]*if \(!processDetailInterestSaving\.value\) \{[\s\S]*clearRouteFlowLastSelectionDetailField\(selectedProcessDetailFieldKey\.value\)/,
  'field config rollback must not clear remembered field while save is in progress'
)

for (const handlerName of ['handleEdgeClick', 'handleEdgeSelect', 'handleBoundaryEdgeSelect']) {
  const handlerBlock =
    graphComponent.match(new RegExp(`const ${handlerName} = [\\s\\S]*?\\n}`))?.[0] || ''
  assert.ok(handlerBlock, `flow graph must keep ${handlerName}`)
  assert.doesNotMatch(
    handlerBlock,
    /persistRouteFlowLastSelection|removeRouteFlowLastSelection/,
    `${handlerName} must not overwrite the remembered process/field selection`
  )
}

const persistBlock =
  graphComponent.match(/const persistRouteFlowLastSelection =[\s\S]*?\n}/)?.[0] || ''
assert.doesNotMatch(
  persistBlock,
  /catch\s*\(/,
  'last-selection storage write errors must surface instead of being swallowed'
)

console.log('mes-route-flow-last-selection-restore-static PASS')
