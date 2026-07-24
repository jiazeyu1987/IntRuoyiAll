const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const assert = require('node:assert/strict')

const designerFile = path.resolve(
  __dirname,
  '../../src/views/mes/pro/route/RouteFlowGraphDesigner.vue'
)

const designer = fs.readFileSync(designerFile, 'utf8')

test('route flow designer exposes a searchable connection selector draft workflow', () => {
  assert.match(designer, /data-flow-action="connect-route-process"/)
  assert.match(designer, />\s*连接工序\s*</)
  assert.match(designer, /data-flow-panel="connection-selector"/)
  assert.match(designer, /data-flow-field="connection-source"/)
  assert.match(designer, /data-flow-field="connection-target"/)
  assert.match(designer, /data-flow-action="confirm-route-process-connection"/)
  assert.match(designer, /filterable/)
  assert.match(designer, /目标工序当前入口/)
  assert.match(designer, /确认后将替换/)

  assert.match(designer, /const connectionPopoverVisible = ref\(false\)/)
  assert.match(
    designer,
    /const connectionSourceRouteProcessId = ref<ConnectionSourceRouteProcessId \| null>\(null\)/
  )
  assert.match(designer, /const connectionTargetRouteProcessId = ref<number \| null>\(null\)/)
  assert.match(designer, /const graphDirty = ref\(false\)/)
  assert.match(designer, /const autoLayoutRevision = ref\(0\)/)
  assert.match(designer, /:data-flow-layout-revision="autoLayoutRevision"/)
  assert.match(designer, /const handleConfirmConnection = async/)
  assert.match(
    designer,
    /sourceId === PROCESS_START_NODE_ID\s*\?\s*addBoundaryEdge\('START', targetId\)\s*:\s*addEdge\(sourceId, targetId\)/
  )
  assert.match(
    designer,
    /focusRouteProcessId: sourceId === PROCESS_START_NODE_ID \? undefined : sourceId/
  )
  assert.match(designer, /await applyAutoLayout\(\{\s*notify: false,[\s\S]*focusRouteProcessId:/)
  assert.match(designer, /autoLayoutRevision\.value \+= 1/)
  assert.match(designer, /connectionTargetRouteProcessId\.value = null/)
  assert.match(designer, /graphDirty\.value = true/)
  assert.match(designer, /graphDirty\.value = false/)
  assert.match(designer, /data-flow-status="unsaved"/)
  assert.match(designer, /effect="dark"/)
  assert.match(designer, />\s*未保存\s*</)
  assert.match(designer, /\.route-flow-graph-designer__unsaved\s*\{[\s\S]*flex:\s*0 0 auto/)

  assert.doesNotMatch(designer, /const connectionModeEnabled = ref\(false\)/)
  assert.doesNotMatch(designer, /handleConnectionModeToggle/)
  assert.doesNotMatch(designer, /handleConnectionNodeSelect/)
  assert.doesNotMatch(designer, /data-flow-guide="connection-mode"/)
  assert.doesNotMatch(designer, /is-connection-source/)
})

test('connection selector can use the start boundary as the source process', () => {
  assert.match(designer, /type ConnectionSourceRouteProcessId = number \| typeof PROCESS_START_NODE_ID/)
  assert.match(designer, /const createStartBoundaryConnectionOption = \(\)/)
  assert.match(designer, /routeProcessId: PROCESS_START_NODE_ID/)
  assert.match(designer, /processName: boundaryLabel\('START'\)/)
  assert.match(designer, /createStartBoundaryConnectionOption\(\),/)
  assert.match(designer, /const sourceId = connectionSourceRouteProcessId\.value/)
  assert.match(designer, /sourceId === PROCESS_START_NODE_ID/)
  assert.match(designer, /addBoundaryEdge\('START', targetId\)/)
  assert.doesNotMatch(
    designer,
    /if \(connectionSourceRouteProcessId\.value === PROCESS_START_NODE_ID\) return undefined/,
    'start boundary targets with existing incoming relations must show the replacement hint'
  )
  assert.match(
    designer,
    /focusRouteProcessId: sourceId === PROCESS_START_NODE_ID \? undefined : sourceId/
  )
})

test('connection process dropdown options display only the process name', () => {
  const optionFormatter = designer.match(
    /const formatConnectionOption = \(node: ConnectionProcessOption\) => \{([\s\S]*?)\n\}/
  )

  assert.ok(optionFormatter, 'connection option formatter should exist')
  assert.match(optionFormatter[1], /node\.processName/)
  assert.doesNotMatch(optionFormatter[1], /node\.sort/)
  assert.doesNotMatch(optionFormatter[1], /node\.processCode/)
  assert.doesNotMatch(optionFormatter[1], /node\.routeProcessId/)
  assert.doesNotMatch(optionFormatter[1], /\.join\(' \/ '\)/)
})
