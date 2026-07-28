package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionCreateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteVersionPlatformAdapterTest {

    @InjectMocks
    private MesProRouteVersionWorkflowServiceImpl workflowService;
    @InjectMocks
    private MesProRouteVersionLifecycleServiceImpl lifecycleService;

    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteService routeService;
    @Mock
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Mock
    private MesProRouteVersionPublishProjectionServiceImpl publishProjectionService;
    @Mock
    private MesProRouteControlledContentAdapter platformAdapter;

    @Test
    void createCandidate_shouldRegisterPlatformDraftRefAfterNativeCandidateInserted() {
        MesProRouteVersionDO active = activeVersion();
        when(routeVersionMapper.selectActiveByRouteIdForUpdate(active.getRouteId())).thenReturn(active);
        when(routeVersionMapper.selectMaxVersionNoByRouteId(active.getRouteId())).thenReturn("V1");
        when(routeService.buildCurrentRouteSnapshotJson(active.getRouteId(), active.getId()))
                .thenReturn(validSnapshotJson());
        MesProRouteVersionCreateReqVO reqVO = new MesProRouteVersionCreateReqVO();
        reqVO.setRouteId(active.getRouteId());
        reqVO.setSourceRouteVersionId(active.getId());
        reqVO.setChangeReason("平台状态机接入");

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(501L);
            workflowService.createCandidate(reqVO);
        }

        ArgumentCaptor<MesProRouteVersionDO> candidateCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).insert(candidateCaptor.capture());
        verify(platformAdapter).recordCandidateCreated(active, candidateCaptor.getValue(), 501L,
                "平台状态机接入");
    }

    @Test
    void submitCandidate_shouldMirrorNativeStatusToPlatformRef() {
        MesProRouteVersionDO active = activeVersion();
        MesProRouteVersionDO candidate = draftCandidate(active);
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(candidate.getRouteId())).thenReturn(active);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(502L);
            workflowService.submitCandidate(candidate.getId());
        }

        verify(platformAdapter).recordSubmitted(candidate, 502L, null);
        verify(platformAdapter).recordApproved(eq(candidate), eq(502L),
                startsWith("ROUTE_VERSION_READY_TO_PUBLISH:1002:"));
    }

    @Test
    void publishCandidate_shouldSupersedePlatformActiveAndActivateCandidate() {
        MesProRouteVersionDO active = activeVersion();
        MesProRouteVersionDO candidate = draftCandidate(active);
        candidate.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH);
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(candidate.getRouteId())).thenReturn(active);

        MesProRouteVersionDO published = lifecycleService.publishCandidate(candidate.getId(), 504L);

        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE, published.getLifecycleStatus());
        verify(platformAdapter).recordPublished(active, candidate, 504L);
    }

    private MesProRouteVersionDO activeVersion() {
        return MesProRouteVersionDO.builder()
                .id(1001L)
                .routeId(9001L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .routeSnapshotJson(validSnapshotJson())
                .build();
    }

    private MesProRouteVersionDO draftCandidate(MesProRouteVersionDO active) {
        return MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(active.getRouteId())
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .sourceRouteVersionId(active.getId())
                .routeSnapshotJson(active.getRouteSnapshotJson())
                .build();
    }

    private String validSnapshotJson() {
        return """
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
                """;
    }
}
