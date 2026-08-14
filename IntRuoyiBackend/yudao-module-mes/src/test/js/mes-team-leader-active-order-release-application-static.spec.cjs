const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const moduleRoot = path.resolve(__dirname, '../../..')
const read = (relativePath) => fs.readFileSync(path.resolve(moduleRoot, relativePath), 'utf8')

const controllerSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'
)
const errorCodeSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/enums/ErrorCodeConstants.java'
)
const applicationServiceSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseApplicationServiceImpl.java'
)
const releaseServiceSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java'
)
const activeOrderRowSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderRow.java'
)
const activeOrderVoSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderRespVO.java'
)

assert(
  controllerSource.includes('@PostMapping("/active-order/release/apply")') &&
    controllerSource.includes('mes:pro-process-pool-team-leader:release-apply') &&
    controllerSource.includes('applyActiveOrderRelease'),
  'Production leader controller must expose the active-order release application endpoint and permission.'
)

assert(
  errorCodeSource.includes('PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_PROGRESS_REQUIRED') &&
    errorCodeSource.includes('PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED') &&
    errorCodeSource.includes('PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_OWNER_REQUIRED'),
  'Release application must have explicit fail-fast error codes for progress, formal sources, and owner/task setup.'
)

assert(
  applicationServiceSource.includes('class MesTeamLeaderActiveOrderReleaseApplicationServiceImpl') &&
    applicationServiceSource.includes('MesProEdhrBatchExecutionService') &&
    applicationServiceSource.includes('MesProEdhrReleaseService') &&
    applicationServiceSource.includes('submitForApproval') &&
    applicationServiceSource.includes('MesProEdhrReleaseSubmitForApprovalCommand'),
  'Release application service must orchestrate eDHR batch creation and pending-approval submission through a dedicated command.'
)

assert(
  applicationServiceSource.includes('MesProRouteFlowProcessBatchRecordMapper') &&
    applicationServiceSource.includes('selectListByRouteProcessIdsAndUseType') &&
    applicationServiceSource.includes('RECORD_CATEGORY_BATCH_RECORD') &&
    !applicationServiceSource.includes('formBindings') &&
    !applicationServiceSource.includes('SLOT_TYPE_MAIN'),
  'Formal batch record checks must use per-process batch record bindings, never formBindings or default MAIN slots.'
)

assert(
  applicationServiceSource.includes('MesPqcProcessInspectionAggregateDetailMapper') &&
    applicationServiceSource.includes('selectListByActiveOrderId') &&
    applicationServiceSource.includes('PROCESS_INSPECTION') &&
    applicationServiceSource.includes('LOSS_REPORT'),
  'Formal process-inspection and loss-report checks must require explicit formal source and slot evidence.'
)

assert(
  !/releaseService\s*\.\s*submit\s*\(/.test(applicationServiceSource) &&
    !applicationServiceSource.includes('MesProEdhrReleaseSubmitReqVO'),
  'Production leader release application must not call the direct release submit API.'
)

assert(
  releaseServiceSource.includes('STATUS_PENDING_APPROVAL') &&
    releaseServiceSource.includes('submitForApproval') &&
    releaseServiceSource.includes('createReleaseApprovalTaskAfterSubmit') &&
    !/submitForApproval[\s\S]*STATUS_RELEASED/.test(releaseServiceSource),
  'Release service must support pending approval without marking the batch released.'
)

assert(
  activeOrderRowSource.includes('releaseApplicationStatus') &&
    activeOrderRowSource.includes('releaseApprovalWorkTaskId') &&
    activeOrderVoSource.includes('releaseApplicationStatus') &&
    activeOrderVoSource.includes('releaseApplicationBlockerSummary'),
  'Active-order list response must expose release application status and blocker summary.'
)

console.log('PASS: MES team leader active-order release application backend static contract')
