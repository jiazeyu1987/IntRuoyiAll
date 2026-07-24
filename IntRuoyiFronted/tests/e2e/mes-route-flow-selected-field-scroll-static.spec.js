const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)

function extractCssRule(source, selector) {
  const pattern = new RegExp(`${selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*\\{([\\s\\S]*?)\\}`)
  const match = source.match(pattern)
  assert.ok(match, `${selector} CSS rule must exist`)
  return match[1]
}

const panelRule = extractCssRule(component, '.route-flow-graph-designer__panel')
assert.match(panelRule, /overflow:\s*hidden;/, 'right panel keeps one-screen boundary without page overflow')

const selectedFieldRule = extractCssRule(
  component,
  '.route-flow-graph-designer__selected-field-section'
)

assert.match(
  selectedFieldRule,
  /flex:\s*1\s+1\s+auto;/,
  'selected field detail section must fill remaining right panel height'
)
assert.match(
  selectedFieldRule,
  /min-height:\s*0;/,
  'selected field detail section must allow flex overflow calculation'
)
assert.match(
  selectedFieldRule,
  /overflow-y:\s*auto;/,
  'selected field detail section must show a scrollbar when form bindings exceed visible height'
)
assert.match(
  selectedFieldRule,
  /overscroll-behavior:\s*contain;/,
  'selected field detail section must keep wheel scrolling inside the detail card'
)

const recordBindingRule = extractCssRule(component, '.route-flow-graph-designer__record-binding-list')
assert.match(
  recordBindingRule,
  /min-height:\s*0;/,
  'dynamic form binding list must not force the detail section past its scroll boundary'
)

console.log('mes-route-flow-selected-field-scroll-static PASS')
