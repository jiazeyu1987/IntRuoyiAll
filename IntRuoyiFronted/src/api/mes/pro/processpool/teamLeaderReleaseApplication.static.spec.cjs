const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const frontendRoot = path.resolve(__dirname, '../../../../..')
const read = (relativePath) => fs.readFileSync(path.resolve(frontendRoot, relativePath), 'utf8')

const apiSource = read('src/api/mes/pro/processpool/teamLeader.ts')
const pageSource = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')

assert(
  apiSource.includes('TeamLeaderActiveOrderReleaseApplyReqVO') &&
    apiSource.includes('TeamLeaderActiveOrderReleaseApplyRespVO') &&
    apiSource.includes('TeamLeaderActiveOrderReleaseBlockerRespVO') &&
    apiSource.includes('applyTeamLeaderActiveOrderRelease') &&
    apiSource.includes('/mes/pro/process-pool/team-leader/active-order/release/apply'),
  'Team leader API must define and call the active-order release application contract.'
)

assert(
  apiSource.includes('releaseApplicationStatus?: string') &&
    apiSource.includes('releaseApplicationBlockerSummary?: string') &&
    apiSource.includes('releaseApprovalWorkTaskId?: number'),
  'Active-order response type must expose release application status and work-task linkage.'
)

assert(
  pageSource.includes('data-team-leader-active-order-release-apply') &&
    pageSource.includes('申请放行') &&
    pageSource.includes('申请生成放行资料') &&
    pageSource.includes('不会直接放行') &&
    pageSource.includes('applyTeamLeaderActiveOrderRelease'),
  'Team leader workbench must render a release-application button, confirmation copy, and API call.'
)

assert(
  pageSource.includes('canApplyActiveOrderRelease') &&
    pageSource.includes('productionProgressPercent') &&
    pageSource.includes('inspectionProgressPercent') &&
    pageSource.includes('releaseApplicationSubmittingId') &&
    pageSource.includes('releaseApplicationBlockers'),
  'Release-application UI must gate by progress, show row loading, and surface backend blockers.'
)

console.log('PASS: MES team leader active-order release application frontend static contract')
