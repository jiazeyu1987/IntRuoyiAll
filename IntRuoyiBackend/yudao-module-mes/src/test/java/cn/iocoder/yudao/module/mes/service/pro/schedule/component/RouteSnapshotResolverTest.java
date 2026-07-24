package cn.iocoder.yudao.module.mes.service.pro.schedule.component;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class RouteSnapshotResolverTest {

    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;

    private RouteSnapshotResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new RouteSnapshotResolver(routeProcessService, routeProcessFlowEdgeMapper);
    }

    @Test
    void resolve_shouldUseSnapshotProcessIdentityWhenRouteProcessProcessIdDrifted() {
        MesProRouteProcessDO currentRouteProcess = routeProcess(3L, 20L, 301L, 30L, 1);
        currentRouteProcess.setPrepareTime(15);
        currentRouteProcess.setWaitTime(5);
        currentRouteProcess.setColorCode("#00AA66");
        currentRouteProcess.setKeyFlag(Boolean.FALSE);
        MesProScheduleOrderProcessDO snapshotProcess = snapshot(601L, 3L, 300L, 1, null, true);
        snapshotProcess.setKeyProcessFlag(Boolean.TRUE);

        RouteSnapshotResolver.ResolvedRoutePlan result = resolver.resolve(
                20L, List.of(currentRouteProcess), List.of(snapshotProcess));

        assertEquals(1, result.routeProcesses().size());
        MesProRouteProcessDO resolved = result.routeProcesses().get(0);
        assertEquals(3L, resolved.getId());
        assertEquals(300L, resolved.getProcessId());
        assertEquals(30L, resolved.getWorkstationId());
        assertEquals(15, resolved.getPrepareTime());
        assertEquals(5, resolved.getWaitTime());
        assertEquals("#00AA66", resolved.getColorCode());
        assertEquals(Boolean.TRUE, resolved.getKeyFlag());
        assertEquals(Set.of(300L, 301L), result.workstationProcessAliasesByRouteProcessId().get(3L));
    }

    @Test
    void resolve_shouldAlignHistoricalTopologySnapshotWhenCurrentRouteProcessIdsChanged() {
        MesProRouteProcessDO currentRoot = routeProcess(13L, 20L, 300L, 30L, 1);
        MesProRouteProcessDO currentSecond = routeProcess(14L, 20L, 301L, 31L, 2);
        MesProScheduleOrderProcessDO historicalRoot = snapshot(601L, 3L, 0L, 1, null, true);
        MesProScheduleOrderProcessDO historicalSecond = snapshot(602L, 4L, 301L, 2, 3L, false);
        doReturn(currentRoot).when(routeProcessService).resolveCurrentRouteProcess(3L, 20L, null);
        doReturn(currentSecond).when(routeProcessService).resolveCurrentRouteProcess(4L, 20L, 301L);

        RouteSnapshotResolver.ResolvedRoutePlan result = resolver.resolve(
                20L, List.of(currentRoot, currentSecond), List.of(historicalRoot, historicalSecond));

        assertEquals(List.of(3L, 4L), result.routeProcesses().stream().map(MesProRouteProcessDO::getId).toList());
        assertEquals(List.of(300L, 301L), result.routeProcesses().stream().map(MesProRouteProcessDO::getProcessId).toList());
        assertEquals(30L, result.routeProcesses().get(0).getWorkstationId());
        assertEquals(31L, result.routeProcesses().get(1).getWorkstationId());
    }

    @Test
    void resolve_shouldKeepLatestPublishedSnapshotWorkstationInsteadOfCurrentRouteWorkstation() {
        MesProRouteProcessDO currentRoot = routeProcess(13L, 20L, 300L, 30L, 1);
        MesProRouteProcessDO currentSecond = routeProcess(14L, 20L, 301L, 31L, 2);
        MesProRouteProcessDO publishedRoot = routeProcess(3L, 20L, 300L, null, 1);
        MesProRouteProcessDO publishedSecond = routeProcess(4L, 20L, 301L, null, 2);
        MesProScheduleOrderProcessDO publishedRootSnapshot = snapshot(601L, 3L, 300L, 1, null, true);
        MesProScheduleOrderProcessDO publishedSecondSnapshot = snapshot(602L, 4L, 301L, 2, 3L, false);
        publishedRootSnapshot.setRouteScheduleConfigId(9001L);
        publishedSecondSnapshot.setRouteScheduleConfigId(9002L);
        doReturn(publishedRoot).when(routeProcessService).resolveFrozenRouteProcess(3L, 20L, 300L);
        doReturn(publishedSecond).when(routeProcessService).resolveFrozenRouteProcess(4L, 20L, 301L);

        RouteSnapshotResolver.ResolvedRoutePlan result = resolver.resolve(
                20L, List.of(currentRoot, currentSecond), List.of(publishedRootSnapshot, publishedSecondSnapshot));

        assertEquals(List.of(3L, 4L), result.routeProcesses().stream().map(MesProRouteProcessDO::getId).toList());
        assertEquals(List.of(300L, 301L), result.routeProcesses().stream().map(MesProRouteProcessDO::getProcessId).toList());
        assertNull(result.routeProcesses().get(0).getWorkstationId());
        assertNull(result.routeProcesses().get(1).getWorkstationId());
    }

    @Test
    void resolve_shouldRecoverTopologyFromRouteFlowEdgesWhenSnapshotTopologyMissing() {
        MesProRouteProcessDO root = routeProcess(11L, 20L, 300L, 30L, 1);
        MesProRouteProcessDO second = routeProcess(12L, 20L, 301L, 31L, 2);
        MesProRouteProcessDO third = routeProcess(13L, 20L, 302L, 32L, 3);
        MesProScheduleOrderProcessDO rootSnapshot = snapshotWithoutTopology(601L, 11L, 300L, 1);
        MesProScheduleOrderProcessDO secondSnapshot = snapshotWithoutTopology(602L, 12L, 301L, 2);
        MesProScheduleOrderProcessDO thirdSnapshot = snapshotWithoutTopology(603L, 13L, 302L, 3);
        doReturn(List.of(edge(11L, 12L, 1), edge(11L, 13L, 2)))
                .when(routeProcessFlowEdgeMapper).selectListByRouteId(20L);

        RouteSnapshotResolver.ResolvedRoutePlan result = resolver.resolve(
                20L, List.of(root, second, third), List.of(rootSnapshot, secondSnapshot, thirdSnapshot));

        assertNull(result.topologyValidationError());
        assertEquals(List.of(11L, 12L, 13L),
                result.scheduleOrderProcesses().stream().map(MesProScheduleOrderProcessDO::getRouteProcessId).toList());
        assertEquals(java.util.Arrays.asList(null, 11L, 11L),
                result.scheduleOrderProcesses().stream().map(MesProScheduleOrderProcessDO::getPredecessorRouteProcessId).toList());
        assertEquals(List.of(Boolean.TRUE, Boolean.FALSE, Boolean.FALSE),
                result.scheduleOrderProcesses().stream().map(MesProScheduleOrderProcessDO::getRootProcessFlag).toList());
    }

    @Test
    void resolve_shouldFailFastWhenSnapshotAndRouteEdgesCannotProvideTopology() {
        MesProRouteProcessDO root = routeProcess(11L, 20L, 300L, 30L, 1);
        MesProRouteProcessDO second = routeProcess(12L, 20L, 301L, 31L, 2);
        MesProScheduleOrderProcessDO rootSnapshot = snapshotWithoutTopology(601L, 11L, 300L, 1);
        MesProScheduleOrderProcessDO secondSnapshot = snapshotWithoutTopology(602L, 12L, 301L, 2);
        doReturn(List.of()).when(routeProcessFlowEdgeMapper).selectListByRouteId(20L);

        RouteSnapshotResolver.ResolvedRoutePlan result = resolver.resolve(
                20L, List.of(root, second), List.of(rootSnapshot, secondSnapshot));

        assertEquals("排产工序缺少有效路线流转关系，routeId=20", result.topologyValidationError());
    }

    private MesProRouteProcessDO routeProcess(Long id, Long routeId, Long processId, Long workstationId, Integer sort) {
        return MesProRouteProcessDO.builder()
                .id(id)
                .routeId(routeId)
                .processId(processId)
                .workstationId(workstationId)
                .sort(sort)
                .prepareTime(0)
                .waitTime(0)
                .build();
    }

    private MesProScheduleOrderProcessDO snapshot(Long id, Long routeProcessId, Long processId, Integer sort,
                                                  Long predecessorRouteProcessId, boolean root) {
        return MesProScheduleOrderProcessDO.builder()
                .id(id)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .sort(sort)
                .predecessorRouteProcessId(predecessorRouteProcessId)
                .rootProcessFlag(root)
                .enabled(Boolean.TRUE)
                .remainingQuantity(BigDecimal.ONE)
                .plannedQuantity(BigDecimal.ONE)
                .build();
    }

    private MesProScheduleOrderProcessDO snapshotWithoutTopology(Long id, Long routeProcessId, Long processId,
                                                                 Integer sort) {
        return MesProScheduleOrderProcessDO.builder()
                .id(id)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .sort(sort)
                .enabled(Boolean.TRUE)
                .remainingQuantity(BigDecimal.ONE)
                .plannedQuantity(BigDecimal.ONE)
                .build();
    }

    private MesProRouteProcessFlowEdgeDO edge(Long sourceRouteProcessId, Long targetRouteProcessId, Integer sort) {
        return MesProRouteProcessFlowEdgeDO.builder()
                .routeId(20L)
                .sourceRouteProcessId(sourceRouteProcessId)
                .targetRouteProcessId(targetRouteProcessId)
                .sort(sort)
                .build();
    }

}
