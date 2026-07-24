const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const graph = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const editPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const editPageBottomActions =
  editPage.match(/class="route-edit-page__actions"[\s\S]*?<\/div>/)?.[0] || ''

const assertNotIncludes = (content, expected, label) => {
  if (content.includes(expected)) {
    throw new Error(`${label} must be removed: ${expected}`)
  }
}

assertNotIncludes(graph, '<strong>工序流转关系</strong>', 'top summary title')
assertNotIncludes(graph, '适配屏幕', 'fit screen toolbar button label')
assertNotIncludes(graph, '@click="handleFitScreen"', 'fit screen toolbar button action')
assertNotIncludes(graph, '<h4>选中工序详情</h4>', 'selected process detail title')
assertNotIncludes(graph, 'route-flow-graph-designer__process-detail-summary', 'selected process detail summary card')
assertNotIncludes(graph, '<h4>当前选择</h4>', 'current selection side panel')
assertNotIncludes(editPageBottomActions, '返回列表', 'bottom back-to-list button')
assertNotIncludes(editPage, '@click="handleBack"', 'bottom back-to-list action')
assertNotIncludes(editPage, 'const handleBack =', 'unused exact back handler')
assert.ok(
  /data-flow-action="save-route-flow"[\s\S]*?@click="handleRequestSubmit"[\s\S]*?保 存/.test(
    graph
  ),
  'save action must move into the top flow graph toolbar after removing the bottom red-framed area'
)
assert.ok(
  graph.includes("v-if=\"selectedProcessDetailField.key === 'relationList'\"") &&
    graph.includes('data-flow-panel="relation-list-detail"'),
  'relation list must remain as an on-demand selected field detail'
)
assert.ok(
  !graph.includes('route-flow-graph-designer__relation-section'),
  'relation list must not remain as a standalone permanent panel'
)
assert.ok(
  !graph.includes('data-flow-action="toggle-key-process"'),
  'key process switch must not remain in the red-box detail panel'
)

console.log('mes-route-flow-red-frame-removal-static PASS')
