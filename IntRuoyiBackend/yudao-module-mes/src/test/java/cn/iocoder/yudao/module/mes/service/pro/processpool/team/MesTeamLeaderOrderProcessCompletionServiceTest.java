package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderOrderProcessCompletionServiceTest {

    @Mock
    private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock
    private MesProProcessPoolEventMapper eventMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesProcessPoolOrderProcessCompletionMapper completionMapper;
    @Mock
    private MesTeamLeaderOrderProcessTargetService orderProcessTargetService;
    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock
    private MesTeamLeaderBatchRecordBackfillService backfillService;

    private MesTeamLeaderOrderProcessCompletionService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderOrderProcessCompletionService(allocationMapper, eventMapper, workOrderMapper,
                completionMapper, orderProcessTargetService, scheduleOrderMapper, scheduleOrderProcessMapper,
                backfillService);
    }

    @Test
    void shouldCompleteOrderProcessAndTriggerBackfillWhenCumulativeQuantityReachesTarget() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationDO confirmedLine = allocation(9001L, "80", 7101L, 1001L, 7001L,
                LocalDateTime.of(2026, 8, 1, 9, 1));
        MesProcessPoolReportAllocationDO priorLine = allocation(9001L, "120", 7100L, 1000L, 7000L,
                LocalDateTime.of(2026, 8, 1, 8, 31));
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder("200")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of(priorLine, confirmedLine));
        when(eventMapper.selectBatchIds(List.of(1000L, 1001L)))
                .thenReturn(List.of(event(1000L, "{\"pressure\":15}",
                                LocalDateTime.of(2026, 8, 1, 8, 30)),
                        event));
        when(orderProcessTargetService.requireTarget(8101L, 9001L, 5001L, 6001L)).thenReturn(target("200"));
        stubFormalSchedule("200", "200");
        when(completionMapper.selectByWorkOrderAndProcessForUpdate(9001L, 5001L, 6001L)).thenReturn(null);
        when(backfillService.backfillCompletedProcess(any(MesTeamLeaderBatchRecordBackfillCommand.class)))
                .thenReturn(new MesTeamLeaderBatchRecordBackfillResult()
                        .setExecutionId(8801L)
                        .setAppliedFieldCount(2));

        service.applyConfirmedAllocations(event, List.of(confirmedLine));

        ArgumentCaptor<MesProcessPoolOrderProcessCompletionDO> completionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolOrderProcessCompletionDO.class);
        verify(completionMapper).insert(completionCaptor.capture());
        MesProcessPoolOrderProcessCompletionDO saved = completionCaptor.getValue();
        assertEquals(9001L, saved.getWorkOrderId());
        assertEquals(5001L, saved.getRouteProcessId());
        assertEquals(6001L, saved.getProcessId());
        assertAmount("200", saved.getTargetQuantity());
        assertAmount("200", saved.getConfirmedQuantity());
        assertEquals(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED, saved.getCompletionStatus());
        assertEquals(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS, saved.getBackfillStatus());
        assertEquals(8801L, saved.getBackfillExecutionId());

        ArgumentCaptor<MesTeamLeaderBatchRecordBackfillCommand> backfillCaptor =
                ArgumentCaptor.forClass(MesTeamLeaderBatchRecordBackfillCommand.class);
        verify(backfillService).backfillCompletedProcess(backfillCaptor.capture());
        MesTeamLeaderBatchRecordBackfillCommand command = backfillCaptor.getValue();
        assertEquals(event.getId(), command.getEvent().getId());
        assertEquals(confirmedLine.getWorkOrderId(), command.getAllocation().getWorkOrderId());
        assertEquals(List.of(1000L, 1001L), command.getSourceEvents().stream()
                .map(MesProProcessPoolEventDO::getId).toList());
        assertEquals(List.of(7100L, 7101L), command.getAllocations().stream()
                .map(MesProcessPoolReportAllocationDO::getId).toList());
        assertEquals("PROCESS_POOL_REPORT_BACKFILL_AGG:9001:5001:6001:" + command.getAggregateHash(),
                command.getIdempotencyKey());
        assertEquals("[1000,1001]", saved.getSourceEventIdsJson());
        assertEquals("[7100,7101]", saved.getSourceAllocationIdsJson());
        assertEquals(command.getAggregateHash(), saved.getAggregateHash());
        assertEquals(command.getIdempotencyKey(), saved.getBackfillIdempotencyKey());
    }

    @Test
    void shouldCompleteSharedAllocationWithoutTriggeringLegacyBatchRecordBackfill() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationDO confirmedLine = allocation(9001L, "200");
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder("200")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of(confirmedLine));
        when(orderProcessTargetService.requireTarget(8101L, 9001L, 5001L, 6001L)).thenReturn(target("200"));
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(9001L))).thenReturn(List.of());
        when(completionMapper.selectByWorkOrderAndProcessForUpdate(9001L, 5001L, 6001L)).thenReturn(null);

        service.reconcileAffectedAllocations(event, List.of(confirmedLine));

        ArgumentCaptor<MesProcessPoolOrderProcessCompletionDO> completionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolOrderProcessCompletionDO.class);
        verify(completionMapper).insert(completionCaptor.capture());
        MesProcessPoolOrderProcessCompletionDO saved = completionCaptor.getValue();
        assertEquals(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED, saved.getCompletionStatus());
        assertEquals(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_NOT_REQUIRED,
                saved.getBackfillStatus());
        assertEquals(null, saved.getBackfillExecutionId());
        verify(backfillService, never()).backfillCompletedProcess(any(MesTeamLeaderBatchRecordBackfillCommand.class));
    }

    @Test
    void shouldKeepOrderProcessInProgressBeforeTargetQuantityIsReached() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationDO confirmedLine = allocation(9001L, "79");
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder("200")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of(allocation(9001L, "120"), confirmedLine));
        when(orderProcessTargetService.requireTarget(8101L, 9001L, 5001L, 6001L)).thenReturn(target("200"));
        stubFormalSchedule("200", "200");
        when(completionMapper.selectByWorkOrderAndProcessForUpdate(9001L, 5001L, 6001L)).thenReturn(null);

        service.applyConfirmedAllocations(event, List.of(confirmedLine));

        ArgumentCaptor<MesProcessPoolOrderProcessCompletionDO> completionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolOrderProcessCompletionDO.class);
        verify(completionMapper).insert(completionCaptor.capture());
        MesProcessPoolOrderProcessCompletionDO saved = completionCaptor.getValue();
        assertAmount("199", saved.getConfirmedQuantity());
        assertEquals(MesProcessPoolOrderProcessCompletionDO.STATUS_IN_PROGRESS, saved.getCompletionStatus());
        assertEquals(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_NOT_REQUIRED, saved.getBackfillStatus());
        verify(scheduleOrderProcessMapper).updateProgress(8801L, new BigDecimal("199.000000"),
                new BigDecimal("1.000000"), new BigDecimal("99.500000"));
        verify(scheduleOrderMapper).updateProgressSummary(7701L, new BigDecimal("200.000000"),
                new BigDecimal("199.000000"), new BigDecimal("1.000000"), new BigDecimal("99.500000"),
                MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus());
        verify(backfillService, never()).backfillCompletedProcess(any(MesTeamLeaderBatchRecordBackfillCommand.class));
    }

    @Test
    void shouldPersistCompletionFromActiveOrderSnapshotWhenScheduleOrderNoLongerExists() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationDO confirmedLine = allocation(9001L, "100");
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder("200")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of(confirmedLine));
        when(orderProcessTargetService.requireTarget(8101L, 9001L, 5001L, 6001L)).thenReturn(target("200"));
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(9001L))).thenReturn(List.of());
        when(completionMapper.selectByWorkOrderAndProcessForUpdate(9001L, 5001L, 6001L)).thenReturn(null);

        service.applyConfirmedAllocations(event, List.of(confirmedLine));

        ArgumentCaptor<MesProcessPoolOrderProcessCompletionDO> completionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolOrderProcessCompletionDO.class);
        verify(completionMapper).insert(completionCaptor.capture());
        MesProcessPoolOrderProcessCompletionDO saved = completionCaptor.getValue();
        assertAmount("100", saved.getConfirmedQuantity());
        assertEquals(MesProcessPoolOrderProcessCompletionDO.STATUS_IN_PROGRESS, saved.getCompletionStatus());
        assertEquals(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_NOT_REQUIRED, saved.getBackfillStatus());
        verify(scheduleOrderProcessMapper, never()).selectListByScheduleOrderId(any());
    }

    @Test
    void shouldUpdateFormalScheduleProgressByProcessTargetWithoutChangingErpQuantity() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationDO confirmedLine = allocation(9001L, "300");
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder("300")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of(confirmedLine));
        when(orderProcessTargetService.requireTarget(8101L, 9001L, 5001L, 6001L)).thenReturn(target("900"));
        stubFormalSchedule("300", "900");
        when(completionMapper.selectByWorkOrderAndProcessForUpdate(9001L, 5001L, 6001L)).thenReturn(null);

        service.applyConfirmedAllocations(event, List.of(confirmedLine));

        verify(scheduleOrderProcessMapper).updateProgress(8801L, new BigDecimal("300.000000"),
                new BigDecimal("600.000000"), new BigDecimal("33.333333"));
        verify(scheduleOrderMapper).updateProgressSummary(7701L, new BigDecimal("900.000000"),
                new BigDecimal("300.000000"), new BigDecimal("600.000000"), new BigDecimal("33.333333"),
                MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus());
    }

    @Test
    void shouldNotBackfillAgainWhenOrderProcessAlreadyCompleted() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationDO confirmedLine = allocation(9001L, "20");
        MesProcessPoolOrderProcessCompletionDO existingCompletion = new MesProcessPoolOrderProcessCompletionDO()
                .setId(7701L)
                .setWorkOrderId(9001L)
                .setRouteProcessId(5001L)
                .setProcessId(6001L)
                .setTargetQuantity(new BigDecimal("200"))
                .setConfirmedQuantity(new BigDecimal("200"))
                .setCompletionStatus(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED)
                .setCompletedAt(LocalDateTime.of(2026, 8, 1, 9, 2))
                .setBackfillStatus(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS)
                .setBackfillExecutionId(8801L);
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder("200")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of(allocation(9001L, "200")));
        when(orderProcessTargetService.requireTarget(8101L, 9001L, 5001L, 6001L)).thenReturn(target("200"));
        stubFormalSchedule("200", "200");
        when(completionMapper.selectByWorkOrderAndProcessForUpdate(9001L, 5001L, 6001L))
                .thenReturn(existingCompletion);

        service.applyConfirmedAllocations(event, List.of(confirmedLine));

        ArgumentCaptor<MesProcessPoolOrderProcessCompletionDO> completionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolOrderProcessCompletionDO.class);
        verify(completionMapper).updateById(completionCaptor.capture());
        MesProcessPoolOrderProcessCompletionDO saved = completionCaptor.getValue();
        assertAmount("200", saved.getConfirmedQuantity());
        assertEquals(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED, saved.getCompletionStatus());
        assertEquals(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS, saved.getBackfillStatus());
        assertEquals(8801L, saved.getBackfillExecutionId());
        verify(backfillService, never()).backfillCompletedProcess(any(MesTeamLeaderBatchRecordBackfillCommand.class));
    }

    @Test
    void shouldPreventOverTargetProgressWhenConcurrentAllocationAlreadyConsumedRemainingQuantity() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationDO confirmedLine = allocation(9001L, "20");
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder("200")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of(allocation(9001L, "190"), confirmedLine));
        when(orderProcessTargetService.requireTarget(8101L, 9001L, 5001L, 6001L)).thenReturn(target("200"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.applyConfirmedAllocations(event, List.of(confirmedLine)));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_REMAINING_NOT_ENOUGH.getCode(),
                ex.getCode());
        verify(completionMapper, never()).insert(any(MesProcessPoolOrderProcessCompletionDO.class));
        verify(completionMapper, never()).updateById(any(MesProcessPoolOrderProcessCompletionDO.class));
        verify(backfillService, never()).backfillCompletedProcess(any(MesTeamLeaderBatchRecordBackfillCommand.class));
    }

    @Test
    void shouldPreserveAdjustableFrontlineOverageAndCapFormalScheduleProgressAtTarget() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationDO frontlineAllocation = allocation(9001L, "10")
                .setAllocationMode(MesProcessPoolReportAllocationDO.MODE_FRONTLINE_SELECTED);
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder("6")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of(frontlineAllocation));
        when(orderProcessTargetService.requireTarget(8101L, 9001L, 5001L, 6001L)).thenReturn(target("6"));
        stubFormalSchedule("6", "6");
        when(completionMapper.selectByWorkOrderAndProcessForUpdate(9001L, 5001L, 6001L)).thenReturn(null);

        service.reconcileAffectedAllocations(event, List.of(frontlineAllocation));

        ArgumentCaptor<MesProcessPoolOrderProcessCompletionDO> completionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolOrderProcessCompletionDO.class);
        verify(completionMapper).insert(completionCaptor.capture());
        MesProcessPoolOrderProcessCompletionDO saved = completionCaptor.getValue();
        assertAmount("6", saved.getTargetQuantity());
        assertAmount("10", saved.getConfirmedQuantity());
        assertEquals(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED, saved.getCompletionStatus());
        assertEquals(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_NOT_REQUIRED,
                saved.getBackfillStatus());
        verify(scheduleOrderProcessMapper).updateProgress(8801L, new BigDecimal("6.000000"),
                new BigDecimal("0.000000"), new BigDecimal("100.000000"));
        verify(scheduleOrderMapper).updateProgressSummary(7701L, new BigDecimal("6.000000"),
                new BigDecimal("6.000000"), new BigDecimal("0.000000"), new BigDecimal("100.000000"),
                MesProScheduleOrderStatusEnum.FINISHED.getStatus());
        verify(backfillService, never()).backfillCompletedProcess(any(MesTeamLeaderBatchRecordBackfillCommand.class));
    }

    @Test
    void shouldWaitForPerProcessSnapshotTargetBeforeBackfill() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationDO confirmedLine = allocation(9001L, "300");
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder("300")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of(allocation(9001L, "300"), allocation(9001L, "300"), confirmedLine));
        when(eventMapper.selectBatchIds(List.of(1001L))).thenReturn(List.of(event));
        when(orderProcessTargetService.requireTarget(8101L, 9001L, 5001L, 6001L)).thenReturn(target("900"));
        stubFormalSchedule("300", "900");
        when(completionMapper.selectByWorkOrderAndProcessForUpdate(9001L, 5001L, 6001L)).thenReturn(null);
        when(backfillService.backfillCompletedProcess(any(MesTeamLeaderBatchRecordBackfillCommand.class)))
                .thenReturn(new MesTeamLeaderBatchRecordBackfillResult()
                        .setExecutionId(8802L)
                        .setAppliedFieldCount(2));

        service.applyConfirmedAllocations(event, List.of(confirmedLine));

        ArgumentCaptor<MesProcessPoolOrderProcessCompletionDO> completionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolOrderProcessCompletionDO.class);
        verify(completionMapper).insert(completionCaptor.capture());
        MesProcessPoolOrderProcessCompletionDO saved = completionCaptor.getValue();
        assertAmount("900", saved.getTargetQuantity());
        assertAmount("900", saved.getConfirmedQuantity());
        assertEquals(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED, saved.getCompletionStatus());
        verify(backfillService).backfillCompletedProcess(any(MesTeamLeaderBatchRecordBackfillCommand.class));
    }

    @Test
    void shouldRecalculateRemovedAAndAddedCByEachTargetRouteProcess() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationDO removedA = allocation(8101L, 9001L, 5101L, "100", 7101L);
        MesProcessPoolReportAllocationDO currentC = allocation(8103L, 9003L, 5301L, "100", 7103L);
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L, 9003L))).thenReturn(List.of(
                workOrder(9001L, "A", "300"), workOrder(9003L, "C", "300")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5101L, 6001L))
                .thenReturn(List.of());
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9003L), 5301L, 6001L))
                .thenReturn(List.of(currentC));
        when(orderProcessTargetService.requireTarget(8101L, 9001L, 5101L, 6001L))
                .thenReturn(new MesTeamLeaderOrderProcessTarget(5101L, 6001L, new BigDecimal("300"),
                        BigDecimal.ONE, new BigDecimal("300")));
        when(orderProcessTargetService.requireTarget(8103L, 9003L, 5301L, 6001L))
                .thenReturn(new MesTeamLeaderOrderProcessTarget(5301L, 6001L, new BigDecimal("300"),
                        BigDecimal.ONE, new BigDecimal("300")));
        stubFormalSchedule(9001L, 7701L, 8801L, 5101L, "300");
        stubFormalSchedule(9003L, 7703L, 8803L, 5301L, "300");
        when(completionMapper.selectByWorkOrderAndProcessForUpdate(9001L, 5101L, 6001L)).thenReturn(null);
        when(completionMapper.selectByWorkOrderAndProcessForUpdate(9003L, 5301L, 6001L)).thenReturn(null);

        service.reconcileAffectedAllocations(event, List.of(removedA, currentC));

        ArgumentCaptor<MesProcessPoolOrderProcessCompletionDO> completionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolOrderProcessCompletionDO.class);
        verify(completionMapper, org.mockito.Mockito.times(2)).insert(completionCaptor.capture());
        List<MesProcessPoolOrderProcessCompletionDO> saved = completionCaptor.getAllValues();
        MesProcessPoolOrderProcessCompletionDO savedA = saved.stream()
                .filter(row -> row.getWorkOrderId().equals(9001L)).findFirst().orElseThrow();
        MesProcessPoolOrderProcessCompletionDO savedC = saved.stream()
                .filter(row -> row.getWorkOrderId().equals(9003L)).findFirst().orElseThrow();
        assertEquals(5101L, savedA.getRouteProcessId());
        assertAmount("0", savedA.getConfirmedQuantity());
        assertEquals(MesProcessPoolOrderProcessCompletionDO.STATUS_IN_PROGRESS, savedA.getCompletionStatus());
        assertEquals(5301L, savedC.getRouteProcessId());
        assertAmount("100", savedC.getConfirmedQuantity());
        verify(scheduleOrderProcessMapper).updateProgress(8801L, new BigDecimal("0.000000"),
                new BigDecimal("300.000000"), new BigDecimal("0.000000"));
        verify(scheduleOrderProcessMapper).updateProgress(8803L, new BigDecimal("100.000000"),
                new BigDecimal("200.000000"), new BigDecimal("33.333333"));
    }

    private static MesProProcessPoolEventDO event() {
        return event(1001L, "{\"outputQuantity\":80,\"pressure\":15}", LocalDateTime.of(2026, 8, 1, 9, 0));
    }

    private static MesProProcessPoolEventDO event(Long id, String rawPayload, LocalDateTime serverSubmitTime) {
        return MesProProcessPoolEventDO.builder()
                .id(id)
                .routeId(7001L)
                .routeProcessId(5001L)
                .processId(6001L)
                .rawPayload(rawPayload)
                .serverSubmitTime(serverSubmitTime)
                .build();
    }

    private static MesProcessPoolReportAllocationDO allocation(Long workOrderId, String quantity) {
        return allocation(workOrderId, quantity, 7100L + new BigDecimal(quantity).longValue(), 1001L, 7001L,
                LocalDateTime.of(2026, 8, 1, 9, 1));
    }

    private static MesProcessPoolReportAllocationDO allocation(Long workOrderId, String quantity, Long id,
                                                               Long eventId, Long reviewId,
                                                               LocalDateTime confirmedAt) {
        return MesProcessPoolReportAllocationDO.builder()
                .id(id)
                .eventId(eventId)
                .reviewId(reviewId)
                .leaderUserId(3001L)
                .activeOrderId(8101L)
                .workOrderId(workOrderId)
                .routeProcessId(5001L)
                .processId(6001L)
                .allocatedQuantity(new BigDecimal(quantity))
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_FIFO)
                .confirmedAt(confirmedAt)
                .build();
    }

    private static MesProcessPoolReportAllocationDO allocation(Long activeOrderId, Long workOrderId,
                                                               Long routeProcessId, String quantity, Long id) {
        return MesProcessPoolReportAllocationDO.builder().id(id).eventId(1001L).reviewId(7001L)
                .leaderUserId(3001L).activeOrderId(activeOrderId).workOrderId(workOrderId)
                .routeProcessId(routeProcessId).processId(6001L).allocatedQuantity(new BigDecimal(quantity))
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_MANUAL)
                .confirmedAt(LocalDateTime.of(2026, 8, 1, 9, 1)).build();
    }

    private static MesProWorkOrderDO workOrder(String quantity) {
        return MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .batchCode("BATCH-9001")
                .quantity(new BigDecimal(quantity))
                .build();
    }

    private static MesProWorkOrderDO workOrder(Long id, String code, String quantity) {
        return MesProWorkOrderDO.builder().id(id).code(code).batchCode("BATCH-" + code)
                .quantity(new BigDecimal(quantity)).build();
    }

    private void stubFormalSchedule(String erpQuantity, String plannedQuantity) {
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(9001L)))
                .thenReturn(List.of(scheduleOrder(erpQuantity)));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(7701L))
                .thenReturn(List.of(scheduleProcess(plannedQuantity)));
    }

    private void stubFormalSchedule(Long workOrderId, Long scheduleOrderId, Long scheduleProcessId,
                                    Long routeProcessId, String plannedQuantity) {
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(workOrderId))).thenReturn(List.of(
                MesProScheduleOrderDO.builder().id(scheduleOrderId).workOrderId(workOrderId)
                        .quantity(new BigDecimal(plannedQuantity))
                        .status(MesProScheduleOrderStatusEnum.SCHEDULED.getStatus()).build()));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(scheduleOrderId)).thenReturn(List.of(
                MesProScheduleOrderProcessDO.builder().id(scheduleProcessId).scheduleOrderId(scheduleOrderId)
                        .routeProcessId(routeProcessId).processId(6001L).enabled(Boolean.TRUE)
                        .plannedQuantity(new BigDecimal(plannedQuantity)).reportedQuantity(BigDecimal.ZERO)
                        .remainingQuantity(new BigDecimal(plannedQuantity)).build()));
    }

    private static MesProScheduleOrderDO scheduleOrder(String quantity) {
        return MesProScheduleOrderDO.builder()
                .id(7701L)
                .workOrderId(9001L)
                .quantity(new BigDecimal(quantity))
                .status(MesProScheduleOrderStatusEnum.SCHEDULED.getStatus())
                .build();
    }

    private static MesProScheduleOrderProcessDO scheduleProcess(String plannedQuantity) {
        return MesProScheduleOrderProcessDO.builder()
                .id(8801L)
                .scheduleOrderId(7701L)
                .routeProcessId(5001L)
                .processId(6001L)
                .enabled(Boolean.TRUE)
                .plannedQuantity(new BigDecimal(plannedQuantity))
                .reportedQuantity(BigDecimal.ZERO)
                .remainingQuantity(new BigDecimal(plannedQuantity))
                .build();
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private static MesTeamLeaderOrderProcessTarget target(String plannedQuantity) {
        return new MesTeamLeaderOrderProcessTarget(5001L, 6001L, new BigDecimal("300"),
                new BigDecimal("3.000000"), new BigDecimal(plannedQuantity));
    }
}
