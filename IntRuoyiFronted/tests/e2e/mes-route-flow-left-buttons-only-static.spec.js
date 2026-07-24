const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)

function assertIncludes(source, expected, label) {
  assert.ok(source.includes(expected), `${label}: expected ${JSON.stringify(expected)}`)
}

function assertNotIncludes(source, unexpected, label) {
  assert.ok(!source.includes(unexpected), `${label}: must not include ${JSON.stringify(unexpected)}`)
}

const leftFieldBlock = component.match(
  /v-for="field in selectedProcessDetailFields"[\s\S]*?data-flow-action="remove-process-detail-field"/
)
assert.ok(leftFieldBlock, 'left selected process detail field block must exist')
const detailButtonStyle = component.match(
  /\.route-flow-graph-designer__selected-detail-button \{[\s\S]*?\n\}/
)
assert.ok(detailButtonStyle, 'selected detail button style block must exist')

assertIncludes(
  leftFieldBlock[0],
  'data-flow-action="select-process-detail-field"',
  'left field item must expose a button action'
)
assertIncludes(
  leftFieldBlock[0],
  'data-flow-detail-field-button',
  'left field item must expose a stable button marker'
)
assertIncludes(
  leftFieldBlock[0],
  ':aria-pressed="selectedProcessDetailFieldKey === field.key"',
  'left field button must expose selected state'
)
assertNotIncludes(
  leftFieldBlock[0],
  'route-flow-graph-designer__selected-detail-editor',
  'left field item must not render field editors'
)
assertNotIncludes(
  leftFieldBlock[0],
  'field.links?.length',
  'left field item must not render related action links'
)
assertNotIncludes(
  leftFieldBlock[0],
  'formatProcessDetailText(field.value)',
  'left field item must not render concrete field values'
)
assertNotIncludes(
  leftFieldBlock[0],
  'isProcessDetailFieldEditable(field.key)',
  'left field item must not own editability rendering'
)
assertNotIncludes(
  component,
  'route-flow-graph-designer__process-config-item-hint',
  'process config hint under add selector must be removed'
)
assertNotIncludes(
  component,
  '先从下拉选择表单槽位',
  'old process config instruction text must be removed'
)
assertIncludes(
  detailButtonStyle[0],
  'width: 100%;',
  'field button hit area must fill the left content column'
)
assertIncludes(
  detailButtonStyle[0],
  'min-height: 38px;',
  'field button hit area must be taller than the text label'
)
assertNotIncludes(
  detailButtonStyle[0],
  'width: fit-content;',
  'field button hit area must not be limited to text width'
)

const rightFieldPanel = component.match(
  /data-flow-panel="selected-field-detail"[\s\S]*?<\/aside>/
)
assert.ok(rightFieldPanel, 'right selected field detail panel must exist')
assertIncludes(
  rightFieldPanel[0],
  'formatProcessDetailText(selectedProcessDetailField.value)',
  'right field panel must render concrete field value'
)
assertIncludes(
  rightFieldPanel[0],
  'selectedProcessDetailField.links?.length',
  'right field panel must render existing related actions'
)
assertIncludes(
  rightFieldPanel[0],
  'selectedProcessDetailFieldSource',
  'right field panel must render field source'
)
assertIncludes(
  rightFieldPanel[0],
  "selectedProcessDetailField.key === 'relationList'",
  'right field panel must own the on-demand relation list branch'
)
assertNotIncludes(
  component,
  'route-flow-graph-designer__relation-section',
  'relation list must not remain as a standalone permanent panel'
)

console.log('mes-route-flow-left-buttons-only-static PASS')
