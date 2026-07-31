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

const routeGraph = fs.readFileSync(routeGraphPath, 'utf8').replace(/\r\n/g, '\n')

assert.match(
  routeGraph,
  /const resolveRecordBindingSlotType = \([\s\S]*?return undefined\s*\n\}/,
  'route flow detail must expose a non-fallback slot resolver for display filtering.'
)
assert.match(
  routeGraph,
  /const requireLegacyBatchRecordSlotType = \([\s\S]*?resolveRecordBindingSlotType\([\s\S]*?report\.formSlotType,[\s\S]*?report\.batchRecordReportId[\s\S]*?throw new Error\([\s\S]*?`批记录表单绑定缺少显式槽位类型/,
  'legacy batch record bindings must use one fail-fast slot resolver instead of defaulting to MAIN.'
)
assert.match(
  routeGraph,
  /const normalizeLegacyBatchRecord = \([\s\S]*?const formSlotType = requireLegacyBatchRecordSlotType\(report\)/,
  'legacy batch record loading must require an explicit slot type.'
)
assert.ok(
  (routeGraph.match(/requireLegacyBatchRecordSlotType\(report\)/g) || []).length >= 4,
  'legacy batch record display, snapshot, link and save paths must all require an explicit slot type.'
)
assert.match(
  routeGraph,
  /const getLegacyBatchRecordsBySlotType = \(formSlotType: ProRouteFlowFormSlotType\) =>[\s\S]*selectedLegacyBatchRecords\.value\.filter/,
  'batch record detail must only read formal batch record reports for the selected route process.'
)
assert.match(
  routeGraph,
  /const isRouteNodeBatchRecordConfigured = \([\s\S]*getRouteNodeLegacyBatchRecords\(node\)[\s\S]*isLegacyBatchRecordConfigured/,
  'batch record node border status must use formal batchRecordReports only.'
)
const batchRecordNodeStatusBlock = routeGraph.match(
  /const isRouteNodeBatchRecordConfigured = \(node: RouteFlowNodeVO\) =>([\s\S]*?)\n\n/
)
assert.ok(batchRecordNodeStatusBlock, 'batch record node border status helper must exist.')
assert.doesNotMatch(
  batchRecordNodeStatusBlock[1],
  /getRouteNodeBatchRecordBindings\(node\)/,
  'batch record node border status must not read formBindings.'
)
assert.match(
  routeGraph,
  /key:\s*'batchRecordFormNames'[\s\S]*value:\s*buildBatchRecordFormValue\(\)[\s\S]*links:\s*buildBatchRecordFormLinks\(\)/,
  'the batch record form field must use dedicated formal batchRecordReports value and links.'
)
const batchRecordValueBlock = routeGraph.match(
  /const buildBatchRecordFormValue = \(\) => \{([\s\S]*?)\n\}/
)
assert.ok(batchRecordValueBlock, 'batch record form field must define a dedicated value builder.')
assert.doesNotMatch(
  batchRecordValueBlock[1],
  /selectedRecordBindings|getRecordBindingsBySlotType|formBindings/,
  'batch record form value must not merge dynamic form slots.'
)
const batchRecordLinksBlock = routeGraph.match(
  /const buildBatchRecordFormLinks = \(\): ProcessDetailLinkItem\[\] =>([\s\S]*?)\n\n/
)
assert.ok(batchRecordLinksBlock, 'batch record form field must define a dedicated link builder.')
assert.doesNotMatch(
  batchRecordLinksBlock[1],
  /selectedRecordBindings|getRecordBindingsBySlotType|formBindings/,
  'batch record form links must not merge dynamic form slots.'
)

console.log('mes-route-flow-batch-record-detail-slot-filter-static PASS')
