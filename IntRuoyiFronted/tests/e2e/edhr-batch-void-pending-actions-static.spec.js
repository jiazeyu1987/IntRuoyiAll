const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const apiBatchPath = path.join(repoRoot, 'src/api/mes/pro/edhr/batchExecution.ts')
const apiChangePath = path.join(repoRoot, 'src/api/mes/pro/edhr/change.ts')

const page = fs.readFileSync(pagePath, 'utf8')
const apiBatch = fs.readFileSync(apiBatchPath, 'utf8')
const apiChange = fs.readFileSync(apiChangePath, 'utf8')

assert(
  apiBatch.includes('pendingVoidChangeEventId?: number'),
  'Batch execution API type must expose pendingVoidChangeEventId for pending void rows.'
)
assert(
  apiBatch.includes('canWithdrawVoidRequest?: boolean'),
  'Batch execution API type must expose canWithdrawVoidRequest for row-level action gating.'
)
assert(
  apiChange.includes('withdrawVoidBatchExecution'),
  'Change API must expose withdrawVoidBatchExecution instead of using generic BPM calls in the page.'
)
assert(
  /withdrawVoidBatchExecution[\s\S]*\/mes\/pro\/edhr-change\/void-batch-execution\/withdraw/.test(apiChange),
  'Change API withdraw action must call the dedicated batch void withdrawal endpoint.'
)
assert(
  page.includes("resolveBatchVoidOperationState(row) === 'pending-withdrawable'") && page.includes('撤回作废申请'),
  'Batch execution list must render a dedicated withdraw action for pending void rows.'
)
assert(
  page.includes('v-else class="edhr-batch-page__actions"'),
  'Normal row actions must be excluded with the final v-else when a void request is pending.'
)
assert(
  /const canOpenCurrentUserFillTask = \(row: EdhrBatchExecutionRespVO\) =>\s*\n\s*resolveBatchVoidOperationState\(row\) === 'normal'/.test(page),
  'Pending void rows must not expose the current fill task action.'
)

console.log('edhr batch pending void action contract passed')
