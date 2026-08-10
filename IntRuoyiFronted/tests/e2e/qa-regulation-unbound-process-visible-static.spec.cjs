const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(frontendRoot, 'src/views/mes/pro/processpool/QaRegulationPage.vue')
const source = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')

assert.match(
  source,
  /QA_UNBOUND_BATCH_RECORD_PROCESS_NAMES_BY_PROJECT_CODE[\s\S]*IDI:[\s\S]*组装螺杆八组件[\s\S]*光固外套四组件[\s\S]*装配[\s\S]*整体粘结/,
  'IDI QA business processes without recognized batch-record bindings must be explicitly allowed.'
)
assert.match(
  source,
  /const resolveQaRegulationItemRouteProcesses[\s\S]*matchedProcesses\.length === 0[\s\S]*source\.routeProcess[\s\S]*batchRecordBindingResolved:\s*false/,
  'An explicitly allowed unbound QA process must use the already resolved formal QA route process without inventing a route mapping.'
)
assert.match(
  source,
  /matchedProcesses\.map\([\s\S]*batchRecordBindingResolved:\s*true/,
  'A uniquely matched QA process must retain its formal route-process binding.'
)
assert.match(
  source,
  /group\.batchRecordBindingResolved\s*=\s*group\.batchRecordBindingResolved\s*&&\s*binding\.batchRecordBindingResolved/,
  'A payload group containing unbound QA business processes must retain that unresolved binding state.'
)
assert.match(
  source,
  /batchRecordBindingSummary:\s*batchRecordBindingResolved\s*\?[\s\S]*resolveFormalBatchRecordBindingSummary[\s\S]*:\s*undefined/,
  'Unbound QA business processes must not display a fabricated batch-record binding summary.'
)
assert.match(
  source,
  /const formatQaItemProcessName[\s\S]*item\.processName\?\.trim\(\)/,
  'The QA list must continue to display the source business-process name.'
)
assert.doesNotMatch(
  source,
  /组装螺杆八组件:\s*\[[^\]]*(?:组装Ⅰ|组装Ⅱ)/,
  'The change must not guess an assembly route-process mapping.'
)
assert.doesNotMatch(
  source,
  /光固外套四组件:\s*\[[^\]]*(?:光固Ⅰ|光固Ⅱ)/,
  'The change must not guess a curing route-process mapping.'
)

console.log('PASS qa-regulation-unbound-process-visible-static')
