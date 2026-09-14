const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const frontendRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.resolve(frontendRoot, relativePath), 'utf8')

const apiSource = read('src/api/mes/pro/processpool/teamLeader.ts')
const pageSource = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const axiosSource = read('src/config/axios/service.ts')

const between = (source, start, end) => {
  const startIndex = source.indexOf(start)
  assert.notEqual(startIndex, -1, `Missing start marker: ${start}`)
  const endIndex = source.indexOf(end, startIndex + start.length)
  assert.notEqual(endIndex, -1, `Missing end marker: ${end}`)
  return source.slice(startIndex, endIndex)
}

const statusContract = between(
  apiSource,
  'export type TeamLeaderActiveOrderReleaseApplicationStatus =',
  'export interface TeamLeaderActiveOrderProcessRemainingQuantity'
)
for (const status of [
  'PQC_RELEASE_PENDING',
  'PQC_RELEASE_REJECTED',
  'REPORT_UPLOAD_PENDING',
  'MANAGER_RELEASE_PENDING',
  'RELEASED'
]) {
  assert(statusContract.includes(`'${status}'`), `Missing persistent release status: ${status}`)
}
assert(!statusContract.includes('PENDING_RELEASE_APPROVAL'), 'Legacy release status must not remain')
assert(!statusContract.includes("'BLOCKED'"), 'Blockers are failures, not a persistent application status')

const applyResponse = between(
  apiSource,
  'export interface TeamLeaderActiveOrderReleaseApplyRespVO',
  'export interface TeamDeviceRespVO'
)
for (const stringId of [
  'applicationId',
  'activeOrderId',
  'workOrderId',
  'routeId',
  'routeVersionId',
  'pqcReleaseWorkTaskId'
]) {
  assert.match(applyResponse, new RegExp(`${stringId}: string\\b`), `${stringId} must preserve JSON string precision`)
}
for (const forbiddenField of [
  'batchExecutionId',
  'releaseTransactionId',
  'releaseApprovalWorkTaskId',
  'statusName',
  'dossierSummary'
]) {
  assert(!applyResponse.includes(forbiddenField), `SP-1 response must not expose ${forbiddenField}`)
}

assert(
  apiSource.includes('getTeamLeaderActiveOrderRelease') &&
    apiSource.includes("url: '/mes/pro/process-pool/team-leader/active-order/release/get'") &&
    apiSource.includes('params: { activeOrderId }'),
  'Uncertain submission recovery must use the formal activeOrderId receipt endpoint'
)

for (const label of ['待PQC放行', 'PQC已拒绝', '待上传放行报告', '待管理者代表放行', '已放行']) {
  assert(pageSource.includes(label), `Missing user-visible status label: ${label}`)
}
assert(pageSource.includes('getTeamLeaderActiveOrderRelease'), 'Page must call the formal receipt endpoint')
assert(pageSource.includes('resolveActiveOrderReleaseFailure'), 'Page must parse structured release blockers')
assert(pageSource.includes('pqcReleaseWorkTaskId'), 'Page must validate and project the PQC release task receipt')
assert(pageSource.includes('不会创建批次、报告上传任务或最终放行事务'), 'Confirmation must describe the SP-1 boundary')

for (const forbiddenPageContract of [
  'PENDING_RELEASE_APPROVAL',
  'result.batchExecutionId',
  'result.releaseTransactionId',
  'result.releaseApprovalWorkTaskId',
  'result.dossierSummary',
  'result.statusName'
]) {
  assert(!pageSource.includes(forbiddenPageContract), `Page still depends on legacy contract: ${forbiddenPageContract}`)
}

assert(
  axiosSource.includes('data?.data ?? data?.details ?? null'),
  'Axios errors must retain CommonResult.data so structured blockers reach the page'
)

console.log('PASS: SP-1 production release frontend contract')
