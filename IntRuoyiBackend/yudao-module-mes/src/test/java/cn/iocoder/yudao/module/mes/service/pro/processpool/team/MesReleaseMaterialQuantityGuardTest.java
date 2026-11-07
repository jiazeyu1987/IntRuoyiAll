package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesReleaseMaterialQuantityGuardTest {
    @Test void fourMaterialsAtTenEachAreTenProcessUnits() { assertDoesNotThrow(() -> check("10")); }
    @Test void oneMaterialOverTargetStillBlocksEvenWhenOtherMaterialsAreAtTarget() {
        assertThrows(MesReleaseFlowBlockerException.class, () -> check("11"));
    }
    private void check(String firstQuantity) {
        var allocationMapper = mock(MesProcessPoolReportAllocationMapper.class);
        var eventMapper = mock(MesProProcessPoolEventMapper.class);
        var service = new MesTeamLeaderActiveOrderReleaseGenerationService(null,null,null,null,null,null,
                allocationMapper,null,null,null,null,null,null,null);
        ReflectionTestUtils.setField(service,"productionEventMapper",eventMapper);
        var order = MesProcessPoolActiveOrderDO.builder().id(1L).workOrderId(2L).routeId(3L).build();
        var process = MesProcessPoolActiveOrderProcessSnapshotDO.builder().routeProcessId(4L).processId(5L)
                .plannedQuantitySnapshot(BigDecimal.TEN).productionConfigSnapshotJson("{\"outputMaterialIds\":[101,102,103,104]}").build();
        var allocations = new java.util.ArrayList<MesProcessPoolReportAllocationDO>();
        var events = new java.util.ArrayList<MesProProcessPoolEventDO>();
        for (long i=1; i<=4; i++) {
            BigDecimal qty = new BigDecimal(i==1 ? firstQuantity : "10");
            allocations.add(MesProcessPoolReportAllocationDO.builder().id(200+i).activeOrderId(1L).workOrderId(2L)
                    .routeProcessId(4L).processId(5L).eventId(300+i).allocatedQuantity(qty).build());
            events.add(MesProProcessPoolEventDO.builder().id(300+i).workOrderId(2L).routeId(3L).routeProcessId(4L)
                    .processId(5L).eventType("PRODUCTION_SUBMIT").reportOutputQuantity(qty)
                    .rawPayload("{\"materialDetails\":[{\"materialId\":"+(100+i)+",\"outputQuantity\":"+qty+"}]}").build());
        }
        when(allocationMapper.selectListByActiveOrderIds(List.of(1L))).thenReturn(allocations);
        when(eventMapper.selectProductionSubmitsByIdsForUpdate(List.of(301L,302L,303L,304L))).thenReturn(events);
        ReflectionTestUtils.invokeMethod(service,"requireNoProductionQuantityConflict",order,List.of(process));
    }
}
