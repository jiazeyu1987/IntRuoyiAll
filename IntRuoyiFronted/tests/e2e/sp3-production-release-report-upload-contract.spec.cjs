const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const frontendRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.resolve(frontendRoot, relativePath), 'utf8')

const workTaskApi = read('src/api/mes/pro/edhr/workTask.ts')
const batchExecutionApi = read('src/api/mes/pro/edhr/batchExecution.ts')
const boardPage = read('src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue')
const detailPage = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')

for (const nodeType of [
  'INCOMING_INSPECTION_REPORT',
  'STERILIZATION_REPORT',
  'FINISHED_PRODUCT_INSPECTION_REPORT',
  'FINISHED_PRODUCT_INSPECTION_RECORD'
]) {
  assert(
    workTaskApi.includes(nodeType) || detailPage.includes(nodeType),
    `Missing production-release report node: ${nodeType}`
  )
}

for (const field of ['nodeTypes?: string[]', 'batchExecutionId?: string']) {
  assert(workTaskApi.includes(field), `Missing candidate report query field: ${field}`)
}

for (const marker of [
  'prepareEdhrProductionReleaseReportAttachmentUpload',
  'completeEdhrProductionReleaseReportNode',
  'MesProductionReleaseReportNodeCompleteRespVO',
  "formData.append('expectedVersion'",
  "formData.append('idempotencyKey'",
  '/task/special-node/attachment/prepare-upload',
  '/task/special-node/complete'
]) {
  assert(batchExecutionApi.includes(marker), `Missing SP-3 report API contract: ${marker}`)
}
for (const field of [
  'batchExecutionId: string',
  'batchTaskId: string',
  'workTaskId: string',
  'releaseTransactionId?: string',
  'managerReleaseWorkTaskId?: string',
  'version: number'
]) {
  assert(batchExecutionApi.includes(field), `Missing precision-safe report response field: ${field}`)
}

for (const marker of [
  'data-production-release-report-open',
  'isProductionReleaseReportTask',
  'openProductionReleaseReportTask',
  "url.searchParams.set('batchTaskId'",
  "url.searchParams.set('nodeType'",
  "url.searchParams.set('expectedVersion'"
]) {
  assert(boardPage.includes(marker), `Missing report workbench entry behavior: ${marker}`)
}

for (const marker of [
  'data-production-release-report-upload',
  'data-production-release-report-complete',
  'loadProductionReleaseReportCandidates',
  'resolveProductionReleaseReportWorkTask',
  'createProductionReleaseReportIdempotencyKey',
  'prepareEdhrProductionReleaseReportAttachmentUpload',
  'completeEdhrProductionReleaseReportNode',
  "result.reportUploadStatus === 'MANAGER_RELEASE_PENDING'",
  'result.releaseTransactionId',
  'result.managerReleaseWorkTaskId'
]) {
  assert(detailPage.includes(marker), `Missing report detail behavior: ${marker}`)
}

assert(
  detailPage.includes('v-if="!isProductionReleaseReportNode(selectedTaskForEvidence)"') &&
    detailPage.includes('v-if="!isProductionReleaseReportNode(selectedSpecialNodeForEvidence)"'),
  'Production-release report nodes must not expose skip or pending-attachment delete actions'
)
assert(
  /if \(isProductionReleaseReportNode\(row\)\) \{[\s\S]*?handleCompleteSpecialNode\(row\)/.test(
    detailPage
  ),
  'Every production-release report node must enter the report completion path'
)
assert(
  !/default-success/i.test(`${workTaskApi}\n${batchExecutionApi}\n${boardPage}\n${detailPage}`),
  'Default-success paths are forbidden'
)

console.log('PASS: SP-3 production release report upload frontend contract')
