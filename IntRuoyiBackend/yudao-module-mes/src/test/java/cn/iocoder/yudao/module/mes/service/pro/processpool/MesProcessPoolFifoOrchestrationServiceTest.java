package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolFifoAllocationLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolFifoAllocationLineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolQuantityFragmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProcessPoolFifoOrchestrationServiceTest {

    @Mock
    private MesProProcessPoolQuantityFragmentMapper quantityFragmentMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesProcessPoolFifoAllocationLineMapper allocationLineMapper;

    private MesProcessPoolFifoOrchestrationService orchestrationService;

    @BeforeEach
    void setUp() {
        MesProcessPoolFifoAllocationService allocationService =
                new MesProcessPoolFifoAllocationService(allocationLineMapper);
        orchestrationService = new MesProcessPoolFifoOrchestrationService(
                quantityFragmentMapper, workOrderMapper, allocationLineMapper, allocationService);
    }

    @Test
    void shouldLoadFormalAvailableOutputFragmentsAndProductionWorkOrdersBeforeFifoAllocation() {
        MesProProcessPoolQuantityFragmentDO fragment = outputFragment(2000L, "100", "100");
        MesProWorkOrderDO later = workOrder(20L, "WO-B", LocalDateTime.of(2026, 8, 2, 8, 0), "60");
        MesProWorkOrderDO earlier = workOrder(10L, "WO-A", LocalDateTime.of(2026, 8, 1, 8, 0), "60");
        when(quantityFragmentMapper.selectAvailableOutputListForUpdate(4000L)).thenReturn(List.of(fragment));
        when(workOrderMapper.selectListByIdsForUpdate(List.of(20L, 10L))).thenReturn(List.of(later, earlier));
        when(allocationLineMapper.selectListByTargetWorkOrderIdsAndRouteProcessIdForUpdate(List.of(20L, 10L), 5000L))
                .thenReturn(List.of());
        when(allocationLineMapper.selectListBySourceQuantityFragmentIdsForUpdate(List.of(2000L))).thenReturn(List.of());
        when(allocationLineMapper.insertBatch(anyCollection())).thenReturn(Boolean.TRUE);
        when(quantityFragmentMapper.updateAllocationProgress(2000L, new BigDecimal("100"),
                BigDecimal.ZERO, MesProProcessPoolQuantityFragmentDO.ALLOCATION_STATUS_ALLOCATED))
                .thenReturn(1);

        MesProcessPoolFifoAllocationResult result = orchestrationService.allocateAvailableOutput(
                MesProcessPoolFifoOrchestrationCommand.builder()
                        .allocationBatchNo("FIFO-ORCH-001")
                        .sourceProcessId(4000L)
                        .targetRouteProcessId(5000L)
                        .targetProcessId(4000L)
                        .targetWorkOrderIds(List.of(20L, 10L))
                        .build());

        assertEquals(List.of("WO-A", "WO-B"), result.getLines().stream()
                .map(MesProcessPoolFifoAllocationLineDO::getTargetWorkOrderCode).toList());
        assertAmount("60", result.getLines().get(0).getAllocatedQuantity());
        assertAmount("40", result.getLines().get(1).getAllocatedQuantity());
        verify(quantityFragmentMapper).selectAvailableOutputListForUpdate(4000L);
        verify(workOrderMapper).selectListByIdsForUpdate(List.of(20L, 10L));
        verify(quantityFragmentMapper).updateAllocationProgress(2000L, new BigDecimal("100"),
                BigDecimal.ZERO, MesProProcessPoolQuantityFragmentDO.ALLOCATION_STATUS_ALLOCATED);
    }

    @Test
    void shouldBlockWhenAnyTargetWorkOrderHasNoPlannedStartTime() {
        MesProProcessPoolQuantityFragmentDO fragment = outputFragment(2000L, "100", "100");
        MesProWorkOrderDO missing = workOrder(10L, "WO-MISSING", null, "60");
        when(quantityFragmentMapper.selectAvailableOutputListForUpdate(4000L)).thenReturn(List.of(fragment));
        when(workOrderMapper.selectListByIdsForUpdate(List.of(10L))).thenReturn(List.of(missing));
        when(allocationLineMapper.selectListByTargetWorkOrderIdsAndRouteProcessIdForUpdate(List.of(10L), 5000L))
                .thenReturn(List.of());

        ServiceException ex = assertThrows(ServiceException.class, () -> orchestrationService.allocateAvailableOutput(
                MesProcessPoolFifoOrchestrationCommand.builder()
                        .allocationBatchNo("FIFO-ORCH-MISSING")
                        .sourceProcessId(4000L)
                        .targetRouteProcessId(5000L)
                        .targetProcessId(4000L)
                        .targetWorkOrderIds(List.of(10L))
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_FIFO_WORK_ORDER_PLANNED_START_TIME_REQUIRED.getCode(),
                ex.getCode());
        verify(allocationLineMapper, never()).insertBatch(anyCollection());
        verify(quantityFragmentMapper, never()).updateAllocationProgress(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldBlockWhenFormalTargetWorkOrderIsMissing() {
        when(quantityFragmentMapper.selectAvailableOutputListForUpdate(4000L))
                .thenReturn(List.of(outputFragment(2000L, "100", "100")));
        when(workOrderMapper.selectListByIdsForUpdate(List.of(10L, 20L)))
                .thenReturn(List.of(workOrder(10L, "WO-A", LocalDateTime.of(2026, 8, 1, 8, 0), "60")));

        ServiceException ex = assertThrows(ServiceException.class, () -> orchestrationService.allocateAvailableOutput(
                MesProcessPoolFifoOrchestrationCommand.builder()
                        .allocationBatchNo("FIFO-ORCH-MISSING-WO")
                        .sourceProcessId(4000L)
                        .targetRouteProcessId(5000L)
                        .targetProcessId(4000L)
                        .targetWorkOrderIds(List.of(10L, 20L))
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_FIFO_CONTEXT_REQUIRED.getCode(), ex.getCode());
        verify(allocationLineMapper, never()).insertBatch(anyCollection());
    }

    private static MesProProcessPoolQuantityFragmentDO outputFragment(Long fragmentId,
                                                                       String totalQuantity,
                                                                       String availableQuantity) {
        return MesProProcessPoolQuantityFragmentDO.builder()
                .id(fragmentId)
                .poolId(100L)
                .eventId(1000L)
                .workOrderId(9000L)
                .routeId(7000L)
                .routeProcessId(3000L)
                .processId(4000L)
                .sourceQuantityType("OUTPUT")
                .totalQuantity(new BigDecimal(totalQuantity))
                .allocatedQuantity(BigDecimal.ZERO)
                .availableQuantity(new BigDecimal(availableQuantity))
                .allocationStatus(MesProProcessPoolQuantityFragmentDO.ALLOCATION_STATUS_AVAILABLE)
                .locked(Boolean.FALSE)
                .build();
    }

    private static MesProWorkOrderDO workOrder(Long id, String code,
                                               LocalDateTime plannedStartTime,
                                               String quantity) {
        return MesProWorkOrderDO.builder()
                .id(id)
                .code(code)
                .plannedStartTime(plannedStartTime)
                .quantity(new BigDecimal(quantity))
                .quantityProduced(BigDecimal.ZERO)
                .build();
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
