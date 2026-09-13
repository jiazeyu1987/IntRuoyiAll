const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const backend = path.resolve(__dirname, '../../../../')
const repo = path.resolve(backend, '..')
const mes = path.join(backend, 'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes')
const front = path.join(repo, 'IntRuoyiFronted/src/views/mes/pro')

const read = (base, file) => fs.readFileSync(path.join(base, file), 'utf8')
const sliceBetween = (source, start, end) => {
  const startAt = source.indexOf(start)
  assert.notEqual(startAt, -1, `missing start anchor: ${start}`)
  const endAt = source.indexOf(end, startAt + start.length)
  assert.notEqual(endAt, -1, `missing end anchor: ${end}`)
  return source.slice(startAt, endAt)
}
const sliceMethod = (source, signature) => {
  const startAt = source.indexOf(signature)
  assert.notEqual(startAt, -1, `missing method anchor: ${signature}`)
  const bodyStart = source.indexOf('{', startAt)
  assert.notEqual(bodyStart, -1, `missing method body: ${signature}`)
  let depth = 0
  for (let i = bodyStart; i < source.length; i += 1) {
    const char = source[i]
    if (char === '{') {
      depth += 1
    } else if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(startAt, i + 1)
      }
    }
  }
  assert.fail(`unterminated method body: ${signature}`)
}

const regulation = read(mes, 'service/qa/regulation/MesQaInspectionRegulationServiceImpl.java')
const commonSetSave = sliceBetween(
  regulation,
  'public MesQaCommonRegulationSetRespVO.Version saveCommonRegulationSetVersion(',
  'public void deleteCommonRegulationSetVersion'
)
assert(commonSetSave.includes('assertCommonRegulationSetVersionMutable(version)'),
  'EDHR-STATIC-001: published common regulation set versions must be immutable before update/member replace')
const commonSetMutable = sliceMethod(
  regulation,
  'private void assertCommonRegulationSetVersionMutable(MesQaCommonRegulationSetVersionDO version)'
)
assert(commonSetMutable.includes('STATUS_DRAFT') && commonSetMutable.includes('!Objects.equals'),
  'EDHR-STATIC-001: only draft common regulation set versions may be modified or deleted')
const deleteCommonSetVersion = sliceMethod(regulation, 'public void deleteCommonRegulationSetVersion(Long setVersionId)')
assert(deleteCommonSetVersion.includes('assertCommonRegulationSetVersionMutable(version)')
  && deleteCommonSetVersion.indexOf('assertCommonRegulationSetVersionMutable(version)')
  < deleteCommonSetVersion.indexOf('commonRegulationSetVersionMemberMapper.deleteBySetVersionId'),
  'EDHR-STATIC-001: deleting a common set version must also require a draft version before any physical delete')
const regulationPage = read(front, 'processpool/QaRegulationPage.vue')
const canEditCommonSetVersion = sliceBetween(
  regulationPage,
  'const canEditCommonSetVersion = (version?: QaCommonRegulationSetVO',
  'const openSelectedCommonRegulationSetVersionDialog'
)
assert(canEditCommonSetVersion.includes("version?.lifecycleStatus === 'DRAFT'"),
  'EDHR-STATIC-001: frontend must only allow editing draft common set versions')
assert(regulationPage.includes(':disabled="!canEditCommonSetVersion(row)"')
  && regulationPage.includes('非草稿通用规程套版本不可删除'),
  'EDHR-STATIC-001: frontend must block deleting non-draft common set versions')

const allocation = read(mes, 'service/pro/processpool/team/MesReportAllocationCommandService.java')
const saveAllocation = sliceMethod(
  allocation,
  'public MesReportAllocationSnapshot save(MesReportAllocationSaveCommand command)'
)
const reviewEvidenceRequirement = sliceMethod(
  allocation,
  'private ReviewEvidenceRequirement reviewEvidenceRequirement(MesProProcessPoolEventDO event,'
)
assert(saveAllocation.includes('ReviewEvidenceRequirement reviewRequirement = reviewEvidenceRequirement(event, current)')
  && saveAllocation.includes('reviewRequirement.required()')
  && saveAllocation.includes('requireReview(event, command,')
  && saveAllocation.includes('attachReviewToCurrentRowsByEventId')
  && reviewEvidenceRequirement.includes('anyMatch(allocation -> allocation.getReviewId() == null)')
  && reviewEvidenceRequirement.includes('missingReviewId || reviewToBackfill != null'),
  'EDHR-STATIC-002: unchanged first allocations with null reviewId must still create formal review evidence')
