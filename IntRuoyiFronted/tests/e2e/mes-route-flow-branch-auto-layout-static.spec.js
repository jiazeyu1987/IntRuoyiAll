const fs = require('node:fs')
const path = require('node:path')

const componentPath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/route/RouteFlowGraphDesigner.vue'
)
const component = fs.readFileSync(componentPath, 'utf8').replace(/\r\n/g, '\n')

function assertIncludes(content, expected, label) {
  if (!content.includes(expected)) {
    throw new Error(`${label} missing: ${expected}`)
  }
}

function assertNotIncludes(content, expected, label) {
  if (content.includes(expected)) {
    throw new Error(`${label} must not include: ${expected}`)
  }
}

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

const applyAutoLayout = extractConstFunction(component, 'applyAutoLayout')
const buildBranchLayoutPositions = extractConstFunction(component, 'buildBranchLayoutPositions')
const buildMergedGraphLayoutPositions = extractConstFunction(component, 'buildMergedGraphLayoutPositions')
const handleConnect = extractConstFunction(component, 'handleConnect')
const handleEdgesChange = extractConstFunction(component, 'handleEdgesChange')
const handleEdgeUpdate = extractConstFunction(component, 'handleEdgeUpdate')
const handleEdgeDelete = extractConstFunction(component, 'handleEdgeDelete')
const handleFitBranch = extractConstFunction(component, 'handleFitBranch')
const collectBranchRouteProcessIds = extractConstFunction(component, 'collectBranchRouteProcessIds')

assertNotIncludes(
  applyAutoLayout,
  'MAX_VISIBLE_COLUMNS',
  'automatic relation layout must not switch to a dense snake layout'
)
assertNotIncludes(
  applyAutoLayout,
  'defaultNodePosition(index)',
  'automatic relation layout must not place nodes by array order'
)
assertNotIncludes(
  component,
  '已按一屏蛇形网格自动布局',
  'automatic layout feedback must not claim a snake layout'
)
assertIncludes(
  applyAutoLayout,
  'buildBranchLayoutPositions()',
  'automatic layout must use branch positions'
)
assertIncludes(
  component,
  'focusRouteProcessId?: number',
  'topology-triggered layout must identify the branch that should stay readable'
)
assertIncludes(
  applyAutoLayout,
  'fitGraphAfterLayout(options.focusRouteProcessId)',
  'automatic layout must focus the changed branch after positioning the whole graph'
)
assertIncludes(
  buildBranchLayoutPositions,
  'const placeSubtree =',
  'branch layout must recursively place complete subtrees'
)
assertIncludes(
  buildBranchLayoutPositions,
  'const hasMergeTargets =',
  'branch layout must detect merge nodes with multiple incoming relations'
)
assertIncludes(
  buildBranchLayoutPositions,
  'buildMergedGraphLayoutPositions',
  'branch layout must switch to DAG layout for multiple-entry merge nodes'
)
assertIncludes(
  component,
  'const buildMergedGraphLayoutPositions =',
  'DAG layout helper must exist for one-node-multiple-entry graphs'
)
assertIncludes(
  component,
  'const topologicalOrder = buildTopologicalOrder',
  'DAG layout must use topological order instead of recursive tree ownership'
)
assertIncludes(
  component,
  'const resolveLayerYPositions =',
  'DAG layout must resolve same-layer spacing so merge graphs remain readable'
)
assertIncludes(
  buildMergedGraphLayoutPositions,
  'const hasMultipleParents =',
  'DAG layout must explicitly identify merge nodes with multiple incoming parents'
)
assertIncludes(
  buildMergedGraphLayoutPositions,
  'const alignMergeParentDepths =',
  'DAG layout must align direct merge parents into the column immediately before the merge target'
)
assertIncludes(
  buildMergedGraphLayoutPositions,
  'depthById.set(parentRouteProcessId, targetDepth - 1)',
  'DAG layout must promote shallow direct parents so long merge edges do not visually pass through unrelated middle nodes'
)
assertIncludes(
  buildMergedGraphLayoutPositions,
  'const enforceChildDepths =',
  'DAG layout must re-enforce child depths after promoting merge parents'
)
assertIncludes(
  buildMergedGraphLayoutPositions,
  'const alignMergeNodesToParents =',
  'DAG layout must have a final merge-centering pass after parent positions settle'
)
assertIncludes(
  buildMergedGraphLayoutPositions,
  'children.length > 0 && !hasMultipleParents(routeProcessId)',
  'DAG layout must keep merge nodes centered on their incoming parents even when they have outgoing children'
)
assertIncludes(
  buildMergedGraphLayoutPositions,
  'alignParentsToChildren()\n  alignMergeNodesToParents()\n  alignParentsToChildren()\n  alignMergeNodesToParents()',
  'DAG layout must recenter merge nodes after the final parent-to-child alignment pass'
)
assertIncludes(
  buildBranchLayoutPositions,
  '(firstChildY + lastChildY) / 2',
  'a parent must be vertically centered over its direct branches'
)
assertIncludes(
  component,
  'const collectBranchRouteProcessIds =',
  'branch viewport must collect complete descendant subtree'
)
assertIncludes(
  collectBranchRouteProcessIds,
  'const pending = [sourceRouteProcessId]',
  'branch collection must start from the changed source process'
)
assertIncludes(
  collectBranchRouteProcessIds,
  'branchRouteProcessIds.add(routeProcessId)',
  'branch collection must keep every visited subtree process'
)
assertIncludes(
  collectBranchRouteProcessIds,
  'edge.sourceRouteProcessId === routeProcessId',
  'branch collection must follow child edges recursively'
)
assertIncludes(
  handleFitBranch,
  'const branchRouteProcessIds = collectBranchRouteProcessIds(sourceRouteProcessId)',
  'branch fitting must use the full descendant subtree'
)
assertIncludes(
  handleFitBranch,
  'nodes: branchRouteProcessIds.map(String)',
  'branch fitting must fit all descendant subtree nodes'
)
assertNotIncludes(
  handleFitBranch,
  'childRouteProcessIds',
  'branch fitting must not stop at direct children'
)
assertIncludes(
  buildBranchLayoutPositions,
  '关系图存在循环，无法自动布局',
  'invalid cyclic graphs must fail clearly'
)
assertNotIncludes(
  buildBranchLayoutPositions,
  '工序存在多个入口，无法自动布局',
  'multiple incoming relations must be valid and automatically laid out'
)

for (const [name, handler] of [
  ['handleConnect', handleConnect],
  ['handleEdgeUpdate', handleEdgeUpdate]
]) {
  assertIncludes(
    handler,
    'const edgeAdded = addEdge(',
    `${name} must inspect edge mutation success`
  )
  assertIncludes(
    handler,
    'focusRouteProcessId:',
    `${name} must relayout after a successful edge mutation`
  )
}

assertIncludes(
  handleEdgesChange,
  'focusRouteProcessId:',
  'canvas edge removal must relayout ordinary process nodes'
)
assertIncludes(
  handleEdgeDelete,
  'focusRouteProcessId:',
  'relation-list edge deletion must relayout ordinary process nodes'
)

console.log('mes-route-flow-branch-auto-layout-static PASS')
