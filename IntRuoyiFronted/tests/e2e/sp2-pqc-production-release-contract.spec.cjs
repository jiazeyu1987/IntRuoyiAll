const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const frontendRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.resolve(frontendRoot, relativePath), 'utf8')

const releaseApi = read('src/api/mes/pro/productionRelease/index.ts')
const workTaskApi = read('src/api/mes/pro/edhr/workTask.ts')
const boardPage = read('src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue')

for (const endpoint of [
  '/mes/pro/production-release/pqc/approve',
  '/mes/pro/production-release/pqc/reject',
  '/mes/pro/production-release/get'
]) {
  assert(releaseApi.includes(endpoint), `Missing SP-2 endpoint: ${endpoint}`)
}

for (const field of [
  'applicationId: string',
  'pqcReleaseWorkTaskId: string',
  'batchExecutionId?: string'
]) {
  assert(releaseApi.includes(field), `Missing precision-safe release contract: ${field}`)
}
for (const field of ['expectedVersion: number', 'idempotencyKey: string', 'rejectReason: string']) {
  assert(releaseApi.includes(field), `Missing PQC decision request field: ${field}`)
}
assert(releaseApi.includes('reportUploadTasks: MesProductionReleaseReportUploadTaskRespVO[]'))

assert(
  workTaskApi.includes("EDHR_WORK_TASK_TYPE_PQC_PRODUCTION_RELEASE = 'PQC_PRODUCTION_RELEASE'"),
  'Shared work-task contract must expose the PQC production-release task type'
)
for (const field of ['id: string', 'businessScopeId?: string', 'version?: number']) {
  assert(workTaskApi.includes(field), `Missing work-task projection field: ${field}`)
}

assert(
  !boardPage.includes(
    'taskType: EDHR_WORK_TASK_TYPE_REVIEW,\n              status: resolveMyPageStatus()'
  ),
  'Candidate page must not hide PQC tasks behind a REVIEW-only query'
)
for (const marker of [
  'data-pqc-release-approve',
  'data-pqc-release-reject',
  'pqcDecisionForm.rejectReason',
  'resolvePqcProductionReleaseFailure',
  'recoverUncertainPqcProductionReleaseDecision',
  'getPqcProductionRelease',
  'result.batchExecutionId',
  'result.reportUploadTasks'
]) {
  assert(boardPage.includes(marker), `Missing PQC workbench behavior: ${marker}`)
}
assert(
  boardPage.includes("activeTab.value === 'candidate'") &&
    boardPage.includes('EDHR_WORK_TASK_STATUS_TODO'),
  'PQC decision actions must be limited to the candidate TODO view'
)
assert(
  !/canHandlePqcProductionRelease[\s\S]*?!row\.inactionReason\?\.trim\(\)/.test(boardPage),
  'Candidate evidence text must not be treated as a disabled-state flag'
)
assert(boardPage.includes('expectedVersion'), 'PQC decision must submit the authoritative version')
assert(boardPage.includes('idempotencyKey'), 'PQC decision must submit an idempotency key')
assert(
  !/default-success/i.test(`${releaseApi}\n${boardPage}`),
  'Default-success paths are forbidden'
)

console.log('PASS: SP-2 PQC production release frontend contract')
