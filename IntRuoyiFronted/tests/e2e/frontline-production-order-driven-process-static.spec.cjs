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

function extractFunction(source, name) {
  const start = source.indexOf('const ' + name + ' =')
  assert.ok(start >= 0, 'missing function: ' + name)
  const open = source.indexOf('{', start)
  assert.ok(open > start, 'missing function body: ' + name)
  let depth = 0
  for (let index = open; index < source.length; index += 1) {
    if (source[index] === '{') depth += 1
    if (source[index] === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(start, index + 1)
      }
    }
  }
  assert.fail('unterminated function: ' + name)
}

assert.match(
  context,
  /productionProcessOptions:\s*FrontlineDeviceRouteProcessVO\[\]/,
  'state must retain the complete formal production process source for route switching.'
)
assert.match(
  context,
  /export const selectFrontlineProductionActiveOrder/,
  'production order selection must have one formal state transition.'
)
const selectOrder = extractFunction(context, 'selectFrontlineProductionActiveOrder')
assert.match(
  selectOrder,
  /state\.selectedActiveOrder\s*=\s*activeOrder[\s\S]*state\.selectedProcess\s*=\s*undefined/,
  'switching orders must select the order and clear the previous process first.'
)
assert.match(
  selectOrder,
  /await ProFeedbackApi\.getFrontlineDeviceAccountProcesses\(\)[\s\S]*processes\.filter\([\s\S]*process\.routeId\s*===\s*activeOrder\.routeId/,
  'process options must be refreshed and derived only from the selected work order route.'
)
assert.match(
  selectOrder,
  /state\.processOptions\s*=\s*routeProcesses/,
  'the process picker must receive only the selected order route processes.'
)
assert.match(
  selectOrder,
  /new Error\(.+正式工艺路线.+\)[\s\S]*throw error/,
  'missing formal route processes must fail explicitly.'
)

const handleOrder = extractFunction(panel, 'handleSelectActiveOrder')
assert.match(
  handleOrder,
  /await selectFrontlineProductionActiveOrder\(deviceState,\s*activeOrder\)/,
  'production order clicks must await the order-driven process refresh transition.'
)
assert.match(
  handleOrder,
  /findInitialProcess\(processes,\s*requestedProcessIdentity\)[\s\S]*try\s*\{[\s\S]*await handleSelectProcess\(initialProcess\)[\s\S]*\}\s*catch\s*\(error\)\s*\{[\s\S]*message\.error\(resolveErrorMessage\(error\)\)/,
  'production order clicks must select the first process from the new route.'
)
assert.doesNotMatch(
  handleOrder,
  /所选活跃订单不包含当前工序/,
  'switching to another route must not be rejected because the old process belongs to the previous order.'
)

const initialize = extractFunction(panel, 'initializeProductionSelection')
assert.match(
  initialize,
  /loadFrontlineProductionActiveOrders\(deviceState\)[\s\S]*requestedActiveOrder\s*\|\|\s*activeOrders\[0\]/,
  'entering production must choose the first formal work order when no requested order matches.'
)
assert.match(
  initialize,
  /requestedProcessIdentity[\s\S]*await handleSelectActiveOrder\(initialActiveOrder,\s*requestedProcessIdentity\)/,
  'initialization must reuse the same order-driven process workflow.'
)
assert.doesNotMatch(
  initialize,
  /findInitialProcess\(processes\)[\s\S]*handleSelectProcess\(initialProcess\)[\s\S]*initialActiveOrder/,
  'initialization must not select a global process before selecting the work order.'
)

console.log('PASS: frontline production work order drives the route-specific process list')
