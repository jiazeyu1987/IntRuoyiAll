package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolFifoAllocationLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolQuantityFragmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolFifoAllocationLineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesReportAllocationQuantityFragmentServiceTest {

    @Mock private MesProProcessPoolQuantityFragmentMapper fragmentMapper;
    @Mock private MesProcessPoolFifoAllocationLineMapper lineMapper;
    @Mock private MesProWorkOrderMapper workOrderMapper;

    private MesReportAllocationQuantityFragmentService service;

    @BeforeEach
    void setUp() {
        service = new MesReportAllocationQuantityFragmentService(fragmentMapper, lineMapper, workOrderMapper);
    }

    @Test
    void shouldRebuildCurrentFragmentLinesInAllocationAndFragmentOrder() {
        MesProProcessPoolEventDO event = event();
        List<MesProProcessPoolQuantityFragmentDO> fragments = List.of(
                fragment(6101L, "60"), fragment(6102L, "50"));
        List<MesProcessPoolFifoAllocationLineDO> oldLines = List.of(
                MesProcessPoolFifoAllocationLineDO.builder().id(6201L).sourceEventId(1001L)
                        .lifecycleStatus("CURRENT").build());
        when(fragmentMapper.selectOutputListByProductionSubmitEventIdForUpdate(1001L)).thenReturn(fragments);
        when(lineMapper.selectListBySourceEventIdForUpdate(1001L)).thenReturn(oldLines);
        when(workOrderMapper.selectListByIds(List.of(9001L, 9003L))).thenReturn(List.of(
                workOrder(9001L, "A"), workOrder(9003L, "C")));
        when(lineMapper.supersedeCurrentRows(List.of(6201L), 2)).thenReturn(1);
        when(lineMapper.insertBatch(anyCollection())).thenReturn(true);
        when(fragmentMapper.updateById(any(MesProProcessPoolQuantityFragmentDO.class))).thenReturn(1);

        service.rebuildForVersion(event, 2, List.of(
                allocation(7101L, 8101L, 9001L, 5101L, "80"),
                allocation(7102L, 8103L, 9003L, 5301L, "20")));

        ArgumentCaptor<Collection<MesProcessPoolFifoAllocationLineDO>> lines =
                ArgumentCaptor.forClass(Collection.class);
        verify(lineMapper).insertBatch(lines.capture());
        List<MesProcessPoolFifoAllocationLineDO> saved = List.copyOf(lines.getValue());
        assertEquals(3, saved.size());
        assertLine(saved.get(0), 6101L, 9001L, "60");
        assertLine(saved.get(1), 6102L, 9001L, "20");
        assertLine(saved.get(2), 6102L, 9003L, "20");
        saved.forEach(line -> {
            assertEquals(2, line.getReportAllocationVersion());
            assertEquals("CURRENT", line.getLifecycleStatus());
        });
        assertAmount("60", fragments.get(0).getAllocatedQuantity());
        assertAmount("0", fragments.get(0).getAvailableQuantity());
        assertAmount("40", fragments.get(1).getAllocatedQuantity());
        assertAmount("10", fragments.get(1).getAvailableQuantity());
    }

    @Test
    void insufficientFormalFragmentsMustFailBeforeSupersedingCurrentLines() {
        MesProProcessPoolEventDO event = event();
        when(fragmentMapper.selectOutputListByProductionSubmitEventIdForUpdate(1001L))
                .thenReturn(List.of(fragment(6101L, "50")));
        when(lineMapper.selectListBySourceEventIdForUpdate(1001L)).thenReturn(List.of());
        when(workOrderMapper.selectListByIds(List.of(9001L))).thenReturn(List.of(workOrder(9001L, "A")));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.rebuildForVersion(event, 2,
                List.of(allocation(7101L, 8101L, 9001L, 5101L, "80"))));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_TOTAL_MISMATCH.getCode(), ex.getCode());
        verify(lineMapper, never()).supersedeCurrentRows(anyCollection(), any());
        verify(lineMapper, never()).insertBatch(anyCollection());
        verify(fragmentMapper, never()).updateById(any(MesProProcessPoolQuantityFragmentDO.class));
    }

    private static MesProProcessPoolEventDO event() {
        return MesProProcessPoolEventDO.builder().id(1001L).poolId(2001L).routeProcessId(5001L)
                .processId(6001L).build();
    }

    private static MesProProcessPoolQuantityFragmentDO fragment(Long id, String quantity) {
        BigDecimal amount = new BigDecimal(quantity);
        return MesProProcessPoolQuantityFragmentDO.builder().id(id).poolId(2001L).eventId(1001L)
                .productionSubmitEventId(1001L).routeProcessId(5001L).processId(6001L)
                .sourceQuantityType(MesProProcessPoolQuantityFragmentDO.SOURCE_QUANTITY_TYPE_OUTPUT)
                .totalQuantity(amount).allocatedQuantity(BigDecimal.ZERO).availableQuantity(amount)
                .allocationStatus("AVAILABLE").build();
    }

    private static MesProcessPoolReportAllocationDO allocation(Long id, Long activeOrderId, Long workOrderId,
                                                                Long routeProcessId, String quantity) {
        return MesProcessPoolReportAllocationDO.builder().id(id).eventId(1001L).activeOrderId(activeOrderId)
                .workOrderId(workOrderId).routeProcessId(routeProcessId).processId(6001L)
                .allocatedQuantity(new BigDecimal(quantity)).lifecycleStatus("CURRENT").build();
    }

    private static MesProWorkOrderDO workOrder(Long id, String code) {
        return MesProWorkOrderDO.builder().id(id).code(code).build();
    }

    private static void assertLine(MesProcessPoolFifoAllocationLineDO line, Long fragmentId, Long workOrderId,
                                   String quantity) {
        assertEquals(fragmentId, line.getSourceQuantityFragmentId());
        assertEquals(workOrderId, line.getTargetWorkOrderId());
        assertAmount(quantity, line.getAllocatedQuantity());
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
