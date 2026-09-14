package cn.iocoder.yudao.module.mes.service.pro.schedule.component;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ScheduleTopologyResolverMultiPredecessorTest {

    @Test
    void validateAndOrder_shouldAcceptMergeWithAllPredecessors() {
        ScheduleTopologyResolver resolver = new ScheduleTopologyResolver();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder().id(900L).build();
        List<MesProRouteProcessDO> routeProcesses = List.of(
                routeProcess(1L, 1), routeProcess(2L, 2), routeProcess(3L, 3));
        List<MesProScheduleOrderProcessDO> snapshotProcesses = List.of(
                snapshot(11L, 1L, 1, "[]", true),
                snapshot(12L, 2L, 2, "[]", true),
                snapshot(13L, 3L, 3, "[1,2]", false));

        assertNull(resolver.validateRouteProcessTopologySnapshot(
                scheduleOrder, 901L, snapshotProcesses, routeProcesses));
        assertEquals(List.of(1L, 2L, 3L), resolver.orderRouteProcessesByDependency(
                scheduleOrder, 901L, snapshotProcesses, routeProcesses)
                .stream().map(MesProRouteProcessDO::getId).toList());
    }

    private MesProRouteProcessDO routeProcess(Long id, Integer sort) {
        return MesProRouteProcessDO.builder().id(id).processId(id + 100).sort(sort).build();
    }

    private MesProScheduleOrderProcessDO snapshot(Long id, Long routeProcessId, Integer sort,
                                                   String predecessorIdsJson, boolean root) {
        return MesProScheduleOrderProcessDO.builder()
                .id(id).routeProcessId(routeProcessId).processId(routeProcessId + 100).sort(sort)
                .predecessorRouteProcessIdsJson(predecessorIdsJson).rootProcessFlag(root)
                .enabled(Boolean.TRUE).remainingQuantity(BigDecimal.ONE).plannedQuantity(BigDecimal.ONE)
                .build();
    }
}
