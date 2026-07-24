package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowBoundaryEdgeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowEdgeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowLayoutReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowValidationRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowBoundaryEdgeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowBoundaryEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowLayoutMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteProcessFlowBoundaryEdgeServiceTest {

    @InjectMocks
    private MesProRouteProcessFlowServiceImpl flowService;

    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteProcessFlowEdgeMapper flowEdgeMapper;
    @Mock
    private MesProRouteProcessFlowBoundaryEdgeMapper boundaryEdgeMapper;
    @Mock
    private MesProRouteProcessFlowLayoutMapper flowLayoutMapper;
    @Mock
    private MesProProcessService processService;
    @Mock
    private MesProRouteProcessService routeProcessService;

    @Test
    void validateGraph_shouldAllowOneStartToFanOutIntoMultipleEnds() {
        Long routeId = 9010L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeProcessMapper.selectListByRouteId(routeId)).thenReturn(List.of(
                process(61L, routeId, 601L, 1, true),
                process(62L, routeId, 602L, 2, false),
                process(63L, routeId, 603L, 3, false),
                process(64L, routeId, 604L, 4, false)
        ));

        MesProRouteProcessFlowValidationRespVO result = flowService.validateGraph(saveReq(routeId,
                List.of(edge(61L, 62L), edge(61L, 63L), edge(63L, 64L)),
                List.of(boundary("START", 61L, 1), boundary("END", 62L, 1), boundary("END", 64L, 2))));

        assertTrue(result.getValid());
        assertEquals("VALID", result.getValidationStatus());
    }

    @Test
    void validateGraph_shouldRejectMissingOrMismatchedBoundaryEdges() {
        Long routeId = 9011L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeProcessMapper.selectListByRouteId(routeId)).thenReturn(List.of(
                process(71L, routeId, 701L, 1, true),
                process(72L, routeId, 702L, 2, false),
                process(73L, routeId, 703L, 3, false)
        ));

        MesProRouteProcessFlowValidationRespVO result = flowService.validateGraph(saveReq(routeId,
                List.of(edge(71L, 72L), edge(71L, 73L)),
                List.of(boundary("START", 72L, 1), boundary("END", 72L, 1))));

        assertFalse(result.getValid());
        assertTrue(result.getValidationMessages().stream()
                .anyMatch(message -> message.getCode().equals("START_BOUNDARY_MISMATCH")));
        assertTrue(result.getValidationMessages().stream()
                .anyMatch(message -> message.getCode().equals("END_BOUNDARY_MISMATCH")));
    }

    @Test
    void saveGraph_shouldAtomicallyReplaceBoundaryEdgesWithGraphVersion() {
        Long routeId = 9012L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeProcessMapper.selectListByRouteId(routeId)).thenReturn(List.of(
                process(81L, routeId, 801L, 1, true),
                process(82L, routeId, 802L, 2, false)
        ));
        when(flowLayoutMapper.selectMaxGraphVersionByRouteId(routeId)).thenReturn(2L);

        MesProRouteProcessFlowValidationRespVO result = flowService.saveGraph(saveReq(routeId,
                List.of(edge(81L, 82L)),
                List.of(boundary("START", 81L, 1), boundary("END", 82L, 1))));

        assertTrue(result.getValid());
        assertEquals(3L, result.getGraphVersion());
        verify(boundaryEdgeMapper).deleteByRouteId(routeId);
        verify(boundaryEdgeMapper).insert(argThat((MesProRouteProcessFlowBoundaryEdgeDO edge) ->
                edge.getRouteId().equals(routeId)
                        && edge.getBoundaryType().equals("START")
                        && edge.getRouteProcessId().equals(81L)
                        && edge.getGraphVersion().equals(3L)));
        verify(boundaryEdgeMapper).insert(argThat((MesProRouteProcessFlowBoundaryEdgeDO edge) ->
                edge.getRouteId().equals(routeId)
                        && edge.getBoundaryType().equals("END")
                        && edge.getRouteProcessId().equals(82L)
                        && edge.getGraphVersion().equals(3L)));
    }

    @Test
    void copyAndDelete_shouldMapAndRemoveBoundaryEdges() {
        Long sourceRouteId = 9013L;
        Long targetRouteId = 9014L;
        when(flowEdgeMapper.selectListByRouteId(sourceRouteId)).thenReturn(List.of());
        when(boundaryEdgeMapper.selectListByRouteId(sourceRouteId)).thenReturn(List.of(
                boundaryDO(sourceRouteId, "START", 91L, 1, 4L),
                boundaryDO(sourceRouteId, "END", 92L, 1, 4L)
        ));
        when(flowLayoutMapper.selectListByRouteId(sourceRouteId)).thenReturn(List.of());
        when(flowLayoutMapper.selectMaxGraphVersionByRouteId(targetRouteId)).thenReturn(0L);

        flowService.copyGraph(sourceRouteId, targetRouteId, Map.of(91L, 191L, 92L, 192L));

        verify(boundaryEdgeMapper).deleteByRouteId(targetRouteId);
        verify(boundaryEdgeMapper).insert(argThat((MesProRouteProcessFlowBoundaryEdgeDO edge) ->
                edge.getRouteId().equals(targetRouteId)
                        && edge.getBoundaryType().equals("START")
                        && edge.getRouteProcessId().equals(191L)
                        && edge.getGraphVersion().equals(1L)));
        verify(boundaryEdgeMapper).insert(argThat((MesProRouteProcessFlowBoundaryEdgeDO edge) ->
                edge.getRouteId().equals(targetRouteId)
                        && edge.getBoundaryType().equals("END")
                        && edge.getRouteProcessId().equals(192L)
                        && edge.getGraphVersion().equals(1L)));

        flowService.deleteByRouteProcessId(targetRouteId, 191L);
        verify(boundaryEdgeMapper).deleteByRouteProcessId(targetRouteId, 191L);
    }

    private static MesProRouteDO route(Long routeId) {
        return MesProRouteDO.builder()
                .id(routeId)
                .status(CommonStatusEnum.DISABLE.getStatus())
                .build();
    }

    private static MesProRouteProcessDO process(Long id, Long routeId, Long processId, Integer sort,
                                                Boolean keyFlag) {
        return MesProRouteProcessDO.builder()
                .id(id)
                .routeId(routeId)
                .processId(processId)
                .sort(sort)
                .keyFlag(keyFlag)
                .checkFlag(false)
                .build();
    }

    private static MesProRouteProcessFlowEdgeReqVO edge(Long source, Long target) {
        MesProRouteProcessFlowEdgeReqVO edge = new MesProRouteProcessFlowEdgeReqVO();
        edge.setSourceRouteProcessId(source);
        edge.setTargetRouteProcessId(target);
        edge.setRelationType("NORMAL");
        return edge;
    }

    private static MesProRouteProcessFlowBoundaryEdgeReqVO boundary(String type, Long routeProcessId,
                                                                    Integer sort) {
        MesProRouteProcessFlowBoundaryEdgeReqVO edge = new MesProRouteProcessFlowBoundaryEdgeReqVO();
        edge.setBoundaryType(type);
        edge.setRouteProcessId(routeProcessId);
        edge.setSort(sort);
        return edge;
    }

    private static MesProRouteProcessFlowSaveReqVO saveReq(
            Long routeId,
            List<MesProRouteProcessFlowEdgeReqVO> edges,
            List<MesProRouteProcessFlowBoundaryEdgeReqVO> boundaryEdges) {
        MesProRouteProcessFlowSaveReqVO reqVO = new MesProRouteProcessFlowSaveReqVO();
        reqVO.setRouteId(routeId);
        reqVO.setGraphVersion(2L);
        reqVO.setEdges(edges);
        reqVO.setBoundaryEdges(boundaryEdges);
        reqVO.setLayouts(List.of(layout(81L, 0, 0), layout(82L, 200, 0)));
        return reqVO;
    }

    private static MesProRouteProcessFlowLayoutReqVO layout(Long routeProcessId, Integer x, Integer y) {
        MesProRouteProcessFlowLayoutReqVO layout = new MesProRouteProcessFlowLayoutReqVO();
        layout.setRouteProcessId(routeProcessId);
        layout.setX(x);
        layout.setY(y);
        return layout;
    }

    private static MesProRouteProcessFlowBoundaryEdgeDO boundaryDO(Long routeId, String type,
                                                                   Long routeProcessId, Integer sort,
                                                                   Long graphVersion) {
        return MesProRouteProcessFlowBoundaryEdgeDO.builder()
                .routeId(routeId)
                .boundaryType(type)
                .routeProcessId(routeProcessId)
                .sort(sort)
                .graphVersion(graphVersion)
                .build();
    }
}
