const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const panelSource = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
).replace(/\r\n/g, '\n')
const contextSource = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts'),
  'utf8'
).replace(/\r\n/g, '\n')

const sliceBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `${label} missing start marker`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.ok(end > start, `${label} missing end marker`)
  return source.slice(start, end)
}

const initializeProductionSelection = sliceBetween(
  panelSource,
  'const initializeProductionSelection = async () => {',
  'onMounted(async () => {',
  'production initial selection'
)
const hydrateContextFromRoute = sliceBetween(
  panelSource,
  'const hydrateContextFromRoute = () => {',
  'const firstRouteQueryText',
  'route context hydration'
)

assert.match(
  initializeProductionSelection,
  /const\s+activeOrders\s*=\s*await\s+loadFrontlineProductionActiveOrders\(deviceState\)/,
  'Frontline production must load the realtime active-order list before selecting an order.'
)
assert.match(
  initializeProductionSelection,
  /const\s+initialActiveOrder\s*=\s*activeOrders\.find\(\(order\)\s*=>\s*!order\.readBlocked\)/,
  'Frontline production must choose the current selectable order from the realtime active-order list.'
)
assert.doesNotMatch(
  initializeProductionSelection,
  /requestedActiveOrder|context\.workOrderId|order\.workOrderId\s*===/,
  'Frontline production initialization must not match or branch on a URL workOrderId.'
)
assert.match(
  initializeProductionSelection,
  /routeId:\s*initialActiveOrder\.routeId[\s\S]*await\s+handleSelectActiveOrder\(\s*initialActiveOrder\s*,\s*requestedProcessIdentity\s*\)/,
  'Frontline production must select the realtime active-order row and derive process selection from that row.'
)
assert.match(
  hydrateContextFromRoute,
  /context\.workOrderId\s*=\s*undefined/,
  'Frontline startup must not hydrate workOrderId from the URL route query.'
)
assert.doesNotMatch(
  hydrateContextFromRoute,
  /firstRouteQueryNumber\(\['workOrderId', 'productionOrderId', 'orderId'\]\)/,
  'Route workOrderId aliases must not drive frontline startup context.'
)

const productionOrderSelection = sliceBetween(
  contextSource,
  'export const selectFrontlineProductionActiveOrder',
  'export const selectFrontlinePqcActiveOrder',
  'production active-order process loading'
)
assert.match(
  productionOrderSelection,
  /getFrontlineProductionActiveOrderProcesses\(\s*activeOrder\.activeOrderId\s*\)/,
  'Frozen process requests must use the matched realtime active-order ID.'
)
assert.doesNotMatch(
  productionOrderSelection,
  /getFrontlineProductionActiveOrderProcesses\(\s*activeOrder\.workOrderId\s*\)/,
  'Frozen process requests must never use the production work-order ID as activeOrderId.'
)

console.log('PASS: frontline production initial selection ignores URL workOrderId')
