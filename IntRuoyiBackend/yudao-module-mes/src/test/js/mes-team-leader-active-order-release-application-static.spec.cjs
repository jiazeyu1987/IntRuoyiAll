const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const moduleRoot = path.resolve(__dirname, '../../..')
const read = (relativePath) => fs.readFileSync(path.resolve(moduleRoot, relativePath), 'utf8')
const sliceBetween = (source, start, end) => {
  const startIndex = source.indexOf(start)
  assert.ok(startIndex >= 0, `Cannot find start anchor: ${start}`)
  const endIndex = source.indexOf(end, startIndex + start.length)
  assert.ok(endIndex > startIndex, `Cannot find end anchor after: ${start}`)
  return source.slice(startIndex, endIndex)
}

const controllerSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'
)
const errorCodeSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/enums/ErrorCodeConstants.java'
)
const applicationServiceSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseApplicationServiceImpl.java'
)
const generationServiceSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseGenerationService.java'
)
const persistenceServiceSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseApplicationPersistenceService.java'
)
const batchRecordWriterSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl.java'
)
const processInspectionWriterSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl.java'
)
const lossReportWriterSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseLossReportWriterImpl.java'
)
const pqcReleaseSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java'
)
const managerReleaseSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesProductionReleaseManagerApprovalServiceImpl.java'
)
const dossierPortSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleaseDossierPortImpl.java'
)
const activeOrderServiceSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java'
)
const versionUpgradeServiceSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderVersionUpgradeServiceImpl.java'
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
    applicationServiceSource.includes('MesTeamLeaderActiveOrderCompletionService') &&
    applicationServiceSource.includes('MesTeamLeaderActiveOrderReleaseGenerationService') &&
    applicationServiceSource.includes('MesReleaseFlowIdempotency.requireKey') &&
    applicationServiceSource.includes('generationService.replayExisting') &&
    applicationServiceSource.includes('completionService.completeForRelease') &&
    applicationServiceSource.includes('generationService.generate') &&
    applicationServiceSource.indexOf('generationService.replayExisting') <
      applicationServiceSource.indexOf('completionService.completeForRelease') &&
    !applicationServiceSource.includes('MesProEdhrBatchExecutionService') &&
    !applicationServiceSource.includes('MesProEdhrReleaseService') &&
    !applicationServiceSource.includes('submitForApproval'),
  'Release application service must replay existing applications before completion backfill, then only complete/backfill and generate a PQC release application in SP-1.'
)

const generationBeforeHashBlock = sliceBetween(
  generationServiceSource,
  'public MesTeamLeaderActiveOrderReleaseApplicationResult generate',
  'List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots'
)
const replayExistingBlock = sliceBetween(
  generationServiceSource,
  'public MesTeamLeaderActiveOrderReleaseApplicationResult replayExisting',
  'public MesTeamLeaderActiveOrderReleaseApplicationResult get'
)
const findExistingApplicationBlock = sliceBetween(
  generationServiceSource,
  'private MesProcessPoolActiveOrderReleaseApplicationDO findExistingApplication',
  'private MesProcessPoolActiveOrderReleaseApplicationDO requireCurrentApplication'
)
assert(
  generationBeforeHashBlock.includes('findExistingApplication(activeOrder.getId(), requestKey, businessKey)') &&
    generationBeforeHashBlock.indexOf('findExistingApplication(activeOrder.getId(), requestKey, businessKey)') <
      generationBeforeHashBlock.indexOf('return persistenceService.toResult(requireCurrentApplication(existing))') &&
    replayExistingBlock.includes('findExistingApplication(activeOrder.getId(), requestKey, businessKey)') &&
    findExistingApplicationBlock.includes('selectByRequestIdempotencyKey') &&
    findExistingApplicationBlock.includes('selectByBusinessIdempotencyKey') &&
    !generationServiceSource.includes('sameRequestPayload') &&
    persistenceServiceSource.includes('persistPending') &&
    persistenceServiceSource.includes('PQC_PRODUCTION_RELEASE') &&
    persistenceServiceSource.includes('CandidateUserSnapshot'),
  'SP-1 generation must replay existing idempotent receipts before hashing current sources and must create a frozen PQC task.'
)

const rebuildBlock = sliceBetween(
  activeOrderServiceSource,
  'public MesTeamLeaderActiveOrderRebuildResult rebuildActiveOrder',
  'private MesProcessPoolActiveOrderDO requireActiveOrderForRebuild'
)
const removeBlock = sliceBetween(
  activeOrderServiceSource,
  'public void removeActiveOrder',
  'private MesTeamLeaderActiveOrderAddResult'
)
assert(
  rebuildBlock.includes('requireNoReleaseApplication(preview)') &&
    rebuildBlock.indexOf('requireNoReleaseApplication(preview)') <
      rebuildBlock.indexOf('cleanupActiveOrderRuntimeHistory') &&
    removeBlock.includes('requireNoReleaseApplication(activeOrder.getId())') &&
    removeBlock.indexOf('requireNoReleaseApplication(activeOrder.getId())') <
      removeBlock.indexOf('reportAllocationOrderChangeService.invalidateActiveOrder') &&
    activeOrderServiceSource.includes('selectListByActiveOrderIdsForUpdate') &&
    activeOrderServiceSource.includes('PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_APPLICATION_LOCKED'),
  'Active-order rebuild and removal must fail fast when a release application exists, before destructive writes.'
)