const requireReview = sliceBetween(
  allocation,
  'MesProcessPoolSubmissionReviewDO reviewToBackfill) {',
  'private ReviewSignaturePayload recordApprovedReviewSignature'
)
assert(requireReview.includes('StrUtil.isBlank(command.getSignaturePassword())'),
  'EDHR-STATIC-003: allocation approval must require signature password')
assert(requireReview.includes('setReviewSignatureSnapshotJson') && allocation.includes('buildApprovedReviewSignatureSnapshot'),
  'EDHR-STATIC-003: allocation approval must persist structured signature snapshot')
const teamLeaderApi = read(path.join(repo, 'IntRuoyiFronted/src/api/mes/pro/processpool'), 'teamLeader.ts')
const confirmAllocationApi = sliceBetween(
  teamLeaderApi,
  'export const confirmTeamLeaderReportAllocation = async (',
  'export const getCurrentTeamLeaderReportAllocation'
)
assert(teamLeaderApi.includes('signaturePassword: string')
  && confirmAllocationApi.includes('requireReviewSignaturePayload(data)'),
  'EDHR-STATIC-003: allocation confirm API must require the electronic signature payload before POST')
const teamLeaderWorkbench = read(front, 'processpool/TeamLeaderWorkbenchPage.vue')
const reviewDialogTemplate = sliceBetween(
  teamLeaderWorkbench,
  'v-model="reviewVisible"',
  '<el-table'
)
assert(!reviewDialogTemplate.includes('v-if="reviewDialogMode !== \'ALLOCATION\'"')
  && reviewDialogTemplate.includes('data-team-leader-review-signature'),
  'EDHR-STATIC-003: allocation review dialog must display the electronic signature input')
const submitReview = sliceMethod(teamLeaderWorkbench, 'const submitReview = async () =>')
const allocationSubmitBranch = sliceBetween(
  submitReview,
  "if (reviewDialogMode.value === 'ALLOCATION')",
  '} else {'
)
assert(allocationSubmitBranch.includes('buildReviewSignaturePayload()')
  && allocationSubmitBranch.includes('...reviewSignaturePayload'),
  'EDHR-STATIC-003: allocation submit branch must send electronic signature password')

const lossReader = read(mes, 'service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseLossSourceReaderImpl.java')
assert(!lossReader.includes('matchingEvents.size() > 1'),
  'EDHR-STATIC-004: loss source reader must allow multiple formal production events per process')
const completionBackfill = read(mes, 'service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionBackfillPortImpl.java')
assert(completionBackfill.includes('coversEveryProcessSnapshot')
  && !completionBackfill.includes('getProcessSources().size() != snapshots.size()'),
  'EDHR-STATIC-004: completion backfill must accept multiple formal sources per process while requiring every snapshot to be covered')

const processReader = read(mes, 'service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionReaderImpl.java')
assert(/selectLockedQa\(\s*task,\s*lockedDccQa\s*\)/.test(processReader)
  && processReader.includes('task.getRegulationVersionId()'),
  'EDHR-STATIC-005: process inspection reader must use each task frozen QA regulation identity')
const processWriter = read(mes, 'service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl.java')
assert(processWriter.includes('OWNER_MODULE_MES_QA_COMMON'),
  'EDHR-STATIC-005: process inspection writer must accept generic/common QA regulation provenance')

const detailPanel = read(front, 'processpool/components/ActiveOrderSubmissionDetailPanel.vue')
assert(detailPanel.includes('resolveLossProductionSignatures')
  && detailPanel.includes('submission.productionSubmitterSignatures')
  && !detailPanel.includes('process.submissions,\n        submission.productionEventId'),
  'EDHR-STATIC-006: loss rows must use row-scoped production signature evidence from each PQC submission')
