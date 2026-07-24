const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const graphComponentPath = path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const graphComponent = fs.readFileSync(graphComponentPath, 'utf8')

const assertIncludes = (content, expected, label) => {
  if (!content.includes(expected)) {
    throw new Error(`${label} missing: ${expected}`)
  }
}

assertIncludes(
  graphComponent,
  'pendingDeletedRouteProcessIds',
  'route process deletion draft state'
)
assertIncludes(
  graphComponent,
  'isPendingDeletedFlowNode',
  'pending-deleted route process nodes must be blocked from VueFlow node sync'
)
assertIncludes(
  graphComponent,
  'const isActiveRouteProcessId = (routeProcessId: number) => {',
  'pending-deleted route process IDs must have one active-state guard'
)
assertIncludes(
  graphComponent,
  'routeNodes.value.filter(isActiveRouteNode).map(toFlowNode)',
  'syncFlowElements must not re-render pending-deleted route processes after adding another process'
)
assertIncludes(
  graphComponent,
  'nodes.filter(',
  'VueFlow v-model setter must not rehydrate pending-deleted route processes'
)
assertIncludes(
  graphComponent,
  '(node) => !isBoundaryNodeId(node.id) && !isPendingDeletedFlowNode(node)',
  'VueFlow v-model setter pending-delete predicate'
)
assertIncludes(
  graphComponent,
  'syncRouteNodesFromFlowModel(nodes)',
  'VueFlow v-model setter must sync user-deleted nodes back to routeNodes'
)
assertIncludes(
  graphComponent,
  '@nodes-change="handleNodesChange"',
  'VueFlow native node delete events must sync user-deleted nodes back to routeNodes'
)
assertIncludes(
  graphComponent,
  ':delete-key-code="null"',
  'VueFlow native delete key must be disabled so route-process draft deletion owns Delete behavior'
)
assertIncludes(
  graphComponent,
  "change.type === 'remove'",
  'VueFlow node remove changes must be translated into route process draft deletes'
)
assertIncludes(
  graphComponent,
  'handleCanvasDeleteKeydown',
  'keyboard Delete on selected graph node must delete route process from draft data'
)
assertIncludes(
  graphComponent,
  'confirmRemoveRouteProcessFromDraft',
  'keyboard Delete and delete button must share the same route process delete confirmation flow'
)
assertIncludes(
  graphComponent,
  'await confirmRemoveRouteProcessFromDraft(node)',
  'delete button must use the unified delete-confirm helper'
)
assertIncludes(
  graphComponent,
  'await confirmRemoveRouteProcessFromDraft(selectedNode.value)',
  'canvas Delete must use the unified delete-confirm helper'
)
assertIncludes(
  graphComponent,
  "target.closest('[data-flow-node=\"route-process\"]')",
  'canvas Delete must not open a second confirmation when the focused route-process node owns Delete'
)
assertIncludes(
  graphComponent,
  '确认从当前工艺路线删除工序',
  'unified route process deletion must show the confirmation dialog'
)
assertIncludes(
  graphComponent,
  'handleRouteProcessNodeKeydown',
  'keyboard Delete on focused route process node must delete route process from draft data'
)
assertIncludes(
  graphComponent,
  "window.addEventListener('keydown', handleCanvasDeleteKeydown)",
  'graph keyboard delete handler must be mounted for real canvas deletion'
)
assertIncludes(
  graphComponent,
  'const removedRouteProcessIds = Array.from(previousRouteProcessIds).filter(',
  'VueFlow node model sync must detect route process nodes deleted outside the toolbar button'
)
assertIncludes(
  graphComponent,
  'routeNodes.value = routeNodes.value.filter(',
  'VueFlow node model sync must remove user-deleted route processes from routeNodes before add process'
)
assertIncludes(
  graphComponent,
  '(node) => !removedRouteProcessIdSet.has(node.routeProcessId)',
  'VueFlow node model sync remove predicate'
)
assertIncludes(
  graphComponent,
  'isActiveRouteProcessId(edge.sourceRouteProcessId)',
  'stale edges from pending-deleted route processes must stay hidden'
)
assertIncludes(
  graphComponent,
  'routeProcessCreates: routeNodes.value',
  'save payload must still derive creates from route process nodes'
)
assertIncludes(
  graphComponent,
  '.filter(isActiveRouteNode)',
  'save payload must exclude stale pending-deleted route process nodes'
)

console.log('mes-route-flow-graph-draft-delete-sync-static PASS')
