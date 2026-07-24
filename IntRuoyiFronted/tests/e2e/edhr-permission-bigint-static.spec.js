const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const detailPage = readSource('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const listPage = readSource('src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const batchExecutionApi = readSource('src/api/mes/pro/edhr/batchExecution.ts')

const routeIdPages = [
  'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue',
  'src/views/mes/pro/edhr-batch/BatchExecutionTemplatePage.vue',
  'src/views/mes/pro/edhr-batch/BatchExecutionTemplateSimulatePage.vue',
  'src/views/mes/pro/edhr/SignaturePage.vue',
  'src/views/mes/pro/edhr/ApprovalDetailPage.vue'
]

const extractFunction = (source, name, options = {}) => {
  const asyncPrefix = options.async === false ? `const ${name} =` : `const ${name} = async`
  const start = source.indexOf(asyncPrefix)
  assert.notEqual(start, -1, `missing ${name}`)
  const nextConst = source.indexOf('\nconst ', start + asyncPrefix.length)
  assert.notEqual(nextConst, -1, `cannot find end of ${name}`)
  return source.slice(start, nextConst)
}

assert.doesNotMatch(
  detailPage,
  /findActiveBusinessAction/,
  'eDHR batch detail read/secondary load path must not call form-center active-instance because it requires form:instance:create.'
)

assert.doesNotMatch(
  listPage,
  /findActiveBusinessAction/,
  'eDHR batch list withdraw path must not use active-instance as a fallback; the list row must carry the BPM process instance id.'
)

const loadBatchDetailSecondaryData = extractFunction(detailPage, 'loadBatchDetailSecondaryData')
assert.doesNotMatch(
  loadBatchDetailSecondaryData,
  /loadActiveReleaseAction/,
  'eDHR secondary detail load must not enrich read-only state through active-instance.'
)

const handleWithdrawVoidRequest = extractFunction(listPage, 'handleWithdrawVoidRequest')
assert.match(
  handleWithdrawVoidRequest,
  /const\s+processInstanceId\s*=\s*row\.pendingVoidProcessInstanceId/,
  'withdraw void must use the process instance id already returned by the batch row.'
)
assert.doesNotMatch(
  handleWithdrawVoidRequest,
  /findActiveBusinessAction|buildVoidBusinessActionContext/,
  'withdraw void must fail fast when the row lacks pendingVoidProcessInstanceId instead of calling active-instance.'
)

const unsafeRouteQueryIdPatterns = [
  /Number\s*\(\s*route\.query\.(?:id|workTaskId|taskId|executionId|batchTaskId)\s*\)/,
  /parsePositiveNumber\s*\(\s*route\.query\.(?:id|workTaskId|taskId|executionId|batchTaskId)\s*\)/
]

for (const relativePath of routeIdPages) {
  const source = readSource(relativePath)
  for (const pattern of unsafeRouteQueryIdPatterns) {
    assert.doesNotMatch(
      source,
      pattern,
      `${relativePath} must preserve route query ids as strings; JavaScript Number loses BIGINT precision.`
    )
  }
}

assert.match(
  batchExecutionApi,
  /export type EdhrRouteId = string \| number/,
  'batch execution API ids used by route-driven pages must accept string ids to preserve BIGINT precision.'
)

console.log('PASS: eDHR permission boundary and BIGINT route id static contract')
