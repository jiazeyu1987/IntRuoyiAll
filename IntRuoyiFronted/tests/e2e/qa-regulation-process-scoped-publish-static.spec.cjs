const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/QaRegulationPage.vue'
)
const source = fs.readFileSync(pagePath, 'utf8')

assert.match(
  source,
  /interface QaRouteScopeAutoSource[\s\S]*routeProcesses:\s*ProRouteProcessVO\[\][\s\S]*scheduleConfigs:\s*ProRouteFlowProcessConfigVO\[\][\s\S]*batchConfigs:\s*ProRouteFlowProcessConfigVO\[\]/,
  'QA route scope must retain every process from the active route-version snapshot.'
)
assert.match(
  source,
  /resolveQaVersionRouteProcesses[\s\S]*routeProcessId[\s\S]*processCode[\s\S]*processId/,
  'QA publishing must resolve frozen route-process ids from the active route version instead of current draft rows.'
)
assert.match(
  source,
  /QA_PROCESS_SCOPE_BINDINGS_BY_PROJECT_CODE[\s\S]*ID:[\s\S]*'清洗\/精洗':\s*\['精洗',\s*'清洗'\]/,
  'Balloon pressure-pump wash items must be explicitly bound to the formal fine-wash and cleaning route processes.'
)
assert.doesNotMatch(
  source,
  /'清洗\/精洗':\s*\[[^\]]*'粗洗'/,
  'Balloon pressure-pump formal QA does not configure rough wash and must not publish a rough-wash regulation.'
)
assert.match(
  source,
  /resolveQaRegulationItemRouteProcesses[\s\S]*buildQaRegulationSavePayloads[\s\S]*itemsByRouteProcessId/,
  'QA items must be grouped into one payload per matching route process.'
)
assert.match(
  source,
  /routeProcessId,[\s\S]*processId,[\s\S]*regulationCode:\s*buildQaProcessRegulationCode/,
  'Each route-process regulation must carry the resolved route-process identity and a unique code.'
)
assert.match(
  source,
  /items:\s*buildQaRegulationSaveItems\(items\)/,
  'Each route-process regulation must contain only the items grouped for that process.'
)
assert.match(
  source,
  /for \(const payload of payloads\)[\s\S]*saveQaRegulationDraft\(payload\)/,
  'Saving a QA draft must persist every process-scoped payload.'
)
assert.match(
  source,
  /for \(const payload of payloads\)[\s\S]*publishQaRegulation\(payload\)/,
  'Publishing a QA regulation must publish every process-scoped payload.'
)
assert.doesNotMatch(
  source,
  /items:\s*buildQaRegulationSaveItems\(\)/,
  'A single route process must never receive the full cross-process QA item table.'
)

console.log('PASS qa-regulation-process-scoped-publish-static')
