package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesFifoOutputMaterialAllocationTest {
    private final MesProcessPoolActiveOrderMapper orders = mock(MesProcessPoolActiveOrderMapper.class);
    private final MesProWorkOrderMapper workOrders = mock(MesProWorkOrderMapper.class);
    private final MesProcessPoolReportAllocationMapper allocations = mock(MesProcessPoolReportAllocationMapper.class);
    private final MesTeamLeaderOrderProcessTargetService targets = mock(MesTeamLeaderOrderProcessTargetService.class);
    private final MesProProcessPoolEventMapper events = mock(MesProProcessPoolEventMapper.class);
    private final MesProcessPoolActiveOrderProcessSnapshotMapper snapshots = mock(MesProcessPoolActiveOrderProcessSnapshotMapper.class);
    private MesTeamLeaderFifoAllocationService service;
    private MesProProcessPoolEventDO current;
    private MesProcessPoolActiveOrderDO order;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderFifoAllocationService(orders, workOrders, allocations, targets,
                mock(MesWorkOrderAbnormalStateService.class), events, snapshots);
        order = MesProcessPoolActiveOrderDO.builder().id(81L).leaderUserId(30L)
                .workOrderId(91L).routeId(40L).activeStatus("ACTIVE").businessStatus("ACTIVE").build();
        when(orders.selectActiveListByLeader(30L)).thenReturn(List.of(order));
        when(workOrders.selectListByIdsForUpdate(List.of(91L))).thenReturn(List.of(
                MesProWorkOrderDO.builder().id(91L).code("WO-B").quantity(new BigDecimal("100")).build()));
        when(targets.findUniqueTargetForProcess(order, 60L)).thenReturn(Optional.of(
                new MesTeamLeaderOrderProcessTarget(50L, 60L, new BigDecimal("100"), BigDecimal.ONE, new BigDecimal("100"))));
        when(snapshots.selectByActiveOrderAndProcess(81L, 50L, 60L)).thenReturn(
                MesProcessPoolActiveOrderProcessSnapshotDO.builder().activeOrderId(81L).workOrderId(91L)
                        .routeId(40L).routeProcessId(50L).processId(60L).plannedQuantitySnapshot(new BigDecimal("100"))
                        .productionConfigSnapshotJson("{\"outputMaterialIds\":[501,502]}").build());
        current = event(10L, "100", "{\"materialId\":502,\"outputQuantity\":100}");
        when(events.selectById(10L)).thenReturn(current);
    }

    @Test
    void fifoCanAllocateSecondMaterialAfterFirstMaterialReachesPlan() {
        previous("100", "{\"materialId\":501,\"outputQuantity\":100}");
        assertAmount("100", preview().getTotalAllocatedQuantity());
    }

    @Test
    void fifoUsesReportedMaterialRemainingInsteadOfConservativeMinimumProgress() {
        current.setRawPayload("{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":100}]}");
        previous("20", "{\"materialId\":501,\"outputQuantity\":80},{\"materialId\":502,\"outputQuantity\":20}");
        assertAmount("20", preview().getTotalAllocatedQuantity());
    }

    @Test
    void fifoScalesUnequalCurrentMaterialsBeforeComputingAvailablePool() {
        current.setRawPayload("{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":150},{\"materialId\":502,\"outputQuantity\":100}]}");
        previous("25", "{\"materialId\":501,\"outputQuantity\":25}");
        assertAmount("50", preview().getTotalAllocatedQuantity());
    }

    @Test
    void fifoSkipsAnotherRouteSharingProcessAndAllocatesMatchingOrder() {
        MesProcessPoolActiveOrderDO earlier = MesProcessPoolActiveOrderDO.builder().id(80L).leaderUserId(30L)
                .workOrderId(90L).routeId(41L).activeStatus("ACTIVE").businessStatus("ACTIVE").build();
        when(orders.selectActiveListByLeader(30L)).thenReturn(List.of(earlier, order));
        when(workOrders.selectListByIdsForUpdate(anyCollection())).thenReturn(List.of(
                MesProWorkOrderDO.builder().id(90L).code("WO-A").quantity(new BigDecimal("100")).build(),
                MesProWorkOrderDO.builder().id(91L).code("WO-B").quantity(new BigDecimal("100")).build()));
        when(targets.findUniqueTargetForProcess(earlier, 60L)).thenReturn(Optional.of(
                new MesTeamLeaderOrderProcessTarget(51L, 60L, new BigDecimal("100"), BigDecimal.ONE,
                        new BigDecimal("100"))));
        when(snapshots.selectByActiveOrderAndProcess(80L, 51L, 60L)).thenReturn(
                MesProcessPoolActiveOrderProcessSnapshotDO.builder().activeOrderId(80L).workOrderId(90L)
                        .routeId(41L).routeProcessId(51L).processId(60L).plannedQuantitySnapshot(new BigDecimal("100"))
                        .productionConfigSnapshotJson("{\"outputMaterialIds\":[501,502]}").build());

        MesTeamLeaderReportAllocationPreview result = preview();

        assertAmount("100", result.getTotalAllocatedQuantity());
        assertAmount("0", result.getUnallocatedQuantity());
        assertEquals(List.of(81L), result.getLines().stream()
                .map(MesTeamLeaderReportAllocationPreviewLine::getActiveOrderId).toList());
    }

    @Test
    void fifoMatchingOrderWithInvalidMaterialSourceStillFails() {
        current.setRawPayload("{\"materialDetails\":[{\"materialId\":999,\"outputQuantity\":100}]}");

        ServiceException error = assertThrows(ServiceException.class, this::preview);

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING.getCode(),
                error.getCode());
    }

    @Test
    void fifoMissingFormalEventCannotReturnSuccessfulEmptyPreview() {
        when(events.selectById(10L)).thenReturn(null);
        ServiceException error = assertThrows(ServiceException.class, this::preview);
        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), error.getCode());
        verify(orders, never()).selectActiveListByLeader(anyLong());
    }

    @Test
    void fifoEventWithMissingRouteCannotReturnSuccessfulEmptyPreview() {
        current.setRouteId(null);
        ServiceException error = assertThrows(ServiceException.class, this::preview);
        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), error.getCode());
        verify(orders, never()).selectActiveListByLeader(anyLong());
    }

    private MesTeamLeaderReportAllocationPreview preview() {
        return service.previewFifoAllocation(MesTeamLeaderFifoAllocationReqBO.builder().leaderUserId(30L)
                .eventId(10L).processId(60L).routeProcessId(50L).confirmQuantity(new BigDecimal("100"))
                .excludedEventId(10L).build());
    }

    private void previous(String poolQuantity, String details) {
        when(allocations.selectListByActiveOrderIdsAndProcessForUpdate(List.of(81L), 60L)).thenReturn(List.of(
                MesProcessPoolReportAllocationDO.builder().eventId(11L).activeOrderId(81L).workOrderId(91L)
                        .routeProcessId(50L).processId(60L).allocatedQuantity(new BigDecimal(poolQuantity)).build()));
        when(events.selectBatchIds(anyCollection())).thenReturn(List.of(event(11L, poolQuantity, details)));
    }

    private static MesProProcessPoolEventDO event(Long id, String poolQuantity, String details) {
        return MesProProcessPoolEventDO.builder().id(id).workOrderId(91L).routeId(40L).routeProcessId(50L)
                .processId(60L).eventType("PRODUCTION_SUBMIT").reportOutputQuantity(new BigDecimal(poolQuantity))
                .rawPayload("{\"materialDetails\":[" + details + "]}").build();
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
