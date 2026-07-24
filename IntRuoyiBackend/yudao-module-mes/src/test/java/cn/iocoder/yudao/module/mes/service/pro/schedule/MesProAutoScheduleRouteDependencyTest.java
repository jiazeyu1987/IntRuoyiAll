package cn.iocoder.yudao.module.mes.service.pro.schedule;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.service.pro.schedule.component.ScheduleTopologyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProAutoScheduleRouteDependencyTest {

    private final MesProAutoScheduleServiceImpl service = new MesProAutoScheduleServiceImpl();
    private final SchedulePlanner planner = new SchedulePlanner();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "scheduleTopologyResolver", new ScheduleTopologyResolver());
    }

    @Test
    void routeDependency_shouldOrderTreeAndBuildDirectLinks() throws Exception {
        SchedulePlanner.ScheduleComputation computation = newScheduleComputation();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(501L)
                .workOrderId(601L)
                .build();
        List<MesProScheduleOrderProcessDO> snapshots = List.of(
                snapshot(11L, 101L, null, true, 1),
                snapshot(12L, 102L, 11L, false, 2),
                snapshot(13L, 103L, 11L, false, 3),
                snapshot(14L, 104L, 13L, false, 4));
        computation.scheduleOrders = List.of(scheduleOrder);
        computation.scheduleOrderProcessesByOrderId = Map.of(501L, snapshots);

        List<MesProRouteProcessDO> ordered = ReflectionTestUtils.invokeMethod(
                service,
                "orderRouteProcessesByDependency",
                computation,
                601L,
                List.of(
                        routeProcess(14L, 104L, 4),
                        routeProcess(12L, 102L, 2),
                        routeProcess(11L, 101L, 1),
                        routeProcess(13L, 103L, 3)));

        assertEquals(List.of(11L, 12L, 13L, 14L),
                ordered.stream().map(MesProRouteProcessDO::getId).toList());

        List<SchedulePlanner.LinkPlan> links = planner.buildLinkPlans(computation);
        List<String> pairs = new ArrayList<>();
        for (SchedulePlanner.LinkPlan link : links) {
            pairs.add(link.sourceProcessId + "->" + link.targetProcessId);
        }
        assertEquals(List.of("101->102", "101->103", "103->104"), pairs);
    }

    @Test
    void routeDependency_shouldNotInferDependencyFromSortWhenTopologySnapshotMissing() throws Exception {
        SchedulePlanner.ScheduleComputation computation = newScheduleComputation();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(501L)
                .workOrderId(601L)
                .build();
        List<MesProScheduleOrderProcessDO> snapshots = List.of(
                snapshotWithoutTopology(11L, 101L, 1),
                snapshotWithoutTopology(12L, 102L, 2));
        computation.scheduleOrders = List.of(scheduleOrder);
        computation.scheduleOrderProcessesByOrderId = Map.of(501L, snapshots);

        List<SchedulePlanner.LinkPlan> links = planner.buildLinkPlans(computation);
        assertEquals(List.of(), links);
    }

    private SchedulePlanner.ScheduleComputation newScheduleComputation() {
        return new SchedulePlanner.ScheduleComputation();
    }

    private MesProScheduleOrderProcessDO snapshot(Long routeProcessId,
                                                  Long processId,
                                                  Long predecessorRouteProcessId,
                                                  boolean root,
                                                  int sort) {
        return MesProScheduleOrderProcessDO.builder()
                .routeProcessId(routeProcessId)
                .processId(processId)
                .predecessorRouteProcessId(predecessorRouteProcessId)
                .rootProcessFlag(root)
                .sort(sort)
                .build();
    }

    private MesProScheduleOrderProcessDO snapshotWithoutTopology(Long routeProcessId,
                                                                 Long processId,
                                                                 int sort) {
        return MesProScheduleOrderProcessDO.builder()
                .routeProcessId(routeProcessId)
                .processId(processId)
                .sort(sort)
                .enabled(Boolean.TRUE)
                .build();
    }

    private MesProRouteProcessDO routeProcess(Long id, Long processId, int sort) {
        return MesProRouteProcessDO.builder()
                .id(id)
                .routeId(701L)
                .processId(processId)
                .sort(sort)
                .build();
    }
}
