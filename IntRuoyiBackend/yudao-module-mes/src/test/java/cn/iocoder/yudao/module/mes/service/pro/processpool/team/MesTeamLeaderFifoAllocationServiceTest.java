package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderFifoAllocationServiceTest {

    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock
    private MesTeamLeaderOrderProcessTargetService orderProcessTargetService;
    @Mock
    private MesWorkOrderAbnormalStateService abnormalStateService;

    private MesTeamLeaderFifoAllocationService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderFifoAllocationService(activeOrderMapper, workOrderMapper, allocationMapper,
                orderProcessTargetService, abnormalStateService);
    }

    @Test
    void shouldPreviewByActiveOrderJoinedAtAndRemainingQuantity() {
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(List.of(
                activeOrder(8101L, 9001L, "2026-07-31T08:00:00"),
                activeOrder(8102L, 9002L, "2026-07-31T09:00:00")));
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L, 9002L))).thenReturn(List.of(
                workOrder(9001L, "WO-9001", "200"),
                workOrder(9002L, "WO-9002", "200")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L, 9002L), 5001L, 6001L))
                .thenReturn(List.of(allocation(9001L, "150"), allocation(9002L, "170")));
        when(orderProcessTargetService.findTarget(activeOrder(8101L, 9001L, "2026-07-31T08:00:00"),
                5001L, 6001L)).thenReturn(Optional.of(target("200")));
        when(orderProcessTargetService.findTarget(activeOrder(8102L, 9002L, "2026-07-31T09:00:00"),
                5001L, 6001L)).thenReturn(Optional.of(target("200")));

        MesTeamLeaderReportAllocationPreview preview = service.previewFifoAllocation(
                MesTeamLeaderFifoAllocationReqBO.builder()
                        .leaderUserId(3001L)
                        .eventId(1001L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .confirmQuantity(new BigDecimal("80"))
                        .build());

        assertEquals(2, preview.getLines().size());
        assertEquals(8101L, preview.getLines().get(0).getActiveOrderId());
        assertEquals(9001L, preview.getLines().get(0).getWorkOrderId());
        assertAmount("50", preview.getLines().get(0).getAllocatedQuantity());
        assertEquals(8102L, preview.getLines().get(1).getActiveOrderId());
        assertEquals(9002L, preview.getLines().get(1).getWorkOrderId());
        assertAmount("30", preview.getLines().get(1).getAllocatedQuantity());
        assertAmount("80", preview.getTotalAllocatedQuantity());
    }

    @Test
    void shouldUseStableWorkOrderIdSortWhenJoinedAtTies() {
        LocalDateTime joinedAt = LocalDateTime.of(2026, 7, 31, 8, 0);
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(List.of(
                activeOrder(8102L, 9002L, joinedAt),
                activeOrder(8101L, 9001L, joinedAt)));
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L, 9002L))).thenReturn(List.of(
                workOrder(9001L, "WO-9001", "200"),
                workOrder(9002L, "WO-9002", "200")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L, 9002L), 5001L, 6001L))
                .thenReturn(List.of());
        when(orderProcessTargetService.findTarget(activeOrder(8101L, 9001L, joinedAt), 5001L, 6001L))
                .thenReturn(Optional.of(target("200")));


        MesTeamLeaderReportAllocationPreview preview = service.previewFifoAllocation(
                MesTeamLeaderFifoAllocationReqBO.builder()
                        .leaderUserId(3001L)
                        .eventId(1001L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .confirmQuantity(new BigDecimal("80"))
                        .build());

        assertEquals(List.of(9001L), preview.getLines().stream()
                .map(MesTeamLeaderReportAllocationPreviewLine::getWorkOrderId)
                .toList());
        assertAmount("80", preview.getLines().get(0).getAllocatedQuantity());
    }

    @Test
    void shouldBlockWhenActiveOrderRemainingQuantityIsNotEnough() {
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(List.of(
                activeOrder(8101L, 9001L, "2026-07-31T08:00:00")));
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(
                workOrder(9001L, "WO-9001", "200")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of(allocation(9001L, "150")));
        when(orderProcessTargetService.findTarget(activeOrder(8101L, 9001L, "2026-07-31T08:00:00"),
                5001L, 6001L)).thenReturn(Optional.of(target("200")));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.previewFifoAllocation(
                MesTeamLeaderFifoAllocationReqBO.builder()
                        .leaderUserId(3001L)
                        .eventId(1001L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .confirmQuantity(new BigDecimal("80"))
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_REMAINING_NOT_ENOUGH.getCode(),
                ex.getCode());
        verify(allocationMapper, never()).insertBatch(org.mockito.ArgumentMatchers.anyCollection());
    }

    @Test
    void shouldExcludeOpenAbnormalOrdersFromFifoAllocation() {
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(List.of(
                activeOrder(8101L, 9001L, "2026-07-31T08:00:00"),
                activeOrder(8102L, 9002L, "2026-07-31T09:00:00")));
        when(abnormalStateService.findOpenWorkOrderIds(List.of(9001L, 9002L))).thenReturn(Set.of(9001L));

        List<MesProcessPoolActiveOrderDO> result = service.sortedActiveOrders(3001L);

        assertEquals(List.of(9002L), result.stream().map(MesProcessPoolActiveOrderDO::getWorkOrderId).toList());
    }

    @Test
    void shouldPreviewRemainingQuantityFromPerProcessSnapshotTarget() {
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(List.of(
                activeOrder(8101L, 9001L, "2026-07-31T08:00:00")));
        when(workOrderMapper.selectListByIdsForUpdate(List.of(9001L))).thenReturn(List.of(
                workOrder(9001L, "WO-9001", "300")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(List.of(9001L), 5001L, 6001L))
                .thenReturn(List.of(allocation(9001L, "600")));
        when(orderProcessTargetService.findTarget(activeOrder(8101L, 9001L, "2026-07-31T08:00:00"),
                5001L, 6001L)).thenReturn(Optional.of(target("900")));

        MesTeamLeaderReportAllocationPreview preview = service.previewFifoAllocation(
                MesTeamLeaderFifoAllocationReqBO.builder()
                        .leaderUserId(3001L)
                        .eventId(1001L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .confirmQuantity(new BigDecimal("300"))
                        .build());

        assertEquals(1, preview.getLines().size());
        assertAmount("300", preview.getLines().get(0).getAllocatedQuantity());
        assertAmount("300", preview.getLines().get(0).getRemainingQuantityBeforeAllocation());
        verify(orderProcessTargetService).findTarget(activeOrder(8101L, 9001L, "2026-07-31T08:00:00"),
                5001L, 6001L);
    }

    @Test
    void shouldSkipActiveOrdersWithoutCurrentRouteProcessSnapshotDuringFifoPreview() {
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(List.of(
                activeOrder(35L, 980022L, "2026-08-07T10:25:34"),
                activeOrder(48L, 980019L, "2026-08-08T11:58:12")));
        when(workOrderMapper.selectListByIdsForUpdate(List.of(980022L, 980019L))).thenReturn(List.of(
                workOrder(980022L, "CODX-AO5-20260807-01", "10"),
                workOrder(980019L, "PQC-E2E-FS-20260804", "100")));
        when(allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(
                List.of(980022L, 980019L), 980645L, 922985L)).thenReturn(List.of());
        when(orderProcessTargetService.findTarget(activeOrder(35L, 980022L, "2026-08-07T10:25:34"),
                980645L, 922985L)).thenReturn(Optional.empty());
        when(orderProcessTargetService.findTarget(activeOrder(48L, 980019L, "2026-08-08T11:58:12"),
                980645L, 922985L)).thenReturn(Optional.of(target("100")));

        MesTeamLeaderReportAllocationPreview preview = service.previewFifoAllocation(
                MesTeamLeaderFifoAllocationReqBO.builder()
                        .leaderUserId(3001L)
                        .eventId(1001L)
                        .routeProcessId(980645L)
                        .processId(922985L)
                        .confirmQuantity(new BigDecimal("80"))
                        .build());

        assertEquals(1, preview.getLines().size());
        assertEquals(48L, preview.getLines().get(0).getActiveOrderId());
        assertEquals(980019L, preview.getLines().get(0).getWorkOrderId());
        assertAmount("80", preview.getLines().get(0).getAllocatedQuantity());
    }

    private static MesProcessPoolActiveOrderDO activeOrder(Long id, Long workOrderId, String joinedAt) {
        return activeOrder(id, workOrderId, LocalDateTime.parse(joinedAt));
    }

    private static MesProcessPoolActiveOrderDO activeOrder(Long id, Long workOrderId, LocalDateTime joinedAt) {
        return MesProcessPoolActiveOrderDO.builder()
                .id(id)
                .leaderUserId(3001L)
                .workOrderId(workOrderId)
                .activeStatus("ACTIVE")
                .joinedAt(joinedAt)
                .build();
    }

    private static MesProWorkOrderDO workOrder(Long id, String code, String quantity) {
        return MesProWorkOrderDO.builder()
                .id(id)
                .code(code)
                .quantity(new BigDecimal(quantity))
                .build();
    }

    private static MesProcessPoolReportAllocationDO allocation(Long workOrderId, String quantity) {
        return MesProcessPoolReportAllocationDO.builder()
                .activeOrderId(workOrderId.equals(9001L) ? 8101L : 8102L)
                .workOrderId(workOrderId)
                .routeProcessId(5001L)
                .processId(6001L)
                .allocatedQuantity(new BigDecimal(quantity))
                .build();
    }

    private static MesTeamLeaderOrderProcessTarget target(String plannedQuantity) {
        return new MesTeamLeaderOrderProcessTarget(5001L, 6001L, new BigDecimal("300"),
                new BigDecimal("3.000000"), new BigDecimal(plannedQuantity));
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
