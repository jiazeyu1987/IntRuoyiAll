const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(read('package.json'))
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

const extractConstFunction = (content, functionName) => {
  const marker = `const ${functionName} =`
  const start = content.indexOf(marker)
  if (start === -1) {
    throw new Error(`${functionName} function missing`)
  }
  const arrowBodyStart = content.indexOf('=> {', start)
  const braceStart =
    arrowBodyStart === -1 ? content.indexOf('{', start) : content.indexOf('{', arrowBodyStart)
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

assertIncludes(
  packageJson.scripts['e2e:mes:route-flow-height-aware-layout:static'],
  'node tests/e2e/mes-route-flow-height-aware-layout-static.spec.js',
  'package.json should expose the height-aware layout static check'
)

assertIncludes(component, 'ref="graphCanvasRef"', 'auto layout must read the visible canvas height')
assertIncludes(
  component,
  'const graphCanvasRef = ref<HTMLElement>()',
  'canvas height ref should be typed and local to the graph component'
)
assertIncludes(
  component,
  'const resolveAutoLayoutRowCapacity =',
  'auto layout must calculate available vertical rows'
)

const resolveAutoLayoutRowCapacity = extractConstFunction(component, 'resolveAutoLayoutRowCapacity')
const buildHeightAwareLinearLayoutPositions = extractConstFunction(
  component,
  'buildHeightAwareLinearLayoutPositions'
)
const applyHeightAwareTailChainLayoutPositions = extractConstFunction(
  component,
  'applyHeightAwareTailChainLayoutPositions'
)
const buildMergedGraphLayoutPositions = extractConstFunction(
  component,
  'buildMergedGraphLayoutPositions'
)
const buildBranchLayoutPositions = extractConstFunction(component, 'buildBranchLayoutPositions')

assertIncludes(
  resolveAutoLayoutRowCapacity,
  'graphCanvasRef.value?.clientHeight',
  'row capacity must come from the actual canvas height'
)
assertIncludes(
  resolveAutoLayoutRowCapacity,
  'NODE_HEIGHT',
  'row capacity must account for node height'
)
assertIncludes(
  resolveAutoLayoutRowCapacity,
  'ROW_GAP',
  'row capacity must use the established vertical spacing'
)
assertNotIncludes(
  resolveAutoLayoutRowCapacity,
  'MAX_VISIBLE_COLUMNS',
  'height-aware layout must not reuse the old fixed visible column cap'
)

assertIncludes(
  buildHeightAwareLinearLayoutPositions,
  'rowCapacity',
  'linear layout should use a resolved vertical row capacity'
)
assertIncludes(
  buildHeightAwareLinearLayoutPositions,
  'columnIndex',
  'linear layout should advance columns only after filling visible height'
)
assertIncludes(
  buildHeightAwareLinearLayoutPositions,
  'columnIndex % 2 === 0 ? rowIndex : rowCapacity - 1 - rowIndex',
  'linear layout should snake vertically so consecutive edges stay short'
)
assertIncludes(
  buildHeightAwareLinearLayoutPositions,
  'x: LAYOUT_LEFT_PADDING + columnIndex * COLUMN_GAP',
  'linear layout should keep the main direction left-to-right by column'
)
assertIncludes(
  buildHeightAwareLinearLayoutPositions,
  'y: LAYOUT_TOP_PADDING + resolvedRowIndex * ROW_GAP',
  'linear layout should use height before extending sideways'
)
assertIncludes(
  applyHeightAwareTailChainLayoutPositions,
  'const applyHeightAwareTailChainLayoutPositions = ({',
  'tail snake layout should receive one typed options object so tree and DAG callers pass the same runtime shape'
)
assertNotIncludes(
  component,
  '} as never)',
  'tail snake layout callers must not hide a runtime signature mismatch with a never cast'
)
assertIncludes(
  applyHeightAwareTailChainLayoutPositions,
  'parents.length > 1 || children.length > 1',
  'tail layout should start after a merge or branch point'
)
assertIncludes(
  applyHeightAwareTailChainLayoutPositions,
  'chainRouteProcessIds.length <= rowCapacity',
  'short branch tails should stay in the ordinary tree/DAG layout'
)
assertIncludes(
  applyHeightAwareTailChainLayoutPositions,
  'anchorPosition.y',
  'tail snake should stay visually attached to the merge or branch anchor row'
)
assertIncludes(
  applyHeightAwareTailChainLayoutPositions,
  'const wrappedPositions = buildHeightAwareLinearLayoutPositions(chainRouteProcessIds, rowCapacity)',
  'long branch tails should reuse the same height-aware snake algorithm'
)
assertIncludes(
  applyHeightAwareTailChainLayoutPositions,
  'position.x = anchorPosition.x + COLUMN_GAP + (position.x - LAYOUT_LEFT_PADDING)',
  'tail snake should begin after the branch or merge anchor instead of resetting to the canvas left'
)
assertIncludes(
  buildMergedGraphLayoutPositions,
  'applyHeightAwareTailChainLayoutPositions({',
  'DAG/merge layout must snake long post-merge tails'
)

assertIncludes(
  buildBranchLayoutPositions,
  'const rowCapacity = resolveAutoLayoutRowCapacity()',
  'branch layout entry should resolve available height once per layout run'
)
assertIncludes(
  buildBranchLayoutPositions,
  'const hasTreeBranches =',
  'layout must distinguish linear chains from tree branches'
)
assertIncludes(
  buildBranchLayoutPositions,
  '!hasTreeBranches',
  'pure linear chains should use the height-aware layout path'
)
assertIncludes(
  buildBranchLayoutPositions,
  'buildHeightAwareLinearLayoutPositions(topologicalOrder, rowCapacity)',
  'linear chains should be positioned by row capacity instead of one horizontal line'
)
assertIncludes(
  buildBranchLayoutPositions,
  'children.length === 0',
  'tree branch layout must still allocate rows by leaf branches'
)
assertIncludes(
  buildBranchLayoutPositions,
  'applyHeightAwareTailChainLayoutPositions({',
  'tree branch layout must snake long single-child tails after a branch point'
)

console.log('mes-route-flow-height-aware-layout-static PASS')
