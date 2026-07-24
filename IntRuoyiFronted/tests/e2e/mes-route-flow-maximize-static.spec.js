const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const component = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

const assertIncludes = (content, expected, label) => {
  assert.ok(content.includes(expected), `${label} missing: ${expected}`)
}

const assertNotIncludes = (content, forbidden, label) => {
  assert.ok(!content.includes(forbidden), `${label} must not include: ${forbidden}`)
}

const toolbarStart = component.indexOf('<div class="route-flow-graph-designer__toolbar">')
const toolbarEnd = component.indexOf('<div v-loading="loading"', toolbarStart)
assert.ok(toolbarStart >= 0 && toolbarEnd > toolbarStart, 'route flow toolbar template should exist')
const toolbar = component.slice(toolbarStart, toolbarEnd)

assertIncludes(
  component,
  ':class="{ \'is-maximized\': isRouteFlowMaximized }"',
  'route flow root must expose an explicit maximized class'
)
assertIncludes(
  toolbar,
  'data-flow-action="toggle-route-flow-maximize"',
  'toolbar must expose the maximize or restore action'
)
assertIncludes(
  toolbar,
  ':aria-pressed="isRouteFlowMaximized"',
  'maximize action must expose pressed state for accessibility'
)
assertIncludes(
  toolbar,
  ":icon=\"isRouteFlowMaximized ? 'ep:close' : 'ep:full-screen'\"",
  'maximize action must switch between maximize and restore icons'
)
assertIncludes(
  toolbar,
  "{{ isRouteFlowMaximized ? '恢复' : '最大化' }}",
  'maximize action must switch copy between maximize and restore'
)
assertIncludes(
  component,
  'const isRouteFlowMaximized = ref(false)',
  'component must keep local app fullscreen state'
)
assertIncludes(
  component,
  'const handleToggleRouteFlowMaximized = async () =>',
  'component must toggle app fullscreen state'
)
assertIncludes(
  component,
  'const handleRouteFlowMaximizeKeydown = (event: KeyboardEvent) =>',
  'component must support Esc restore'
)
assertIncludes(
  component,
  "window.addEventListener('keydown', handleRouteFlowMaximizeKeydown)",
  'component must register Esc restore listener'
)
assertIncludes(
  component,
  '.route-flow-graph-designer.is-maximized',
  'component must style app fullscreen root state'
)
assertIncludes(component, 'position: fixed;', 'maximized root must cover the viewport')
assertIncludes(component, 'z-index: 2200;', 'maximized root must float over the page shell')
assertNotIncludes(
  component,
  'data-flow-action="generate-linear-draft"',
  'toolbar must remove automatic generation action'
)
assertNotIncludes(toolbar, '刷新', 'toolbar must remove refresh button copy')
assertNotIncludes(toolbar, 'ep:refresh', 'toolbar must remove refresh icon')
assertNotIncludes(toolbar, '自动生成', 'toolbar must remove automatic generation copy')
assertNotIncludes(component, 'handleGenerateLinearDraft', 'component must remove unused automatic generation handler')
assertNotIncludes(component, 'requestFullscreen', 'app fullscreen must not use browser Fullscreen API')
assertNotIncludes(component, 'fullscreenElement', 'app fullscreen must not use browser Fullscreen API state')

console.log('mes-route-flow-maximize-static PASS')
