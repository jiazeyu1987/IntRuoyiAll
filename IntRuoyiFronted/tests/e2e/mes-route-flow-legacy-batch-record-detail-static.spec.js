const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')

const flowConfigApi = read('src/api/mes/pro/route/flowconfig.ts')
const flowGraph = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

const batchRecordInterface = flowConfigApi.match(
  /export interface ProRouteFlowBatchRecordVO \{([\s\S]*?)\n\}/
)
assert.ok(batchRecordInterface, 'flow config API must expose legacy batch record report response rows.')
assert.match(
  batchRecordInterface[1],
  /batchRecordReportName\?:\s*string\s*\|\s*null/,
  'legacy batch record rows must expose report names for right-side field detail display.'
)
assert.match(
  flowConfigApi,
  /batchRecordReports\?:\s*ProRouteFlowBatchRecordVO\[\]/,
  'process config response must carry legacy batchRecordReports alongside dynamic formBindings.'
)

assert.match(
  flowGraph,
  /type RouteFlowLegacyBatchRecord\s*=\s*ProRouteFlowBatchRecordVO/,
  'route flow graph must keep legacy report bindings as a read-only display source.'
)
assert.match(
  flowGraph,
  /legacyBatchRecords:\s*cloneLegacyBatchRecords\(draft\.legacyBatchRecords\)/,
  'selected process drafts must preserve legacy batch records when applying local state.'
)
assert.match(
  flowGraph,
  /buildLegacyBatchRecords\(batchRow\?\.batchRecordReports\)/,
  'selected process attributes must load legacy batchRecordReports from the BATCH config response.'
)
assert.match(
  flowGraph,
  /buildBatchRecordFormValue\(\)/,
  'batchRecordFormNames field must use the formal batch record form projection.'
)
assert.match(
  flowGraph,
  /const getSelectedBatchRecordForms = \(\) =>[\s\S]*selectedLegacyBatchRecords\.value[\s\S]*isMainBatchRecordForm/,
  'batchRecordFormNames value must read only formal batch record reports for the selected route process.'
)
assert.match(
  flowGraph,
  /batchRecordReports:\s*processConfig\.batchRecordReports/,
  'route flow graph save payload must preserve formal batch record reports in the draft snapshot.'
)

console.log('mes-route-flow-legacy-batch-record-detail-static PASS')
