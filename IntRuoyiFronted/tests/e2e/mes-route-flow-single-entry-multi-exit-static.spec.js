const fs = require('fs')
const path = require('path')

const componentPath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/route/RouteFlowGraphDesigner.vue'
)
const component = fs.readFileSync(componentPath, 'utf8')

function assertIncludes(text, expected, label) {
  if (!text.includes(expected)) {
    throw new Error(`${label}: missing ${expected}`)
  }
}

function assertNotIncludes(text, expected, label) {
  if (text.includes(expected)) {
    throw new Error(`${label}: unexpected ${expected}`)
  }
}

assertIncludes(
  component,
  'const wouldCreateCycle =',
  'the graph designer must reject a connection that creates a cycle'
)
assertNotIncludes(
  component,
  'edge.targetRouteProcessId !== targetRouteProcessId',
  'the old single-entry replacement contract should be absent after the merge-node requirement changes'
)
assertIncludes(
  component,
  'edge.sourceRouteProcessId === sourceRouteProcessId',
  'the graph designer must detect an existing identical connection'
)
assertIncludes(
  component,
  'routeEdges.value = [...candidateEdges, nextEdge]',
  'the graph designer must atomically retain other incoming and outgoing edges and add the new edge'
)
assertNotIncludes(
  component,
  '已将工序入口从',
  'adding another incoming edge must not display replacement copy'
)
assertIncludes(
  component,
  '@edge-update="handleEdgeUpdate"',
  'dragging an existing edge endpoint must use the same topology rules'
)

console.log('mes route flow single-entry multiple-exit static contract passed')
