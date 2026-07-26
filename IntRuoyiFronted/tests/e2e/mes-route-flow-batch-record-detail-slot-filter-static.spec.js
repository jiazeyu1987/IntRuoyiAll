const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const routeGraphPath = path.join(
  frontendRoot,
  'src',
  'views',
  'mes',
  'pro',
  'route',
  'RouteFlowGraphDesigner.vue'
)

const routeGraph = fs.readFileSync(routeGraphPath, 'utf8')

assert.match(
  routeGraph,
  /const resolveRecordBindingSlotType = \([\s\S]*?return undefined\s*\n\}/,
  'route flow detail must expose a non-fallback slot resolver for display filtering.'
)
assert.match(
  routeGraph,
  /const getRecordBindingsBySlotType = \(formSlotType: ProRouteFlowFormSlotType\) =>[\s\S]*resolveRecordBindingSlotType\(binding\.formSlotType, binding\.formBindingKey\) === formSlotType/,
  'batch record detail must only include form bindings explicitly assigned to MAIN.'
)
assert.match(
  routeGraph,
  /const getLegacyBatchRecordsBySlotType = \(formSlotType: ProRouteFlowFormSlotType\) =>[\s\S]*resolveRecordBindingSlotType\(report\.formSlotType, report\.batchRecordReportId\) === formSlotType/,
  'batch record detail must only include legacy reports explicitly assigned to MAIN.'
)
assert.match(
  routeGraph,
  /const isRouteNodeRecordBindingConfigured = \([\s\S]*resolveRecordBindingSlotType\(binding\.formSlotType, binding\.formBindingKey\) === formSlotType[\s\S]*resolveRecordBindingSlotType\(report\.formSlotType, report\.batchRecordReportId\) === formSlotType/,
  'batch record node border status must not treat unrelated route forms as MAIN bindings.'
)
assert.doesNotMatch(
  routeGraph,
  /getRecordBindingsBySlotType[\s\S]*normalizeRecordBindingSlotType\(binding\.formSlotType, binding\.formBindingKey\) === formSlotType/,
  'right-side detail filtering must not default missing or unrelated route forms into MAIN.'
)
assert.match(
  routeGraph,
  /key:\s*'batchRecordFormNames'[\s\S]*value:\s*buildRecordBindingValue\('MAIN'\)[\s\S]*links:\s*buildRecordBindingLinks\('MAIN'\)/,
  'the batch record form field must keep rendering the MAIN-only value and links.'
)

console.log('mes-route-flow-batch-record-detail-slot-filter-static PASS')
