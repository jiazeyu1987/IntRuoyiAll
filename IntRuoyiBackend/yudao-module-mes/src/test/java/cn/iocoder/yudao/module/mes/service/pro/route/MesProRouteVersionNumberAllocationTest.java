package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionCreateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteVersionNumberAllocationTest {

    @InjectMocks
    private MesProRouteVersionWorkflowServiceImpl service;

    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteService routeService;
    @Mock
    private MesProRouteControlledContentAdapter platformAdapter;

    @Test
    void createCandidate_shouldAllocateVersionNoInsideLockedRouteVersionPath() {
        MesProRouteVersionDO active = activeVersion();
        when(routeVersionMapper.selectActiveByRouteIdForUpdate(active.getRouteId())).thenReturn(active);
        when(routeVersionMapper.selectMaxVersionNoByRouteId(active.getRouteId())).thenReturn("V4");
        when(routeService.buildCurrentRouteSnapshotJson(active.getRouteId(), active.getId()))
                .thenReturn(active.getRouteSnapshotJson());
        MesProRouteVersionCreateReqVO reqVO = new MesProRouteVersionCreateReqVO();
        reqVO.setRouteId(active.getRouteId());
        reqVO.setSourceRouteVersionId(active.getId());
        reqVO.setChangeReason("版本号锁内分配");

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(501L);
            service.createCandidate(reqVO);
        }

        ArgumentCaptor<MesProRouteVersionDO> candidateCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).insert(candidateCaptor.capture());
        assertEquals("V5", candidateCaptor.getValue().getVersionNo());
        InOrder order = inOrder(routeVersionMapper, routeService, platformAdapter);
        order.verify(routeVersionMapper).selectActiveByRouteIdForUpdate(active.getRouteId());
        order.verify(routeVersionMapper).selectOpenCandidateByRouteId(active.getRouteId());
        order.verify(routeService).buildCurrentRouteSnapshotJson(active.getRouteId(), active.getId());
        order.verify(routeVersionMapper).selectMaxVersionNoByRouteId(active.getRouteId());
        order.verify(routeVersionMapper).insert(candidateCaptor.getValue());
        order.verify(platformAdapter).recordCandidateCreated(active, candidateCaptor.getValue(), 501L,
                "版本号锁内分配");
    }

    private MesProRouteVersionDO activeVersion() {
        return MesProRouteVersionDO.builder()
                .id(1001L)
                .routeId(9001L)
                .versionNo("V4")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .routeSnapshotJson("""
                        {
                          "routeId": 9001,
                          "routeCode": "RT-9001",
                          "routeName": "测试路线",
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {"routeProcessId": 10, "processId": 20, "sort": 1}
                              ],
                              "edges": []
                            },
                            "products": [],
                            "scheduleConfigs": [],
                            "batchUseConfigs": [],
                            "scheduleUseConfigs": []
                          }
                        }
                        """)
                .build();
    }
}
