package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.scheduleorder.MesProScheduleOrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MesProRouteProcessTopologySnapshotTest {

    @Test
    void scheduleOrderSnapshot_shouldRejectDisconnectedCycleEvenWithSingleRootAndEdgeCount() {
        MesProScheduleOrderServiceImpl service = new MesProScheduleOrderServiceImpl();
        MesProRouteProcessFlowEdgeMapper edgeMapper = mock(MesProRouteProcessFlowEdgeMapper.class);
        ReflectionTestUtils.setField(service, "routeProcessFlowEdgeMapper", edgeMapper);
        when(edgeMapper.selectListByRouteId(701L)).thenReturn(List.of(edge(12L, 13L, 1), edge(13L, 12L, 2)));

        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(service,
                "buildRouteProcessPredecessorMap", 701L,
                List.of(routeProcess(11L), routeProcess(12L), routeProcess(13L))));
    }

    @Test
    void edhrSnapshot_shouldRejectDisconnectedCycleEvenWithSingleRootAndEdgeCount() {
        MesProEdhrBatchExecutionServiceImpl service = new MesProEdhrBatchExecutionServiceImpl();
        MesProRouteProcessFlowEdgeMapper edgeMapper = mock(MesProRouteProcessFlowEdgeMapper.class);
        ReflectionTestUtils.setField(service, "routeProcessFlowEdgeMapper", edgeMapper);
        when(edgeMapper.selectListByRouteId(701L)).thenReturn(List.of(edge(12L, 13L, 1), edge(13L, 12L, 2)));

        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(service,
                "buildRouteProcessPredecessorMap", 701L,
                List.of(routeProcess(11L), routeProcess(12L), routeProcess(13L))));
    }

    private MesProRouteProcessDO routeProcess(Long id) {
        return MesProRouteProcessDO.builder()
                .id(id)
                .routeId(701L)
                .processId(id + 1000L)
                .sort(id.intValue())
                .build();
    }

    private MesProRouteProcessFlowEdgeDO edge(Long sourceId, Long targetId, int sort) {
        return MesProRouteProcessFlowEdgeDO.builder()
                .routeId(701L)
                .sourceRouteProcessId(sourceId)
                .targetRouteProcessId(targetId)
                .relationType("NORMAL")
                .sort(sort)
                .build();
    }
}
