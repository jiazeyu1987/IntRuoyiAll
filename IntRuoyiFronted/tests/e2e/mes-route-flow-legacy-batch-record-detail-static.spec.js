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
  /buildRecordBindingValue\('MAIN'\)/,
  'batchRecordFormNames field must keep using the existing field projection entry.'
)
assert.match(
  flowGraph,
  /getLegacyBatchRecordsBySlotType\(formSlotType\)[\s\S]*getLegacyBatchRecordDisplayName/,
  'batchRecordFormNames value must merge legacy report names by form slot type.'
)
assert.doesNotMatch(
  flowGraph,
  /batchRecordReports:\s*processConfig\.batchRecordReports/,
  'route flow graph save payload must not submit legacy batchRecordReports.'
)

console.log('mes-route-flow-legacy-batch-record-detail-static PASS')
