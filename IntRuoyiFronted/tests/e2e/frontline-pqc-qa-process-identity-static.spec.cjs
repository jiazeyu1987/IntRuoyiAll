const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const apiSource = read('src/api/mes/pro/feedback/index.ts')
const helperSource = read('src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts')
const pageSource = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const pqcSwitchBuilderSource = helperSource.slice(
  helperSource.indexOf('export const buildFrontlinePqcEmployeeSwitchPayload'),
  helperSource.indexOf('export const buildFrontlinePqcActiveOrderProcessCacheKey')
)
const pqcSubmitBuilderSource = pageSource.slice(
  pageSource.indexOf('const buildPqcInspectionSubmitPayload'),
  pageSource.indexOf('const formatLocalDateTime')
)

assert.match(apiSource, /interface FrontlinePqcProcessVO/)
assert.match(apiSource, /qaProcessId:\s*number/)
assert.match(apiSource, /qaProcessName:\s*string/)
assert.match(apiSource, /regulationVersionId:\s*number/)
assert.match(apiSource, /getFrontlinePqcActiveOrderProcesses[\s\S]*FrontlinePqcProcessVO\[\]/)
assert.match(apiSource, /interface FrontlinePqcSwitchActualEmployeeReqVO[\s\S]*qaProcessId:\s*number/)
assert.match(apiSource, /interface FrontlinePqcInspectionSubmitReqVO[\s\S]*qaProcessId:\s*number/)

assert.match(helperSource, /process\.qaProcessId/)
assert.match(helperSource, /process\.regulationVersionId/)
assert.doesNotMatch(
  pqcSwitchBuilderSource,
  /routeProcessId:\s*process\.routeProcessId|processId:\s*process\.processId/
)

assert.match(pageSource, /qaProcessId:\s*process\.qaProcessId/)
assert.doesNotMatch(
  pqcSubmitBuilderSource,
  /routeProcessId:\s*process\.routeProcessId|processId:\s*process\.processId/
)
assert.match(pageSource, /process\.qaProcessName/)

console.log('PASS: frontline PQC frontend uses QA process identity only')
