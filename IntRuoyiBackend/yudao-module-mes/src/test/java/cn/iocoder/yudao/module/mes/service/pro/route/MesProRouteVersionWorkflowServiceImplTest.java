package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteVersionWorkflowServiceImplTest {

    @InjectMocks
    private MesProRouteVersionWorkflowServiceImpl service;

    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Mock
    private MesProRouteService routeService;
    @Mock
    private MesProRouteControlledContentAdapter platformAdapter;

    @Test
    void submitCandidate_shouldRecordControlledContentWhileNativeCandidateIsStillDraft() {
        MesProRouteVersionDO active = activeVersion();
        MesProRouteVersionDO candidate = draftCandidate(active);
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.countOpenCandidatesByRouteId(candidate.getRouteId())).thenReturn(1L);
        when(routeVersionMapper.selectActiveByRouteId(candidate.getRouteId())).thenReturn(active);
        doAnswer(invocation -> {
            MesProRouteVersionDO submittedCandidate = invocation.getArgument(0);
            assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT,
                    submittedCandidate.getLifecycleStatus(),
                    "controlled content submit must see the historical draft state before native status mutation");
            return null;
        }).when(platformAdapter).recordSubmitted(any(MesProRouteVersionDO.class), eq(502L), isNull());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(502L);

            MesProRouteVersionDO submitted = service.submitCandidate(candidate.getId());

            assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH,
                    submitted.getLifecycleStatus());
        }

        verify(platformAdapter).recordApproved(any(MesProRouteVersionDO.class), eq(502L),
                org.mockito.ArgumentMatchers.startsWith("ROUTE_VERSION_READY_TO_PUBLISH:"));
        ArgumentCaptor<MesProRouteVersionDO> updateCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(updateCaptor.capture());
        assertEquals(candidate.getId(), updateCaptor.getValue().getId());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH,
                updateCaptor.getValue().getLifecycleStatus());
    }

    private MesProRouteVersionDO activeVersion() {
        return MesProRouteVersionDO.builder()
                .id(1001L)
                .routeId(9001L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .build();
    }

    private MesProRouteVersionDO draftCandidate(MesProRouteVersionDO active) {
        return MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(active.getRouteId())
                .versionNo("V5")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .sourceRouteVersionId(active.getId())
                .routeSnapshotJson(completeRouteSnapshot())
                .build();
    }

    private String completeRouteSnapshot() {
        return "{\"routeId\":9001,\"routeCode\":\"ROUTE-XLSX-00001\",\"routeName\":\"route\","
                + "\"configSnapshots\":{\"flowGraph\":{\"nodes\":[{\"routeProcessId\":922483}]},"
                + "\"products\":[],\"scheduleConfigs\":[],\"batchUseConfigs\":[],\"scheduleUseConfigs\":[]}}";
    }
}
