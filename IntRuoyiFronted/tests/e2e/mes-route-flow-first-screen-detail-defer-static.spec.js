const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)

function extractConstFunction(source, name) {
  const startText = `const ${name} = async () => {`
  const start = source.indexOf(startText)
  assert.notEqual(start, -1, `${name} function must exist`)
  let depth = 0
  let end = -1
  for (let index = start + startText.indexOf('{'); index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) {
        end = index + 1
        break
      }
    }
  }
  assert.notEqual(end, -1, `${name} function must have a complete body`)
  return source.slice(start, end)
}

const loadGraphBlock = extractConstFunction(component, 'loadGraph')

assert.ok(
  component.includes('const deferSelectedProcessDetailLoad = () => {'),
  'selected process detail load must have a dedicated deferred helper'
)
assert.ok(
  loadGraphBlock.includes('deferSelectedProcessDetailLoad()'),
  'graph loading must schedule selected process detail after the graph is ready'
)
assert.ok(
  !loadGraphBlock.includes('await loadSelectedProcessDetail'),
  'graph loading must not await selected process detail requests'
)
assert.ok(
  loadGraphBlock.indexOf('syncFlowElements()') < loadGraphBlock.indexOf('deferSelectedProcessDetailLoad()'),
  'graph elements must be synchronized before deferred detail loading starts'
)

console.log('mes-route-flow-first-screen-detail-defer-static PASS')
