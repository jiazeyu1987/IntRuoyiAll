const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routeApi = read('src/api/mes/pro/route/index.ts')
const routeFlowConfigApi = read('src/api/mes/pro/route/flowconfig.ts')
const routeFormContent = read('src/views/mes/pro/route/RouteFormContent.vue')
const staleProcessListPath = path.join(root, 'src/views/mes/pro/route/RouteProcessList.vue')
const graphComponentPath = path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

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

assertIncludes(routeApi, 'RouteFlowNodeVO', 'flow graph node type')
assertIncludes(routeApi, 'RouteFlowEdgeVO', 'flow graph edge type')
assertIncludes(routeApi, 'RouteFlowBoundaryEdgeVO', 'flow graph boundary edge type')
assertIncludes(routeApi, 'RouteFlowGraphVO', 'flow graph response type')
assertIncludes(routeApi, 'routeProcessUpdates?: RouteFlowRouteProcessUpdateReqVO[]', 'flow graph process update payload')
assertIncludes(routeApi, 'getRouteProcessFlowGraph', 'flow graph get API')
assertIncludes(
  routeApi,
  'getRouteProcessFlowGraph: async (routeId: number, routeVersionId?: number)',
  'flow graph get API accepts route version context'
)
assertIncludes(routeApi, 'params: { routeId, routeVersionId }', 'flow graph get API sends route version context')
assertIncludes(routeApi, 'saveRouteProcessFlowGraph', 'flow graph save API')
assertIncludes(routeApi, 'validateRouteProcessFlowGraph', 'flow graph validate API')
assertIncludes(routeApi, '/mes/pro/route-process-flow/get', 'flow graph get endpoint')
assertIncludes(routeApi, '/mes/pro/route-process-flow/save', 'flow graph save endpoint')
assertIncludes(routeApi, '/mes/pro/route-process-flow/validate', 'flow graph validate endpoint')
assertIncludes(
  routeFlowConfigApi,
  'getProcessConfigList: async (routeId: number, useType: ProRouteFlowConfigType, routeVersionId?: number)',
  'route flow config get API accepts route version context'
)
assertIncludes(
  routeFlowConfigApi,
  'params: { routeId, useType, routeVersionId }',
  'route flow config get API sends route version context'
)

assertIncludes(routeFormContent, 'RouteFlowGraphDesigner', 'route form flow graph component')
assertIncludes(routeFormContent, 'label="流转关系图"', 'route form flow graph tab')
assertIncludes(routeFormContent, 'name="flow"', 'route form flow graph tab name')
assertNotIncludes(routeFormContent, 'label="组成工序"', 'removed route process composition tab')
assertNotIncludes(routeFormContent, '<RouteProcessList', 'removed route process settings tab render')
assertIncludes(routeFormContent, ':route-version-edit-context="routeVersionEditContext"', 'route process tab receives version context')
assertNotIncludes(routeFormContent, 'routeProcessListRef', 'removed route process tab refresh ref')
assertNotIncludes(routeFormContent, '@saved="handleFlowGraphSaved"', 'removed list refresh hook')
if (fs.existsSync(staleProcessListPath)) {
  throw new Error('RouteProcessList.vue must be removed after moving process configuration into flow graph')
}

if (!fs.existsSync(graphComponentPath)) {
  throw new Error('RouteFlowGraphDesigner.vue missing')
}

