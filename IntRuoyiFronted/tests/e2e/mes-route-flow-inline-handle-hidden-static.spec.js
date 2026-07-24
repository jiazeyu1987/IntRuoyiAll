const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const assert = require('node:assert/strict')

const designerFile = path.resolve(
  __dirname,
  '../../src/views/mes/pro/route/RouteFlowGraphDesigner.vue'
)

const designer = fs.readFileSync(designerFile, 'utf8')

const routeProcessTemplate = designer.match(
  /<template #node-route-process="\{ data \}">([\s\S]*?)<\/template>/
)
const boundaryTemplate = designer.match(
  /<template #node-route-boundary="\{ data \}">([\s\S]*?)<\/template>/
)

test('all route flow connector handles are hidden while flow anchors remain mounted', () => {
  assert.ok(routeProcessTemplate, 'route process node template should exist')
  assert.ok(boundaryTemplate, 'route boundary node template should exist')
  const template = `${routeProcessTemplate[1]}\n${boundaryTemplate[1]}`

  assert.match(template, /id="target-left"/)
  assert.match(template, /id="source-right"/)
  assert.match(template, /type="target"/)
  assert.match(template, /type="source"/)
  assert.doesNotMatch(
    template,
    /class="[^"]*\broute-flow-graph-designer__handle\b[^"]*\bis-visible\b[^"]*"/,
    'connector circles should not be visibly rendered on any route flow node'
  )
})