const activeOrderDetailService = read(mes, 'service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java')
assert(activeOrderDetailService.includes('loadEventParties(activeOrderId, tasks)')
  && activeOrderDetailService.includes('task.getBusinessDate(), trimToNull(task.getShiftCode()), task.getRoundNo()')
  && activeOrderDetailService.includes('task.getSubmittedEventId(), eventParty == null ? null : eventParty.getProductionEventId()')
  && activeOrderDetailService.includes('productionEventIds.add(eventParty.getProductionEventId())')
  && activeOrderDetailService.includes('productionSubmitterSignaturesByEventId')
  && activeOrderDetailService.includes('setProductionSubmitterSignatures(productionSignatures)')
  && activeOrderDetailService.includes('setProductionEventId(productionIds.isEmpty() ? null : productionIds.get(0))'),
  'EDHR-STATIC-006: backend detail response must keep PQC loss rows split by row-owned submitted/production events and carry row production signatures')
assert(activeOrderDetailService.includes('private record PqcSubmissionIdentity(ProcessIdentity processIdentity, Long qaProcessId, String qaItemCode,')
  && activeOrderDetailService.includes('String inspectionRuleKey, LocalDate businessDate, String shiftCode,')
  && activeOrderDetailService.includes('Integer roundNo, Long submittedEventId, Long productionEventId)'),
  'EDHR-STATIC-006: PQC detail grouping key must include per-submission business date, shift, round, submitted event, and production event')
const activeOrderDetailServiceTest = read(
  path.join(backend, 'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes'),
  'service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImplTest.java'
)
assert(activeOrderDetailServiceTest.includes('shouldKeepSamePqcScrapItemRowsSeparatedBySubmittedAndProductionEvent')
  && activeOrderDetailServiceTest.includes('assertEquals(List.of(7001L), firstScrap.getProductionEventIds())')
  && activeOrderDetailServiceTest.includes('assertEquals(List.of(7002L), secondScrap.getProductionEventIds())')
  && activeOrderDetailServiceTest.includes('firstScrap.getProductionSubmitterSignatures().get(0).getSignatureId()')
  && activeOrderDetailServiceTest.includes('secondScrap.getProductionSubmitterSignatures().get(0).getSignatureId()'),
  'EDHR-STATIC-006: regression test must prove same QA item/rule scrap rows remain separated by production event')
const activeOrderController = read(mes, 'controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java')
assert(activeOrderController.includes('.setProductionEventId(submission.getProductionEventId())')
  && activeOrderController.includes('.setProductionEventIds(submission.getProductionEventIds())')
  && activeOrderController.includes('.setProductionSubmitterSignatures(submission.getProductionSubmitterSignatures().stream()'),
  'EDHR-STATIC-006: controller must map production event identities and production signatures to the frontend VO')
const teamLeaderTypes = read(path.join(repo, 'IntRuoyiFronted/src/api/mes/pro/processpool'), 'teamLeader.ts')
assert(teamLeaderTypes.includes('productionSubmitterSignatures?: TeamLeaderActiveOrderSignatureDetailRespVO[]'),
  'EDHR-STATIC-006: frontend type must expose row-owned production submitter signatures')

const authService = read(mes, 'service/pro/frontline/MesFrontlineSubmitAuthorizationServiceImpl.java')
assert(authService.includes('assertActiveOrderOpenForProduction'),
  'EDHR-STATIC-007: frontline submission authorization must reject completed/release-application active orders')
assert(allocation.includes('assertActiveOrdersOpenForProduction'),
  'EDHR-STATIC-007: allocation writes must reject completed/release-application active orders')

const managerInit = read(mes, 'service/pro/productionrelease/manager/MesProductionReleaseManagerStageInitializerImpl.java')
assert(managerInit.includes('resolveBusinessReadinessChecks') && !managerInit.includes('.dhrStatus(\"PASS\")'),
  'EDHR-STATIC-008: manager release initializer must not default all business checks to PASS')
const managerApproval = read(mes, 'service/pro/productionrelease/manager/MesProductionReleaseManagerApprovalServiceImpl.java')
assert(managerApproval.includes('requireBusinessReadiness(application, batch)')
  && managerApproval.includes('businessReadinessService.resolveBusinessReadinessChecks(batch)'),
  'EDHR-STATIC-008: manager approval must re-check business readiness before final release')

const disposeReq = read(mes, 'controller/admin/pro/batchrecord/vo/MesProEdhrNonconformanceReviewDisposeReqVO.java')
const disposeService = read(mes, 'service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java')
assert(disposeReq.includes('signaturePassword'),
  'EDHR-STATIC-009: QA disposition request must carry electronic signature password')
