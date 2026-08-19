package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.util.AssertUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowBoundaryEdgeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowEdgeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowGraphRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowLayoutReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowValidationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process.MesProRouteProcessSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowBoundaryEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowLayoutDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowBoundaryEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowLayoutMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteProcessFlowServiceImplTest {

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
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteCandidateConfigService routeCandidateConfigService;
    @Mock
    private MesMdWorkstationService workstationService;

    @Test
    void getGraph_shouldReturnUninitializedWithoutCreatingEdges() {
        Long routeId = 9001L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeProcessMapper.selectListByRouteId(routeId)).thenReturn(List.of(
                process(11L, routeId, 101L, 1, true),
                process(12L, routeId, 102L, 2, false)
        ));
        when(processService.getProcessList(List.of(101L, 102L))).thenReturn(List.of(
                processDefinition(101L, "PROC-001", "粗洗工序生产记录"),
                processDefinition(102L, "PROC-002", "精洗工序生产记录")
        ));
        when(flowEdgeMapper.selectListByRouteId(routeId)).thenReturn(List.of());
        when(boundaryEdgeMapper.selectListByRouteId(routeId)).thenReturn(List.of());
        when(flowLayoutMapper.selectListByRouteId(routeId)).thenReturn(List.of());
        when(flowLayoutMapper.selectMaxGraphVersionByRouteId(routeId)).thenReturn(0L);

        MesProRouteProcessFlowGraphRespVO graph = flowService.getGraph(routeId);

        assertEquals("UNINITIALIZED", graph.getValidationStatus());
        assertEquals(2, graph.getNodes().size());
        assertEquals("PROC-001", graph.getNodes().get(0).getProcessCode());
        assertEquals("粗洗工序生产记录", graph.getNodes().get(0).getProcessName());
        assertEquals("PROC-002", graph.getNodes().get(1).getProcessCode());
        assertEquals("精洗工序生产记录", graph.getNodes().get(1).getProcessName());
        assertTrue(graph.getEdges().isEmpty());
        assertEquals(0L, graph.getGraphVersion());
        verify(flowEdgeMapper, never()).insert(any(MesProRouteProcessFlowEdgeDO.class));
        verify(boundaryEdgeMapper, never()).insert(any(MesProRouteProcessFlowBoundaryEdgeDO.class));
        verify(flowLayoutMapper, never()).insert(any(MesProRouteProcessFlowLayoutDO.class));
    }

    @Test
    void getGraph_shouldReadActiveGraphWhenRouteVersionIdIsActive() {
        Long routeId = 9001L;
        Long activeRouteVersionId = 9901L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeVersionMapper.selectById(activeRouteVersionId)).thenReturn(MesProRouteVersionDO.builder()
                .id(activeRouteVersionId)
                .routeId(routeId)
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .build());
        when(routeProcessMapper.selectListByRouteId(routeId)).thenReturn(List.of(
                process(11L, routeId, 101L, 1, true)
        ));
        when(processService.getProcessList(List.of(101L))).thenReturn(List.of(
                processDefinition(101L, "PROC-001", "生效工序")
        ));
        when(flowEdgeMapper.selectListByRouteId(routeId)).thenReturn(List.of());
        when(boundaryEdgeMapper.selectListByRouteId(routeId)).thenReturn(List.of());
        when(flowLayoutMapper.selectListByRouteId(routeId)).thenReturn(List.of());
        when(flowLayoutMapper.selectMaxGraphVersionByRouteId(routeId)).thenReturn(0L);

        MesProRouteProcessFlowGraphRespVO graph = flowService.getGraph(routeId, activeRouteVersionId);

        assertEquals("UNINITIALIZED", graph.getValidationStatus());
        assertEquals(1, graph.getNodes().size());
        assertEquals(11L, graph.getNodes().get(0).getRouteProcessId());
        assertEquals("PROC-001", graph.getNodes().get(0).getProcessCode());
        verify(routeCandidateConfigService, never()).saveConfigSnapshot(any(), any(), any());
    }

    @Test
    void getGraph_shouldReadDraftCandidateFlowGraphSnapshot() {
        Long routeId = 9011L;
        Long candidateRouteVersionId = 9911L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeVersionMapper.selectById(candidateRouteVersionId)).thenReturn(MesProRouteVersionDO.builder()
                .id(candidateRouteVersionId)
                .routeId(routeId)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 9011,
                          "configSnapshots": {
                            "flowGraph": {
                              "routeId": 9011,
                              "graphVersion": 12,
                              "nodes": [
                                {"routeProcessId": 111, "processId": 1001, "routeProcessWorkstationId": 81001, "workstationId": 81001, "sort": 1, "keyFlag": true, "checkFlag": false},
                                {"routeProcessId": 112, "processId": 1002, "sort": 2, "keyFlag": false, "checkFlag": false}
                              ],
                              "edges": [
                                {"sourceRouteProcessId": 111, "targetRouteProcessId": 112, "relationType": "NORMAL"}
                              ],
                              "boundaryEdges": [
                                {"boundaryType": "START", "routeProcessId": 111, "sort": 1},
                                {"boundaryType": "END", "routeProcessId": 112, "sort": 1}
                              ],
                              "layouts": [
                                {"routeProcessId": 111, "x": 10, "y": 20},
                                {"routeProcessId": 112, "x": 220, "y": 20}
                              ]
                            }
                          }
                        }
                        """)
                .build());
        when(processService.getProcessList(List.of(1001L, 1002L))).thenReturn(List.of(
                processDefinition(1001L, "PROC-1001", "候选首工序"),
                processDefinition(1002L, "PROC-1002", "候选末工序")
        ));
        when(workstationService.getWorkstationListByProcessIds(List.of(1001L, 1002L))).thenReturn(List.of(
                workstation(81001L, 1001L, "WS-CAND-001", "候选首工序工作站")
        ));

        MesProRouteProcessFlowGraphRespVO graph = flowService.getGraph(routeId, candidateRouteVersionId);

        assertTrue(graph.getValid());
        assertEquals("VALID", graph.getValidationStatus());
        assertEquals(12L, graph.getGraphVersion());
        assertEquals(2, graph.getNodes().size());
        assertEquals(111L, graph.getNodes().get(0).getRouteProcessId());
        assertEquals("PROC-1001", graph.getNodes().get(0).getProcessCode());
        assertEquals(81001L, graph.getNodes().get(0).getWorkstationId());
        assertEquals(81001L, graph.getNodes().get(0).getRouteProcessWorkstationId());
        assertEquals("WS-CAND-001", graph.getNodes().get(0).getWorkstationCode());
        assertEquals("候选首工序工作站", graph.getNodes().get(0).getWorkstationName());
        assertEquals(10, graph.getNodes().get(0).getX());
        assertEquals(1, graph.getEdges().size());
        assertEquals(111L, graph.getEdges().get(0).getSourceRouteProcessId());
        assertEquals(112L, graph.getEdges().get(0).getTargetRouteProcessId());
        assertEquals(2, graph.getBoundaryEdges().size());
        verify(routeProcessMapper, never()).selectListByRouteId(routeId);
        verify(flowEdgeMapper, never()).selectListByRouteId(routeId);
        verify(boundaryEdgeMapper, never()).selectListByRouteId(routeId);
        verify(flowLayoutMapper, never()).selectListByRouteId(routeId);
    }

    @Test
    void getGraph_shouldNormalizeWorkstationProcessIdentityForCandidateNodes() {
        Long routeId = 9013L;
        Long candidateRouteVersionId = 9913L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeVersionMapper.selectById(candidateRouteVersionId)).thenReturn(MesProRouteVersionDO.builder()
                .id(candidateRouteVersionId)
                .routeId(routeId)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 9013,
                          "configSnapshots": {
                            "flowGraph": {
                              "routeId": 9013,
                              "graphVersion": 7,
                              "nodes": [
                                {"routeProcessId": 131, "processId": 1001, "routeProcessWorkstationId": null, "workstationId": 81001, "sort": 1, "keyFlag": true, "checkFlag": false},
                                {"routeProcessId": 132, "processId": 1002, "sort": 2, "keyFlag": false, "checkFlag": false}
                              ],
                              "edges": [
                                {"sourceRouteProcessId": 131, "targetRouteProcessId": 132, "relationType": "NORMAL"}
                              ],
                              "boundaryEdges": [
                                {"boundaryType": "START", "routeProcessId": 131, "sort": 1},
                                {"boundaryType": "END", "routeProcessId": 132, "sort": 1}
                              ],
                              "layouts": [
                                {"routeProcessId": 131, "x": 10, "y": 20},
                                {"routeProcessId": 132, "x": 220, "y": 20}
                              ]
                            }
                          }
                        }
                        """)
                .build());
        when(processService.getProcessList(List.of(1001L, 1002L))).thenReturn(List.of(
                processDefinition(1001L, "PROC-1001", "候选首工序"),
                processDefinition(1002L, "PROC-1002", "候选末工序")
        ));
        when(workstationService.getWorkstationListByProcessIds(List.of(1001L, 1002L))).thenReturn(List.of(
                workstation(81001L, 9101L, "WS-ALIAS-001", "历史同编码工序工作站")
        ));
        org.mockito.Mockito.lenient().when(routeProcessService.getProcessIdentityMap(List.of(1001L, 1002L)))
                .thenReturn(java.util.Map.of(1001L, 1001L, 1002L, 1002L, 9101L, 1001L));

        MesProRouteProcessFlowGraphRespVO graph = flowService.getGraph(routeId, candidateRouteVersionId);

        assertEquals(81001L, graph.getNodes().get(0).getWorkstationId());
        assertNull(graph.getNodes().get(0).getRouteProcessWorkstationId());
        assertEquals("WS-ALIAS-001", graph.getNodes().get(0).getWorkstationCode());
        assertEquals("历史同编码工序工作站", graph.getNodes().get(0).getWorkstationName());
    }

    @Test
    void getGraph_shouldReadPendingApprovalCandidateFlowGraphSnapshot() {
        Long routeId = 9012L;
        Long candidateRouteVersionId = 9912L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeVersionMapper.selectById(candidateRouteVersionId)).thenReturn(MesProRouteVersionDO.builder()
                .id(candidateRouteVersionId)
                .routeId(routeId)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL)
                .routeSnapshotJson("""
                        {
                          "routeId": 9012,
                          "configSnapshots": {
                            "flowGraph": {
                              "routeId": 9012,
                              "graphVersion": 18,
                              "nodes": [
                                {"routeProcessId": 211, "processId": 2001, "sort": 1, "keyFlag": true, "checkFlag": false},
                                {"routeProcessId": 212, "processId": 2002, "sort": 2, "keyFlag": false, "checkFlag": false}
                              ],
                              "edges": [
                                {"sourceRouteProcessId": 211, "targetRouteProcessId": 212, "relationType": "NORMAL"}
                              ],
                              "boundaryEdges": [
                                {"boundaryType": "START", "routeProcessId": 211, "sort": 1},
                                {"boundaryType": "END", "routeProcessId": 212, "sort": 1}
                              ],
                              "layouts": [
                                {"routeProcessId": 211, "x": 10, "y": 20},
                                {"routeProcessId": 212, "x": 220, "y": 20}
                              ]
                            }
                          }
                        }
                        """)
                .build());
        when(processService.getProcessList(List.of(2001L, 2002L))).thenReturn(List.of(
                processDefinition(2001L, "PROC-2001", "审批中首工序"),
                processDefinition(2002L, "PROC-2002", "审批中末工序")
        ));

        MesProRouteProcessFlowGraphRespVO graph = flowService.getGraph(routeId, candidateRouteVersionId);

        assertTrue(graph.getValid());
        assertEquals("VALID", graph.getValidationStatus());
        assertEquals(18L, graph.getGraphVersion());
        assertEquals(2, graph.getNodes().size());
        assertEquals(211L, graph.getNodes().get(0).getRouteProcessId());
        assertEquals("PROC-2001", graph.getNodes().get(0).getProcessCode());
        assertEquals(10, graph.getNodes().get(0).getX());
        assertEquals(212L, graph.getNodes().get(1).getRouteProcessId());
        assertEquals("PROC-2002", graph.getNodes().get(1).getProcessCode());
        assertEquals(1, graph.getEdges().size());
        assertEquals(2, graph.getBoundaryEdges().size());
        verify(routeProcessMapper, never()).selectListByRouteId(routeId);
        verify(flowEdgeMapper, never()).selectListByRouteId(routeId);
        verify(boundaryEdgeMapper, never()).selectListByRouteId(routeId);
        verify(flowLayoutMapper, never()).selectListByRouteId(routeId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"REJECTED", "CANCELLED", "SUPERSEDED"})
    void getGraph_shouldReadClosedHistoricalRouteVersionSnapshot(String lifecycleStatus) {
        Long routeId = 9020L;
        Long routeVersionId = 9920L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeVersionMapper.selectById(routeVersionId)).thenReturn(MesProRouteVersionDO.builder()
                .id(routeVersionId)
                .routeId(routeId)
                .active(Boolean.FALSE)
                .lifecycleStatus(lifecycleStatus)
                .routeSnapshotJson("""
                        {
                          "routeId": 9020,
                          "configSnapshots": {
                            "flowGraph": {
                              "routeId": 9020,
                              "graphVersion": 23,
                              "nodes": [
                                {"routeProcessId": 321, "processId": 3001, "sort": 1, "keyFlag": true, "checkFlag": false}
                              ],
                              "edges": [],
                              "boundaryEdges": [],
                              "layouts": [
                                {"routeProcessId": 321, "x": 30, "y": 40}
                              ]
                            }
                          }
                        }
                        """)
                .build());
        when(processService.getProcessList(List.of(3001L))).thenReturn(List.of(
                processDefinition(3001L, "PROC-3001", "历史版本工序")
        ));

        MesProRouteProcessFlowGraphRespVO graph = flowService.getGraph(routeId, routeVersionId);

        assertEquals("UNINITIALIZED", graph.getValidationStatus());
        assertEquals(23L, graph.getGraphVersion());
        assertEquals(1, graph.getNodes().size());
        assertEquals(321L, graph.getNodes().get(0).getRouteProcessId());
        assertEquals("PROC-3001", graph.getNodes().get(0).getProcessCode());
        assertEquals(30, graph.getNodes().get(0).getX());
        verify(routeProcessMapper, never()).selectListByRouteId(routeId);
        verify(flowEdgeMapper, never()).selectListByRouteId(routeId);
        verify(boundaryEdgeMapper, never()).selectListByRouteId(routeId);
        verify(flowLayoutMapper, never()).selectListByRouteId(routeId);
    }

    @Test
    void saveGraph_shouldPersistGraphWithoutSyncingLegacyNextProcessFields() {
        Long routeId = 9002L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeProcessMapper.selectListByRouteId(routeId)).thenReturn(List.of(
                process(21L, routeId, 201L, 1, true),
                process(22L, routeId, 202L, 2, false),
                process(23L, routeId, 203L, 3, false)
        ));
        when(flowLayoutMapper.selectMaxGraphVersionByRouteId(routeId)).thenReturn(3L);

        MesProRouteProcessFlowValidationRespVO result = flowService.saveGraph(saveReq(routeId, 3L,
                List.of(edge(21L, 22L), edge(22L, 23L)),
                List.of(layout(21L, 10, 20), layout(22L, 220, 20), layout(23L, 430, 80))));

        assertTrue(result.getValid());
        assertEquals("VALID", result.getValidationStatus());
        assertEquals(4L, result.getGraphVersion());
        verify(flowEdgeMapper).deleteByRouteId(routeId);
        verify(boundaryEdgeMapper).deleteByRouteId(routeId);
        verify(flowLayoutMapper).deleteByRouteId(routeId);
        verify(flowEdgeMapper).insert(argThat((MesProRouteProcessFlowEdgeDO edge) -> edge.getGraphVersion().equals(4L)
                && edge.getRouteId().equals(routeId)
                && edge.getSourceRouteProcessId().equals(21L)
                && edge.getTargetRouteProcessId().equals(22L)));
        verify(flowLayoutMapper).insert(argThat((MesProRouteProcessFlowLayoutDO layout) -> layout.getGraphVersion().equals(4L)
                && layout.getRouteProcessId().equals(23L)
                && layout.getX().equals(430)
                && layout.getY().equals(80)));
        verify(boundaryEdgeMapper).insert(argThat((MesProRouteProcessFlowBoundaryEdgeDO edge) ->
                edge.getBoundaryType().equals("START") && edge.getRouteProcessId().equals(21L)));
        verify(boundaryEdgeMapper).insert(argThat((MesProRouteProcessFlowBoundaryEdgeDO edge) ->
                edge.getBoundaryType().equals("END") && edge.getRouteProcessId().equals(23L)));
        verify(routeProcessMapper, never()).updateFlowNextProcess(any(), any(), any(), any());
    }

    @Test
    void saveGraph_shouldWriteDraftCandidateSnapshotWithoutMutatingActiveGraph() {
        Long routeId = 9002L;
        Long candidateRouteVersionId = 9902L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeVersionMapper.selectById(candidateRouteVersionId)).thenReturn(MesProRouteVersionDO.builder()
                .id(candidateRouteVersionId)
                .routeId(routeId)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 9002,
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 3,
                              "nodes": [
                                {"routeProcessId": 21, "processId": 201, "sort": 1, "keyFlag": true, "checkFlag": false},
                                {"routeProcessId": 22, "processId": 202, "sort": 2, "keyFlag": false, "checkFlag": false}
                              ],
                              "edges": [],
                              "boundaryEdges": [],
                              "layouts": []
                            }
                          }
                        }
                        """)
                .build());
        MesProRouteProcessFlowSaveReqVO reqVO = saveReq(routeId, 3L,
                List.of(edge(21L, 22L)),
                List.of(layout(21L, 10, 20), layout(22L, 220, 20)));
        reqVO.setRouteVersionId(candidateRouteVersionId);
        MesProRouteProcessSaveReqVO routeProcessUpdate = new MesProRouteProcessSaveReqVO();
        routeProcessUpdate.setId(21L);
        routeProcessUpdate.setRouteId(routeId);
        routeProcessUpdate.setProcessId(201L);
        routeProcessUpdate.setWorkstationId(81001L);
        routeProcessUpdate.setSort(1);
        routeProcessUpdate.setKeyFlag(Boolean.TRUE);
        routeProcessUpdate.setCheckFlag(Boolean.FALSE);
        reqVO.setRouteProcessUpdates(List.of(routeProcessUpdate));

        MesProRouteProcessFlowValidationRespVO result = flowService.saveGraph(reqVO);

        assertTrue(result.getValid());
        assertEquals(4L, result.getGraphVersion());
        verify(routeCandidateConfigService).saveConfigSnapshot(eq(candidateRouteVersionId), eq("flowGraph"),
                argThat(snapshot -> snapshot.toString().contains("graphVersion=4")
                        && snapshot.toString().contains("nodes")
                        && snapshot.toString().contains("routeProcessId=21")
                        && candidateNodeHasWorkstation(snapshot, 21L, 81001L)
                        && candidateNodeOmitsDisplayWorkstation(snapshot, 21L)));
        verify(flowEdgeMapper, never()).deleteByRouteId(routeId);
        verify(boundaryEdgeMapper, never()).deleteByRouteId(routeId);
        verify(flowLayoutMapper, never()).deleteByRouteId(routeId);
        verify(flowEdgeMapper, never()).insert(any(MesProRouteProcessFlowEdgeDO.class));
        verify(boundaryEdgeMapper, never()).insert(any(MesProRouteProcessFlowBoundaryEdgeDO.class));
        verify(flowLayoutMapper, never()).insert(any(MesProRouteProcessFlowLayoutDO.class));
    }

    @Test
    void saveGraph_shouldUseDraftCandidateSnapshotGraphVersionWhenLiveGraphDiffers() {
        Long routeId = 9015L;
        Long candidateRouteVersionId = 9915L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeVersionMapper.selectById(candidateRouteVersionId)).thenReturn(MesProRouteVersionDO.builder()
                .id(candidateRouteVersionId)
                .routeId(routeId)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 9015,
                          "configSnapshots": {
                            "flowGraph": {
                              "routeId": 9015,
                              "graphVersion": 12,
                              "nodes": [
                                {"routeProcessId": 151, "processId": 1501, "sort": 1, "keyFlag": true, "checkFlag": false},
                                {"routeProcessId": 152, "processId": 1502, "sort": 2, "keyFlag": false, "checkFlag": false}
                              ],
                              "edges": [
                                {"sourceRouteProcessId": 151, "targetRouteProcessId": 152, "relationType": "NORMAL"}
                              ],
                              "boundaryEdges": [
                                {"boundaryType": "START", "routeProcessId": 151, "sort": 1},
                                {"boundaryType": "END", "routeProcessId": 152, "sort": 1}
                              ],
                              "layouts": [
                                {"routeProcessId": 151, "x": 10, "y": 20},
                                {"routeProcessId": 152, "x": 220, "y": 20}
                              ]
                            }
                          }
                        }
                        """)
                .build());
        MesProRouteProcessFlowSaveReqVO reqVO = saveReq(routeId, 12L,
                List.of(edge(151L, 152L)),
                List.of(layout(151L, 10, 20), layout(152L, 220, 20)));
        reqVO.setRouteVersionId(candidateRouteVersionId);

        MesProRouteProcessFlowValidationRespVO result = flowService.saveGraph(reqVO);

        assertTrue(result.getValid());
        assertEquals(13L, result.getGraphVersion());
        verify(routeCandidateConfigService).saveConfigSnapshot(eq(candidateRouteVersionId), eq("flowGraph"),
                argThat(snapshot -> snapshot.toString().contains("graphVersion=13")
                        && snapshot.toString().contains("routeProcessId=151")));
        verify(routeProcessMapper, never()).selectListByRouteId(routeId);
        verify(flowEdgeMapper, never()).deleteByRouteId(routeId);
        verify(boundaryEdgeMapper, never()).deleteByRouteId(routeId);
        verify(flowLayoutMapper, never()).deleteByRouteId(routeId);
    }

    @Test
    void saveGraph_shouldSyncDraftCandidateScheduleConfigSnapshotForCreatedProcess() {
        Long routeId = 9020L;
        Long candidateRouteVersionId = 9920L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeVersionMapper.selectById(candidateRouteVersionId)).thenReturn(MesProRouteVersionDO.builder()
                .id(candidateRouteVersionId)
                .routeId(routeId)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 9020,
                          "configSnapshots": {
                            "scheduleConfigs": {
                              "21": {
                                "routeVersionId": 9920,
                                "routeProcessId": 21,
                                "sort": 1,
                                "capacityMode": "MANUAL_OVERRIDE",
                                "hourlyCapacity": 12.5,
                                "nightShiftEnabled": true
                              }
                            },
                            "flowGraph": {
                              "graphVersion": 3,
                              "nodes": [
                                {"routeProcessId": 21, "processId": 201, "sort": 1, "keyFlag": true, "checkFlag": false}
                              ],
                              "edges": [],
                              "boundaryEdges": [],
                              "layouts": []
                            }
                          }
                        }
                        """)
                .build());
        when(processService.getProcess(202L)).thenReturn(processDefinition(202L, "PROC-202", "候选新增工序"));

        MesProRouteProcessFlowSaveReqVO reqVO = saveReq(routeId, 3L,
                List.of(edge(21L, -1L)),
                List.of(layout(21L, 10, 20), layout(-1L, 220, 20)));
        reqVO.setRouteVersionId(candidateRouteVersionId);
        MesProRouteProcessSaveReqVO createdProcess = draftCreate(-1L, routeId, 202L, 2);
        createdProcess.setWorkstationId(81401L);
        reqVO.setRouteProcessCreates(List.of(createdProcess));

        MesProRouteProcessFlowValidationRespVO result = flowService.saveGraph(reqVO);

        assertTrue(result.getValid());
        verify(routeCandidateConfigService).saveConfigSnapshot(eq(candidateRouteVersionId), eq("scheduleConfigs"),
                argThat(snapshot -> snapshot.toString().contains("\"21\"")
                        && snapshot.toString().contains("\"hourlyCapacity\":12.5")
                        && snapshot.toString().contains("\"-1\"")
                        && snapshot.toString().contains("\"routeProcessId\":-1")
                        && snapshot.toString().contains("\"sort\":2")
                        && snapshot.toString().contains("\"capacityMode\":\"RESOURCE_CALCULATED\"")
                        && snapshot.toString().contains("\"nightShiftEnabled\":false")));
        verify(routeCandidateConfigService).saveConfigSnapshot(eq(candidateRouteVersionId), eq("flowGraph"), any());
        verify(routeCandidateConfigService).saveConfigSnapshot(eq(candidateRouteVersionId), eq("flowGraph"),
                argThat(snapshot -> candidateNodeHasWorkstation(snapshot, -1L, 81401L)));
        verify(routeProcessService, never()).createRouteProcess(any(MesProRouteProcessSaveReqVO.class));
    }

    @Test
    void saveGraph_shouldRejectPendingApprovalCandidateSnapshotWriteWithoutMutatingActiveGraph() {
        Long routeId = 9013L;
        Long candidateRouteVersionId = 9913L;
        when(routeVersionMapper.selectById(candidateRouteVersionId)).thenReturn(MesProRouteVersionDO.builder()
                .id(candidateRouteVersionId)
                .routeId(routeId)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL)
                .build());
        MesProRouteProcessFlowSaveReqVO reqVO = saveReq(routeId, 4L,
                List.of(edge(311L, 312L)),
                List.of(layout(311L, 10, 20), layout(312L, 220, 20)));
        reqVO.setRouteVersionId(candidateRouteVersionId);

        AssertUtils.assertServiceException(
                () -> flowService.saveGraph(reqVO),
                ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                candidateRouteVersionId,
                MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL
        );

        verify(routeCandidateConfigService, never()).saveConfigSnapshot(any(), any(), any());
        verify(flowEdgeMapper, never()).deleteByRouteId(routeId);
        verify(boundaryEdgeMapper, never()).deleteByRouteId(routeId);
        verify(flowLayoutMapper, never()).deleteByRouteId(routeId);
        verify(flowEdgeMapper, never()).insert(any(MesProRouteProcessFlowEdgeDO.class));
        verify(boundaryEdgeMapper, never()).insert(any(MesProRouteProcessFlowBoundaryEdgeDO.class));
        verify(flowLayoutMapper, never()).insert(any(MesProRouteProcessFlowLayoutDO.class));
    }

    @Test
    void saveGraph_shouldRejectCancelledSnapshotWriteWithoutMutatingActiveGraph() {
        Long routeId = 9021L;
        Long routeVersionId = 9921L;
        when(routeVersionMapper.selectById(routeVersionId)).thenReturn(MesProRouteVersionDO.builder()
                .id(routeVersionId)
                .routeId(routeId)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_CANCELLED)
                .build());
        MesProRouteProcessFlowSaveReqVO reqVO = saveReq(routeId, 4L,
                List.of(edge(321L, 322L)),
                List.of(layout(321L, 10, 20), layout(322L, 220, 20)));
        reqVO.setRouteVersionId(routeVersionId);

        AssertUtils.assertServiceException(
                () -> flowService.saveGraph(reqVO),
                ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                routeVersionId,
                MesProRouteVersionLifecycleServiceImpl.STATUS_CANCELLED
        );

        verify(routeCandidateConfigService, never()).saveConfigSnapshot(any(), any(), any());
        verify(flowEdgeMapper, never()).deleteByRouteId(routeId);
        verify(boundaryEdgeMapper, never()).deleteByRouteId(routeId);
        verify(flowLayoutMapper, never()).deleteByRouteId(routeId);
        verify(flowEdgeMapper, never()).insert(any(MesProRouteProcessFlowEdgeDO.class));
        verify(boundaryEdgeMapper, never()).insert(any(MesProRouteProcessFlowBoundaryEdgeDO.class));
        verify(flowLayoutMapper, never()).insert(any(MesProRouteProcessFlowLayoutDO.class));
    }

    @Test
    void saveGraph_shouldWriteExistingKeyFlagUpdatesToDraftSnapshotWithoutMutatingActiveProcess() {
        Long routeId = 9010L;
        Long candidateRouteVersionId = 9910L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeVersionMapper.selectById(candidateRouteVersionId)).thenReturn(MesProRouteVersionDO.builder()
                .id(candidateRouteVersionId)
                .routeId(routeId)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 9010,
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 7,
                              "nodes": [
                                {"routeProcessId": 101, "processId": 1001, "sort": 1, "keyFlag": true, "checkFlag": false},
                                {"routeProcessId": 102, "processId": 1002, "sort": 2, "keyFlag": false, "checkFlag": false}
                              ],
                              "edges": [],
                              "boundaryEdges": [],
                              "layouts": []
                            }
                          }
                        }
                        """)
                .build());
        MesProRouteProcessFlowSaveReqVO reqVO = saveReq(routeId, 7L,
                List.of(edge(101L, 102L)),
                List.of(layout(101L, 10, 20), layout(102L, 220, 20)));
        reqVO.setRouteVersionId(candidateRouteVersionId);
        reqVO.setRouteProcessUpdates(List.of(
                draftUpdate(101L, routeId, false),
                draftUpdate(102L, routeId, true)
        ));

        MesProRouteProcessFlowValidationRespVO result = flowService.saveGraph(reqVO);

        assertTrue(result.getValid());
        assertEquals(8L, result.getGraphVersion());
        verify(routeCandidateConfigService).saveConfigSnapshot(eq(candidateRouteVersionId), eq("flowGraph"),
                argThat(snapshot -> snapshot.toString().contains("routeProcessId=101")
                        && snapshot.toString().contains("keyFlag=false")
                        && snapshot.toString().contains("routeProcessId=102")
                        && snapshot.toString().contains("keyFlag=true")));
        verify(routeProcessService, never()).updateRouteProcess(any(MesProRouteProcessSaveReqVO.class));
    }

    @Test
    void saveGraph_shouldWriteExistingWorkstationUpdateToDraftSnapshotWithoutMutatingActiveProcess() {
        Long routeId = 9014L;
        Long candidateRouteVersionId = 9914L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeVersionMapper.selectById(candidateRouteVersionId)).thenReturn(MesProRouteVersionDO.builder()
                .id(candidateRouteVersionId)
                .routeId(routeId)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 9014,
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 9,
                              "nodes": [
                                {"routeProcessId": 141, "processId": 1401, "sort": 1, "keyFlag": true, "checkFlag": false},
                                {"routeProcessId": 142, "processId": 1402, "sort": 2, "keyFlag": false, "checkFlag": false}
                              ],
                              "edges": [],
                              "boundaryEdges": [],
                              "layouts": []
                            }
                          }
                        }
                        """)
                .build());
        MesProRouteProcessFlowSaveReqVO reqVO = saveReq(routeId, 9L,
                List.of(edge(141L, 142L)),
                List.of(layout(141L, 10, 20), layout(142L, 220, 20)));
        reqVO.setRouteVersionId(candidateRouteVersionId);
        MesProRouteProcessSaveReqVO workstationUpdate = draftUpdate(142L, routeId, false);
        workstationUpdate.setWorkstationId(81402L);
        reqVO.setRouteProcessUpdates(List.of(workstationUpdate));

        MesProRouteProcessFlowValidationRespVO result = flowService.saveGraph(reqVO);

        assertTrue(result.getValid());
        assertEquals(10L, result.getGraphVersion());
        verify(routeCandidateConfigService).saveConfigSnapshot(eq(candidateRouteVersionId), eq("flowGraph"),
                argThat((java.util.Map<String, Object> snapshot) ->
                        candidateNodeHasWorkstation(snapshot, 142L, 81402L)));
        verify(routeProcessService, never()).updateRouteProcess(any(MesProRouteProcessSaveReqVO.class));
    }

    @Test
    void saveGraph_shouldPersistRouteProcessDraftChangesOnlyDuringGraphSave() {
        Long routeId = 9007L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        MesProRouteProcessDO existingFirst = process(71L, routeId, 701L, 1, true);
        MesProRouteProcessDO deletedSecond = process(72L, routeId, 702L, 2, false);
        MesProRouteProcessDO persistedSecond = process(7001L, routeId, 703L, 2, false);
        when(routeProcessMapper.selectListByRouteId(routeId)).thenReturn(
                List.of(existingFirst, deletedSecond),
                List.of(existingFirst, persistedSecond),
                List.of(existingFirst, persistedSecond)
        );
        when(routeProcessMapper.selectById(72L)).thenReturn(deletedSecond);
        when(processService.getProcess(703L)).thenReturn(processDefinition(703L, "PROC-703", "草稿新增工序"));
        when(flowLayoutMapper.selectMaxGraphVersionByRouteId(routeId)).thenReturn(5L);
        when(routeProcessService.createRouteProcess(argThat((MesProRouteProcessSaveReqVO req) ->
                req.getRouteId().equals(routeId)
                        && req.getProcessId().equals(703L)
                        && req.getSort().equals(2)
                        && req.getLinkType().equals(3)
        ))).thenReturn(7001L);

        MesProRouteProcessFlowSaveReqVO reqVO = saveReq(routeId, 5L,
                List.of(edge(71L, -1L)),
                List.of(layout(71L, 10, 20), layout(-1L, 220, 20)));
        MesProRouteProcessSaveReqVO createdProcess = draftCreate(-1L, routeId, 703L, 2);
        createdProcess.setWorkstationId(81703L);
        reqVO.setRouteProcessCreates(List.of(createdProcess));
        reqVO.setRouteProcessDeletes(List.of(72L));

        MesProRouteProcessFlowValidationRespVO result = flowService.saveGraph(reqVO);

        assertTrue(result.getValid());
        assertEquals("VALID", result.getValidationStatus());
        assertEquals(6L, result.getGraphVersion());
        verify(routeProcessService).deleteRouteProcess(72L);
        verify(routeProcessService).createRouteProcess(argThat(req ->
                req.getWorkstationId().equals(81703L)));
        verify(flowEdgeMapper).insert(argThat((MesProRouteProcessFlowEdgeDO edge) ->
                edge.getSourceRouteProcessId().equals(71L)
                        && edge.getTargetRouteProcessId().equals(7001L)
                        && edge.getGraphVersion().equals(6L)
        ));
        verify(flowLayoutMapper).insert(argThat((MesProRouteProcessFlowLayoutDO layout) ->
                layout.getRouteProcessId().equals(7001L)
                        && layout.getGraphVersion().equals(6L)
        ));
        verify(boundaryEdgeMapper).insert(argThat((MesProRouteProcessFlowBoundaryEdgeDO edge) ->
                edge.getBoundaryType().equals("END")
                        && edge.getRouteProcessId().equals(7001L)
                        && edge.getGraphVersion().equals(6L)
        ));
        verify(routeProcessMapper, never()).updateFlowNextProcess(any(), any(), any(), any());
    }

    @Test
    void validateGraph_shouldUseDraftCandidateSnapshotRouteProcesses() {
        Long routeId = 9015L;
        Long candidateRouteVersionId = 9915L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeVersionMapper.selectById(candidateRouteVersionId)).thenReturn(MesProRouteVersionDO.builder()
                .id(candidateRouteVersionId)
                .routeId(routeId)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 9015,
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 12,
                              "nodes": [
                                {"routeProcessId": 151, "processId": 1501, "sort": 1, "keyFlag": true, "checkFlag": false},
                                {"routeProcessId": 152, "processId": 1502, "sort": 2, "keyFlag": false, "checkFlag": false}
                              ],
                              "edges": [
                                {"sourceRouteProcessId": 151, "targetRouteProcessId": 152, "relationType": "NORMAL"}
                              ],
                              "boundaryEdges": [
                                {"boundaryType": "START", "routeProcessId": 151, "sort": 1},
                                {"boundaryType": "END", "routeProcessId": 152, "sort": 1}
                              ],
                              "layouts": [
                                {"routeProcessId": 151, "x": 10, "y": 20},
                                {"routeProcessId": 152, "x": 220, "y": 20}
                              ]
                            }
                          }
                        }
                        """)
                .build());
        MesProRouteProcessFlowSaveReqVO reqVO = saveReq(routeId, 12L,
                List.of(edge(151L, 152L)),
                List.of(layout(151L, 10, 20), layout(152L, 220, 20)));
        reqVO.setRouteVersionId(candidateRouteVersionId);

        MesProRouteProcessFlowValidationRespVO result = flowService.validateGraph(reqVO);

        assertTrue(result.getValid());
        assertTrue(result.getValidationMessages().isEmpty());
        verify(routeProcessMapper, never()).selectListByRouteId(routeId);
    }

    @Test
    void validateGraph_shouldAcceptSingleEntryMultipleExitTree() {
        Long routeId = 9006L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeProcessMapper.selectListByRouteId(routeId)).thenReturn(List.of(
                process(61L, routeId, 601L, 1, true),
                process(62L, routeId, 602L, 2, false),
                process(63L, routeId, 603L, 3, false),
                process(64L, routeId, 604L, 4, false)
        ));

        MesProRouteProcessFlowValidationRespVO result = flowService.validateGraph(saveReq(routeId, 0L,
                List.of(edge(61L, 62L), edge(61L, 63L), edge(63L, 64L)),
                List.of(layout(61L, 0, 0), layout(62L, 100, 0), layout(63L, 100, 100), layout(64L, 200, 0))));

        assertTrue(result.getValid());
        assertEquals("VALID", result.getValidationStatus());
        assertTrue(result.getValidationMessages().isEmpty());
    }

    @Test
    void validateGraph_shouldAcceptMultipleIncomingEdges() {
        Long routeId = 9008L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeProcessMapper.selectListByRouteId(routeId)).thenReturn(List.of(
                process(81L, routeId, 801L, 1, true),
                process(82L, routeId, 802L, 2, false),
                process(83L, routeId, 803L, 3, false),
                process(84L, routeId, 804L, 4, false)
        ));

        MesProRouteProcessFlowValidationRespVO result = flowService.validateGraph(saveReq(routeId, 0L,
                List.of(edge(81L, 82L), edge(81L, 83L), edge(82L, 83L), edge(83L, 84L)),
                List.of(layout(81L, 0, 0), layout(82L, 100, 0), layout(83L, 100, 100), layout(84L, 200, 0))));

        assertTrue(result.getValid());
        assertEquals("VALID", result.getValidationStatus());
        assertTrue(result.getValidationMessages().isEmpty());
    }

    @Test
    void validateGraph_shouldUseDraftCandidateFlowGraphSnapshotNodes() {
        Long routeId = 9014L;
        Long candidateRouteVersionId = 9914L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeVersionMapper.selectById(candidateRouteVersionId)).thenReturn(MesProRouteVersionDO.builder()
                .id(candidateRouteVersionId)
                .routeId(routeId)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 9014,
                          "configSnapshots": {
                            "flowGraph": {
                              "routeId": 9014,
                              "graphVersion": 12,
                              "nodes": [
                                {"routeProcessId": 141, "processId": 1401, "sort": 1, "keyFlag": true, "checkFlag": false},
                                {"routeProcessId": 142, "processId": 1402, "sort": 2, "keyFlag": false, "checkFlag": false}
                              ],
                              "edges": [
                                {"sourceRouteProcessId": 141, "targetRouteProcessId": 142, "relationType": "NORMAL"}
                              ],
                              "boundaryEdges": [
                                {"boundaryType": "START", "routeProcessId": 141, "sort": 1},
                                {"boundaryType": "END", "routeProcessId": 142, "sort": 1}
                              ],
                              "layouts": [
                                {"routeProcessId": 141, "x": 10, "y": 20},
                                {"routeProcessId": 142, "x": 220, "y": 20}
                              ]
                            }
                          }
                        }
                        """)
                .build());
        MesProRouteProcessFlowSaveReqVO reqVO = saveReq(routeId, 12L,
                List.of(edge(141L, 142L)),
                List.of(layout(141L, 10, 20), layout(142L, 220, 20)));
        reqVO.setRouteVersionId(candidateRouteVersionId);

        MesProRouteProcessFlowValidationRespVO result = flowService.validateGraph(reqVO);

        assertTrue(result.getValid());
        assertEquals("VALID", result.getValidationStatus());
        assertEquals(12L, result.getGraphVersion());
        assertTrue(result.getValidationMessages().isEmpty());
        verify(routeProcessMapper, never()).selectListByRouteId(routeId);
    }

    @Test
    void validateGraph_shouldAcceptStartBoundaryMultipleRootProcesses() {
        Long routeId = 9009L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeProcessMapper.selectListByRouteId(routeId)).thenReturn(List.of(
                process(91L, routeId, 901L, 1, true),
                process(92L, routeId, 902L, 2, false),
                process(93L, routeId, 903L, 3, false),
                process(94L, routeId, 904L, 4, false)
        ));

        MesProRouteProcessFlowValidationRespVO result = flowService.validateGraph(saveReq(routeId, 0L,
                List.of(edge(91L, 93L), edge(92L, 94L)),
                List.of(layout(91L, 0, 0), layout(92L, 0, 100), layout(93L, 200, 0), layout(94L, 200, 100))));

        assertTrue(result.getValid());
        assertEquals("VALID", result.getValidationStatus());
        assertTrue(result.getValidationMessages().isEmpty());
    }

    @Test
    void validateGraph_shouldRejectCycleDuplicateSelfCrossRouteAndIsolatedBoundaries() {
        Long routeId = 9003L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeProcessMapper.selectListByRouteId(routeId)).thenReturn(List.of(
                process(31L, routeId, 301L, 1, true),
                process(32L, routeId, 302L, 2, false),
                process(33L, routeId, 303L, 3, false),
                process(34L, routeId, 304L, 4, false)
        ));

        MesProRouteProcessFlowValidationRespVO result = flowService.validateGraph(saveReq(routeId, 0L,
                List.of(edge(31L, 32L), edge(32L, 31L), edge(31L, 32L), edge(33L, 33L), edge(31L, 9999L)),
                List.of(layout(31L, 0, 0), layout(32L, 100, 0), layout(33L, 200, 0), layout(34L, 300, 0))));

        assertFalse(result.getValid());
        assertEquals("INVALID", result.getValidationStatus());
        assertTrue(result.getValidationMessages().stream().anyMatch(msg -> msg.getCode().equals("CYCLE_DETECTED")));
        assertTrue(result.getValidationMessages().stream().anyMatch(msg -> msg.getCode().equals("DUPLICATE_EDGE")));
        assertTrue(result.getValidationMessages().stream().anyMatch(msg -> msg.getCode().equals("SELF_LOOP")));
        assertTrue(result.getValidationMessages().stream().anyMatch(msg -> msg.getCode().equals("CROSS_ROUTE_EDGE")));
        assertTrue(result.getValidationMessages().stream().anyMatch(msg -> msg.getCode().equals("ISOLATED_NODE")));
        assertTrue(result.getValidationMessages().stream()
                .anyMatch(msg -> msg.getCode().equals("START_BOUNDARY_MISMATCH")));
        assertTrue(result.getValidationMessages().stream()
                .anyMatch(msg -> msg.getCode().equals("END_BOUNDARY_MISMATCH")));
    }

    @Test
    void saveGraph_shouldRejectGraphVersionConflictWithoutPartialWrite() {
        Long routeId = 9004L;
        when(flowLayoutMapper.selectMaxGraphVersionByRouteId(routeId)).thenReturn(9L);

        AssertUtils.assertServiceException(
                () -> flowService.saveGraph(saveReq(routeId, 8L,
                        List.of(edge(41L, 42L)),
                        List.of(layout(41L, 0, 0), layout(42L, 100, 0)))),
                ErrorCodeConstants.PRO_ROUTE_PROCESS_FLOW_VERSION_CONFLICT
        );

        verify(flowEdgeMapper, never()).deleteByRouteId(any());
        verify(boundaryEdgeMapper, never()).deleteByRouteId(any());
        verify(flowLayoutMapper, never()).deleteByRouteId(any());
    }

    @Test
    void validateRouteEnable_shouldRejectInvalidPersistedGraph() {
        Long routeId = 9005L;
        when(routeMapper.selectById(routeId)).thenReturn(route(routeId));
        when(routeProcessMapper.selectListByRouteId(routeId)).thenReturn(List.of(
                process(51L, routeId, 501L, 1, true),
                process(52L, routeId, 502L, 2, false),
                process(53L, routeId, 503L, 3, false)
        ));
        when(processService.getProcessList(List.of(501L, 502L, 503L))).thenReturn(List.of(
                processDefinition(501L, "PROC-501", "首道工序"),
                processDefinition(502L, "PROC-502", "中间工序"),
                processDefinition(503L, "PROC-503", "末道工序")
        ));
        when(flowEdgeMapper.selectListByRouteId(routeId)).thenReturn(List.of(edgeDO(routeId, 51L, 52L, 1L)));
        when(boundaryEdgeMapper.selectListByRouteId(routeId)).thenReturn(List.of());
        when(flowLayoutMapper.selectListByRouteId(routeId)).thenReturn(List.of());

        AssertUtils.assertServiceException(
                () -> flowService.validateRouteEnable(routeId),
                ErrorCodeConstants.PRO_ROUTE_PROCESS_FLOW_INVALID
        );
    }

    private static MesProRouteDO route(Long routeId) {
        return MesProRouteDO.builder()
                .id(routeId)
                .status(CommonStatusEnum.DISABLE.getStatus())
                .build();
    }

    private static MesProRouteProcessDO process(Long id, Long routeId, Long processId, Integer sort, Boolean keyFlag) {
        return MesProRouteProcessDO.builder()
                .id(id)
                .routeId(routeId)
                .processId(processId)
                .sort(sort)
                .keyFlag(keyFlag)
                .checkFlag(false)
                .build();
    }

    private static MesProProcessDO processDefinition(Long id, String code, String name) {
        return MesProProcessDO.builder()
                .id(id)
                .code(code)
                .name(name)
                .build();
    }

    private static MesMdWorkstationDO workstation(Long id, Long processId, String code, String name) {
        return MesMdWorkstationDO.builder()
                .id(id)
                .processId(processId)
                .code(code)
                .name(name)
                .build();
    }

    private static MesProRouteProcessFlowEdgeReqVO edge(Long source, Long target) {
        MesProRouteProcessFlowEdgeReqVO edge = new MesProRouteProcessFlowEdgeReqVO();
        edge.setSourceRouteProcessId(source);
        edge.setTargetRouteProcessId(target);
        edge.setRelationType("NORMAL");
        return edge;
    }

    private static MesProRouteProcessFlowLayoutReqVO layout(Long routeProcessId, Integer x, Integer y) {
        MesProRouteProcessFlowLayoutReqVO layout = new MesProRouteProcessFlowLayoutReqVO();
        layout.setRouteProcessId(routeProcessId);
        layout.setX(x);
        layout.setY(y);
        return layout;
    }

    private static MesProRouteProcessFlowSaveReqVO saveReq(Long routeId, Long graphVersion,
                                                           List<MesProRouteProcessFlowEdgeReqVO> edges,
                                                           List<MesProRouteProcessFlowLayoutReqVO> layouts) {
        MesProRouteProcessFlowSaveReqVO reqVO = new MesProRouteProcessFlowSaveReqVO();
        reqVO.setRouteId(routeId);
        reqVO.setGraphVersion(graphVersion);
        reqVO.setEdges(edges);
        reqVO.setBoundaryEdges(boundaryEdges(edges, layouts));
        reqVO.setLayouts(layouts);
        return reqVO;
    }

    private static List<MesProRouteProcessFlowBoundaryEdgeReqVO> boundaryEdges(
            List<MesProRouteProcessFlowEdgeReqVO> edges,
            List<MesProRouteProcessFlowLayoutReqVO> layouts) {
        Set<Long> routeProcessIds = layouts.stream()
                .map(MesProRouteProcessFlowLayoutReqVO::getRouteProcessId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<Long> incoming = edges.stream()
                .map(MesProRouteProcessFlowEdgeReqVO::getTargetRouteProcessId)
                .filter(routeProcessIds::contains)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<Long> outgoing = edges.stream()
                .map(MesProRouteProcessFlowEdgeReqVO::getSourceRouteProcessId)
                .filter(routeProcessIds::contains)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<MesProRouteProcessFlowBoundaryEdgeReqVO> result = new java.util.ArrayList<>();
        int startSort = 1;
        for (Long routeProcessId : routeProcessIds) {
            if (!incoming.contains(routeProcessId)) {
                result.add(boundary("START", routeProcessId, startSort++));
            }
        }
        int endSort = 1;
        for (Long routeProcessId : routeProcessIds) {
            if (!outgoing.contains(routeProcessId)) {
                result.add(boundary("END", routeProcessId, endSort++));
            }
        }
        return result;
    }

    private static MesProRouteProcessFlowBoundaryEdgeReqVO boundary(String type, Long routeProcessId,
                                                                    Integer sort) {
        MesProRouteProcessFlowBoundaryEdgeReqVO edge = new MesProRouteProcessFlowBoundaryEdgeReqVO();
        edge.setBoundaryType(type);
        edge.setRouteProcessId(routeProcessId);
        edge.setSort(sort);
        return edge;
    }

    private static MesProRouteProcessSaveReqVO draftCreate(Long clientRouteProcessId, Long routeId,
                                                           Long processId, Integer sort) {
        MesProRouteProcessSaveReqVO reqVO = new MesProRouteProcessSaveReqVO();
        reqVO.setClientRouteProcessId(clientRouteProcessId);
        reqVO.setRouteId(routeId);
        reqVO.setProcessId(processId);
        reqVO.setSort(sort);
        reqVO.setLinkType(3);
        reqVO.setPrepareTime(0);
        reqVO.setWaitTime(0);
        reqVO.setColorCode("#00AEF3");
        reqVO.setKeyFlag(false);
        reqVO.setCheckFlag(false);
        return reqVO;
    }

    private static MesProRouteProcessSaveReqVO draftUpdate(Long routeProcessId, Long routeId, Boolean keyFlag) {
        MesProRouteProcessSaveReqVO reqVO = new MesProRouteProcessSaveReqVO();
        reqVO.setId(routeProcessId);
        reqVO.setRouteId(routeId);
        reqVO.setKeyFlag(keyFlag);
        return reqVO;
    }

    private static boolean candidateNodeHasWorkstation(Object snapshotValue,
                                                       Long routeProcessId,
                                                       Long workstationId) {
        if (!(snapshotValue instanceof java.util.Map<?, ?> snapshot)) {
            return false;
        }
        Object nodesValue = snapshot.get("nodes");
        if (!(nodesValue instanceof List<?> nodes)) {
            return false;
        }
        return nodes.stream().anyMatch(value -> value instanceof java.util.Map<?, ?> node
                && routeProcessId.equals(node.get("routeProcessId"))
                && workstationId.equals(node.get("routeProcessWorkstationId")));
    }

    private static boolean candidateNodeOmitsDisplayWorkstation(Object snapshotValue,
                                                                Long routeProcessId) {
        if (!(snapshotValue instanceof java.util.Map<?, ?> snapshot)) {
            return false;
        }
        Object nodesValue = snapshot.get("nodes");
        if (!(nodesValue instanceof List<?> nodes)) {
            return false;
        }
        return nodes.stream().anyMatch(value -> value instanceof java.util.Map<?, ?> node
                && routeProcessId.equals(node.get("routeProcessId"))
                && !node.containsKey("workstationId"));
    }

    private static MesProRouteProcessFlowEdgeDO edgeDO(Long routeId, Long source, Long target, Long graphVersion) {
        return MesProRouteProcessFlowEdgeDO.builder()
                .routeId(routeId)
                .sourceRouteProcessId(source)
                .targetRouteProcessId(target)
                .relationType("NORMAL")
                .graphVersion(graphVersion)
                .build();
    }
}
