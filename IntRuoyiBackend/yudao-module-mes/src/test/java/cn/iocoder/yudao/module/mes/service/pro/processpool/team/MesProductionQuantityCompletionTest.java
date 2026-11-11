package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MesProductionQuantityCompletionTest {
    @ParameterizedTest
    @CsvSource({"999,0", "1000,100", "1050,100", "1100,100"})
    void completionUsesBaseFloorRatherThanFactorCeiling(String actual, int expectedPercent) {
        var progress = readProgress(actual, false);
        assertEquals(0,new BigDecimal(expectedPercent).compareTo(progress.getProductionProgressPercent()));
        assertFalse(progress.isDoubleComplete(),"Production quantity never substitutes for required PQC completion");
    }

    @Test
    void historicalOverLimitCannotPassCompletionEvenWithConfirmedPqc() {
        var error = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> readProgress("1101", true));
        assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants
                .PRO_PROCESS_POOL_PRODUCTION_QUANTITY_EXCEEDED.getCode(), error.getCode());
    }

    private MesTeamLeaderActiveOrderCompletionProgress readProgress(String actual, boolean pqcConfirmed) {
        var snapshots=mock(MesProcessPoolActiveOrderProcessSnapshotMapper.class);
        var allocations=mock(MesProcessPoolReportAllocationMapper.class);
        var tasks=mock(MesPqcInspectionTaskMapper.class);
        var events=mock(MesProProcessPoolEventMapper.class);
        var port=new MesTeamLeaderActiveOrderCompletionProgressPortImpl(snapshots,allocations,tasks,events);
        ReflectionTestUtils.setField(port,"packagingAccess",mock(MesPackagingLeaderAccess.class));
        ReflectionTestUtils.setField(port,"pqcCoverageService",mock(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesOrderReleaseCompletenessService.class));
        var order=MesProcessPoolActiveOrderDO.builder().id(10L).leaderUserId(20L).workOrderId(30L).routeId(40L).routeVersionId(41L).activeStatus("ACTIVE").version(1).build();
        when(snapshots.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(MesProcessPoolActiveOrderProcessSnapshotDO.builder()
            .id(100L).activeOrderId(10L).workOrderId(30L).routeId(40L).routeVersionId(41L).routeProcessId(101L).processId(1L)
            .erpFixedQuantitySnapshot(new BigDecimal("1000")).productionQuantityFactorSnapshot(new BigDecimal("1.1"))
            .plannedQuantitySnapshot(new BigDecimal("1100")).productionConfigSnapshotJson("{\"outputMaterialIds\":[501]}").build()));
        when(allocations.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(MesProcessPoolReportAllocationDO.builder()
            .id(201L).eventId(401L).activeOrderId(10L).workOrderId(30L).routeProcessId(101L).processId(1L).allocatedQuantity(new BigDecimal(actual)).build()));
        when(events.selectProductionSubmitsByIdsForUpdate(anyList())).thenReturn(List.of(MesProProcessPoolEventDO.builder()
            .id(401L).eventType("PRODUCTION_SUBMIT").workOrderId(30L).routeId(40L).routeProcessId(101L).processId(1L)
            .reportOutputQuantity(new BigDecimal(actual)).rawPayload("{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":"+actual+"}]}").build()));
        when(tasks.selectEffectiveListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(
            cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO.builder()
                .id(301L).activeOrderId(10L).workOrderId(30L).routeId(40L).routeVersionId(41L)
                .qaProcessId(901L).regulationVersionId(99L).taskStatus(pqcConfirmed ? "CONFIRMED" : "SUBMITTED").build()));
        return port.read(20L,order);
    }

    @Test
    void listFlagsAnyOverLimitOutputIndependentlyOfConservativeProgress() {
        var order=MesProcessPoolActiveOrderDO.builder().id(10L).workOrderId(30L).routeId(40L).build();
        var snapshot=MesProcessPoolActiveOrderProcessSnapshotDO.builder().activeOrderId(10L).workOrderId(30L)
                .routeProcessId(101L).processId(1L).erpFixedQuantitySnapshot(new BigDecimal("1000"))
                .productionQuantityFactorSnapshot(new BigDecimal("1.1")).plannedQuantitySnapshot(new BigDecimal("1100"))
                .productionConfigSnapshotJson("{\"outputMaterialIds\":[501,502]}").build();
        var allocation=MesProcessPoolReportAllocationDO.builder().activeOrderId(10L).workOrderId(30L)
                .routeProcessId(101L).processId(1L).eventId(401L).allocatedQuantity(new BigDecimal("1000")).build();
        var event=MesProProcessPoolEventDO.builder().id(401L).eventType("PRODUCTION_SUBMIT")
                .workOrderId(30L).routeId(40L)
                .routeProcessId(101L).processId(1L).reportOutputQuantity(new BigDecimal("1000"))
                .rawPayload("{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":1101},{\"materialId\":502,\"outputQuantity\":1000}]}").build();
        List<MesTeamLeaderActiveOrderRow.ProcessRemainingQuantity> rows=ReflectionTestUtils.invokeMethod(
                MesTeamLeaderActiveOrderServiceImpl.class,"resolveProcessRemainingQuantities",order,List.of(snapshot),
                java.util.Map.of(),List.of(event),List.of(allocation));
        assertEquals(1,rows.size());
        assertTrue(rows.get(0).getQuantityConflict());
        assertEquals(0,BigDecimal.ONE.compareTo(rows.get(0).getOverageQuantity()));
    }
}
