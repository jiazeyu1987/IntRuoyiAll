const fs = require('fs')
const path = require('path')

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

if (!packageJson.dependencies || !packageJson.dependencies['@vue-flow/core']) {
  throw new Error('@vue-flow/core dependency missing')
}

assertIncludes(component, "from '@vue-flow/core'", 'vue-flow import')
assertIncludes(component, '<VueFlow', 'VueFlow component')
assertIncludes(component, '<Handle', 'VueFlow handle component')
assertIncludes(component, '@connect="handleConnect"', 'edge connect interaction')
assertIncludes(component, '@node-drag-stop="handleNodeDragStop"', 'node drag persistence')
assertIncludes(component, 'fitView', 'fit screen operation')
assertIncludes(component, 'route-flow-graph-designer__flow', 'flow canvas class')
assertIncludes(
  component,
  'height: calc(100vh - 210px);',
  'one-screen fixed height fills removed bottom space'
)
assertIncludes(
  component,
  'max-height: none;',
  'flow graph must not keep the old bottom-space height cap'
)
assertIncludes(component, 'overflow: hidden;', 'no page scrollbar overflow')
assertIncludes(
  component,
  'grid-template-columns: 220px minmax(0, 1fr) 260px;',
  'single-screen process and relation panels'
)
assertIncludes(
  component,
  'const MAX_VISIBLE_COLUMNS = 5',
  'bounded visible columns for dense routes'
)
assertIncludes(component, 'defaultNodePosition', 'single-screen default node layout')
assertIncludes(
  component,
  'row % 2 === 0 ? columnInRow : MAX_VISIBLE_COLUMNS - 1 - columnInRow',
  'snake layout keeps nodes readable'
)
assertNotIncludes(
  component,
  '已按一屏蛇形网格自动布局',
  'relation auto-layout must not downgrade to an order-based snake layout'
)
assertIncludes(
  component,
  'buildBranchLayoutPositions',
  'relation auto-layout must preserve visible branches'
)
assertIncludes(
  component,
  'resolveEdgeHandles',
  'linear draft edges must choose handles by relative node position'
)
assertIncludes(
  component,
  'sourceHandle: handles.sourceHandle',
  'flow edge source handle must be dynamic'
)
assertIncludes(
  component,
  'targetHandle: handles.targetHandle',
  'flow edge target handle must be dynamic'
)
assertIncludes(component, 'id="source-bottom"', 'vertical wrap source handle')
assertIncludes(component, 'id="target-top"', 'vertical wrap target handle')
assertIncludes(component, 'id="source-left"', 'reverse row source handle')
assertIncludes(component, 'id="target-right"', 'reverse row target handle')
assertIncludes(
  component,
  'class="route-flow-graph-designer__handle is-in is-left"',
  'left target remains the only visible target handle'
)
assertIncludes(
  component,
  'class="route-flow-graph-designer__handle is-out is-right"',
  'right source remains the only visible source handle'
)
assertIncludes(
  component,
  'route-flow-graph-designer__handle.is-anchor',
  'auxiliary edge anchors must be hidden'
)
assertIncludes(
  component,
  'pointer-events: none;',
  'hidden anchors must not create extra clickable blue circles'
)
assertIncludes(component, 'opacity: 0;', 'hidden anchors must not be visible')
assertIncludes(
  component,
  'left: -25px;',
  'right-to-left arrow source anchor must avoid the visible blue target handle'
)
assertIncludes(
  component,
  'right: -25px;',
  'right-to-left arrow target anchor must avoid the visible blue source handle'
)
assertIncludes(component, 'width: 24px;', 'large drag handle width')
assertIncludes(component, 'height: 24px;', 'large drag handle height')
assertIncludes(component, 'cursor: crosshair;', 'drag handle affordance')
assertIncludes(
  component,
  "v-if=\"selectedProcessDetailField.key === 'relationList'\"",
  'relation list on-demand field branch'
)
assertIncludes(component, 'data-flow-panel="relation-list-detail"', 'relation list detail marker')
assertIncludes(component, 'data-flow-action="delete-edge-list"', 'relation list delete action')
assertIncludes(component, 'handleEdgeSelect', 'relation list selection interaction')
assertIncludes(component, 'route-flow-graph-designer__relation-list', 'bounded relation list')
assertNotIncludes(
  component,
  'route-flow-graph-designer__relation-section',
  'standalone permanent relation list panel'
)
assertNotIncludes(component, '<h4>当前选择</h4>', 'current selection panel removed')
assertNotIncludes(
  component,
  '.route-flow-graph-designer__panel-section:first-child',
  'removed current selection fixed-height rule'
)
assertIncludes(
  component,
  'showSaveValidationToast',
  'save automatically surfaces validation result as toast'
)
assertNotIncludes(component, '保存关系图', 'standalone flow graph save button removed')
assertNotIncludes(component, '校验结果', 'validation result panel removed')
assertNotIncludes(component, '暂无校验消息', 'empty validation result panel removed')
assertNotIncludes(component, 'focusValidationTarget', 'validation panel click-to-focus removed')
assertNotIncludes(
  component,
  '<svg class="route-flow-graph-designer__edges"',
  'self-rendered SVG edge layer'
)
assertNotIncludes(component, 'canvasPoint(event', 'manual canvas coordinate fallback')

console.log('mes-route-flow-graph-one-screen-static PASS')
