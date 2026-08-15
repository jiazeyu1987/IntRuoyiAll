const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const graphComponent = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)

const assertIncludes = (content, expected, label) => {
  if (!content.includes(expected)) {
    throw new Error(`${label} missing: ${expected}`)
  }
}

assertIncludes(
  graphComponent,
  'const loadedRouteProcessIds = ref<Set<number>>(new Set())',
  'candidate graph must track route process identities loaded from its snapshot'
)
assertIncludes(
  graphComponent,
  'const resetLoadedRouteProcessIds = () => {',
  'candidate graph load and successful save must establish an identity baseline'
)
assertIncludes(
  graphComponent,
  'nextDraftRouteProcessId.value = resolveNextDraftRouteProcessId()',
  'new draft identity must be allocated below candidate snapshot negative identities'
)
assertIncludes(
  graphComponent,
  '.filter((node) => isNewDraftRouteProcess(node.routeProcessId))',
  'save payload must submit only route processes created in the current page session'
)
assertIncludes(
  graphComponent,
  'removedRouteProcessIds.filter(isLoadedRouteProcessId)',
  'deleting a negative route process loaded from the candidate snapshot must remain a delete'
)
assertIncludes(
  graphComponent,
  'if (!isLoadedRouteProcessId(node.routeProcessId)) return false',
  'loaded negative route processes must participate in update change detection'
)
assertIncludes(
  graphComponent,
  'resetLoadedRouteProcessIds()\n  nextDraftRouteProcessId.value = resolveNextDraftRouteProcessId()\n  resetRouteProcessKeyFlagBaselines()',
  'successful candidate save must promote current negative identities to the next baseline'
)

console.log('mes-route-candidate-negative-process-resave-static PASS')
