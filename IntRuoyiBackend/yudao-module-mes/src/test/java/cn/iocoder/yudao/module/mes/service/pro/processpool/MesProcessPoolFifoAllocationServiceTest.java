package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolFifoAllocationLineDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolFifoAllocationLineMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProcessPoolFifoAllocationServiceTest {

    @Mock
    private MesProcessPoolFifoAllocationLineMapper allocationLineMapper;

    private MesProcessPoolFifoAllocationService allocationService;

    @BeforeEach
    void setUp() {
        allocationService = new MesProcessPoolFifoAllocationService(allocationLineMapper);
    }

    @Test
    void shouldAllocateWorkOrdersByPlannedStartTime() {
        MesProcessPoolAllocatableQuantityFragment fragment = fragment(100L, 1000L, 2000L, "100");
        MesProcessPoolFifoTargetWorkOrder later = workOrder(20L, "WO-B",
                LocalDateTime.of(2026, 8, 2, 8, 0), "60");
        MesProcessPoolFifoTargetWorkOrder earlier = workOrder(10L, "WO-A",
                LocalDateTime.of(2026, 8, 1, 8, 0), "60");
        when(allocationLineMapper.selectListBySourceQuantityFragmentIdsForUpdate(List.of(2000L))).thenReturn(List.of());
        when(allocationLineMapper.insertBatch(anyCollection())).thenReturn(Boolean.TRUE);

        MesProcessPoolFifoAllocationResult result = allocationService.allocate(
                MesProcessPoolFifoAllocationCommand.of("F7-ALLOC-001", List.of(fragment), List.of(later, earlier)));

        List<MesProcessPoolFifoAllocationLineDO> lines = result.getLines();
        assertEquals(2, lines.size());
        assertEquals(List.of("WO-A", "WO-B"), lines.stream()
                .map(MesProcessPoolFifoAllocationLineDO::getTargetWorkOrderCode).toList());
        assertAmount("60", lines.get(0).getAllocatedQuantity());
        assertAmount("40", lines.get(1).getAllocatedQuantity());
        assertEquals(10L, lines.get(0).getTargetWorkOrderId());
        assertEquals(20L, lines.get(1).getTargetWorkOrderId());
        assertAmount("100", result.getTotalAllocatedQuantity());
    }

    @Test
    void shouldBlockWhenPlannedStartTimeIsMissing() {
        MesProcessPoolAllocatableQuantityFragment fragment = fragment(100L, 1000L, 2000L, "100");
        MesProcessPoolFifoTargetWorkOrder missingPlannedStartTime = workOrder(10L, "WO-MISSING", null, "60");

        ServiceException ex = assertThrows(ServiceException.class, () -> allocationService.allocate(
                MesProcessPoolFifoAllocationCommand.of("F7-ALLOC-002",
                        List.of(fragment), List.of(missingPlannedStartTime))));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_FIFO_WORK_ORDER_PLANNED_START_TIME_REQUIRED.getCode(),
                ex.getCode());
        verify(allocationLineMapper, never()).insertBatch(anyCollection());
    }

    @Test
    void shouldBlockWhenPlannedStartTimeTieNeedsSecondarySortRule() {
        MesProcessPoolAllocatableQuantityFragment fragment = fragment(100L, 1000L, 2000L, "100");
        LocalDateTime plannedStartTime = LocalDateTime.of(2026, 8, 1, 8, 0);
        MesProcessPoolFifoTargetWorkOrder first = workOrder(10L, "WO-A", plannedStartTime, "60");
        MesProcessPoolFifoTargetWorkOrder second = workOrder(20L, "WO-B", plannedStartTime, "60");

        ServiceException ex = assertThrows(ServiceException.class, () -> allocationService.allocate(
                MesProcessPoolFifoAllocationCommand.of("F7-ALLOC-TIE", List.of(fragment), List.of(first, second))));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_FIFO_WORK_ORDER_PLANNED_START_TIME_DUPLICATE.getCode(),
                ex.getCode());
        verify(allocationLineMapper, never()).insertBatch(anyCollection());
    }

    @Test
    void shouldPersistAllocationLinesFromEventFragmentsToWorkOrder() {
        MesProcessPoolAllocatableQuantityFragment first = fragment(100L, 1000L, 2000L, "50");
        MesProcessPoolAllocatableQuantityFragment second = fragment(100L, 1001L, 2001L, "40");
        MesProcessPoolFifoTargetWorkOrder target = workOrder(10L, "WO-A",
                LocalDateTime.of(2026, 8, 1, 8, 0), "70");
        when(allocationLineMapper.selectListBySourceQuantityFragmentIdsForUpdate(List.of(2000L, 2001L)))
                .thenReturn(List.of());
        when(allocationLineMapper.insertBatch(anyCollection())).thenReturn(Boolean.TRUE);

        allocationService.allocate(MesProcessPoolFifoAllocationCommand.of("F7-ALLOC-003",
                List.of(first, second), List.of(target)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MesProcessPoolFifoAllocationLineDO>> captor =
                ArgumentCaptor.forClass(Collection.class);
        verify(allocationLineMapper).insertBatch(captor.capture());
        List<MesProcessPoolFifoAllocationLineDO> lines = List.copyOf(captor.getValue());
        assertEquals(2, lines.size());
        assertTraceLine(lines.get(0), 100L, 1000L, 2000L, 10L, "WO-A", "50", "50");
        assertTraceLine(lines.get(1), 100L, 1001L, 2001L, 10L, "WO-A", "40", "20");
    }

    private static MesProcessPoolAllocatableQuantityFragment fragment(Long poolId, Long eventId,
                                                                      Long fragmentId, String quantity) {
        return MesProcessPoolAllocatableQuantityFragment.builder()
                .processPoolId(poolId)
                .sourceEventId(eventId)
                .sourceQuantityFragmentId(fragmentId)
                .sourceRouteProcessId(3000L)
                .sourceProcessId(4000L)
                .quantity(new BigDecimal(quantity))
                .build();
    }

    private static MesProcessPoolFifoTargetWorkOrder workOrder(Long workOrderId, String code,
                                                               LocalDateTime plannedStartTime, String requiredQuantity) {
        return MesProcessPoolFifoTargetWorkOrder.builder()
                .workOrderId(workOrderId)
                .workOrderCode(code)
                .plannedStartTime(plannedStartTime)
                .targetRouteProcessId(5000L + workOrderId)
                .targetProcessId(4000L)
                .requiredQuantity(new BigDecimal(requiredQuantity))
                .alreadyAllocatedQuantity(BigDecimal.ZERO)
                .build();
    }

    private static void assertTraceLine(MesProcessPoolFifoAllocationLineDO line, Long poolId, Long eventId,
                                        Long fragmentId, Long targetWorkOrderId, String targetWorkOrderCode,
                                        String sourceQuantity, String allocatedQuantity) {
        assertNotNull(line);
        assertEquals(poolId, line.getProcessPoolId());
        assertEquals(eventId, line.getSourceEventId());
        assertEquals(fragmentId, line.getSourceQuantityFragmentId());
        assertEquals(targetWorkOrderId, line.getTargetWorkOrderId());
        assertEquals(targetWorkOrderCode, line.getTargetWorkOrderCode());
        assertEquals(4000L, line.getTargetProcessId());
        assertAmount(sourceQuantity, line.getSourceFragmentQuantity());
        assertAmount(allocatedQuantity, line.getAllocatedQuantity());
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
