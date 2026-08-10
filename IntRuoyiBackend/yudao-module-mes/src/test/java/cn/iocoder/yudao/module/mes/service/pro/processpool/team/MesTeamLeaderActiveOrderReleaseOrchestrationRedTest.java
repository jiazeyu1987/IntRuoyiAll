package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrCandidateResolver;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseService;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderReleaseOrchestrationRedTest {

    private static final Long LEADER_USER_ID = 1001L;
    private static final Long ACTIVE_ORDER_ID = 2001L;
    private static final Long WORK_ORDER_ID = 3001L;
    private static final Long ROUTE_ID = 4001L;
    private static final Long ROUTE_VERSION_ID = 4002L;
    private static final Long ROUTE_PROCESS_ID = 5001L;
    private static final Long PROCESS_ID = 6001L;
    private static final Long BATCH_EXECUTION_ID = 7001L;
    private static final String SOURCE_HASH = "canonical-source-hash";

    @Mock private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock private MesProWorkOrderMapper workOrderMapper;
    @Mock private MesMdItemMapper itemMapper;
    @Mock private MesProRouteMapper routeMapper;
    @Mock private MesProRouteVersionMapper routeVersionMapper;
    @Mock private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    @Mock private MesProcessPoolOrderProcessCompletionMapper completionMapper;
    @Mock private MesPqcInspectionTaskMapper pqcTaskMapper;
    @Mock private MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper;
    @Mock private MesWorkOrderAbnormalStateService abnormalStateService;
    @Mock private MesProProcessPoolEventMapper eventMapper;
    @Mock private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock private MesProcessPoolSubmissionReviewMapper reviewMapper;
    @Mock private MesTeamLeaderActiveOrderReleaseBatchRecordWriter batchRecordWriter;
    @Mock private MesTeamLeaderActiveOrderReleaseProcessInspectionWriter processInspectionWriter;
    @Mock private MesTeamLeaderActiveOrderReleaseLossReportWriter lossReportWriter;
    @Mock private MesTeamLeaderActiveOrderReleaseDossierCompletenessChecker completenessChecker;
    @Mock private MesProEdhrBatchExecutionService batchExecutionService;
    @Mock private MesProEdhrReleaseService releaseService;
    @Mock private MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    @Mock private MesProEdhrWorkTaskAssignmentRuleMapper assignmentRuleMapper;
    @Mock private MesProEdhrCandidateResolver candidateResolver;
    @Mock private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Mock private MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher sourceSnapshotHasher;

    private MesTeamLeaderActiveOrderReleaseApplicationPersistenceService persistenceService;
    private MesTeamLeaderActiveOrderReleaseGenerationService generationService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        persistenceService = new MesTeamLeaderActiveOrderReleaseApplicationPersistenceService(applicationMapper);
        generationService = new MesTeamLeaderActiveOrderReleaseGenerationService(
                activeOrderMapper, workOrderMapper, itemMapper, routeMapper, routeVersionMapper,
                processSnapshotMapper, completionMapper, pqcTaskMapper,
                aggregateDetailMapper, abnormalStateService, eventMapper, allocationMapper, reviewMapper,
                batchRecordWriter, processInspectionWriter, lossReportWriter, completenessChecker,
                batchExecutionService, releaseService, applicationMapper, persistenceService,
                assignmentRuleMapper, candidateResolver, batchTaskMapper, sourceSnapshotHasher);

        lenient().when(applicationMapper.selectByRequestIdempotencyKey(ACTIVE_ORDER_ID, "release-request-1"))
                .thenReturn(null);
        lenient().when(applicationMapper.selectByBusinessIdempotencyKey(any(), any())).thenReturn(null);
        lenient().when(activeOrderMapper.selectByIdForUpdate(ACTIVE_ORDER_ID)).thenReturn(activeOrder());
        lenient().when(workOrderMapper.selectByIdForUpdate(WORK_ORDER_ID)).thenReturn(workOrder());
        lenient().when(itemMapper.selectByIdForUpdate(3101L)).thenReturn(product());
        lenient().when(routeMapper.selectByIdForUpdate(ROUTE_ID)).thenReturn(route());
        lenient().when(routeVersionMapper.selectByIdForUpdate(ROUTE_VERSION_ID)).thenReturn(routeVersion());
        lenient().when(processSnapshotMapper.selectListByActiveOrderIdForUpdate(ACTIVE_ORDER_ID))
                .thenReturn(List.of(snapshot()));
        lenient().when(completionMapper.selectListByWorkOrderIds(List.of(WORK_ORDER_ID)))
                .thenReturn(List.of(completion()));
        lenient().when(pqcTaskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID))
                .thenReturn(List.of(pqcTask(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED)));
        lenient().when(aggregateDetailMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID))
                .thenReturn(List.of(aggregateDetail()));
        lenient().when(abnormalStateService.hasOpenAbnormal(WORK_ORDER_ID)).thenReturn(false);
        lenient().when(eventMapper.selectProductionSubmitsByWorkOrderAndRoute(WORK_ORDER_ID, ROUTE_ID))
                .thenReturn(List.of());

        lenient().when(batchRecordWriter.plan(any())).thenAnswer(invocation ->
                batchPlan(invocation.getArgument(0), List.of()));
        lenient().when(processInspectionWriter.plan(any())).thenAnswer(invocation ->
                inspectionPlan(invocation.getArgument(0), List.of()));
        lenient().when(lossReportWriter.plan(any())).thenAnswer(invocation ->
                lossPlan(invocation.getArgument(0), List.of()));
        lenient().when(assignmentRuleMapper.selectEnabledByScopeAndType("ROUTE", ROUTE_ID, "RELEASE_APPROVE"))
                .thenReturn(releaseRule());
        lenient().when(candidateResolver.resolveAssignmentRule(any())).thenReturn(
                new MesProEdhrCandidateResolver.MesProEdhrCandidateContract("USER", 9201L, "9201"));
        lenient().when(sourceSnapshotHasher.hash(any())).thenReturn(SOURCE_HASH);

        lenient().when(batchExecutionService.openOrCreate(any())).thenReturn(batch());
        lenient().when(batchRecordWriter.write(any(), any())).thenReturn(batchWrite());
        lenient().when(processInspectionWriter.write(any(), any())).thenReturn(inspectionWrite());
        lenient().when(lossReportWriter.write(any(), any())).thenReturn(lossWrite());
        lenient().when(batchTaskMapper.selectListByBatchExecutionId(BATCH_EXECUTION_ID))
                .thenReturn(List.of(batchTask(), inspectionTask()));
        lenient().when(completenessChecker.check(any())).thenAnswer(invocation ->
                new MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerImpl()
                        .check(invocation.getArgument(0)));
        lenient().when(releaseService.precheck(any())).thenReturn(precheck());
        lenient().when(releaseService.submitForApproval(any())).thenReturn(submitted());
        lenient().doAnswer(invocation -> {
            MesProcessPoolActiveOrderReleaseApplicationDO application = invocation.getArgument(0);
            application.setId(10001L);
            return 1;
        }).when(applicationMapper).insert(any(MesProcessPoolActiveOrderReleaseApplicationDO.class));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void applyMustPlanAllWritersThenWriteInFixedOrderAndOnlyThenCreateReleaseApproveTask() {
        MesTeamLeaderActiveOrderReleaseApplicationResult result = generationService.generate(
                LEADER_USER_ID, command());

        verify(activeOrderMapper).selectByIdForUpdate(ACTIVE_ORDER_ID);
        verify(workOrderMapper).selectByIdForUpdate(WORK_ORDER_ID);
        verify(itemMapper).selectByIdForUpdate(3101L);
        verify(routeMapper).selectByIdForUpdate(ROUTE_ID);
        verify(routeVersionMapper).selectByIdForUpdate(ROUTE_VERSION_ID);
        verify(processSnapshotMapper).selectListByActiveOrderIdForUpdate(ACTIVE_ORDER_ID);
        InOrder order = inOrder(batchRecordWriter, processInspectionWriter, lossReportWriter,
                batchExecutionService, batchTaskMapper, completenessChecker, releaseService);
        order.verify(batchRecordWriter).plan(any());
        order.verify(processInspectionWriter).plan(any());
        order.verify(lossReportWriter).plan(any());
        order.verify(batchExecutionService).openOrCreate(any());
        order.verify(batchRecordWriter).write(any(), org.mockito.ArgumentMatchers.eq(BATCH_EXECUTION_ID));
        order.verify(processInspectionWriter).write(any(), org.mockito.ArgumentMatchers.eq(BATCH_EXECUTION_ID));
        order.verify(lossReportWriter).write(any(), org.mockito.ArgumentMatchers.eq(BATCH_EXECUTION_ID));
        order.verify(batchTaskMapper).selectListByBatchExecutionId(BATCH_EXECUTION_ID);
        order.verify(completenessChecker).check(any());
        order.verify(releaseService).precheck(any());
        order.verify(releaseService).submitForApproval(any());

        ArgumentCaptor<MesTeamLeaderActiveOrderReleaseDossierCompletenessCommand> completenessCaptor =
                ArgumentCaptor.forClass(MesTeamLeaderActiveOrderReleaseDossierCompletenessCommand.class);
        verify(completenessChecker).check(completenessCaptor.capture());
        Set<String> documentTypes = completenessCaptor.getValue().getDocuments().stream()
                .map(MesTeamLeaderActiveOrderReleaseDocumentEvidence::getDocumentType)
                .collect(java.util.stream.Collectors.toSet());
        assertAll(
                () -> assertEquals("PENDING_RELEASE_APPROVAL", result.getStatus()),
                () -> assertEquals(9001L, result.getReleaseApprovalWorkTaskId()),
                () -> assertNotNull(result.getDossierSummary()),
                () -> assertEquals(4, result.getDossierSummary().getSignatureEvidenceCount(),
                        "跨批记录与损耗单复用的同一正式生产签名只能计数一次"),
                () -> assertEquals(SOURCE_HASH, result.getDossierSummary().getSourceSnapshotHash()),
                () -> assertEquals(Set.of("BATCH_RECORD", "PROCESS_INSPECTION", "LOSS_REPORT"), documentTypes),
                () -> assertEquals(List.of(9201L), completenessCaptor.getValue().getReleaseOwnerCandidateUserIds()),
                () -> assertFalse(completenessCaptor.getValue().getDocuments().isEmpty()));
    }

    @Test
    void anyPlanningBlockerMustStopBeforeOpeningBatchOrWritingPartialDossier() {
        when(lossReportWriter.plan(any())).thenAnswer(invocation -> lossPlan(invocation.getArgument(0), List.of(
                blocker("LOSS_SOURCE_REQUIRED"))));

        MesTeamLeaderActiveOrderReleaseBlockedException failure = assertThrows(
                MesTeamLeaderActiveOrderReleaseBlockedException.class,
                () -> generationService.generate(LEADER_USER_ID, command()));

        assertAll(
                () -> assertEquals("BLOCKED", failure.getApplication().getApplicationStatus()),
                () -> assertTrue(failure.getApplication().getBlockerSnapshotJson().contains("LOSS_SOURCE_REQUIRED")),
                () -> assertNull(failure.getApplication().getBatchExecutionId()),
                () -> assertNull(failure.getApplication().getReleaseApprovalWorkTaskId()));
        verify(batchExecutionService, never()).openOrCreate(any());
        verify(batchRecordWriter, never()).write(any(), any());
        verify(processInspectionWriter, never()).write(any(), any());
        verify(lossReportWriter, never()).write(any(), any());
        verify(releaseService, never()).precheck(any());
        verify(releaseService, never()).submitForApproval(any());
        verify(applicationMapper, never()).insert(any(MesProcessPoolActiveOrderReleaseApplicationDO.class));
    }

    @Test
    void missingReleaseApproveRuleMustBlockWithoutUsingAnotherOwnerSource() {
        when(assignmentRuleMapper.selectEnabledByScopeAndType("ROUTE", ROUTE_ID, "RELEASE_APPROVE"))
                .thenReturn(null);

        MesTeamLeaderActiveOrderReleaseBlockedException failure = assertThrows(
                MesTeamLeaderActiveOrderReleaseBlockedException.class,
                () -> generationService.generate(LEADER_USER_ID, command()));

        assertTrue(failure.getApplication().getBlockerSnapshotJson().contains("RELEASE_OWNER_REQUIRED"));
        verify(candidateResolver, never()).resolveAssignmentRule(any());
        verify(batchExecutionService, never()).openOrCreate(any());
        verify(releaseService, never()).precheck(any());
        verify(releaseService, never()).submitForApproval(any());
    }

    @Test
    void completenessBlockerAfterWritesMustStopBeforePrecheckAndApproval() {
        when(completenessChecker.check(any())).thenReturn(
                new MesTeamLeaderActiveOrderReleaseDossierCompletenessResult()
                        .setComplete(false).setBlockers(List.of(blocker("DOSSIER_COMPLETENESS_BLOCKED"))));

        MesTeamLeaderActiveOrderReleaseBlockedException failure = assertThrows(
                MesTeamLeaderActiveOrderReleaseBlockedException.class,
                () -> generationService.generate(LEADER_USER_ID, command()));

        assertTrue(failure.getApplication().getBlockerSnapshotJson().contains("DOSSIER_COMPLETENESS_BLOCKED"));
        verify(batchRecordWriter).write(any(), org.mockito.ArgumentMatchers.eq(BATCH_EXECUTION_ID));
        verify(processInspectionWriter).write(any(), org.mockito.ArgumentMatchers.eq(BATCH_EXECUTION_ID));
        verify(lossReportWriter).write(any(), org.mockito.ArgumentMatchers.eq(BATCH_EXECUTION_ID));
        verify(releaseService, never()).precheck(any());
        verify(releaseService, never()).submitForApproval(any());
        verify(applicationMapper, never()).insert(any(MesProcessPoolActiveOrderReleaseApplicationDO.class));
    }

    @Test
    void precheckBlockerAfterCompleteDossierMustRollbackWithoutApprovalTaskOrPendingReceipt() {
        when(releaseService.precheck(any())).thenReturn(new MesProEdhrReleaseRespVO()
                .setReleaseTransactionId(8001L).setBlockingCheckCount(1).setFailedCheckCount(0)
                .setPrecheckSummary("required release check failed"));

        MesTeamLeaderActiveOrderReleaseBlockedException failure = assertThrows(
                MesTeamLeaderActiveOrderReleaseBlockedException.class,
                () -> generationService.generate(LEADER_USER_ID, command()));

        assertTrue(failure.getApplication().getBlockerSnapshotJson().contains("RELEASE_PRECHECK_BLOCKED"));
        verify(batchRecordWriter).write(any(), org.mockito.ArgumentMatchers.eq(BATCH_EXECUTION_ID));
        verify(processInspectionWriter).write(any(), org.mockito.ArgumentMatchers.eq(BATCH_EXECUTION_ID));
        verify(lossReportWriter).write(any(), org.mockito.ArgumentMatchers.eq(BATCH_EXECUTION_ID));
        verify(releaseService, never()).submitForApproval(any());
        verify(applicationMapper, never()).insert(any(MesProcessPoolActiveOrderReleaseApplicationDO.class));
    }

    @Test
    void writerReceiptMustMatchTheCanonicalPlanEvidenceBeforeCompleteness() {
        when(batchRecordWriter.write(any(), any())).thenReturn(
                batchWrite().setSourceValueHashes(List.of("tampered-source-hash")));

        assertThrows(ServiceException.class, () -> generationService.generate(LEADER_USER_ID, command()));

        verify(completenessChecker, never()).check(any());
        verify(releaseService, never()).precheck(any());
        verify(releaseService, never()).submitForApproval(any());
        verify(applicationMapper, never()).insert(any(MesProcessPoolActiveOrderReleaseApplicationDO.class));
    }

    @Test
    void submittedOnlyPqcMustNotCountTowardFormalInspectionCompletion() {
        when(pqcTaskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID))
                .thenReturn(List.of(pqcTask(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)));

        assertThrows(ServiceException.class, () -> generationService.generate(LEADER_USER_ID, command()));

        verify(batchRecordWriter, never()).plan(any());
        verify(processInspectionWriter, never()).plan(any());
        verify(lossReportWriter, never()).plan(any());
        verify(batchExecutionService, never()).openOrCreate(any());
        verify(releaseService, never()).precheck(any());
        verify(releaseService, never()).submitForApproval(any());
    }

    @Test
    void requestIdempotencyMustReturnExistingReceiptAfterCurrentLeaderAuthorizationWithoutReplanning() {
        MesProcessPoolActiveOrderReleaseApplicationDO existing = pendingApplication("old-source", "old-business")
                .setId(11001L);
        when(applicationMapper.selectByRequestIdempotencyKey(ACTIVE_ORDER_ID, "release-request-1"))
                .thenReturn(existing);

        MesTeamLeaderActiveOrderReleaseApplicationResult result = generationService.generate(
                LEADER_USER_ID, command());

        assertEquals(11001L, result.getApplicationId());
        verify(activeOrderMapper).selectByIdForUpdate(ACTIVE_ORDER_ID);
        verify(batchRecordWriter, never()).plan(any());
        verify(batchExecutionService, never()).openOrCreate(any());
    }

    @Test
    void requestIdempotencyMustNotBypassCurrentLeaderAuthorization() {
        assertThrows(ServiceException.class, () -> generationService.generate(9999L, command()));

        verify(activeOrderMapper).selectByIdForUpdate(ACTIVE_ORDER_ID);
        verify(batchRecordWriter, never()).plan(any());
        verify(batchExecutionService, never()).openOrCreate(any());
    }

    @Test
    void overlongIdempotencyKeyMustFailBeforeReadingOrGeneratingReleaseState() {
        MesTeamLeaderActiveOrderReleaseApplyCommand invalid = command().setIdempotencyKey("x".repeat(129));

        assertThrows(ServiceException.class, () -> generationService.generate(LEADER_USER_ID, invalid));

        verify(applicationMapper, never()).selectByRequestIdempotencyKey(any(), any());
        verify(activeOrderMapper, never()).selectByIdForUpdate(any());
        verify(batchRecordWriter, never()).plan(any());
    }

    @Test
    void overlongApplyRemarkMustFailBeforeReadingOrGeneratingReleaseState() {
        MesTeamLeaderActiveOrderReleaseApplyCommand invalid = command().setApplyRemark("r".repeat(501));

        assertThrows(ServiceException.class, () -> generationService.generate(LEADER_USER_ID, invalid));

        verify(applicationMapper, never()).selectByRequestIdempotencyKey(any(), any());
        verify(activeOrderMapper, never()).selectByIdForUpdate(any());
        verify(batchRecordWriter, never()).plan(any());
    }

    @Test
    void businessIdempotencyMustReturnExistingReceiptBeforeOpeningAnotherBatch() {
        MesProcessPoolActiveOrderReleaseApplicationDO existing = pendingApplication(
                SOURCE_HASH, "AO_RELEASE_SOURCE_V1|3001|4002|" + SOURCE_HASH).setId(12001L);
        when(applicationMapper.selectByBusinessIdempotencyKey(
                ACTIVE_ORDER_ID, "AO_RELEASE_SOURCE_V1|3001|4002|" + SOURCE_HASH)).thenReturn(existing);

        MesTeamLeaderActiveOrderReleaseApplicationResult result = generationService.generate(
                LEADER_USER_ID, command());

        assertEquals(12001L, result.getApplicationId());
        verify(batchRecordWriter).plan(any());
        verify(processInspectionWriter).plan(any());
        verify(lossReportWriter).plan(any());
        verify(batchExecutionService, never()).openOrCreate(any());
        verify(batchRecordWriter, never()).write(any(), any());
        verify(releaseService, never()).precheck(any());
    }

    @Test
    void businessIdempotencyMustRejectReceiptWhoseFormalSnapshotDoesNotMatchItsKey() {
        String businessKey = "AO_RELEASE_SOURCE_V1|3001|4002|" + SOURCE_HASH;
        MesProcessPoolActiveOrderReleaseApplicationDO existing = pendingApplication(
                "different-source", businessKey).setId(12001L);
        when(applicationMapper.selectByBusinessIdempotencyKey(ACTIVE_ORDER_ID, businessKey)).thenReturn(existing);

        assertThrows(ServiceException.class, () -> generationService.generate(LEADER_USER_ID, command()));

        verify(batchExecutionService, never()).openOrCreate(any());
        verify(batchRecordWriter, never()).write(any(), any());
    }

    @Test
    void facadeMustPersistBlockedReceiptOnlyAfterGenerationFailureEscapesItsTransaction() {
        MesTeamLeaderActiveOrderReleaseGenerationService failedGeneration =
                mock(MesTeamLeaderActiveOrderReleaseGenerationService.class);
        MesTeamLeaderActiveOrderReleaseApplicationPersistenceService blockedPersistence =
                mock(MesTeamLeaderActiveOrderReleaseApplicationPersistenceService.class);
        MesProcessPoolActiveOrderReleaseApplicationDO blocked = pendingApplication(
                SOURCE_HASH, "business-blocked").setApplicationStatus("BLOCKED")
                .setBatchExecutionId(null).setReleaseTransactionId(null).setReleaseApprovalWorkTaskId(null);
        when(failedGeneration.generate(LEADER_USER_ID, command()))
                .thenThrow(new MesTeamLeaderActiveOrderReleaseBlockedException(blocked));
        when(blockedPersistence.persistBlocked(blocked)).thenReturn(
                new MesTeamLeaderActiveOrderReleaseApplicationResult().setStatus("BLOCKED"));
        MesTeamLeaderActiveOrderReleaseApplicationServiceImpl facade =
                new MesTeamLeaderActiveOrderReleaseApplicationServiceImpl(failedGeneration, blockedPersistence);

        MesTeamLeaderActiveOrderReleaseApplicationResult result = facade.apply(LEADER_USER_ID, command());

        assertEquals("BLOCKED", result.getStatus());
        InOrder order = inOrder(failedGeneration, blockedPersistence);
        order.verify(failedGeneration).generate(LEADER_USER_ID, command());
        order.verify(blockedPersistence).persistBlocked(blocked);
    }

    @Test
    void concurrentRequestKeyConflictMustNotReturnReceiptForDifferentFormalSnapshot() {
        MesProcessPoolActiveOrderReleaseApplicationDO incoming = pendingApplication(
                SOURCE_HASH, "business-current").setApplicationStatus("BLOCKED")
                .setBatchExecutionId(null).setReleaseTransactionId(null).setReleaseApprovalWorkTaskId(null);
        MesProcessPoolActiveOrderReleaseApplicationDO conflicting = pendingApplication(
                "different-source", "business-different").setId(13001L);
        org.mockito.Mockito.doThrow(new DuplicateKeyException("request key conflict"))
                .when(applicationMapper).insert(incoming);
        when(applicationMapper.selectByRequestIdempotencyKey(ACTIVE_ORDER_ID, "release-request-1"))
                .thenReturn(conflicting);
        when(applicationMapper.selectByBusinessIdempotencyKey(ACTIVE_ORDER_ID, "business-current"))
                .thenReturn(null);

        assertThrows(DuplicateKeyException.class, () -> persistenceService.persistBlocked(incoming));
    }

    @Test
    void transactionContractMustSeparateAtomicGenerationFromBlockedReceiptPersistence() throws Exception {
        Transactional generation = MesTeamLeaderActiveOrderReleaseGenerationService.class
                .getMethod("generate", Long.class, MesTeamLeaderActiveOrderReleaseApplyCommand.class)
                .getAnnotation(Transactional.class);
        Transactional pending = MesTeamLeaderActiveOrderReleaseApplicationPersistenceService.class
                .getMethod("persistPending", MesProcessPoolActiveOrderReleaseApplicationDO.class)
                .getAnnotation(Transactional.class);
        Transactional blocked = MesTeamLeaderActiveOrderReleaseApplicationPersistenceService.class
                .getMethod("persistBlocked", MesProcessPoolActiveOrderReleaseApplicationDO.class)
                .getAnnotation(Transactional.class);

        assertAll(
                () -> assertNotNull(generation),
                () -> assertNotNull(pending),
                () -> assertNotNull(blocked),
                () -> assertTrue(List.of(generation.rollbackFor()).contains(Exception.class)),
                () -> assertEquals(Propagation.MANDATORY, pending.propagation()),
                () -> assertEquals(Propagation.REQUIRES_NEW, blocked.propagation()));
    }

    private static MesTeamLeaderActiveOrderReleaseApplyCommand command() {
        return new MesTeamLeaderActiveOrderReleaseApplyCommand()
                .setActiveOrderId(ACTIVE_ORDER_ID)
                .setIdempotencyKey("release-request-1")
                .setApplyRemark("V4 A2 integration");
    }

    private static MesProcessPoolActiveOrderDO activeOrder() {
        return MesProcessPoolActiveOrderDO.builder().id(ACTIVE_ORDER_ID).leaderUserId(LEADER_USER_ID)
                .workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID)
                .activeStatus("ACTIVE").businessStatus("PRODUCING")
                .erpFixedQuantitySnapshot(BigDecimal.TEN).build();
    }

    private static MesProWorkOrderDO workOrder() {
        return MesProWorkOrderDO.builder().id(WORK_ORDER_ID).code("WO-V4-A2-001")
                .batchCode("BATCH-V4-A2-001").productId(3101L).quantity(BigDecimal.TEN).build();
    }

    private static MesMdItemDO product() {
        return MesMdItemDO.builder().id(3101L).code("PRODUCT-3101").specification("V4")
                .unitMeasureId(3102L).itemTypeId(3103L).status(0).batchFlag(true).build();
    }

    private static MesProRouteDO route() {
        return MesProRouteDO.builder().id(ROUTE_ID).code("ROUTE-4001").status(0).build();
    }

    private static MesProRouteVersionDO routeVersion() {
        return MesProRouteVersionDO.builder().id(ROUTE_VERSION_ID).routeId(ROUTE_ID).versionNo("V1")
                .active(true).lifecycleStatus("ACTIVE").routeSnapshotJson("{\"version\":1}")
                .publishedBy(1000L).publishedTime(LocalDateTime.of(2026, 8, 8, 8, 0)).build();
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO snapshot() {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder().id(4101L).activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID)
                .routeProcessId(ROUTE_PROCESS_ID).processId(PROCESS_ID).build();
    }

    private static MesProcessPoolOrderProcessCompletionDO completion() {
        return MesProcessPoolOrderProcessCompletionDO.builder().id(5101L).workOrderId(WORK_ORDER_ID)
                .routeProcessId(ROUTE_PROCESS_ID).processId(PROCESS_ID)
                .targetQuantity(BigDecimal.TEN).confirmedQuantity(BigDecimal.TEN)
                .completionStatus(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED)
                .completedAt(LocalDateTime.of(2026, 8, 9, 9, 0))
                .backfillStatus(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS)
                .backfillExecutionId(5201L).lastEventId(5301L).lastReviewId(5401L)
                .sourceEventIdsJson("[5301]").sourceAllocationIdsJson("[5501]")
                .aggregateHash("production-aggregate").backfillIdempotencyKey("production-backfill-key")
                .build();
    }

    private static MesPqcInspectionTaskDO pqcTask(String status) {
        return MesPqcInspectionTaskDO.builder().id(6101L).activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID)
                .routeProcessId(ROUTE_PROCESS_ID).processId(PROCESS_ID).taskStatus(status).build();
    }

    private static MesPqcProcessInspectionAggregateDetailDO aggregateDetail() {
        return MesPqcProcessInspectionAggregateDetailDO.builder().id(6201L).pqcTaskId(6101L)
                .activeOrderId(ACTIVE_ORDER_ID).workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID)
                .routeVersionId(ROUTE_VERSION_ID).routeProcessId(ROUTE_PROCESS_ID).processId(PROCESS_ID).build();
    }

    private static MesTeamLeaderActiveOrderReleaseBatchRecordPlan batchPlan(
            MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand command,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        MesTeamLeaderActiveOrderReleaseBatchRecordPlan.PreparedProcess prepared =
                new MesTeamLeaderActiveOrderReleaseBatchRecordPlan.PreparedProcess()
                        .setSource(new MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand.ProcessSource()
                                .setSnapshot(snapshot()).setCompletion(completion())
                                .setSourceEvents(List.of()).setAllocations(List.of()).setReviews(List.of()))
                        .setBinding(binding(7101L, "BATCH-REPORT", "BATCH_RECORD", null))
                        .setRules(List.of(rule(8101L)));
        return new MesTeamLeaderActiveOrderReleaseBatchRecordPlan().setCommand(command)
                .setPreparedProcesses(List.of(prepared)).setSourceObjectIds(List.of(5101L, 7101L))
                .setSourceValueHashes(List.of("batch-source-hash"))
                .setSignatureEvidence(List.of(batchSignature("FILLER", 8201L),
                        batchSignature("REVIEWER", 8202L))).setBlockers(blockers);
    }

    private static MesTeamLeaderActiveOrderReleaseProcessInspectionPlan inspectionPlan(
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source =
                new MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource()
                        .setTask(pqcTask(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED));
        MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.PreparedInspection prepared =
                new MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.PreparedInspection()
                        .setSource(source)
                        .setBinding(binding(7102L, "PQC-REPORT", "INTERNAL_RECORD", "PROCESS_INSPECTION"))
                        .setMappedValues(List.of(new MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.MappedValue()
                                .setRule(rule(8102L)).setValue("PASS").setDisplayValue("合格")))
                        .setEvidenceHash("inspection-evidence");
        return new MesTeamLeaderActiveOrderReleaseProcessInspectionPlan().setCommand(command)
                .setPreparedInspections(List.of(prepared)).setSourceObjectIds(List.of(6101L, 7102L))
                .setSourceValueHashes(List.of("inspection-source-hash"))
                .setSignatureEvidence(List.of(inspectionSignature("FILLER", 8301L),
                        inspectionSignature("REVIEWER", 8302L))).setBlockers(blockers);
    }

    private static MesTeamLeaderActiveOrderReleaseLossReportPlan lossPlan(
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        MesTeamLeaderActiveOrderReleaseLossReportPlan.PreparedLossReport prepared =
                new MesTeamLeaderActiveOrderReleaseLossReportPlan.PreparedLossReport()
                        .setSources(List.of())
                        .setBinding(binding(7103L, "LOSS-REPORT", "INTERNAL_RECORD", "LOSS_REPORT"))
                        .setRules(List.of(rule(8103L))).setMappedValues(java.util.Map.of("loss", BigDecimal.ONE))
                        .setTargetSnapshotHashes(List.of("loss-target-hash"))
                        .setEvidenceHash("loss-evidence");
        return new MesTeamLeaderActiveOrderReleaseLossReportPlan().setCommand(command)
                .setPreparedReports(List.of(prepared)).setSourceObjectIds(List.of(7103L, 8501L))
                .setSourceValueHashes(List.of("loss-source-hash"))
                .setSignatureEvidence(List.of(commonSignature("FILLER", 8201L),
                        commonSignature("REVIEWER", 8202L))).setBlockers(blockers);
    }

    private static MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult batchWrite() {
        return new MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult().setDocumentType("BATCH_RECORD")
                .setBatchRecordExecutionIds(List.of(8601L)).setFieldAuditIds(List.of(8602L))
                .setFieldAuditHeadHashes(List.of("batch-audit-head"))
                .setSourceObjectIds(List.of(5101L, 7101L)).setSourceValueHashes(List.of("batch-source-hash"))
                .setSignatureEvidence(List.of(batchSignature("FILLER", 8201L),
                        batchSignature("REVIEWER", 8202L))).setBlockers(List.of());
    }

    private static MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult inspectionWrite() {
        return new MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult()
                .setDocumentType("PROCESS_INSPECTION").setBatchRecordExecutionIds(List.of(8701L))
                .setFieldAuditIds(List.of(8702L)).setFieldAuditHeadHashes(List.of("inspection-audit-head"))
                .setSourceObjectIds(List.of(6101L, 7102L))
                .setSourceValueHashes(List.of("inspection-source-hash"))
                .setSignatureEvidence(List.of(inspectionSignature("FILLER", 8301L),
                        inspectionSignature("REVIEWER", 8302L))).setBlockers(List.of());
    }

    private static MesTeamLeaderActiveOrderReleaseLossReportWriteResult lossWrite() {
        MesTeamLeaderActiveOrderReleaseDocumentEvidence document =
                new MesTeamLeaderActiveOrderReleaseDocumentEvidence().setDocumentType("LOSS_REPORT")
                        .setBatchExecutionId(BATCH_EXECUTION_ID).setBatchExecutionTaskId(8801L)
                        .setBatchRecordExecutionIds(List.of(8802L)).setTargetReportIds(List.of("LOSS-REPORT"))
                        .setTargetDefinitionIds(List.of(7203L)).setTargetVersionIds(List.of(7303L))
                        .setTargetSnapshotHashes(List.of("loss-target-hash")).setFieldAuditIds(List.of(8803L))
                        .setRequiredFieldCount(1).setAuditedRequiredFieldCount(1)
                        .setSourceObjectIds(List.of(7103L, 8501L)).setSourceValueHashes(List.of("loss-source-hash"))
                        .setSignatureEvidence(List.of(commonSignature("FILLER", 8201L),
                                commonSignature("REVIEWER", 8202L)))
                        .setSourceSnapshotHash(SOURCE_HASH).setSourceConsistent(true);
        return new MesTeamLeaderActiveOrderReleaseLossReportWriteResult().setDocumentType("LOSS_REPORT")
                .setBatchRecordExecutionIds(List.of(8802L)).setFieldAuditIds(List.of(8803L))
                .setFieldAuditHeadHashes(List.of("loss-audit-head"))
                .setSourceObjectIds(List.of(7103L, 8501L)).setSourceValueHashes(List.of("loss-source-hash"))
                .setSignatureEvidence(List.of(commonSignature("FILLER", 8201L),
                        commonSignature("REVIEWER", 8202L)))
                .setDocumentEvidence(List.of(document)).setBlockers(List.of());
    }

    private static MesProEdhrBatchExecutionTaskDO batchTask() {
        return task(8901L, 7101L, "BATCH-REPORT", 7201L, 7301L, "BATCH_RECORD", null);
    }

    private static MesProEdhrBatchExecutionTaskDO inspectionTask() {
        return task(8902L, 7102L, "PQC-REPORT", 7202L, 7302L,
                "INTERNAL_RECORD", "PROCESS_INSPECTION");
    }

    private static MesProEdhrBatchExecutionTaskDO task(Long id, Long bindingId, String reportId,
                                                       Long definitionId, Long versionId,
                                                       String category, String slot) {
        return MesProEdhrBatchExecutionTaskDO.builder().id(id).batchExecutionId(BATCH_EXECUTION_ID)
                .routeProcessId(ROUTE_PROCESS_ID).processId(PROCESS_ID).routeBindingId(bindingId)
                .batchRecordReportId(reportId).batchRecordDefinitionId(definitionId)
                .batchRecordVersionId(versionId).recordCategory(category).formSlotType(slot)
                .routeBindingSnapshotHash("route-binding-snapshot-" + id)
                .slotConfigSnapshotHash("slot-snapshot-" + id).build();
    }

    private static MesProRouteFlowProcessBatchRecordDO binding(Long id, String reportId,
                                                                String category, String slot) {
        return MesProRouteFlowProcessBatchRecordDO.builder().id(id).routeId(ROUTE_ID)
                .routeProcessId(ROUTE_PROCESS_ID).useType("BATCH").batchRecordReportId(reportId)
                .batchRecordDefinitionId(id + 100).batchRecordVersionId(id + 200)
                .recordCategory(category).formSlotType(slot).build();
    }

    private static MesProBatchRecordCellLinkRuleDO rule(Long id) {
        return new MesProBatchRecordCellLinkRuleDO().setId(id).setRuleVersion(1L)
                .setSourceType("FORMAL_SOURCE").setSourceFieldCode("value")
                .setTargetReportId("TARGET").setTargetRowIndex(0).setTargetColumnIndex(0)
                .setTargetCellKey("0:0").setTargetValueType("STRING").setEnabled(true);
    }

    private static MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult.SignatureEvidence batchSignature(
            String role, Long signatureId) {
        return new MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult.SignatureEvidence().setRole(role)
                .setSourceType("PRODUCTION").setSourceId(5301L).setSignatureId(signatureId)
                .setUserId(LEADER_USER_ID).setSignedAt(LocalDateTime.of(2026, 8, 9, 9, 5))
                .setEvidenceHash("signature-" + signatureId);
    }

    private static MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult.SignatureEvidence inspectionSignature(
            String role, Long signatureId) {
        return new MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult.SignatureEvidence().setRole(role)
                .setSourceType("PQC").setSourceId(6101L).setSignatureId(signatureId)
                .setUserId(LEADER_USER_ID).setSignedAt(LocalDateTime.of(2026, 8, 9, 9, 10))
                .setEvidenceHash("signature-" + signatureId);
    }

    private static MesTeamLeaderActiveOrderReleaseSignatureEvidence commonSignature(String role, Long signatureId) {
        return new MesTeamLeaderActiveOrderReleaseSignatureEvidence().setRole(role).setSourceType("LOSS")
                .setSourceId(8501L).setSignatureId(signatureId).setUserId(LEADER_USER_ID)
                .setSignedAt(LocalDateTime.of(2026, 8, 9, 9, 15)).setEvidenceHash("signature-" + signatureId);
    }

    private static MesProEdhrWorkTaskAssignmentRuleDO releaseRule() {
        return MesProEdhrWorkTaskAssignmentRuleDO.builder().id(9101L).scopeType("ROUTE")
                .scopeId(ROUTE_ID).taskType("RELEASE_APPROVE").candidateSourceType("USER")
                .candidateSourceId(9201L).enabled(true).build();
    }

    private static EdhrBatchExecutionRespVO batch() {
        return new EdhrBatchExecutionRespVO().setId(BATCH_EXECUTION_ID).setWorkOrderId(WORK_ORDER_ID)
                .setBatchCode("BATCH-V4-A2-001").setProductId(3101L).setRouteId(ROUTE_ID)
                .setRouteVersionId(ROUTE_VERSION_ID);
    }

    private static MesProEdhrReleaseRespVO precheck() {
        return new MesProEdhrReleaseRespVO().setReleaseTransactionId(8001L)
                .setBlockingCheckCount(0).setFailedCheckCount(0)
                .setLastPrecheckAt(LocalDateTime.of(2026, 8, 9, 10, 0));
    }

    private static MesProEdhrReleaseRespVO submitted() {
        return new MesProEdhrReleaseRespVO().setReleaseTransactionId(8001L)
                .setReleaseApprovalWorkTaskId(9001L)
                .setLastPrecheckAt(LocalDateTime.of(2026, 8, 9, 10, 1));
    }

    private static MesTeamLeaderActiveOrderReleaseBlocker blocker(String type) {
        return new MesTeamLeaderActiveOrderReleaseBlocker().setBlockerType(type)
                .setObjectType("TEST").setObjectId("1").setReason(type).setSuggestion("fix");
    }

    private static MesProcessPoolActiveOrderReleaseApplicationDO pendingApplication(
            String sourceHash, String businessKey) {
        return new MesProcessPoolActiveOrderReleaseApplicationDO().setActiveOrderId(ACTIVE_ORDER_ID)
                .setWorkOrderId(WORK_ORDER_ID).setWorkOrderCode("WO-V4-A2-001")
                .setRouteVersionId(ROUTE_VERSION_ID)
                .setBatchExecutionId(BATCH_EXECUTION_ID).setReleaseTransactionId(8001L)
                .setReleaseApprovalWorkTaskId(9001L).setApplicationStatus("PENDING_RELEASE_APPROVAL")
                .setSourceSnapshotHash(sourceHash).setRequestIdempotencyKey("release-request-1")
                .setBusinessIdempotencyKey(businessKey).setDossierSummaryJson(JSON.toJSONString(
                        new MesTeamLeaderActiveOrderReleaseDossierSummary().setSourceSnapshotHash(sourceHash)))
                .setAppliedAt(LocalDateTime.of(2026, 8, 9, 10, 0));
    }
}