const versionUpgradePreviewBlock = sliceBetween(
  versionUpgradeServiceSource,
  'public MesTeamLeaderActiveOrderVersionUpgradePreview preview',
  'public MesTeamLeaderActiveOrderVersionUpgradeSubmitResult submit'
)
const versionUpgradeSubmitBlock = sliceBetween(
  versionUpgradeServiceSource,
  'public MesTeamLeaderActiveOrderVersionUpgradeSubmitResult submit',
  'private void requireNoReleaseApplication'
)
assert(
  versionUpgradePreviewBlock.includes('releaseApplicationMapper.selectListByActiveOrderIds') &&
    versionUpgradePreviewBlock.includes('活跃订单已进入生产放行链路，禁止版本升级重启') &&
    versionUpgradeSubmitBlock.includes('requireNoReleaseApplication(lockedActiveOrder.getId())') &&
    versionUpgradeSubmitBlock.indexOf('requireNoReleaseApplication(lockedActiveOrder.getId())') <
      versionUpgradeSubmitBlock.indexOf('freezeForVersionUpgrade') &&
    versionUpgradeServiceSource.includes('selectListByActiveOrderIdsForUpdate'),
  'Active-order version upgrade must surface and enforce the release-application lock before freezing the order.'
)

assert(
  batchRecordWriterSource.includes('MesProRouteFlowProcessBatchRecordMapper') &&
    batchRecordWriterSource.includes('selectListByRouteProcessIdsAndUseType') &&
    batchRecordWriterSource.includes('RECORD_CATEGORY_BATCH_RECORD') &&
    batchRecordWriterSource.includes('PROCESS_POOL_REPORT') &&
    !batchRecordWriterSource.includes('formBindings') &&
    !batchRecordWriterSource.includes('SLOT_TYPE_MAIN'),
  'Formal batch-record dossier checks must use per-process batch record bindings, never formBindings or default MAIN slots.'
)

assert(
  processInspectionWriterSource.includes('MesProRouteFlowProcessBatchRecordMapper') &&
    processInspectionWriterSource.includes('PROCESS_INSPECTION') &&
    processInspectionWriterSource.includes('selectListByRouteProcessIdsAndUseType') &&
    lossReportWriterSource.includes('MesProRouteFlowProcessBatchRecordMapper') &&
    lossReportWriterSource.includes('LOSS_REPORT') &&
    lossReportWriterSource.includes('selectListByRouteProcessIdsAndUseType'),
  'Formal process-inspection and loss-report dossier checks must require explicit route-owned report bindings.'
)

assert(
  pqcReleaseSource.includes('batchExecutionPort.openOrCreate') &&
    pqcReleaseSource.includes('dossierPort.write') &&
    pqcReleaseSource.includes('reportStageInitializer.initializeRequiredReportStage') &&
    dossierPortSource.includes('sourceSnapshotHasher.hash') &&
    dossierPortSource.includes('authoritative production or inspection sources changed after SP-1') &&
    !/releaseService\s*\.\s*submit\s*\(/.test(applicationServiceSource) &&
    !applicationServiceSource.includes('MesProEdhrReleaseSubmitReqVO'),
  'PQC approval must be the first step that opens the release batch execution and writes the dossier from frozen active-order sources.'
)

const pqcAuthorizationBlock = sliceBetween(
  pqcReleaseSource,
  'private void requireAuthorized',
  'private boolean containsCandidate'
)
const pqcPageBlock = sliceBetween(
  pqcReleaseSource,
  'public PageResult<MesPqcProductionReleasePageItem> getPqcReleasePage',
  'private void applyApprovalReadiness'
)
const managerCandidateBlock = sliceBetween(
  managerReleaseSource,
  'private void requireManagerCandidate',
  'private List<MesProductionReleaseReportNodeEvidence>'
)
assert(
  pqcAuthorizationBlock.includes('containsCandidate(workTask.getCandidateUserSnapshot(), actorUserId)') &&
    !pqcAuthorizationBlock.includes('candidateResolver.resolveRequiredCandidates') &&
    pqcPageBlock.includes('containsCandidate(task.getCandidateUserSnapshot(), actorUserId)') &&
    !pqcPageBlock.includes('candidateResolver.resolveRequiredCandidates') &&
    !pqcPageBlock.includes('PQC_RELEASE_ROLE_REQUIRED') &&
    managerCandidateBlock.includes('containsCandidate(workTask.getCandidateUserSnapshot(), actorUserId)') &&
    managerCandidateBlock.includes('MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE') &&
    !managerCandidateBlock.includes('candidateResolver.resolveRequiredCandidates'),
  'PQC page/actions and manager approval must authorize the frozen work-task candidate snapshot instead of rechecking changed live role membership.'
)

assert(
  activeOrderRowSource.includes('releaseApplicationStatus') &&
    activeOrderRowSource.includes('pqcReleaseWorkTaskId') &&
    activeOrderRowSource.includes('releaseSourceSnapshotHash') &&
    activeOrderRowSource.includes('releaseApplicationVersion') &&
    activeOrderVoSource.includes('releaseApplicationStatus') &&
    activeOrderVoSource.includes('pqcReleaseWorkTaskId') &&
    activeOrderVoSource.includes('releaseSourceSnapshotHash') &&
    activeOrderVoSource.includes('releaseApplicationVersion'),
  'Active-order list response must expose release application status, PQC task, source hash and version.'
)

console.log('PASS: MES team leader active-order release application backend static contract')