assert(disposeService.includes('recordQaDispositionSignature') && disposeService.includes('qaSignatureSnapshotJson'),
  'EDHR-STATIC-009: QA disposition must record verifiable electronic signature evidence')
const nonconformancePage = read(front, 'edhr-nonconformance/NonconformanceReviewPage.vue')
assert(nonconformancePage.includes('signaturePassword') && nonconformancePage.includes('show-password'),
  'EDHR-STATIC-009: frontend must request signature password, not free text signature only')

const batchExecution = read(mes, 'service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java')
assert(batchExecution.includes('validateBatchNotFrozenForSpecialAttachmentSave'),
  'EDHR-STATIC-010: report attachment save must share the frozen-batch gate before file persistence')
assert(disposeService.includes('recomputeWorkOrderTemporaryFreeze') &&
  !disposeService.includes('setTemporaryFrozen(review.getTemporaryFrozenBefore())'),
  'EDHR-STATIC-011: nonconformance dispose must recompute work-order freeze from active reasons')
assert(batchExecution.includes('batch.getRouteCode()') && batchExecution.includes('batch.getRouteName()'),
  'EDHR-STATIC-012: archive manifest must use frozen batch route code/name')
assert(batchExecution.includes('PRO_EDHR_BATCH_EXECUTION_ROUTE_SNAPSHOT_REQUIRED')
  && batchExecution.includes('StrUtil.isBlank(batch.getRouteCode())')
  && batchExecution.includes('StrUtil.isBlank(batch.getRouteName())'),
  'EDHR-STATIC-012: archive manifest must fail fast when the frozen batch route snapshot is missing')

const progress = read(mes, 'service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionProgressPortImpl.java')
assert(progress.includes('calculateConservativeProcessProgress') && progress.includes('selectProductionSubmitsByWorkOrderAndRouteForUpdate'),
  'EDHR-STATIC-013: completion progress must calculate multi-output progress from material-conservative production facts')

const routeProjection = read(mes, 'service/pro/route/MesProRouteVersionPublishProjectionServiceImpl.java')
assert(!routeProjection.includes('routeFlowProcessBatchRecordMapper.deleteByRouteIdAndUseType(routeId, useType)')
  && routeProjection.includes('Historical route-version batch-record bindings stay queryable by ID'),
  'EDHR-STATIC-014: route publish must not physically delete old version batch-record bindings')
assert(!routeProjection.includes('routeFlowProcessBatchRecordMapper.insert(MesProRouteFlowProcessBatchRecordDO.builder()')
  && routeProjection.includes('projected form binding identity is required')
  && routeProjection.includes('projected batch record binding identity is required'),
  'EDHR-STATIC-014: route publish must insert projected bindings through identity-checked variables')
assert(routeProjection.includes('report.put("routeBindingId", projectedBinding.getId())'),
  'EDHR-STATIC-014: route publish must rewrite batchRecordReports with the new projected routeBindingId')
const batchRecordMapper = read(mes, 'dal/mysql/pro/route/MesProRouteFlowProcessBatchRecordMapper.java')
assert(batchRecordMapper.includes('selectCurrentProjectionListByRouteIdAndUseType')
  && batchRecordMapper.includes('INNER JOIN mes_pro_route_flow_process_config pc')
  && batchRecordMapper.includes('pc.id = br.route_flow_process_config_id')
  && batchRecordMapper.includes('pc.deleted = FALSE'),
  'EDHR-STATIC-014: route-level current binding reads must exclude retained historical projection rows')
const routeService = read(mes, 'service/pro/route/MesProRouteServiceImpl.java')
const batchRecordReportSnapshot = sliceMethod(routeService, 'private JSONObject buildBatchRecordReportSnapshot(')
assert(batchRecordReportSnapshot.includes('report.put("routeBindingId", record.getId())'),
  'EDHR-STATIC-014: frozen route batchRecordReports snapshots must carry routeBindingId')
const batchRecordWriter = read(mes, 'service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl.java')
assert(batchRecordWriter.includes('selectVersionedProductionReportBindings')
  && batchRecordWriter.includes('routeVersionSnapshotResolver.resolveVersion(command.getRouteVersionId())')
  && !batchRecordWriter.includes('routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType'),
  'EDHR-STATIC-014: release batch-record writer must resolve bindings through the order frozen route version')

console.log('PASS: eDHR static findings fix contract')