const graphComponent = fs.readFileSync(graphComponentPath, 'utf8')
const routeProcessAddHandler = extractConstFunction(graphComponent, 'handleRouteProcessAdd')
const routeProcessDeleteHandler = extractConstFunction(graphComponent, 'handleRouteProcessDelete')
const saveFromParentHandler = extractConstFunction(graphComponent, 'saveFromParent')
assertIncludes(graphComponent, 'route-flow-graph-designer', 'graph component root class')
assertIncludes(
  graphComponent,
  'ProRouteApi.getRouteProcessFlowGraph(props.routeId, props.routeVersionEditContext?.routeVersionId)',
  'graph load must read candidate flow graph snapshot'
)
assertIncludes(graphComponent, 'ProRouteFlowConfigApi.getProcessConfigList(', 'flow config load must use existing getProcessConfigList API')
assertIncludes(graphComponent, "'SCHEDULE'", 'schedule flow config load must read candidate snapshot')
assertIncludes(graphComponent, "'BATCH'", 'batch flow config load must read candidate snapshot')
assertIncludes(graphComponent, 'props.routeVersionEditContext?.routeVersionId', 'flow config load must pass candidate version context')
assertIncludes(graphComponent, 'handleNodePointerDown', 'node drag interaction')
assertIncludes(graphComponent, 'handlePortPointerDown', 'edge creation interaction')
assertIncludes(graphComponent, 'handleEdgeDelete', 'edge delete interaction')
assertIncludes(graphComponent, 'data-flow-action="add-route-process"', 'stable add route process action selector')
assertIncludes(graphComponent, 'data-flow-action="back-route-list"', 'stable toolbar back action selector')
assertIncludes(graphComponent, 'data-flow-action="delete-route-process"', 'stable delete route process action selector')
assertNotIncludes(graphComponent, 'data-flow-action="add-edge-dialog"', 'toolbar add edge dialog button removed')
assertIncludes(graphComponent, 'data-flow-action="delete-selected-edge"', 'stable selected edge delete action selector')
assertIncludes(graphComponent, 'data-flow-action="select-edge-list"', 'stable relation list select action selector')
assertIncludes(graphComponent, 'handleAutoLayout', 'auto layout action')
assertIncludes(graphComponent, 'handleFitScreen', 'fit screen action')
assertIncludes(graphComponent, 'handleGenerateLinearDraft', 'explicit linear draft action')
assertIncludes(graphComponent, 'showSaveValidationToast', 'save result validation toast')
assertIncludes(graphComponent, 'validateBeforeSubmit', 'parent-driven validation action')
assertIncludes(graphComponent, 'saveFromParent', 'parent-driven save action')
assertIncludes(graphComponent, "defineEmits", 'flow graph emits saved event contract')
assertIncludes(graphComponent, "emit('saved')", 'flow graph emits saved after successful save')
assertIncludes(graphComponent, 'data-flow-node="route-process"', 'stable graph node selector')
assertIncludes(graphComponent, 'data-flow-node="route-boundary"', 'stable graph boundary node selector')
assertIncludes(graphComponent, "'process-start'", 'flow graph start boundary node id')
assertIncludes(graphComponent, "'process-end'", 'flow graph end boundary node id')
assertIncludes(graphComponent, '工序开始', 'flow graph start boundary label')
assertIncludes(graphComponent, '工序结束', 'flow graph end boundary label')
assertIncludes(graphComponent, 'route-flow-graph-designer__boundary-node', 'flow graph boundary node style')
assertIncludes(graphComponent, 'displayFlowNodes', 'flow graph display nodes include virtual boundaries')
assertIncludes(graphComponent, 'displayFlowEdges', 'flow graph display edges include virtual boundary edges')
assertIncludes(graphComponent, 'createBoundaryFlowNodes', 'flow graph builds start and end boundary nodes')
assertIncludes(graphComponent, 'createBoundaryFlowEdges', 'flow graph builds start and end boundary edges')
assertIncludes(graphComponent, 'findBoundaryRouteProcessIds', 'flow graph resolves first and last real processes')
assertIncludes(graphComponent, 'boundaryEdges.value.filter', 'boundary edges must use persisted boundary relations')
assertIncludes(
  graphComponent,
  'boundaryEdges: boundaryEdges.value',
  'save payload must persist boundary edges'
)
assertIncludes(graphComponent, 'entryRouteProcessIds', 'flow graph start boundary must resolve entry processes')
assertIncludes(graphComponent, 'terminalRouteProcessIds', 'flow graph end boundary must resolve terminal processes')
assertIncludes(graphComponent, 'resolveTerminalBoundaryNode', 'end boundary must align with the last terminal process')
assertIncludes(
  graphComponent,
  'y: terminalBoundaryNode ? resolveBoundaryY([terminalBoundaryNode]) : 72',
  'end boundary must use the last terminal process vertical position'
)
assertIncludes(
  graphComponent,
  'source: startBoundary ? PROCESS_START_NODE_ID',
  'start boundary must be the source of start boundary edges'
)
assertIncludes(
  graphComponent,
  'target: startBoundary ? String(boundaryEdge.routeProcessId) : PROCESS_END_NODE_ID',
  'end boundary must be the target of end boundary edges'
)
assertIncludes(graphComponent, "sourceHandle: 'source-right'", 'start and process boundary edges must leave source handles')
assertIncludes(graphComponent, "targetHandle: 'target-left'", 'start and process boundary edges must enter target handles')
assertIncludes(graphComponent, 'isBoundaryNodeId', 'boundary nodes are excluded from persistence and selection')
assertIncludes(graphComponent, 'filter((node) => !isBoundaryNodeId(node.id))', 'save layout sync excludes boundary nodes')
assertIncludes(graphComponent, 'selectable: true', 'boundary nodes are selectable')
assertIncludes(graphComponent, 'connectable: canMutateRouteFlow.value', 'boundary nodes are connectable in edit mode')
assertIncludes(graphComponent, 'draggable: false', 'boundary nodes are not draggable')
assertIncludes(graphComponent, ':data-route-process-id="data.routeNode.routeProcessId"', 'stable route process id selector')
assertIncludes(
  graphComponent,
  "'is-selected': selectedRouteProcessId === data.routeNode.routeProcessId",
  'selected graph node visual state'
)
assertIncludes(
  graphComponent,
  '.route-flow-graph-designer__node.is-selected',
  'selected graph node border style'
)
assertIncludes(graphComponent, 'data-flow-handle="source"', 'stable source handle selector')
assertIncludes(graphComponent, 'data-flow-handle="target"', 'stable target handle selector')
assertNotIncludes(
  graphComponent,
  '@pointerdown.stop="handlePortPointerDown(data.routeNode)"',
  'source handle pointer events must reach Vue Flow so one source can create multiple outgoing edges'
)
assertIncludes(graphComponent, 'data-flow-action="delete-edge-list"', 'stable relation list delete action selector')
assertIncludes(graphComponent, 'pendingDeletedRouteProcessIds', 'deleted route processes must remain draft until bottom save')
assertIncludes(graphComponent, 'persistRouteProcessDraftChanges', 'route process draft changes must persist from bottom save')
assertIncludes(graphComponent, 'buildRouteProcessUpdatePayload', 'existing route process key flag updates must join graph payload')
assertIncludes(graphComponent, 'routeProcessUpdates: buildRouteProcessUpdatePayload()', 'save payload must include route process updates')
assertIncludes(
  saveFromParentHandler,
  'await persistRouteProcessDraftChanges()',
  'bottom save must persist graph route process drafts before saving edges'
)
assertIncludes(
  graphComponent,
  '工序已添加为草稿，请点击顶部保存后生效。',
  'add route process must tell user it is only draft'
)
assertIncludes(
  graphComponent,
  '工序已删除为草稿，请点击顶部保存后生效。',
  'delete route process must tell user it is only draft'
)
assertNotIncludes(
  routeProcessAddHandler,
  'ProRouteProcessApi.createRouteProcess',
  'graph add process click must not auto-save route process'
)
assertNotIncludes(
  routeProcessDeleteHandler,
  'ProRouteProcessApi.deleteRouteProcess',
  'graph delete process click must not auto-save route process'
)
assertNotIncludes(
  graphComponent,
  'ProRouteProcessApi.updateRouteProcess',
  'graph key process draft save must not call active route-process update directly'
)
assertIncludes(graphComponent, 'overflow: hidden', 'one-screen overflow constraint')
assertIncludes(graphComponent, 'ProRouteApi.saveRouteProcessFlowGraph', 'save API call')
assertIncludes(graphComponent, 'ProRouteApi.validateRouteProcessFlowGraph', 'validate API call before bottom save')
assertNotIncludes(graphComponent, '保存关系图', 'standalone flow graph save button removed')
assertNotIncludes(graphComponent, 'handleValidate', 'manual validate action removed')
assertNotIncludes(graphComponent, '校验结果', 'validation result panel removed')
assertNotIncludes(graphComponent, '暂无校验消息', 'empty validation result panel removed')
assertNotIncludes(graphComponent, '<Icon icon="ep:check"', 'manual validate toolbar button removed')

console.log('mes-route-flow-graph-static PASS')
