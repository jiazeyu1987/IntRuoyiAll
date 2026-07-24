const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const graphDesigner = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const formContent = read('src/views/mes/pro/route/RouteFormContent.vue')

assert.match(
  formContent,
  /<RouteFlowGraphDesigner[\s\S]*:active-route-version-no="formData\.activeRouteVersionNo"/,
  'RouteFormContent 必须把当前已发布版本号传给关系图。'
)
assert.doesNotMatch(
  formContent,
  /<RouteFlowGraphDesigner[\s\S]*:pending-route-version-no="formData\.pendingRouteVersionNo"/,
  '关系图只展示当前查看版本，不应再传入待发布版本号做对照展示。'
)

assert.match(
  graphDesigner,
  /activeRouteVersionNo\?:\s*string/,
  'RouteFlowGraphDesigner 必须声明当前已发布版本号 prop，用于生效版本视图。'
)
assert.doesNotMatch(
  graphDesigner,
  /pendingRouteVersionNo\?:|pendingRouteVersionStatus\?:/,
  'RouteFlowGraphDesigner 当前版本单标签不应再接收待发布版本对照 props。'
)

assert.match(
  graphDesigner,
  /data-flow-status="route-version-summary"/,
  '关系图工具栏必须提供稳定的版本标识区域。'
)
assert.match(graphDesigner, /当前查看：/, '关系图必须显示当前查看的是哪个版本状态。')

assert.match(
  graphDesigner,
  /currentRouteVersionNoText[\s\S]*props\.routeVersionEditContext\?\.versionNo[\s\S]*props\.activeRouteVersionNo/,
  '当前版本号必须来自当前候选上下文或当前已发布版本。'
)
assert.match(
  graphDesigner,
  /currentRouteVersionViewLabel[\s\S]*currentRouteVersionNoText\.value/,
  '当前查看标签必须同时包含当前查看类型和当前版本号。'
)
assert.match(
  graphDesigner,
  /const resolveRouteVersionStatusLabel/,
  '关系图版本标识必须复用明确的生命周期状态文案。'
)

const summaryStart = graphDesigner.indexOf('data-flow-status="route-version-summary"')
assert.notStrictEqual(summaryStart, -1, '关系图版本标识区域必须存在。')
const summaryEnd = graphDesigner.indexOf('</div>', summaryStart)
const versionSummaryTemplate = graphDesigner.slice(summaryStart, summaryEnd)
const versionPillCount = (
  versionSummaryTemplate.match(/class="route-flow-graph-designer__version-pill/g) || []
).length
assert.strictEqual(versionPillCount, 1, '关系图版本标识区域只能显示一个当前查看版本标签。')
assert.doesNotMatch(
  versionSummaryTemplate,
  /candidateRouteVersion|publishedRouteVersion|草稿版本：|已发布版本：/,
  '关系图红框内不应再显示草稿版本/已发布版本对照标签。'
)

for (const className of [
  'route-flow-graph-designer__route-title',
  'route-flow-graph-designer__version-summary',
  'route-flow-graph-designer__version-pill'
]) {
  assert.match(
    graphDesigner,
    new RegExp(className),
    `关系图工具栏必须包含 ${className} 样式。`
  )
}

console.log('mes-route-flow-version-badge-static PASS')
