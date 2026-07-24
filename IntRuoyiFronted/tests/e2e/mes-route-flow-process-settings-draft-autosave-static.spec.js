const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(read('package.json'))
const graphComponent = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const routeFormContent = read('src/views/mes/pro/route/RouteFormContent.vue')
const routeEditPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const routeProcessList = read('src/views/mes/pro/route/RouteProcessList.vue')

assert.equal(
  packageJson.scripts?.['e2e:mes:route-flow-process-settings-draft-autosave:static'],
  'node tests/e2e/mes-route-flow-process-settings-draft-autosave-static.spec.js',
  'package.json must expose the process settings draft autosave static contract'
)

assert.doesNotMatch(
  graphComponent,
  /data-flow-action="save-selected-process-settings"[\s\S]*?保存工序设置/,
  'flow graph selected process panel must not keep a dedicated process setting save button'
)
assert.doesNotMatch(
  graphComponent,
  /保存工序设置/,
  'flow graph selected process panel must remove the dedicated process setting save label'
)
assert.doesNotMatch(
  graphComponent,
  /data-flow-field-editor="productionQuantityFactor"/,
  'flow graph selected process panel must not render production factor editor'
)
assert.match(
  graphComponent,
  /const normalizeProductionQuantityFactor = \(value\?: number \| string \| null\) =>[\s\S]*toFixed\(2\)/,
  'flow graph production factor draft normalization must persist at most two decimals'
)
assert.match(
  routeProcessList,
  /<el-input-number[\s\S]*:precision="2"[\s\S]*:step="0\.01"[\s\S]*data-route-process-setting-field="productionQuantityFactor"/,
  'route process list production factor editor must allow at most two decimal places'
)
assert.match(
  routeProcessList,
  /const normalizeProductionQuantityFactor = \(value\?: number \| string \| null\) =>[\s\S]*toFixed\(2\)/,
  'route process list production factor normalization must persist at most two decimals'
)

for (const token of [
  'selectedProcessAttributeDrafts',
  'saveSelectedProcessAttributeDrafts',
  'buildRouteProcessUpdatePayload',
  'routeProcessUpdates: buildRouteProcessUpdatePayload()',
  'hasWorkspaceDraftChanges',
  'discardWorkspaceDraftChanges'
]) {
  assert.match(graphComponent, new RegExp(token), `flow graph must include ${token}`)
}

assert.match(
  graphComponent,
  /const handleKeyProcessToggle = async \(enabled: boolean\) =>[\s\S]*markGraphDraftChanged\(\)[\s\S]*关键工序已保存为草稿/,
  'key process toggle must save to local workspace draft instead of immediately persisting'
)
assert.doesNotMatch(
  graphComponent.match(/const handleKeyProcessToggle = async \(enabled: boolean\) =>[\s\S]*?\n}/)?.[0] || '',
  /ProRouteProcessApi\.updateRouteProcess/,
  'key process toggle handler must not write the backend before the top-level save'
)
assert.match(
  graphComponent,
  /const saveFromParent = async \(\) =>[\s\S]*persistRouteProcessDraftChanges\(\)[\s\S]*saveSelectedProcessAttributeDrafts\(\)/,
  'top-level route save must persist flow graph and process setting drafts'
)
assert.doesNotMatch(
  graphComponent,
  /ProRouteProcessApi\.updateRouteProcess/,
  'key process drafts must be persisted through routeProcessUpdates instead of direct route-process/update'
)

assert.match(
  routeFormContent,
  /工作区有变动，是否保存？/,
  'route form must prompt before leaving a dirty flow workspace'
)
assert.match(routeFormContent, /confirmButtonText: '保存'/, 'exit prompt must offer save')
assert.match(routeFormContent, /cancelButtonText: '不保存'/, 'exit prompt must offer discard')
assert.match(
  routeFormContent,
  /discardWorkspaceDraftChanges\(\)/,
  'discarding exit must clear in-memory flow workspace drafts'
)
assert.match(
  routeEditPage,
  /const confirmRouteEditPageLeave = async[\s\S]*confirmFlowGraphDraftSaveBeforeExit[\s\S]*onBeforeRouteLeave\([\s\S]*confirmRouteEditPageLeave/,
  'route edit page must route non-toolbar exits through the unified guard and still protect dirty flow workspaces'
)

console.log('mes-route-flow-process-settings-draft-autosave-static PASS')
