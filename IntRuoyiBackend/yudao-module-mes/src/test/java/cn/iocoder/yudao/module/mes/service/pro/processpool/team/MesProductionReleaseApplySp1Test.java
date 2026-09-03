package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditCommand;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditEventType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditRecorder;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRequiredCandidateResolver;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCandidates;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProductionReleaseApplySp1Test {

    private static final Long TENANT_ID = 1L;
    private static final Long LEADER_USER_ID = 1001L;
    private static final Long ACTIVE_ORDER_ID = 2001L;
    private static final Long WORK_ORDER_ID = 3001L;
    private static final Long ROUTE_ID = 4001L;
    private static final Long ROUTE_VERSION_ID = 4002L;
    private static final Long ROUTE_PROCESS_ID = 5001L;
    private static final Long PROCESS_ID = 6001L;
    private static final Long APPLICATION_ID = 7001L;
    private static final Long WORK_TASK_ID = 8001L;
    private static final Long PQC_ROLE_ID = 9001L;

    @Mock private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock private MesProWorkOrderMapper workOrderMapper;
    @Mock private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    @Mock private MesProcessPoolOrderProcessCompletionMapper completionMapper;
    @Mock private MesPqcInspectionTaskMapper pqcTaskMapper;
    @Mock private MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper;
    @Mock private MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    @Mock private MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock private MesProductionReleaseRequiredCandidateResolver candidateResolver;
    @Mock private MesReleaseFlowAuditRecorder auditRecorder;
    @Mock private MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper;

    private MesTeamLeaderActiveOrderReleaseGenerationService generationService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        MesTeamLeaderActiveOrderReleaseApplicationPersistenceService persistenceService =
                new MesTeamLeaderActiveOrderReleaseApplicationPersistenceService(
                        applicationMapper, workTaskMapper, auditRecorder);
        generationService = new MesTeamLeaderActiveOrderReleaseGenerationService(
                activeOrderMapper, workOrderMapper, processSnapshotMapper, completionMapper,
                pqcTaskMapper, aggregateDetailMapper, allocationMapper, applicationMapper, workTaskMapper,
                persistenceService, candidateResolver, new MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher(),
                pickListBindingMapper);

        lenient().when(activeOrderMapper.selectByIdForUpdate(ACTIVE_ORDER_ID)).thenReturn(activeOrder());
        lenient().when(pickListBindingMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO
                        .builder().id(8801L).activeOrderId(ACTIVE_ORDER_ID).workOrderId(WORK_ORDER_ID)
                        .pickListId(8901L).sourceSnapshotHash("pick-hash").build()));
        lenient().when(workOrderMapper.selectByIdForUpdate(WORK_ORDER_ID)).thenReturn(workOrder());
        lenient().when(processSnapshotMapper.selectListByActiveOrderIdForUpdate(ACTIVE_ORDER_ID))
                .thenReturn(List.of(snapshot()));
        lenient().when(completionMapper.selectListByWorkOrderIds(List.of(WORK_ORDER_ID)))
                .thenReturn(List.of(completion(BigDecimal.TEN, "production-aggregate")));
        lenient().when(pqcTaskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID))
                .thenReturn(List.of(pqcTask()));
        lenient().when(aggregateDetailMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID))
                .thenReturn(List.of(aggregateDetail()));
        lenient().when(allocationMapper.selectListByActiveOrderIds(List.of(ACTIVE_ORDER_ID)))
                .thenReturn(List.of(allocation(BigDecimal.TEN)));
        lenient().when(candidateResolver.resolveRequiredCandidates(
                        TENANT_ID, MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER))
                .thenReturn(candidates());
        lenient().doAnswer(invocation -> {
            MesProcessPoolActiveOrderReleaseApplicationDO application = invocation.getArgument(0);
            application.setId(APPLICATION_ID);
            return 1;
        }).when(applicationMapper).insert(any(MesProcessPoolActiveOrderReleaseApplicationDO.class));
        lenient().doAnswer(invocation -> {
            MesProEdhrWorkTaskDO task = invocation.getArgument(0);
            task.setId(WORK_TASK_ID);
            return 1;
        }).when(workTaskMapper).insert(any(MesProEdhrWorkTaskDO.class));
        lenient().when(applicationMapper.updateById(any(MesProcessPoolActiveOrderReleaseApplicationDO.class)))
                .thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void applyCreatesOnlyApplicationAndOnePqcTaskWithFrozenCandidatesAndAudit() {
        MesTeamLeaderActiveOrderReleaseApplicationResult result =
                generationService.generate(LEADER_USER_ID, command("release-request-1"));

        ArgumentCaptor<MesProcessPoolActiveOrderReleaseApplicationDO> applicationCaptor =
                ArgumentCaptor.forClass(MesProcessPoolActiveOrderReleaseApplicationDO.class);
        ArgumentCaptor<MesProEdhrWorkTaskDO> taskCaptor = ArgumentCaptor.forClass(MesProEdhrWorkTaskDO.class);
        ArgumentCaptor<MesReleaseFlowAuditCommand> auditCaptor =
                ArgumentCaptor.forClass(MesReleaseFlowAuditCommand.class);
        verify(applicationMapper).insert(applicationCaptor.capture());
        verify(workTaskMapper).insert(taskCaptor.capture());
        verify(auditRecorder).record(auditCaptor.capture());

        MesProcessPoolActiveOrderReleaseApplicationDO application = applicationCaptor.getValue();
        MesProEdhrWorkTaskDO task = taskCaptor.getValue();
        String expectedBusinessKey = DigestUtil.sha256Hex(
                "PQC_RELEASE|1|2001|3001|BATCH-001|4001|4002");
        assertAll(
                () -> assertEquals(APPLICATION_ID, result.getApplicationId()),
                () -> assertEquals(WORK_TASK_ID, result.getPqcReleaseWorkTaskId()),
                () -> assertEquals("BATCH-001", result.getBatchCode()),
                () -> assertEquals(ROUTE_ID, result.getRouteId()),
                () -> assertEquals(ROUTE_VERSION_ID, result.getRouteVersionId()),
                () -> assertEquals(MesReleaseFlowStatus.PQC_RELEASE_PENDING, result.getStatus()),
                () -> assertEquals(1, result.getVersion()),
                () -> assertNotNull(result.getSourceSnapshotHash()),
                () -> assertEquals(expectedBusinessKey, application.getBusinessIdempotencyKey()),
                () -> assertNull(application.getBatchExecutionId()),
                () -> assertNull(application.getReleaseTransactionId()),
                () -> assertNull(application.getReleaseApprovalWorkTaskId()),
                () -> assertEquals(MesReleaseFlowStatus.PQC_RELEASE_PENDING, application.getApplicationStatus()),
                () -> assertEquals("PQC_PRODUCTION_RELEASE", task.getTaskType()),
                () -> assertEquals("RELEASE_APPLICATION", task.getBusinessScopeType()),
                () -> assertEquals(APPLICATION_ID, task.getBusinessScopeId()),
                () -> assertNull(task.getBatchExecutionId()),
                () -> assertNull(task.getBatchTaskId()),
                () -> assertNull(task.getRouteProcessId()),
                () -> assertEquals("ROLE", task.getCandidateSourceType()),
                () -> assertEquals(PQC_ROLE_ID, task.getCandidateSourceId()),
                () -> assertEquals("7101,7102", task.getCandidateUserSnapshot()),
                () -> assertEquals(MesProEdhrWorkTaskStatus.TODO, task.getStatus()),
                () -> assertEquals(MesReleaseFlowAuditEventType.PQC_PRODUCTION_RELEASE_APPLIED,
                        auditCaptor.getValue().getEventType()),
                () -> assertEquals(APPLICATION_ID, auditCaptor.getValue().getApplicationId()),
                () -> assertEquals(WORK_TASK_ID, auditCaptor.getValue().getWorkTaskId()));
    }

    @Test
    void productionBelowOneHundredPercentBlocksBeforeAnyWrite() {
        when(completionMapper.selectListByWorkOrderIds(List.of(WORK_ORDER_ID)))
                .thenReturn(List.of(completion(BigDecimal.valueOf(9), "production-aggregate")));

        MesReleaseFlowBlockerException failure = assertThrows(MesReleaseFlowBlockerException.class,
                () -> generationService.generate(LEADER_USER_ID, command("release-request-progress")));

        assertBlocker(failure, MesReleaseFlowBlockerType.PRODUCTION_PROGRESS_NOT_COMPLETED);
        verify(applicationMapper, never()).insert(any(MesProcessPoolActiveOrderReleaseApplicationDO.class));
        verify(workTaskMapper, never()).insert(any(MesProEdhrWorkTaskDO.class));
        verify(auditRecorder, never()).record(any());
    }

    @Test
    void processWithoutPqcTaskDoesNotBlockRelease() {
        MesProcessPoolActiveOrderProcessSnapshotDO unconfigured = snapshot(4102L, 5002L, 6002L);
        when(processSnapshotMapper.selectListByActiveOrderIdForUpdate(ACTIVE_ORDER_ID))
                .thenReturn(List.of(snapshot(), unconfigured));
        when(completionMapper.selectListByWorkOrderIds(List.of(WORK_ORDER_ID)))
                .thenReturn(List.of(completion(BigDecimal.TEN, "production-aggregate"),
                        completion(5002L, 6002L, BigDecimal.TEN, "production-aggregate-2")));
        when(allocationMapper.selectListByActiveOrderIds(List.of(ACTIVE_ORDER_ID)))
                .thenReturn(List.of(allocation(BigDecimal.TEN), allocation(5502L, 5002L, 6002L,
                        BigDecimal.TEN)));

        MesTeamLeaderActiveOrderReleaseApplicationResult result =
                generationService.generate(LEADER_USER_ID, command("release-request-no-pqc-process"));

        assertEquals(APPLICATION_ID, result.getApplicationId());
        verify(applicationMapper).insert(any(MesProcessPoolActiveOrderReleaseApplicationDO.class));
    }

    @Test
    void quantityConflictBlocksReleaseBeforeAnyWrite() {
        when(allocationMapper.selectListByActiveOrderIds(List.of(ACTIVE_ORDER_ID)))
                .thenReturn(List.of(allocation(BigDecimal.valueOf(15))));

        MesReleaseFlowBlockerException failure = assertThrows(MesReleaseFlowBlockerException.class,
                () -> generationService.generate(LEADER_USER_ID, command("release-request-quantity-conflict")));

        assertBlocker(failure, MesReleaseFlowBlockerType.PRODUCTION_QUANTITY_CONFLICT);
        verify(applicationMapper, never()).insert(any(MesProcessPoolActiveOrderReleaseApplicationDO.class));
        verify(workTaskMapper, never()).insert(any(MesProEdhrWorkTaskDO.class));
        verify(auditRecorder, never()).record(any());
    }

    @Test
    void nonOwnerLeaderBlocksBeforeAnyWrite() {
        MesReleaseFlowBlockerException failure = assertThrows(MesReleaseFlowBlockerException.class,
                () -> generationService.generate(9999L, command("release-request-forbidden")));

        assertBlocker(failure, MesReleaseFlowBlockerType.ACTIVE_ORDER_LEADER_FORBIDDEN);
        verify(applicationMapper, never()).insert(any(MesProcessPoolActiveOrderReleaseApplicationDO.class));
        verify(workTaskMapper, never()).insert(any(MesProEdhrWorkTaskDO.class));
    }

    @Test
    void candidateResolutionFailureStopsBeforeApplicationWrite() {
        when(candidateResolver.resolveRequiredCandidates(TENANT_ID, MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER))
                .thenThrow(new IllegalStateException("PQC role candidates unavailable"));

        assertThrows(IllegalStateException.class,
                () -> generationService.generate(LEADER_USER_ID, command("release-request-candidate-failure")));

        verify(applicationMapper, never()).insert(any(MesProcessPoolActiveOrderReleaseApplicationDO.class));
        verify(workTaskMapper, never()).insert(any(MesProEdhrWorkTaskDO.class));
        verify(auditRecorder, never()).record(any());
    }

    @Test
    void workTaskWriteFailureStopsBindingAndAuditInTheGenerationTransaction() {
        when(workTaskMapper.insert(any(MesProEdhrWorkTaskDO.class)))
                .thenThrow(new IllegalStateException("work task insert failed"));

        assertThrows(IllegalStateException.class,
                () -> generationService.generate(LEADER_USER_ID, command("release-request-task-failure")));

        verify(applicationMapper).insert(any(MesProcessPoolActiveOrderReleaseApplicationDO.class));
        verify(applicationMapper, never()).updateById(any(MesProcessPoolActiveOrderReleaseApplicationDO.class));
        verify(auditRecorder, never()).record(any());
    }

    @Test
    void sameRequestWithChangedAuthoritativeSnapshotReturnsPayloadConflict() {
        MesProcessPoolActiveOrderReleaseApplicationDO existing = existingApplication()
                .setRequestIdempotencyKey("release-request-conflict")
                .setSourceSnapshotHash("older-authoritative-snapshot");
        when(applicationMapper.selectByRequestIdempotencyKey(ACTIVE_ORDER_ID, "release-request-conflict"))
                .thenReturn(existing);

        MesReleaseFlowBlockerException failure = assertThrows(MesReleaseFlowBlockerException.class,
                () -> generationService.generate(LEADER_USER_ID, command("release-request-conflict")));

        assertBlocker(failure, MesReleaseFlowBlockerType.IDEMPOTENCY_PAYLOAD_CONFLICT);
        verify(applicationMapper, never()).insert(any(MesProcessPoolActiveOrderReleaseApplicationDO.class));
        verify(workTaskMapper, never()).insert(any(MesProEdhrWorkTaskDO.class));
    }

    @Test
    void sameBusinessIdentityWithDifferentRequestReturnsOriginalReceipt() {
        MesProcessPoolActiveOrderReleaseApplicationDO existing = existingApplication()
                .setRequestIdempotencyKey("first-request");
        when(applicationMapper.selectByBusinessIdempotencyKey(
                ACTIVE_ORDER_ID, existing.getBusinessIdempotencyKey())).thenReturn(existing);

        MesTeamLeaderActiveOrderReleaseApplicationResult result =
                generationService.generate(LEADER_USER_ID, command("second-request"));

        assertEquals(APPLICATION_ID, result.getApplicationId());
        assertEquals(WORK_TASK_ID, result.getPqcReleaseWorkTaskId());
        verify(applicationMapper, never()).insert(any(MesProcessPoolActiveOrderReleaseApplicationDO.class));
        verify(workTaskMapper, never()).insert(any(MesProEdhrWorkTaskDO.class));
        verify(auditRecorder, never()).record(any());
    }

    @Test
    void receiptAllowsFrozenPqcCandidateAndRejectsUnrelatedUser() {
        when(applicationMapper.selectLatestByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(existingApplication());
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder());
        when(workTaskMapper.selectById(WORK_TASK_ID)).thenReturn(new MesProEdhrWorkTaskDO()
                .setId(WORK_TASK_ID)
                .setBusinessScopeType("RELEASE_APPLICATION")
                .setBusinessScopeId(APPLICATION_ID)
                .setTaskType("PQC_PRODUCTION_RELEASE")
                .setCandidateUserSnapshot("7101,7102"));

        MesTeamLeaderActiveOrderReleaseApplicationResult receipt =
                generationService.get(7101L, ACTIVE_ORDER_ID);
        MesReleaseFlowBlockerException failure = assertThrows(MesReleaseFlowBlockerException.class,
                () -> generationService.get(7999L, ACTIVE_ORDER_ID));

        assertEquals(APPLICATION_ID, receipt.getApplicationId());
        assertBlocker(failure, MesReleaseFlowBlockerType.ACTIVE_ORDER_LEADER_FORBIDDEN);
    }

    @Test
    void persistenceMustJoinTheGenerationTransaction() throws Exception {
        Transactional generation = MesTeamLeaderActiveOrderReleaseGenerationService.class
                .getMethod("generate", Long.class, MesTeamLeaderActiveOrderReleaseApplyCommand.class)
                .getAnnotation(Transactional.class);
        Transactional persistence = MesTeamLeaderActiveOrderReleaseApplicationPersistenceService.class
                .getMethod("persistPending", MesProcessPoolActiveOrderReleaseApplicationDO.class,
                        MesProductionReleaseRoleCandidates.class)
                .getAnnotation(Transactional.class);

        assertAll(
                () -> assertNotNull(generation),
                () -> assertNotNull(persistence),
                () -> assertTrue(List.of(generation.rollbackFor()).contains(Exception.class)),
                () -> assertEquals(Propagation.MANDATORY, persistence.propagation()));
    }

    private static void assertBlocker(MesReleaseFlowBlockerException failure,
                                      MesReleaseFlowBlockerType expectedType) {
        assertNotNull(failure.getFailure());
        assertFalse(failure.getFailure().getBlockers().isEmpty());
        assertEquals(expectedType, failure.getFailure().getBlockers().get(0).getBlockerType());
    }

    private static MesTeamLeaderActiveOrderReleaseApplyCommand command(String requestKey) {
        return new MesTeamLeaderActiveOrderReleaseApplyCommand()
                .setActiveOrderId(ACTIVE_ORDER_ID)
                .setIdempotencyKey(requestKey)
                .setApplyRemark("生产与检验均已完成");
    }

    private static MesProcessPoolActiveOrderDO activeOrder() {
        return MesProcessPoolActiveOrderDO.builder()
                .id(ACTIVE_ORDER_ID)
                .leaderUserId(LEADER_USER_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeVersionId(ROUTE_VERSION_ID)
                .activeStatus("ACTIVE")
                .businessStatus("PRODUCING")
                .erpFixedQuantitySnapshot(BigDecimal.TEN)
                .build();
    }

    private static MesProWorkOrderDO workOrder() {
        return MesProWorkOrderDO.builder()
                .id(WORK_ORDER_ID)
                .code("WO-001")
                .batchCode("BATCH-001")
                .productId(3101L)
                .quantity(BigDecimal.TEN)
                .build();
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO snapshot() {
        return snapshot(4101L, ROUTE_PROCESS_ID, PROCESS_ID);
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO snapshot(Long id, Long routeProcessId,
                                                                         Long processId) {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .id(id)
                .activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeVersionId(ROUTE_VERSION_ID)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .plannedQuantitySnapshot(BigDecimal.TEN)
                .build();
    }

    private static MesProcessPoolOrderProcessCompletionDO completion(BigDecimal confirmedQuantity,
                                                                       String aggregateHash) {
        return completion(ROUTE_PROCESS_ID, PROCESS_ID, 5101L, 5501L, confirmedQuantity, aggregateHash);
    }

    private static MesProcessPoolOrderProcessCompletionDO completion(Long routeProcessId, Long processId,
                                                                       BigDecimal confirmedQuantity,
                                                                       String aggregateHash) {
        return completion(routeProcessId, processId, 5102L, 5502L, confirmedQuantity, aggregateHash);
    }

    private static MesProcessPoolOrderProcessCompletionDO completion(Long routeProcessId, Long processId,
                                                                       Long id, Long allocationId,
                                                                       BigDecimal confirmedQuantity,
                                                                       String aggregateHash) {
        return MesProcessPoolOrderProcessCompletionDO.builder()
                .id(id)
                .workOrderId(WORK_ORDER_ID)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .targetQuantity(BigDecimal.TEN)
                .confirmedQuantity(confirmedQuantity)
                .completionStatus(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED)
                .completedAt(LocalDateTime.of(2026, 8, 14, 10, 0))
                .backfillStatus(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS)
                .backfillExecutionId(5201L)
                .lastEventId(5301L)
                .lastReviewId(5401L)
                .sourceEventIdsJson("[5301]")
                .sourceAllocationIdsJson("[" + allocationId + "]")
                .aggregateHash(aggregateHash)
                .backfillIdempotencyKey("production-backfill-key")
                .build();
    }

    private static MesProcessPoolReportAllocationDO allocation(BigDecimal allocatedQuantity) {
        return allocation(5501L, ROUTE_PROCESS_ID, PROCESS_ID, allocatedQuantity);
    }

    private static MesProcessPoolReportAllocationDO allocation(Long id, Long routeProcessId, Long processId,
                                                                BigDecimal allocatedQuantity) {
        return MesProcessPoolReportAllocationDO.builder()
                .id(id)
                .activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .allocatedQuantity(allocatedQuantity)
                .lifecycleStatus(MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT)
                .build();
    }

    private static MesPqcInspectionTaskDO pqcTask() {
        return MesPqcInspectionTaskDO.builder()
                .id(6101L)
                .activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeVersionId(ROUTE_VERSION_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED)
                .build();
    }

    private static MesPqcProcessInspectionAggregateDetailDO aggregateDetail() {
        return MesPqcProcessInspectionAggregateDetailDO.builder()
                .id(6201L)
                .pqcTaskId(6101L)
                .activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeVersionId(ROUTE_VERSION_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .build();
    }

    private static MesProductionReleaseRoleCandidates candidates() {
        return new MesProductionReleaseRoleCandidates(PQC_ROLE_ID,
                MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER, List.of(7101L, 7102L), "candidate-hash");
    }

    private static MesProcessPoolActiveOrderReleaseApplicationDO existingApplication() {
        return new MesProcessPoolActiveOrderReleaseApplicationDO()
                .setId(APPLICATION_ID)
                .setActiveOrderId(ACTIVE_ORDER_ID)
                .setWorkOrderId(WORK_ORDER_ID)
                .setWorkOrderCode("WO-001")
                .setBatchCode("BATCH-001")
                .setProductId(3101L)
                .setRouteId(ROUTE_ID)
                .setRouteVersionId(ROUTE_VERSION_ID)
                .setPqcReleaseWorkTaskId(WORK_TASK_ID)
                .setApplicationStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                .setSourceSnapshotHash("source-snapshot")
                .setVersion(1)
                .setRequestIdempotencyKey("release-request-1")
                .setBusinessIdempotencyKey(DigestUtil.sha256Hex(
                        "PQC_RELEASE|1|2001|3001|BATCH-001|4001|4002"))
                .setAppliedBy(LEADER_USER_ID)
                .setAppliedAt(LocalDateTime.of(2026, 8, 14, 12, 0));
    }
}
