const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const componentPath = path.join(
  frontendRoot,
  'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'
)
const source = fs.readFileSync(componentPath, 'utf8')

const assertMatch = (pattern, message) => assert.match(source, pattern, message)

assertMatch(
  /const isFrozenRouteVersionView = computed\([\s\S]*routeVersionEditContext\?\.routeVersionId[\s\S]*lifecycleStatus !== 'DRAFT'[\s\S]*\)/,
  '流转关系图必须声明非 DRAFT 候选版本的冻结只读态。'
)

assertMatch(
  /const routeFlowWriteControlsDisabled = computed\([\s\S]*isFrozenRouteVersionView\.value[\s\S]*\)/,
  '流转关系图必须把审批中/待发布候选纳入页面级写入控件禁用条件。'
)

assertMatch(
  /const showRouteFlowMutationControls = computed\([\s\S]*isEditable\.value[\s\S]*isFrozenRouteVersionView\.value[\s\S]*\)/,
  '冻结态必须保留写入控制位并显示为 disabled，不能通过隐藏按钮掩盖状态机门禁。'
)

assertMatch(
  /const canMutateRouteFlow = computed\([\s\S]*!routeFlowWriteControlsDisabled\.value[\s\S]*\)/,
  '流转关系图必须使用统一 canMutateRouteFlow 判断拖拽、连接、删除和保存等写入动作。'
)

for (const action of [
  'add-route-process',
  'connect-route-process',
  'save-route-flow',
  'delete-route-process',
  'delete-selected-edge',
  'delete-boundary-edge-list',
  'delete-edge-list'
]) {
  assertMatch(
    new RegExp(`data-flow-action="${action}"[\\s\\S]*:disabled="[\\s\\S]*routeFlowWriteControlsDisabled`),
    `冻结态必须禁用 ${action} 控制入口。`
  )
}

for (const action of [
  'add-route-process',
  'connect-route-process',
  'save-route-flow',
  'delete-route-process',
  'delete-selected-edge',
  'delete-boundary-edge-list',
  'delete-edge-list'
]) {
  assertMatch(
    new RegExp(`v-if="showRouteFlowMutationControls"[\\s\\S]*data-flow-action="${action}"`),
    `冻结态必须保留 ${action} 控制入口可见，并通过 disabled 表达不可操作。`
  )
}

assertMatch(
  /:edges-connectable="canMutateRouteFlow"[\s\S]*:edges-updatable="canMutateRouteFlow"[\s\S]*:nodes-connectable="canMutateRouteFlow"[\s\S]*:nodes-draggable="canMutateRouteFlow"/,
  '冻结态必须禁用关系图连接、更新和拖拽能力。'
)

assertMatch(
  /draggable:\s*canMutateRouteFlow\.value[\s\S]*connectable:\s*canMutateRouteFlow\.value/,
  '冻结态必须同步禁用节点模型上的拖拽和连接能力。'
)

assertMatch(
  /const processDetailInterestMutationDisabled = computed\([\s\S]*routeFlowWriteControlsDisabled\.value[\s\S]*\)/,
  '冻结态左侧关注列新增/删除也必须禁用，避免控制栏仍可操作。'
)

assertMatch(
  /const capacityOverrideButtonDisabled = computed\([\s\S]*routeFlowWriteControlsDisabled\.value[\s\S]*\)/,
  '冻结态必须禁用左侧产能覆盖按钮。'
)

for (const guard of [
  'syncRouteNodesFromFlowModel',
  'handleConnect',
  'handleEdgesChange',
  'handleRouteProcessNodeKeydown',
  'handleOpenRouteProcessAddDialog',
  'handleRouteProcessDelete',
  'handleEdgeUpdate',
  'handleNodesChange',
  'handleCanvasDeleteKeydown'
]) {
  assertMatch(
    new RegExp(`const ${guard}[\\s\\S]*canMutateRouteFlow\\.value`),
    `${guard} 必须使用统一冻结态写入门禁。`
  )
}

console.log('PASS: MES route approval freeze control bars static contract')
