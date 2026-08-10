package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationAdjustmentAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationStateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationAdjustmentAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationStateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesReportAllocationCommandServiceTest {

    @Mock private MesTeamLeaderScopeService scopeService;
    @Mock private MesProProcessPoolEventMapper eventMapper;
    @Mock private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock private MesProWorkOrderMapper workOrderMapper;
    @Mock private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock private MesProcessPoolReportAllocationStateMapper stateMapper;
    @Mock private MesProcessPoolReportAllocationAdjustmentAuditMapper auditMapper;
    @Mock private MesProcessPoolSubmissionReviewMapper reviewMapper;
    @Mock private MesReportAllocationPoolQuantityService poolQuantityService;
    @Mock private MesReportAllocationReleaseStateService releaseStateService;
    @Mock private MesTeamLeaderOrderProcessTargetService targetService;
    @Mock private MesTeamLeaderFifoAllocationService fifoService;
    @Mock private MesRouteStartProductionLeaderAuthorizationService routeStartAuthorizationService;
    @Mock private MesReportAllocationQuantityFragmentService quantityFragmentService;
    @Mock private MesTeamLeaderOrderProcessCompletionService completionService;

    private MesReportAllocationCommandService service;

    @BeforeEach
    void setUp() {
        service = new MesReportAllocationCommandService(scopeService, eventMapper, activeOrderMapper, workOrderMapper,
                allocationMapper, stateMapper, auditMapper, reviewMapper, poolQuantityService, releaseStateService,
                targetService, fifoService, routeStartAuthorizationService, quantityFragmentService,
                completionService);
        when(routeStartAuthorizationService.listAuthorizedRouteProcesses(3001L)).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(5001L).processId(6001L).build()));
    }

    @Test
    void productionLeaderScopeMustFollowAuthorizedProcessInsteadOfReportingEmployee() {
        MesProProcessPoolEventDO event = event();
        when(eventMapper.selectById(1001L)).thenReturn(event);
        when(poolQuantityService.requirePoolQuantity(event)).thenReturn(new BigDecimal("411111"));
        when(allocationMapper.selectListByEventId(1001L)).thenReturn(List.of());
        when(releaseStateService.findReleasedActiveOrderIds(List.of())).thenReturn(Set.of());

        MesReportAllocationSnapshot snapshot = service.getCurrent(1001L, 3001L, "PRODUCTION");

        assertAmount("411111", snapshot.getPoolQuantity());
        verify(scopeService, never()).assertCanAccessEmployee(any(), any(), any());
    }

    @Test
    void fifoPreviewMustKeepUnallocatedQuantityInsteadOfFailing() {
        MesProProcessPoolEventDO event = event();
        when(eventMapper.selectById(1001L)).thenReturn(event);
        when(poolQuantityService.requirePoolQuantity(event)).thenReturn(new BigDecimal("300"));
        when(allocationMapper.selectListByEventId(1001L)).thenReturn(List.of());
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(List.of(activeOrder(8101L, 9001L)));
        when(releaseStateService.findReleasedActiveOrderIds(anyCollection())).thenReturn(Set.of());
        when(fifoService.previewFifoAllocation(org.mockito.ArgumentMatchers.any())).thenReturn(
                MesTeamLeaderReportAllocationPreview.builder().poolQuantity(new BigDecimal("300"))
                        .totalAllocatedQuantity(new BigDecimal("230"))
                        .unallocatedQuantity(new BigDecimal("70"))
                        .lines(List.of(MesTeamLeaderReportAllocationPreviewLine.builder()
                                .activeOrderId(8101L).workOrderId(9001L).workOrderCode("A")
                                .routeProcessId(5101L).processId(6001L)
                                .allocatedQuantity(new BigDecimal("230")).build())).build());

        MesReportAllocationSnapshot snapshot = service.previewFifo(1001L, 3001L, "PRODUCTION");

        assertAmount("230", snapshot.getTotalAllocatedQuantity());
        assertAmount("70", snapshot.getUnallocatedQuantity());
    }

    @Test
    void shouldReplaceUnreleasedAWithCAndCreateNewVersion() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationDO oldA = allocation(7101L, 8101L, 9001L, 5101L, "100");
        MesProcessPoolReportAllocationStateDO state = MesProcessPoolReportAllocationStateDO.builder()
                .id(7201L).eventId(1001L).currentVersion(1).build();
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event);
        when(poolQuantityService.requirePoolQuantity(event)).thenReturn(new BigDecimal("300"));
        when(stateMapper.selectByEventIdForUpdate(1001L)).thenReturn(state);
        when(allocationMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of(oldA));
        when(activeOrderMapper.selectActiveListByLeaderForUpdate(3001L)).thenReturn(List.of(
                activeOrder(8101L, 9001L), activeOrder(8103L, 9003L)));
        when(releaseStateService.findReleasedActiveOrderIdsForUpdate(anyCollection())).thenReturn(Set.of());
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L, 9003L))).thenReturn(List.of(
                workOrder(9001L, "A"), workOrder(9003L, "C")));
        when(allocationMapper.selectListByActiveOrderIdsAndProcessForUpdate(Set.of(8103L), 6001L))
                .thenReturn(List.of());
        when(targetService.requireUniqueTargetForProcess(activeOrder(8103L, 9003L), 6001L))
                .thenReturn(new MesTeamLeaderOrderProcessTarget(5301L, 6001L, new BigDecimal("300"),
                        BigDecimal.ONE, new BigDecimal("300")));
        when(reviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(
                MesProcessPoolSubmissionReviewDO.builder().id(7301L).eventId(1001L).build());
        when(allocationMapper.supersedeCurrentRows(List.of(7101L), 2)).thenReturn(1);
        when(allocationMapper.insertBatch(anyCollection())).thenReturn(true);
        when(auditMapper.insertBatch(anyCollection())).thenReturn(true);
        when(stateMapper.updateById(state)).thenReturn(1);
        when(releaseStateService.findReleasedActiveOrderIds(List.of(8103L))).thenReturn(Set.of());
        when(workOrderMapper.selectListByIds(List.of(9003L))).thenReturn(List.of(workOrder(9003L, "C")));

        MesReportAllocationSnapshot snapshot = service.save(saveCommand(1,
                List.of(MesReportAllocationSaveLine.builder().activeOrderId(8103L)
                        .allocatedQuantity(new BigDecimal("100")).build())));

        assertEquals(2, snapshot.getVersion());
        assertEquals(List.of(8103L), snapshot.getLines().stream()
                .map(MesReportAllocationSnapshotLine::getActiveOrderId).toList());
        verify(stateMapper).updateById(state);
        ArgumentCaptor<Collection<MesProcessPoolReportAllocationDO>> fragmentAllocations =
                ArgumentCaptor.forClass(Collection.class);
        verify(quantityFragmentService).rebuildForVersion(org.mockito.ArgumentMatchers.eq(event),
                org.mockito.ArgumentMatchers.eq(2), fragmentAllocations.capture());
        assertEquals(List.of(8103L), fragmentAllocations.getValue().stream()
                .map(MesProcessPoolReportAllocationDO::getActiveOrderId).toList());
        ArgumentCaptor<Collection<MesProcessPoolReportAllocationDO>> affectedAllocations =
                ArgumentCaptor.forClass(Collection.class);
        verify(completionService).reconcileAffectedAllocations(org.mockito.ArgumentMatchers.eq(event),
                affectedAllocations.capture());
        assertEquals(Set.of(8101L, 8103L), affectedAllocations.getValue().stream()
                .map(MesProcessPoolReportAllocationDO::getActiveOrderId).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void shouldPersistProductionLeaderTypeWhenAllocationCreatesReview() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationStateDO state = MesProcessPoolReportAllocationStateDO.builder()
                .id(7201L).eventId(1001L).currentVersion(0).build();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(8101L, 9001L);
        MesProWorkOrderDO workOrder = workOrder(9001L, "A");
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event);
        when(poolQuantityService.requirePoolQuantity(event)).thenReturn(new BigDecimal("300"));
        when(stateMapper.selectByEventIdForUpdate(1001L)).thenReturn(state);
        when(allocationMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of());
        when(activeOrderMapper.selectActiveListByLeaderForUpdate(3001L)).thenReturn(List.of(activeOrder));
        when(releaseStateService.findReleasedActiveOrderIdsForUpdate(anyCollection())).thenReturn(Set.of());
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder));
        when(allocationMapper.selectListByActiveOrderIdsAndProcessForUpdate(Set.of(8101L), 6001L))
                .thenReturn(List.of());
        when(targetService.requireUniqueTargetForProcess(activeOrder, 6001L))
                .thenReturn(new MesTeamLeaderOrderProcessTarget(5101L, 6001L, new BigDecimal("300"),
                        BigDecimal.ZERO, new BigDecimal("300")));
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenReturn(1);
        when(allocationMapper.insertBatch(anyCollection())).thenReturn(true);
        when(auditMapper.insertBatch(anyCollection())).thenReturn(true);
        when(stateMapper.updateById(state)).thenReturn(1);
        when(releaseStateService.findReleasedActiveOrderIds(List.of(8101L))).thenReturn(Set.of());
        when(workOrderMapper.selectListByIds(List.of(9001L))).thenReturn(List.of(workOrder));

        service.save(saveCommand(0, List.of(MesReportAllocationSaveLine.builder()
                .activeOrderId(8101L).allocatedQuantity(new BigDecimal("100")).build())));

        ArgumentCaptor<MesProcessPoolSubmissionReviewDO> reviewCaptor =
                ArgumentCaptor.forClass(MesProcessPoolSubmissionReviewDO.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        assertEquals("PRODUCTION", reviewCaptor.getValue().getLeaderType());
    }

    @Test
    void shouldCapRequestedAllocationToOrderProcessRemainingAndKeepPoolResidual() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationStateDO state = MesProcessPoolReportAllocationStateDO.builder()
                .id(7201L).eventId(1001L).currentVersion(0).build();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(8101L, 9001L);
        MesProWorkOrderDO workOrder = workOrder(9001L, "A");
        MesProcessPoolReportAllocationDO allocatedElsewhere = MesProcessPoolReportAllocationDO.builder()
                .id(7001L).eventId(999L).activeOrderId(8101L).workOrderId(9001L)
                .routeProcessId(5101L).processId(6001L).allocatedQuantity(new BigDecimal("80"))
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_MANUAL)
                .lifecycleStatus(MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT).createdVersion(1).build();
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event);
        when(poolQuantityService.requirePoolQuantity(event)).thenReturn(new BigDecimal("80"));
        when(stateMapper.selectByEventIdForUpdate(1001L)).thenReturn(state);
        when(allocationMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of());
        when(activeOrderMapper.selectActiveListByLeaderForUpdate(3001L)).thenReturn(List.of(activeOrder));
        when(releaseStateService.findReleasedActiveOrderIdsForUpdate(anyCollection())).thenReturn(Set.of());
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder));
        when(allocationMapper.selectListByActiveOrderIdsAndProcessForUpdate(Set.of(8101L), 6001L))
                .thenReturn(List.of(allocatedElsewhere));
        when(targetService.requireUniqueTargetForProcess(activeOrder, 6001L))
                .thenReturn(new MesTeamLeaderOrderProcessTarget(5101L, 6001L, new BigDecimal("100"),
                        BigDecimal.ONE, new BigDecimal("100")));
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenReturn(1);
        when(allocationMapper.insertBatch(anyCollection())).thenReturn(true);
        when(auditMapper.insertBatch(anyCollection())).thenReturn(true);
        when(stateMapper.updateById(state)).thenReturn(1);
        when(releaseStateService.findReleasedActiveOrderIds(List.of(8101L))).thenReturn(Set.of());
        when(workOrderMapper.selectListByIds(List.of(9001L))).thenReturn(List.of(workOrder));

        MesReportAllocationSnapshot snapshot = service.save(saveCommand(0, List.of(
                MesReportAllocationSaveLine.builder().activeOrderId(8101L)
                        .allocatedQuantity(new BigDecimal("80")).build())));

        assertAmount("20", snapshot.getTotalAllocatedQuantity());
        assertAmount("60", snapshot.getUnallocatedQuantity());
        ArgumentCaptor<Collection<MesProcessPoolReportAllocationDO>> insertedCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(allocationMapper).insertBatch(insertedCaptor.capture());
        assertAmount("20", insertedCaptor.getValue().iterator().next().getAllocatedQuantity());
        verify(quantityFragmentService).rebuildForVersion(event, 1, List.copyOf(insertedCaptor.getValue()));
        verify(completionService).reconcileAffectedAllocations(event, List.copyOf(insertedCaptor.getValue()));
    }

    @Test
    void shouldUseAllocationModeAsAuditReasonWhenReviewRemarkIsBlank() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationStateDO state = MesProcessPoolReportAllocationStateDO.builder()
                .id(7201L).eventId(1001L).currentVersion(0).build();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(8101L, 9001L);
        MesProWorkOrderDO workOrder = workOrder(9001L, "A");
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event);
        when(poolQuantityService.requirePoolQuantity(event)).thenReturn(new BigDecimal("300"));
        when(stateMapper.selectByEventIdForUpdate(1001L)).thenReturn(state);
        when(allocationMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of());
        when(activeOrderMapper.selectActiveListByLeaderForUpdate(3001L)).thenReturn(List.of(activeOrder));
        when(releaseStateService.findReleasedActiveOrderIdsForUpdate(anyCollection())).thenReturn(Set.of());
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder));
        when(allocationMapper.selectListByActiveOrderIdsAndProcessForUpdate(Set.of(8101L), 6001L))
                .thenReturn(List.of());
        when(targetService.requireUniqueTargetForProcess(activeOrder, 6001L))
                .thenReturn(new MesTeamLeaderOrderProcessTarget(5101L, 6001L, new BigDecimal("300"),
                        BigDecimal.ZERO, new BigDecimal("300")));
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenReturn(1);
        when(allocationMapper.insertBatch(anyCollection())).thenReturn(true);
        when(auditMapper.insertBatch(anyCollection())).thenReturn(true);
        when(stateMapper.updateById(state)).thenReturn(1);
        when(releaseStateService.findReleasedActiveOrderIds(List.of(8101L))).thenReturn(Set.of());
        when(workOrderMapper.selectListByIds(List.of(9001L))).thenReturn(List.of(workOrder));

        MesReportAllocationSaveCommand command = MesReportAllocationSaveCommand.builder()
                .eventId(1001L).leaderUserId(3001L).leaderType("PRODUCTION").expectedVersion(0)
                .idempotencyKey("req-no-reason").allocationMode(MesProcessPoolReportAllocationDO.MODE_FIFO)
                .reason(null).allocations(List.of(MesReportAllocationSaveLine.builder()
                        .activeOrderId(8101L).allocatedQuantity(new BigDecimal("100")).build())).build();
        service.save(command);

        ArgumentCaptor<Collection<MesProcessPoolReportAllocationAdjustmentAuditDO>> auditCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(auditMapper).insertBatch(auditCaptor.capture());
        assertEquals("FIFO自动分配", auditCaptor.getValue().iterator().next().getAdjustmentReason());
    }

    @Test
    void shouldAllowEmptyEditableAllocationAndPreserveEntirePoolAsRemainder() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationDO oldA = allocation(7101L, 8101L, 9001L, 5101L, "100");
        MesProcessPoolReportAllocationStateDO state = MesProcessPoolReportAllocationStateDO.builder()
                .id(7201L).eventId(1001L).currentVersion(1).build();
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event);
        when(poolQuantityService.requirePoolQuantity(event)).thenReturn(new BigDecimal("300"));
        when(stateMapper.selectByEventIdForUpdate(1001L)).thenReturn(state);
        when(allocationMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of(oldA));
        when(activeOrderMapper.selectActiveListByLeaderForUpdate(3001L)).thenReturn(List.of(activeOrder(8101L, 9001L)));
        when(releaseStateService.findReleasedActiveOrderIdsForUpdate(anyCollection())).thenReturn(Set.of());
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder(9001L, "A")));
        when(reviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(
                MesProcessPoolSubmissionReviewDO.builder().id(7301L).eventId(1001L).build());
        when(allocationMapper.supersedeCurrentRows(List.of(7101L), 2)).thenReturn(1);
        when(auditMapper.insertBatch(anyCollection())).thenReturn(true);
        when(stateMapper.updateById(state)).thenReturn(1);

        MesReportAllocationSnapshot snapshot = service.save(saveCommand(1, List.of()));

        assertAmount("0", snapshot.getTotalAllocatedQuantity());
        assertAmount("300", snapshot.getUnallocatedQuantity());
        verify(quantityFragmentService).rebuildForVersion(event, 2, List.of());
        verify(completionService).reconcileAffectedAllocations(event, List.of(oldA));
    }

    @Test
    void shouldTreatNullAndZeroAllocationQuantitiesAsZeroWhenSaving() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationDO oldA = allocation(7101L, 8101L, 9001L, 5101L, "100");
        MesProcessPoolReportAllocationStateDO state = MesProcessPoolReportAllocationStateDO.builder()
                .id(7201L).eventId(1001L).currentVersion(1).build();
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event);
        when(poolQuantityService.requirePoolQuantity(event)).thenReturn(new BigDecimal("300"));
        when(stateMapper.selectByEventIdForUpdate(1001L)).thenReturn(state);
        when(allocationMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of(oldA));
        when(activeOrderMapper.selectActiveListByLeaderForUpdate(3001L)).thenReturn(List.of(activeOrder(8101L, 9001L)));
        when(releaseStateService.findReleasedActiveOrderIdsForUpdate(anyCollection())).thenReturn(Set.of());
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder(9001L, "A")));
        when(reviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(
                MesProcessPoolSubmissionReviewDO.builder().id(7301L).eventId(1001L).build());
        when(allocationMapper.supersedeCurrentRows(List.of(7101L), 2)).thenReturn(1);
        when(auditMapper.insertBatch(anyCollection())).thenReturn(true);
        when(stateMapper.updateById(state)).thenReturn(1);

        MesReportAllocationSnapshot snapshot = service.save(saveCommand(1, List.of(
                MesReportAllocationSaveLine.builder().activeOrderId(8101L).allocatedQuantity(null).build(),
                MesReportAllocationSaveLine.builder().activeOrderId(8101L).allocatedQuantity(BigDecimal.ZERO).build())));

        assertAmount("0", snapshot.getTotalAllocatedQuantity());
        assertAmount("300", snapshot.getUnallocatedQuantity());
        verify(quantityFragmentService).rebuildForVersion(event, 2, List.of());
        verify(completionService).reconcileAffectedAllocations(event, List.of(oldA));
    }

    @Test
    void manualFullAllocationSameAsCurrentMustStillReconcileCompletionProgress() {
        assertFullAllocationSameAsCurrentReconcilesCompletionProgress(MesProcessPoolReportAllocationDO.MODE_MANUAL);
    }

    @Test
    void fifoFullAllocationSameAsCurrentMustStillReconcileCompletionProgress() {
        assertFullAllocationSameAsCurrentReconcilesCompletionProgress(MesProcessPoolReportAllocationDO.MODE_FIFO);
    }

    @Test
    void identicalIdempotencyRetryMustReturnCurrentVersionWithoutWritingAgain() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationStateDO state = MesProcessPoolReportAllocationStateDO.builder()
                .id(7201L).eventId(1001L).currentVersion(2).lastIdempotencyKey("req-1")
                .lastRequestHash("7efa3d3b5dd97ec6d1edf6c65d5086c6e1a8b9cb89f2457d1a590cdc7ed78ef6").build();
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event);
        when(poolQuantityService.requirePoolQuantity(event)).thenReturn(new BigDecimal("300"));
        when(stateMapper.selectByEventIdForUpdate(1001L)).thenReturn(state);
        when(allocationMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of());
        when(releaseStateService.findReleasedActiveOrderIds(List.of())).thenReturn(Set.of());

        MesReportAllocationSnapshot snapshot = service.save(saveCommand(1, List.of()));

        assertEquals(2, snapshot.getVersion());
        verify(allocationMapper, never()).supersedeCurrentRows(anyCollection(), any());
        verify(quantityFragmentService, never()).rebuildForVersion(any(), any(), anyCollection());
        verify(completionService, never()).reconcileAffectedAllocations(any(), anyCollection());
    }

    @Test
    void releasedAllocationMustRejectAnyAttemptToChangeThatOrder() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationDO oldA = allocation(7101L, 8101L, 9001L, 5101L, "100");
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event);
        when(poolQuantityService.requirePoolQuantity(event)).thenReturn(new BigDecimal("300"));
        when(stateMapper.selectByEventIdForUpdate(1001L)).thenReturn(MesProcessPoolReportAllocationStateDO.builder()
                .id(7201L).eventId(1001L).currentVersion(1).build());
        when(allocationMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of(oldA));
        when(activeOrderMapper.selectActiveListByLeaderForUpdate(3001L)).thenReturn(List.of(activeOrder(8101L, 9001L)));
        when(releaseStateService.findReleasedActiveOrderIdsForUpdate(anyCollection())).thenReturn(Set.of(8101L));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.save(saveCommand(1,
                List.of(MesReportAllocationSaveLine.builder().activeOrderId(8101L)
                        .allocatedQuantity(new BigDecimal("50")).build()))));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_RELEASED_LOCKED.getCode(), ex.getCode());
    }

    private static MesReportAllocationSaveCommand saveCommand(Integer version,
                                                               List<MesReportAllocationSaveLine> lines) {
        return saveCommand(version, MesProcessPoolReportAllocationDO.MODE_MANUAL, lines);
    }

    private static MesReportAllocationSaveCommand saveCommand(Integer version, String allocationMode,
                                                               List<MesReportAllocationSaveLine> lines) {
        return MesReportAllocationSaveCommand.builder().eventId(1001L).leaderUserId(3001L)
                .leaderType("PRODUCTION").expectedVersion(version).idempotencyKey("req-1")
                .allocationMode(allocationMode).reason("urgent C").allocations(lines).build();
    }

    private void assertFullAllocationSameAsCurrentReconcilesCompletionProgress(String allocationMode) {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationDO currentFull = allocation(7101L, 8101L, 9001L, 5101L, "300");
        MesProcessPoolReportAllocationStateDO state = MesProcessPoolReportAllocationStateDO.builder()
                .id(7201L).eventId(1001L).currentVersion(1).build();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(8101L, 9001L);
        MesProWorkOrderDO workOrder = workOrder(9001L, "A");
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event);
        when(poolQuantityService.requirePoolQuantity(event)).thenReturn(new BigDecimal("300"));
        when(stateMapper.selectByEventIdForUpdate(1001L)).thenReturn(state);
        when(allocationMapper.selectListByEventIdForUpdate(1001L)).thenReturn(List.of(currentFull));
        when(activeOrderMapper.selectActiveListByLeaderForUpdate(3001L)).thenReturn(List.of(activeOrder));
        when(releaseStateService.findReleasedActiveOrderIdsForUpdate(anyCollection())).thenReturn(Set.of());
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder));
        when(allocationMapper.selectListByActiveOrderIdsAndProcessForUpdate(Set.of(8101L), 6001L))
                .thenReturn(List.of(currentFull));
        when(targetService.requireUniqueTargetForProcess(activeOrder, 6001L))
                .thenReturn(new MesTeamLeaderOrderProcessTarget(5101L, 6001L, new BigDecimal("300"),
                        BigDecimal.ONE, new BigDecimal("300")));
        when(releaseStateService.findReleasedActiveOrderIds(List.of(8101L))).thenReturn(Set.of());
        when(workOrderMapper.selectListByIds(List.of(9001L))).thenReturn(List.of(workOrder));

        MesReportAllocationSnapshot snapshot = service.save(saveCommand(1, allocationMode,
                List.of(MesReportAllocationSaveLine.builder().activeOrderId(8101L)
                        .allocatedQuantity(new BigDecimal("300")).build())));

        assertEquals(1, snapshot.getVersion());
        assertAmount("300", snapshot.getTotalAllocatedQuantity());
        verify(completionService).reconcileAffectedAllocations(event, List.of(currentFull));
        verify(allocationMapper, never()).supersedeCurrentRows(anyCollection(), any());
        verify(quantityFragmentService, never()).rebuildForVersion(any(), any(), anyCollection());
    }

    private static MesProProcessPoolEventDO event() {
        return MesProProcessPoolEventDO.builder().id(1001L).eventType("PRODUCTION_SUBMIT")
                .actualEmployeeId(4001L).routeProcessId(5001L).processId(6001L).build();
    }

    private static MesProcessPoolActiveOrderDO activeOrder(Long id, Long workOrderId) {
        return MesProcessPoolActiveOrderDO.builder().id(id).leaderUserId(3001L).workOrderId(workOrderId)
                .activeStatus("ACTIVE").build();
    }

    private static MesProWorkOrderDO workOrder(Long id, String code) {
        return MesProWorkOrderDO.builder().id(id).code(code).quantity(new BigDecimal("300")).build();
    }

    private static MesProcessPoolReportAllocationDO allocation(Long id, Long activeOrderId, Long workOrderId,
                                                                Long routeProcessId, String quantity) {
        return MesProcessPoolReportAllocationDO.builder().id(id).eventId(1001L).reviewId(7301L)
                .activeOrderId(activeOrderId).workOrderId(workOrderId).routeProcessId(routeProcessId)
                .processId(6001L).allocatedQuantity(new BigDecimal(quantity)).allocationMode("MANUAL")
                .lifecycleStatus("CURRENT").createdVersion(1).build();
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
