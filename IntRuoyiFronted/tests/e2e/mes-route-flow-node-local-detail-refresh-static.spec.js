const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const component = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

const assertIncludes = (content, expected, label) => {
  if (!content.includes(expected)) {
    throw new Error(`${label} missing: ${expected}`)
  }
}

const assertNotIncludes = (content, expected, label) => {
  if (content.includes(expected)) {
    throw new Error(`${label} must not include: ${expected}`)
  }
}

const selectedRouteProcessWatchBlocks = [
  ...component.matchAll(/watch\(\s*selectedRouteProcessId[\s\S]*?\n\)/g)
].map((match) => match[0])

const detailWatchBlocks = selectedRouteProcessWatchBlocks.filter((block) =>
  block.includes('loadSelectedProcessDetail')
)
const routeStateWatchBlocks = selectedRouteProcessWatchBlocks.filter((block) =>
  block.includes('persistRouteFlowReturnState')
)

if (detailWatchBlocks.length !== 1) {
  throw new Error(
    `selection must keep exactly one local detail loader watch, found ${detailWatchBlocks.length}`
  )
}
if (routeStateWatchBlocks.length > 0) {
  throw new Error('plain route process selection must not persist routeProcessId query')
}

assertIncludes(
  component,
  'ensureSelectedProcessRouteConfigCache',
  'route-level selected process detail config cache'
)
assertIncludes(
  component,
  'clearSelectedProcessRouteConfigCache',
  'route-level selected process detail config cache invalidation'
)
assertIncludes(
  component,
  'const routeProcessId = selectedProcessAttributes.routeProcessId || selectedRouteProcessId.value',
  'navigation still carries selected route process for return positioning'
)
assertIncludes(
  component,
  'await persistRouteFlowReturnState()',
  'explicit detail navigation still persists return positioning'
)
assertNotIncludes(
  component,
  'watch(selectedRouteProcessId, () => {\n  void persistRouteFlowReturnState()\n})',
  'plain node selection URL synchronization'
)

console.log('mes-route-flow-node-local-detail-refresh-static PASS')
