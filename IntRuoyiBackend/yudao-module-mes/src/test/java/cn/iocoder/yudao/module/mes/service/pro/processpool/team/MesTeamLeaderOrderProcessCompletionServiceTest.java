package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderOrderProcessCompletionServiceTest {

    @Mock
    private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesProcessPoolOrderProcessCompletionMapper completionMapper;
    @Mock
    private MesTeamLeaderBatchRecordBackfillService backfillService;

    private MesTeamLeaderOrderProcessCompletionService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderOrderProcessCompletionService(allocationMapper, workOrderMapper,
                completionMapper, backfillService);
    }

    @Test
    void shouldCompleteOrderProcessAndTriggerBackfillWhenCumulativeQuantityReachesTarget() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationDO confirmedLine = allocation(9001L, "80");
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder("200")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of(allocation(9001L, "120"), confirmedLine));
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
        assertEquals(event.getId(), backfillCaptor.getValue().getEvent().getId());
        assertEquals(confirmedLine.getWorkOrderId(), backfillCaptor.getValue().getAllocation().getWorkOrderId());
    }

    @Test
    void shouldKeepOrderProcessInProgressBeforeTargetQuantityIsReached() {
        MesProProcessPoolEventDO event = event();
        MesProcessPoolReportAllocationDO confirmedLine = allocation(9001L, "79");
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(workOrder("200")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of(allocation(9001L, "120"), confirmedLine));
        when(completionMapper.selectByWorkOrderAndProcessForUpdate(9001L, 5001L, 6001L)).thenReturn(null);

        service.applyConfirmedAllocations(event, List.of(confirmedLine));

        ArgumentCaptor<MesProcessPoolOrderProcessCompletionDO> completionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolOrderProcessCompletionDO.class);
        verify(completionMapper).insert(completionCaptor.capture());
        MesProcessPoolOrderProcessCompletionDO saved = completionCaptor.getValue();
        assertAmount("199", saved.getConfirmedQuantity());
        assertEquals(MesProcessPoolOrderProcessCompletionDO.STATUS_IN_PROGRESS, saved.getCompletionStatus());
        assertEquals(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_NOT_REQUIRED, saved.getBackfillStatus());
        verify(backfillService, never()).backfillCompletedProcess(any(MesTeamLeaderBatchRecordBackfillCommand.class));
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
                .thenReturn(List.of(allocation(9001L, "200"), confirmedLine));
        when(completionMapper.selectByWorkOrderAndProcessForUpdate(9001L, 5001L, 6001L))
                .thenReturn(existingCompletion);

        service.applyConfirmedAllocations(event, List.of(confirmedLine));

        ArgumentCaptor<MesProcessPoolOrderProcessCompletionDO> completionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolOrderProcessCompletionDO.class);
        verify(completionMapper).updateById(completionCaptor.capture());
        MesProcessPoolOrderProcessCompletionDO saved = completionCaptor.getValue();
        assertAmount("220", saved.getConfirmedQuantity());
        assertEquals(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED, saved.getCompletionStatus());
        assertEquals(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS, saved.getBackfillStatus());
        assertEquals(8801L, saved.getBackfillExecutionId());
        verify(backfillService, never()).backfillCompletedProcess(any(MesTeamLeaderBatchRecordBackfillCommand.class));
    }

    private static MesProProcessPoolEventDO event() {
        return MesProProcessPoolEventDO.builder()
                .id(1001L)
                .routeId(7001L)
                .routeProcessId(5001L)
                .processId(6001L)
                .rawPayload("{\"outputQuantity\":80,\"pressure\":15}")
                .serverSubmitTime(LocalDateTime.of(2026, 8, 1, 9, 0))
                .build();
    }

    private static MesProcessPoolReportAllocationDO allocation(Long workOrderId, String quantity) {
        return MesProcessPoolReportAllocationDO.builder()
                .eventId(1001L)
                .reviewId(7001L)
                .leaderUserId(3001L)
                .activeOrderId(8101L)
                .workOrderId(workOrderId)
                .routeProcessId(5001L)
                .processId(6001L)
                .allocatedQuantity(new BigDecimal(quantity))
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_FIFO)
                .confirmedAt(LocalDateTime.of(2026, 8, 1, 9, 1))
                .build();
    }

    private static MesProWorkOrderDO workOrder(String quantity) {
        return MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .batchCode("BATCH-9001")
                .quantity(new BigDecimal(quantity))
                .build();
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
