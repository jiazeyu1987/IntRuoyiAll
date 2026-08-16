const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
).replace(/\r\n/g, '\n')
const context = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts'),
  'utf8'
).replace(/\r\n/g, '\n')

function extractFunction(source, declaration) {
  const start = source.indexOf(declaration)
  assert.ok(start >= 0, 'missing function: ' + declaration)
  const open = source.indexOf('{', start)
  assert.ok(open > start, 'missing function body: ' + declaration)
  let depth = 0
  for (let index = open; index < source.length; index += 1) {
    if (source[index] === '{') depth += 1
    if (source[index] === '}') {
      depth -= 1
      if (depth === 0) return source.slice(start, index + 1)
    }
  }
  assert.fail('unterminated function: ' + declaration)
}

const selectOrder = extractFunction(
  context,
  'export const selectFrontlineProductionActiveOrder ='
)
assert.match(
  selectOrder,
  /async\s*\(/,
  'production order transition must be asynchronous because every switch refreshes formal processes.'
)
assert.match(
  selectOrder,
  /state\.selectedActiveOrder\s*=\s*activeOrder[\s\S]*state\.processOptions\s*=\s*\[\][\s\S]*await ProFeedbackApi\.getFrontlineDeviceAccountProcesses\(\)/,
  'switching orders must clear the old downstream context before re-reading formal processes.'
)
assert.match(
  selectOrder,
  /state\.productionProcessOptions\s*=\s*processes[\s\S]*process\.routeId\s*===\s*activeOrder\.routeId/,
  'the refreshed process response must replace the old source and be filtered by the new order route.'
)
assert.match(
  selectOrder,
  /productionActiveOrderSelectionRequestToken/,
  'production order refresh must reject stale process responses from earlier switches.'
)

const handleOrder = extractFunction(panel, 'const handleSelectActiveOrder =')
assert.match(
  handleOrder,
  /processes\s*=\s*await selectFrontlineProductionActiveOrder\(deviceState,\s*activeOrder\)/,
  'every production order click must await a fresh formal process read.'
)
assert.match(
  handleOrder,
  /findInitialProcess\(processes/,
  'after refresh, the process selection must be derived from the new order response.'
)
assert.doesNotMatch(
  handleOrder,
  /所选活跃订单不包含当前工序|selectedProcess\.routeId\s*!==\s*activeOrder\.routeId/,
  'the previous process must never block switching to another production order.'
)

const initialize = extractFunction(panel, 'const initializeProductionSelection =')
assert.match(
  initialize,
  /loadFrontlineProductionActiveOrders\(deviceState\)[\s\S]*(?:requestedActiveOrder\s*\|\|\s*activeOrders\[0\]|initialActiveOrder)[\s\S]*await handleSelectActiveOrder\(/,
  'initialization must choose the requested or first order and use the same refresh workflow.'
)
assert.doesNotMatch(
  initialize,
  /loadFrontlineDeviceProcesses\(deviceState\)[\s\S]*handleSelectProcess\(initialProcess\)/,
  'initialization must not select a cached global process before selecting the order.'
)

console.log('PASS: production order switch refreshes formal processes without old-process restriction')
