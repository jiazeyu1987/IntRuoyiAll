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
  /const isMainBatchRecordForm = \(report: RouteFlowLegacyBatchRecord\) =>[\s\S]*resolveRecordBindingSlotType\(report\.formSlotType\) === 'MAIN'/,
  'batch record detail must only include formal reports explicitly assigned to MAIN.'
)
assert.match(
  routeGraph,
  /const getSelectedBatchRecordForms = \(\) =>[\s\S]*selectedLegacyBatchRecords\.value\.filter\(isMainBatchRecordForm\)/,
  'batch record detail must only read formal batch record reports for the selected route process.'
)
assert.match(
  routeGraph,
  /const isRouteNodeBatchRecordFormConfigured = \(node: RouteFlowNodeVO\) =>[\s\S]*getRouteNodeBatchRecordForms\(node\)\.some\(isLegacyBatchRecordConfigured\)/,
  'batch record node border status must only use formal batch record reports.'
)
assert.doesNotMatch(
  routeGraph,
  /isMainBatchRecordForm[\s\S]*normalizeRecordBindingSlotType\(report\.formSlotType,\s*report\.batchRecordReportId\)/,
  'formal batch record filtering must not infer MAIN from a report ID.'
)
assert.match(
  routeGraph,
  /key:\s*'batchRecordFormNames'[\s\S]*value:\s*buildBatchRecordFormValue\(\)[\s\S]*links:\s*buildBatchRecordFormLinks\(\)/,
  'the batch record form field must keep rendering the MAIN-only value and links.'
)

console.log('mes-route-flow-batch-record-detail-slot-filter-static PASS')
