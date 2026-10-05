package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesReportAllocationFlowCycleRegressionTest {

    private final MesProProcessPoolEventMapper events = mock(MesProProcessPoolEventMapper.class);
    private final MesProcessPoolActiveOrderMapper orders = mock(MesProcessPoolActiveOrderMapper.class);
    private final MesProWorkOrderMapper workOrders = mock(MesProWorkOrderMapper.class);
    private final MesProcessPoolReportAllocationMapper allocations = mock(MesProcessPoolReportAllocationMapper.class);
    private final MesProcessPoolReportAllocationStateMapper states = mock(MesProcessPoolReportAllocationStateMapper.class);
    private final MesProcessPoolReportAllocationAdjustmentAuditMapper audits = mock(MesProcessPoolReportAllocationAdjustmentAuditMapper.class);
    private final MesProcessPoolSubmissionReviewMapper reviews = mock(MesProcessPoolSubmissionReviewMapper.class);
    private final MesReportAllocationReleaseStateService releases = mock(MesReportAllocationReleaseStateService.class);
    private final MesTeamLeaderOrderProcessTargetService targets = mock(MesTeamLeaderOrderProcessTargetService.class);
    private final MesProcessPoolActiveOrderProcessSnapshotMapper snapshots = mock(MesProcessPoolActiveOrderProcessSnapshotMapper.class);
    private final MesTeamLeaderOrderProcessCompletionService completion = mock(MesTeamLeaderOrderProcessCompletionService.class);
    private MesReportAllocationCommandService service;
    private final GxpAuditService gxpAuditService = mock(GxpAuditService.class);
    private MesProProcessPoolEventDO submitted;
    private MesProcessPoolActiveOrderDO order;
    private MesProcessPoolActiveOrderProcessSnapshotDO snapshot;

    @BeforeEach
    void setUp() {
        MesRouteStartProductionLeaderAuthorizationService authority = mock(MesRouteStartProductionLeaderAuthorizationService.class);
        when(authority.listAuthorizedRouteProcesses(30L)).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(50L).processId(60L).build()));
        service = new MesReportAllocationCommandService(mock(MesTeamLeaderScopeService.class), events, orders,
                workOrders, allocations, states, audits, reviews, new MesReportAllocationPoolQuantityService(),
                releases, targets, mock(MesTeamLeaderFifoAllocationService.class), authority,
                mock(MesReportAllocationQuantityFragmentService.class), completion,
                mock(MesProductionReportManagementSummaryService.class), snapshots);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "nonconformanceReviewService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "completionReceiptMapper", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper.class));
        { org.springframework.test.util.ReflectionTestUtils.setField(service, "handoffService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService.class)); }
        { org.springframework.test.util.ReflectionTestUtils.setField(service, "returnCorrectionResolver", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesSignedReturnCorrectionResolver.class)); }
        MesProBatchRecordExecutionSignatureService signature = mock(MesProBatchRecordExecutionSignatureService.class);
        when(signature.recordTeamLeaderReviewSignature(any(), any(), any(),
                eq("PROCESS_POOL_EVENT"), eq(10L), eq("生产报工组长复核"))).thenReturn(99L);
        ReflectionTestUtils.setField(service, "signatureService", signature);
        ReflectionTestUtils.setField(service, "gxpAuditService", gxpAuditService);
        submitted = event(10L, 502L, "100");
        order = order(81L, 91L);
        snapshot = snapshot(order, "[501,502]");
        when(events.selectByIdForUpdate(10L)).thenReturn(submitted);
        when(events.selectById(10L)).thenReturn(submitted);
        when(orders.selectActiveListByLeaderForUpdate(30L)).thenReturn(List.of(order));
        when(orders.selectById(81L)).thenReturn(order);
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder().id(91L).code("B").quantity(new BigDecimal("100")).build();
        when(workOrders.selectListByIdsForUpdate(anyCollection())).thenReturn(List.of(workOrder));
        when(workOrders.selectListByIds(anyCollection())).thenReturn(List.of(workOrder));
        when(targets.requireUniqueTargetForProcess(order, 60L)).thenReturn(
                new MesTeamLeaderOrderProcessTarget(50L, 60L, new BigDecimal("100"), BigDecimal.ONE, new BigDecimal("100")));
        when(snapshots.selectListByActiveOrderAndProcessForUpdate(81L, 60L)).thenReturn(List.of(snapshot));
        when(snapshots.selectByActiveOrderAndProcess(81L, 50L, 60L)).thenReturn(snapshot);
        when(states.selectByEventIdForUpdate(10L)).thenReturn(MesProcessPoolReportAllocationStateDO.builder()
                .id(70L).eventId(10L).currentVersion(0).build());
        when(states.updateById(any(MesProcessPoolReportAllocationStateDO.class))).thenReturn(1);
        when(reviews.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(73L);
            return 1;
        });
        when(allocations.insertBatch(anyCollection())).thenReturn(true);
        when(audits.insertBatch(anyCollection())).thenReturn(true);
    }

    @Test
    void unrelatedCompletedOrderMustNotBlockConfirmation() {
        when(orders.selectActiveListByLeaderForUpdate(30L)).thenReturn(List.of(
                order(82L, 92L).setBusinessStatus("COMPLETED"), order));
        MesReportAllocationSnapshot result = service.save(command("100"));
        assertAmount("100", result.getTotalAllocatedQuantity());
        verify(releases).findReleaseApplicationLockedActiveOrderIdsForUpdate(Set.of(81L));
        verify(completion).reconcileAffectedAllocations(eq(submitted), anyCollection());
        var audit = org.mockito.ArgumentCaptor.forClass(GxpAuditCommand.class);
        verify(gxpAuditService).append(audit.capture());
        assertEquals("mes.production.allocation.save", audit.getValue().getOperationId());
        assertEquals("99", audit.getValue().getSignatureRecordId());
        assertTrue(audit.getValue().getLinks().stream().anyMatch(link ->
                "ACTIVE_ORDER".equals(link.objectType()) && "81".equals(link.objectId())));
        assertFalse(audit.getValue().getLinks().stream().anyMatch(link ->
                "ACTIVE_ORDER".equals(link.objectType()) && "82".equals(link.objectId())));
    }

    @Test
    void unrelatedReleaseApplicationMustNotBlockConfirmation() {
        when(orders.selectActiveListByLeaderForUpdate(30L)).thenReturn(List.of(order(82L, 92L), order));
        when(releases.findReleaseApplicationLockedActiveOrderIdsForUpdate(anyCollection())).thenAnswer(invocation ->
                invocation.<Collection<Long>>getArgument(0).contains(82L) ? Set.of(82L) : Set.of());
        assertAmount("100", service.save(command("100")).getTotalAllocatedQuantity());
    }

    @Test
    void completedShareCannotBeSilentlyRemovedToExceedPool() {
        MesProcessPoolActiveOrderDO old = order(82L, 92L).setBusinessStatus("COMPLETED");
        when(orders.selectActiveListByLeaderForUpdate(30L)).thenReturn(List.of(old, order));
        var existing = allocation(10L, old, "100");
        String before = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(existing);
        when(allocations.selectListByEventIdForUpdate(10L)).thenReturn(List.of(existing));
        ServiceException error = assertThrows(ServiceException.class, () -> service.save(command("100")));
        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_TOTAL_MISMATCH.getCode(), error.getCode());
        assertEquals(before, cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(existing));
        verify(allocations, never()).supersedeCurrentRows(anyCollection(), anyInt());
        verify(allocations, never()).insertBatch(anyCollection());
    }

    @Test
    void separateOutputMaterialReportsCanBothBeConfirmedWithoutFalseOverage() {
        MesProProcessPoolEventDO previous = event(11L, 501L, "100");
        stubOtherAllocation(previous, "100");
        MesReportAllocationSnapshot result = service.save(command("100"));
        assertAmount("0", result.getLines().get(0).getOverageQuantity());
        assertFalse(result.getLines().get(0).getNeedsAdjustment());
        assertAmount("100", MesOutputMaterialProgressCalculator.calculateConservativeProcessProgress(order,
                snapshot, List.of(previous, submitted), List.of(allocation(11L, order, "100"), allocation(10L, order, "100"))));
    }

    @Test
    void oldReworkShareCannotBeSilentlyRemovedToExceedPool() {
        MesProcessPoolActiveOrderDO previousCycle = order(82L, 91L)
                .setActiveStatus("REMOVED").setBusinessStatus("REWORKED");
        when(orders.selectByIdForUpdate(82L)).thenReturn(previousCycle);
        var existing = allocation(10L, previousCycle, "100");
        String before = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(existing);
        when(allocations.selectListByEventIdForUpdate(10L)).thenReturn(List.of(existing));
        ServiceException error = assertThrows(ServiceException.class, () -> service.save(command("100")));
        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_TOTAL_MISMATCH.getCode(), error.getCode());
        assertEquals(before, cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(existing));
        verify(allocations, never()).insertBatch(anyCollection());
        verify(allocations, never()).supersedeCurrentRows(anyCollection(), anyInt());
    }

    @Test
    void completedOrderWithoutReleaseApplicationCannotHaveProductionReviewRejected() {
        when(orders.selectByIdForUpdate(81L)).thenReturn(order.setBusinessStatus("COMPLETED"));
        when(allocations.selectListByEventIdForUpdate(10L)).thenReturn(List.of(allocation(10L, order, "100")));
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.rejectProductionSubmission(10L, 30L, "录入不正确", "unit-test"));
        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_RELEASED_LOCKED.getCode(), error.getCode());
        verify(allocations, never()).supersedeCurrentRows(anyCollection(), anyInt());
    }

    @Test
    void currentSnapshotUsesSameMaterialBasedOverageAsConfirmation() {
        MesProProcessPoolEventDO previous = event(11L, 501L, "100");
        stubOtherAllocation(previous, "100");
        when(allocations.selectListByEventId(10L)).thenReturn(List.of(allocation(10L, order, "100")));
        MesReportAllocationSnapshot result = service.getCurrent(10L, 30L, "PRODUCTION");
        assertAmount("0", result.getLines().get(0).getOverageQuantity());
    }

    @Test
    void unequalCombinedMaterialsMustNotHideActualOverage() {
        submitted.setRawPayload("{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":150},{\"materialId\":502,\"outputQuantity\":100}]}");
        ServiceException error = assertThrows(ServiceException.class, () -> service.save(command("100")));
        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_OVERAGE_LIMIT_EXCEEDED.getCode(), error.getCode());
        verify(allocations, never()).insertBatch(anyCollection());
    }

    @Test
    void partialAllocationUsesProportionOfUnequalMaterials() {
        submitted.setRawPayload("{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":150},{\"materialId\":502,\"outputQuantity\":100}]}");
        stubOtherAllocation(event(11L, 501L, "30"), "30");
        ServiceException error = assertThrows(ServiceException.class, () -> service.save(command("50")));
        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_OVERAGE_LIMIT_EXCEEDED.getCode(), error.getCode());
        // 甲 30 + 150 * (50 / 100) = 105; 不能错误地算成 80。
    }

    @Test
    void actualSameMaterialOverageMustRemainRejected() {
        stubOtherAllocation(event(11L, 502L, "100"), "100");
        ServiceException error = assertThrows(ServiceException.class, () -> service.save(command("100")));
        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_OVERAGE_LIMIT_EXCEEDED.getCode(), error.getCode());
    }

    @Test
    void progressRetainsUnequalMaterialAmountsAcrossLaterSubmissions() {
        submitted.setRawPayload("{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":150},{\"materialId\":502,\"outputQuantity\":100}]}");
        assertAmount("150", MesOutputMaterialProgressCalculator.calculateConservativeProcessProgress(order,
                snapshot, List.of(submitted, event(11L, 502L, "50")),
                List.of(allocation(10L, order, "100"), allocation(11L, order, "50"))));
    }

    @Test
    void crossOrderPartialAllocationsConserveUnequalOutputMaterials() {
        submitted.setRawPayload("{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":150},{\"materialId\":502,\"outputQuantity\":100}]}");
        MesProcessPoolActiveOrderDO second = order(82L, 92L);
        List<MesProcessPoolReportAllocationDO> split = List.of(
                allocation(10L, order, "40"), allocation(10L, second, "60"),
                allocation(11L, order, "20"), allocation(12L, second, "30"));
        List<MesProProcessPoolEventDO> facts = List.of(submitted, event(11L, 502L, "20"), event(12L, 502L, "30"));
        BigDecimal firstProgress = MesOutputMaterialProgressCalculator.calculateConservativeProcessProgress(
                order, snapshot, facts, split);
        BigDecimal secondProgress = MesOutputMaterialProgressCalculator.calculateConservativeProcessProgress(
                second, snapshot(second, "[501,502]"), facts, split);
        assertAmount("60", firstProgress);
        assertAmount("90", secondProgress);
        assertAmount("150", firstProgress.add(secondProgress));
    }

    @Test
    void invalidFormalPoolQuantityMustFailInsteadOfInventingAllocationRatio() {
        submitted.setReportOutputQuantity(BigDecimal.ZERO);
        assertThrows(ServiceException.class, () -> MesOutputMaterialProgressCalculator.calculateConservativeProcessProgress(
                order, snapshot, List.of(submitted), List.of(allocation(10L, order, "50"))));
    }

    private void stubOtherAllocation(MesProProcessPoolEventDO previous, String quantity) {
        List<MesProcessPoolReportAllocationDO> other = List.of(allocation(previous.getId(), order, quantity));
        when(allocations.selectListByActiveOrderIdsAndProcessForUpdate(Set.of(81L), 60L)).thenReturn(other);
        when(allocations.selectListByActiveOrderIdsAndProcess(Set.of(81L), 60L)).thenReturn(other);
        when(events.selectById(previous.getId())).thenReturn(previous);
        when(events.selectByIdForUpdate(previous.getId())).thenReturn(previous);
        when(events.selectBatchIds(anyCollection())).thenReturn(List.of(previous));
    }

    private MesReportAllocationSaveCommand command(String quantity) {
        return MesReportAllocationSaveCommand.builder().eventId(10L).leaderUserId(30L).leaderType("PRODUCTION")
                .expectedVersion(0).idempotencyKey("flow-cycle").allocationMode("MANUAL").reason("确认报工")
                .signaturePassword("unit-test").allocations(List.of(MesReportAllocationSaveLine.builder()
                        .activeOrderId(81L).allocatedQuantity(new BigDecimal(quantity)).build())).build();
    }

    private static MesProProcessPoolEventDO event(Long id, Long material, String quantity) {
        return MesProProcessPoolEventDO.builder().id(id).eventType("PRODUCTION_SUBMIT").workOrderId(91L)
                .routeId(40L).routeProcessId(50L).processId(60L).actualEmployeeId(41L).deviceAccountId(42L)
                .reportOutputQuantity(new BigDecimal(quantity)).rawPayload("{\"materialDetails\":[{\"materialId\":"
                        + material + ",\"outputQuantity\":" + quantity + "}]}").build();
    }

    private static MesProcessPoolActiveOrderDO order(Long id, Long workOrderId) {
        return MesProcessPoolActiveOrderDO.builder().id(id).leaderUserId(30L).workOrderId(workOrderId)
                .routeId(40L).activeStatus("ACTIVE").businessStatus("ACTIVE").build();
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO snapshot(MesProcessPoolActiveOrderDO order, String ids) {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder().activeOrderId(order.getId()).workOrderId(order.getWorkOrderId())
                .routeId(40L).routeProcessId(50L).processId(60L).plannedQuantitySnapshot(new BigDecimal("100"))
                .overagePercentSnapshot(BigDecimal.ZERO).productionConfigSnapshotJson("{\"outputMaterialIds\":" + ids + "}").build();
    }

    private static MesProcessPoolReportAllocationDO allocation(Long event, MesProcessPoolActiveOrderDO order, String quantity) {
        return MesProcessPoolReportAllocationDO.builder().id(event + 100).eventId(event).reviewId(73L)
                .activeOrderId(order.getId()).workOrderId(order.getWorkOrderId()).routeProcessId(50L).processId(60L)
                .allocatedQuantity(new BigDecimal(quantity)).allocationMode("MANUAL").lifecycleStatus("CURRENT")
                .createdVersion(1).confirmedAt(LocalDateTime.of(2026, 9, 28, 8, 0)).build();
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
