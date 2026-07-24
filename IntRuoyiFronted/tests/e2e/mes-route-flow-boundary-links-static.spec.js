const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const apiFile = path.join(root, 'src/api/mes/pro/route/index.ts')
const designerFile = path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

const read = (file) => fs.readFileSync(file, 'utf8')

function extractConstFunction(content, functionName) {
  const marker = `const ${functionName} =`
  const start = content.indexOf(marker)
  if (start === -1) {
    throw new Error(`${functionName} function missing`)
  }
  const braceStart = content.indexOf('{', start)
  if (braceStart === -1) {
    throw new Error(`${functionName} function body missing`)
  }
  let depth = 0
  for (let index = braceStart; index < content.length; index += 1) {
    const char = content[index]
    if (char === '{') depth += 1
    if (char === '}') depth -= 1
    if (depth === 0) {
      return content.slice(start, index + 1)
    }
  }
  throw new Error(`${functionName} function body is not closed`)
}

test('route flow API exposes explicit START and END boundary edges', () => {
  const api = read(apiFile)

  assert.match(api, /export type RouteFlowBoundaryType = 'START' \| 'END'/)
  assert.match(api, /export interface RouteFlowBoundaryEdgeVO/)
  assert.match(api, /boundaryType: RouteFlowBoundaryType/)
  assert.match(api, /routeProcessId: number/)
  assert.equal((api.match(/boundaryEdges: RouteFlowBoundaryEdgeVO\[\]/g) || []).length, 2)
})

test('route flow designer selects, connects, persists and deletes boundary relations', () => {
  const designer = read(designerFile)
  const addBoundaryEdge = extractConstFunction(designer, 'addBoundaryEdge')
  const applyAutoLayout = extractConstFunction(designer, 'applyAutoLayout')
  const buildBranchLayoutPositions = extractConstFunction(designer, 'buildBranchLayoutPositions')
  const collectStartBoundaryRootIds = extractConstFunction(designer, 'collectStartBoundaryRootIds')
  const handleConnect = extractConstFunction(designer, 'handleConnect')
  const normalizeStartBoundaryTargetsAsRoots = extractConstFunction(
    designer,
    'normalizeStartBoundaryTargetsAsRoots'
  )

  assert.match(designer, /const boundaryEdges = ref<RouteFlowBoundaryEdgeVO\[\]>/)
  assert.match(designer, /const selectedBoundaryType = ref<RouteFlowBoundaryType \| null>/)
  assert.match(designer, /data-flow-node="route-boundary"/)
  assert.match(designer, /data-flow-panel="selected-boundary-detail"/)
  assert.match(designer, /addBoundaryEdge/)
  assert.match(designer, /handleBoundaryEdgeDelete/)
  assert.match(designer, /boundaryEdges: boundaryEdges\.value/)
  assert.match(designer, /selectable: true/)
  assert.match(designer, /connectable: isEditable\.value/)
  assert.match(designer, /boundaryEdges\.value\.filter/)
  assert.match(designer, /boundaryType === 'START'/)
  assert.match(designer, /retainedBoundaryEdges/)
  assert.match(
    designer,
    /const retainedBoundaryEdges = boundaryEdges\.value/,
    'boundary relations must preserve existing START and END links so multiple roots and terminals can be represented'
  )
  assert.doesNotMatch(
    designer,
    /boundaryType === 'START'\s*\?\s*boundaryEdges\.value\.filter\(\(edge\) => edge\.boundaryType !== 'START'\)/,
    'start boundary must preserve existing outgoing relations'
  )
  assert.doesNotMatch(
    designer,
    /boundaryType === 'END'\s*\?\s*boundaryEdges\.value\.filter\(\(edge\) => edge\.boundaryType !== 'END'\)/,
    'end boundary must preserve existing incoming relations for branch terminal nodes'
  )
  assert.doesNotMatch(designer, /已将工序结束入口从/, 'end boundary must not silently replace an existing terminal relation')
  assert.doesNotMatch(designer, /工序结束只能接入一条关系/)
  assert.match(
    addBoundaryEdge,
    /boundaryType === 'START'[\s\S]*routeEdges\.value\.filter\(\(edge\) => edge\.targetRouteProcessId === routeProcessId\)/,
    'start boundary must find existing ordinary incoming relations for the new root'
  )
  assert.match(
    addBoundaryEdge,
    /routeEdges\.value = routeEdges\.value\.filter\(\(edge\) => edge\.targetRouteProcessId !== routeProcessId\)/,
    'start boundary must remove the ordinary predecessor so the target becomes a real layout root'
  )
  assert.match(
    collectStartBoundaryRootIds,
    /new Set\([\s\S]*boundaryEdges\.value[\s\S]*edge\.boundaryType === 'START'/,
    'start boundary root collector must derive all roots from saved START relations'
  )
  assert.match(
    buildBranchLayoutPositions,
    /const startBoundaryRootIds = collectStartBoundaryRootIds\(\)/,
    'branch layout must read START boundary roots directly instead of only relying on ordinary edge in-degree'
  )
  assert.match(
    buildBranchLayoutPositions,
    /if \(startBoundaryRootIds\.has\(edge\.targetRouteProcessId\)\) \{[\s\S]*return/,
    'branch layout must ignore ordinary incoming edges to START targets so the start node fans out as a tree'
  )
  assert.match(
    normalizeStartBoundaryTargetsAsRoots,
    /const startRouteProcessIds = collectStartBoundaryRootIds\(\)[\s\S]*!startRouteProcessIds\.has\(edge\.targetRouteProcessId\)/,
    'auto layout must treat persisted start-boundary targets as real roots'
  )
  assert.match(
    applyAutoLayout,
    /const normalizedStartRoots = normalizeStartBoundaryTargetsAsRoots\(\)[\s\S]*positions = buildBranchLayoutPositions\(\)/,
    'auto layout must normalize start roots before computing branch positions'
  )
  assert.match(
    handleConnect,
    /connection\.source === PROCESS_START_NODE_ID[\s\S]*const boundaryEdgeAdded = addBoundaryEdge\('START', Number\(connection\.target\)\)[\s\S]*await applyAutoLayout\(\{[\s\S]*focusRouteProcessId: undefined/,
    'dragging from the start boundary must trigger full auto layout after adding a root relation'
  )
})
