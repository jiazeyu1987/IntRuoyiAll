const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const apiSource = fs.readFileSync(
  path.resolve(__dirname, '../../../../api/mes/pro/feedback/index.ts'),
  'utf8'
).replace(/\r\n/g, '\n')
const contextSource = fs.readFileSync(
  path.join(__dirname, 'frontlineDeviceEmployeeContext.ts'),
  'utf8'
).replace(/\r\n/g, '\n')
const panelSource = fs.readFileSync(
  path.join(__dirname, 'FrontlineFixedTemplatePanel.vue'),
  'utf8'
).replace(/\r\n/g, '\n')

const sliceBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `${label} missing start marker`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.ok(end > start, `${label} missing end marker`)
  return source.slice(start, end)
}

assert.match(
  apiSource,
  /getFrontlineProductionActiveOrderProcesses:[\s\S]*active-order\/processes[\s\S]*params:\s*\{\s*activeOrderId\s*\}/,
  'Production must expose an active-order frozen process API.'
)

const productionOrderSelection = sliceBetween(
  contextSource,
  'export const selectFrontlineProductionActiveOrder',
  'export const selectFrontlinePqcActiveOrder',
  'production active-order selection'
)
assert.match(
  productionOrderSelection,
  /getFrontlineProductionActiveOrderProcesses\(\s*activeOrder\.activeOrderId\s*\)/,
  'Selecting a production active order must load that order frozen processes.'
)
assert.doesNotMatch(
  productionOrderSelection,
  /getFrontlineDeviceAccountProcesses\(/,
  'Production active-order selection must not read the current route process list.'
)
assert.match(
  productionOrderSelection,
  /selectedProcess\s*=\s*undefined[\s\S]*selectedEmployee\s*=\s*undefined[\s\S]*runtimeConfig\s*=\s*undefined[\s\S]*template\s*=\s*undefined/,
  'Switching active orders must clear process, employee, runtime config, and template state.'
)

assert.match(
  contextSource,
  /getFrontlineRuntimeConfig\(\{[\s\S]*activeOrderId:\s*process\.activeOrderId[\s\S]*routeProcessId:\s*process\.routeProcessId/,
  'Runtime-config requests must carry the selected active order identity.'
)
assert.match(
  contextSource,
  /FrontlineSwitchActualEmployeeReqVO[\s\S]*activeOrderId/,
  'Employee switching must preserve active-order identity.'
)

const applyOrderContext = sliceBetween(
  panelSource,
  'const applyActiveOrderToContext',
  'const applyProcessToContext',
  'active-order context reset'
)
assert.match(
  applyOrderContext,
  /context\.routeProcessId\s*=\s*undefined[\s\S]*context\.processId\s*=\s*undefined[\s\S]*context\.actualEmployeeId\s*=\s*undefined/,
  'Changing active orders must clear the previous process and employee context.'
)

console.log('PASS: frontline production uses active-order frozen processes')
