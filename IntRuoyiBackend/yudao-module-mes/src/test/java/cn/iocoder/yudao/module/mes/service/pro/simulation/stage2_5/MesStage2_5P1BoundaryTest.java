package cn.iocoder.yudao.module.mes.service.pro.simulation.stage2_5;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesStage2_5P1BoundaryTest {
    @Test
    void p2ReadsEveryFormalBindingWithoutSimulationTags() throws Exception {
        var constructor = MesStage2_5BackfillBatchExecutionSimulationServiceImpl.class.getConstructors()[0];
        Map<Class<?>, Object> dependencies = new LinkedHashMap<>();
        Object[] arguments = java.util.Arrays.stream(constructor.getParameterTypes())
                .map(type -> dependencies.computeIfAbsent(type, key -> mock(key))).toArray();
        var service = (MesStage2_5BackfillBatchExecutionSimulationServiceImpl) constructor.newInstance(arguments);
        var mapper = (MesProcessPoolActiveOrderPickListBindingMapper) dependencies.get(MesProcessPoolActiveOrderPickListBindingMapper.class);
        var first = cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO.builder()
                .id(1L).activeOrderId(81L).workOrderId(91L).pickListId(101L).bindingVersion(1)
                .bindingStatus("BOUND").sourceSnapshotHash("first-source").build();
        var second = cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO.builder()
                .id(2L).activeOrderId(81L).workOrderId(91L).pickListId(102L).bindingVersion(1)
                .bindingStatus("BOUND").sourceSnapshotHash("second-source").build();
        when(mapper.selectListByActiveOrderId(81L)).thenReturn(java.util.List.of(first, second));
        var order = MesProcessPoolActiveOrderDO.builder().id(81L).workOrderId(91L).build();
        Object bindings = org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "requireBindings", order);
        assertEquals(java.util.List.of(first, second), bindings);
    }

    @Test
    void p2ReadsFormalCompletionSourcesWithoutRequiringP1MaterialBindings() throws Exception {
        var constructor = MesStage2_5BackfillBatchExecutionSimulationServiceImpl.class.getConstructors()[0];
        Map<Class<?>, Object> dependencies = new LinkedHashMap<>();
        Object[] arguments = java.util.Arrays.stream(constructor.getParameterTypes())
                .map(type -> dependencies.computeIfAbsent(type, key -> mock(key))).toArray();
        var service = (MesStage2_5BackfillBatchExecutionSimulationServiceImpl) constructor.newInstance(arguments);
        var orders = (MesProcessPoolActiveOrderMapper) dependencies.get(MesProcessPoolActiveOrderMapper.class);
        var workOrders = (MesProWorkOrderMapper) dependencies.get(MesProWorkOrderMapper.class);
        var bindings = (MesProcessPoolActiveOrderPickListBindingMapper) dependencies.get(MesProcessPoolActiveOrderPickListBindingMapper.class);
        var completion = (MesTeamLeaderActiveOrderCompletionService) dependencies.get(MesTeamLeaderActiveOrderCompletionService.class);
        var order = MesProcessPoolActiveOrderDO.builder().id(81L).leaderUserId(1L).workOrderId(91L)
                .routeId(11L).routeVersionId(12L).simulated(true).simulationStage("STAGE1")
                .simulationRunId("P1-81").activeStatus("ACTIVE").version(0).build();
        order.setTenantId(1L);
        var workOrder = MesProWorkOrderDO.builder().id(91L).productId(21L).quantity(BigDecimal.TEN)
                .code("WO-91").orderSourceCode("ERP-91").batchCode("B-91").build();
        workOrder.setTenantId(1L);
        when(orders.selectByIdForUpdate(81L)).thenReturn(order);
        when(workOrders.selectById(91L)).thenReturn(workOrder);
        var missingSource = new IllegalStateException("FORMAL_SOURCE_MISSING");
        when(completion.complete(eq(1L), any())).thenThrow(missingSource);
        TenantContextHolder.setTenantId(1L);
        try {
            assertSame(missingSource, assertThrows(IllegalStateException.class, () -> service.simulate(
                    MesStage2_5BackfillBatchExecutionSimulationCommand.validate("P2-81", 81L, 0, 1L))));
            verifyNoInteractions(bindings);
            verify(orders, never()).deleteById(any(java.io.Serializable.class));
        } finally {
            TenantContextHolder.clear();
        }
    }
    @Test
    void activeOrderBackfillSnapshotDoesNotRequireLegacyDossierTasks() throws Exception {
        var constructor = MesStage2_5BackfillBatchExecutionSimulationServiceImpl.class.getConstructors()[0];
        Map<Class<?>, Object> dependencies = new LinkedHashMap<>();
        Object[] arguments = java.util.Arrays.stream(constructor.getParameterTypes())
                .map(type -> dependencies.computeIfAbsent(type, key -> mock(key))).toArray();
        var service = (MesStage2_5BackfillBatchExecutionSimulationServiceImpl) constructor.newInstance(arguments);
        var order = MesProcessPoolActiveOrderDO.builder().id(81L).workOrderId(91L)
                .routeId(11L).routeVersionId(12L).build();
        var workOrder = MesProWorkOrderDO.builder().id(91L).code("WO-91").orderSourceCode("ERP-91").build();
        var receipt = cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO.builder()
                .id(31L).sourceSnapshotHash("formal-source").hasActualLoss(false).build();
        var backfill = new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesCompletionBackfillReceipt()
                .setBatchRecordId(41L).setProcessInspectionId(42L);
        var batch = new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRespVO()
                .setId(51L).setWorkOrderId(91L).setRouteId(11L).setBatchCode("B-91");
        Map<String, Object> snapshot = org.springframework.test.util.ReflectionTestUtils.invokeMethod(service,
                "buildSnapshot", MesStage2_5BackfillBatchExecutionSimulationCommand.validate("P2-81",81L,0,1L),
                order, workOrder, java.util.List.of(), java.util.List.of(), receipt, batch, backfill);
        assertNotNull(snapshot);
        assertEquals("activeOrderCompletionBackfill.v1", snapshot.get("sourceInputContract"));
        assertEquals("BACKFILL_COMPLETED", snapshot.get("status"));
        assertFalse(snapshot.containsKey("specialNodeUploadStatus"));
        assertEquals(java.util.List.of(), snapshot.get("optionalLossReportLinks"));
        assertTrue(snapshot.get("batchRecordLinks").toString().contains("41"));
        assertTrue(snapshot.get("processInspectionLinks").toString().contains("42"));
        assertTrue(snapshot.get("sourceHash").toString().contains("formal-source"));
        verifyNoInteractions(dependencies.get(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper.class));
        backfill.setProcessInspectionId(null);
        assertThrows(IllegalStateException.class, () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(service,
                "buildSnapshot", MesStage2_5BackfillBatchExecutionSimulationCommand.validate("P2-81",81L,0,1L),
                order,workOrder,java.util.List.of(),java.util.List.of(),receipt,batch,backfill));
    }

}
