package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionBlockerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionBusinessApprovalSubmitService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionWorkflowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteVersionControllerTest {

    @Mock
    private MesProRouteVersionBusinessApprovalSubmitService businessApprovalSubmitService;
    @Mock
    private MesProRouteVersionWorkflowService workflowService;

    @InjectMocks
    private MesProRouteVersionController controller;

    @Test
    void submitCandidate_shouldDelegateToPlatformBusinessApprovalWithoutSignatureAndReturnPendingVersion() {
        LocalDateTime submittedTime = LocalDateTime.of(2026, 7, 22, 9, 30);
        MesProRouteVersionDO pending = MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(10L)
                .versionNo("R-001-V2")
                .active(Boolean.FALSE)
                .lifecycleStatus("PENDING_APPROVAL")
                .sourceRouteVersionId(1001L)
                .submittedTime(submittedTime)
                .build();
        when(businessApprovalSubmitService.submitAndPublishCandidate(1002L)).thenReturn(pending);

        CommonResult<MesProRouteVersionRespVO> response = controller.submitCandidate(1002L);

        MesProRouteVersionRespVO data = response.getData();
        assertEquals(1002L, data.getId());
        assertEquals(10L, data.getRouteId());
        assertEquals("R-001-V2", data.getVersionNo());
        assertEquals(Boolean.FALSE, data.getActive());
        assertEquals("PENDING_APPROVAL", data.getLifecycleStatus());
        assertEquals(1001L, data.getSourceRouteVersionId());
        assertEquals(submittedTime, data.getSubmittedTime());
        verify(businessApprovalSubmitService).submitAndPublishCandidate(1002L);
    }

    @Test
    void submitCandidate_requiresRouteVersionSubmitPermission() throws Exception {
        Method method = MesProRouteVersionController.class.getDeclaredMethod("submitCandidate", Long.class);

        assertEquals("@ss.hasPermission('mes:pro-route:version-submit')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void submitAndPublishCandidate_shouldDelegateOneClickApprovalSubmitWithoutSignature() {
        MesProRouteVersionDO pending = MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(10L)
                .versionNo("R-001-V2")
                .active(Boolean.FALSE)
                .lifecycleStatus("PENDING_APPROVAL")
                .sourceRouteVersionId(1001L)
                .build();
        when(businessApprovalSubmitService.submitAndPublishCandidate(1002L)).thenReturn(pending);

        CommonResult<MesProRouteVersionRespVO> response = controller.submitAndPublishCandidate(1002L);

        assertEquals("PENDING_APPROVAL", response.getData().getLifecycleStatus());
        verify(businessApprovalSubmitService).submitAndPublishCandidate(1002L);
    }

    @Test
    void candidateWorkflow_shouldExposeListCreateSubmitCancelAndBlockers() {
        MesProRouteVersionDO draft = MesProRouteVersionDO.builder()
                .id(2002L)
                .routeId(20L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus("DRAFT")
                .sourceRouteVersionId(2001L)
                .approvalProcessInstanceId("fbede791-8138-11f1-80b5-00155d3585b8")
                .build();
        MesProRouteVersionCreateReqVO createReqVO = new MesProRouteVersionCreateReqVO();
        createReqVO.setRouteId(20L);
        createReqVO.setSourceRouteVersionId(2001L);
        createReqVO.setChangeReason("调整工序");
        MesProRouteVersionBlockerRespVO blockers = new MesProRouteVersionBlockerRespVO();
        blockers.setRouteVersionId(2002L);
        blockers.setPublishable(Boolean.TRUE);
        blockers.setBlockers(List.of());

        when(workflowService.listByRouteId(20L)).thenReturn(List.of(draft));
        when(workflowService.getVersion(2002L)).thenReturn(draft);
        when(workflowService.createCandidate(createReqVO)).thenReturn(draft);
        when(workflowService.withdrawCandidate(2002L)).thenReturn(draft);
        when(workflowService.reopenRejectedCandidate(2002L)).thenReturn(draft);
        when(workflowService.cancelCandidate(2002L)).thenReturn(draft);
        when(workflowService.getPublishBlockers(2002L)).thenReturn(blockers);

        assertEquals(1, controller.listByRouteId(20L).getData().size());
        assertEquals(2002L, controller.getVersion(2002L).getData().getId());
        assertEquals("DRAFT", controller.createCandidate(createReqVO).getData().getLifecycleStatus());
        assertEquals("DRAFT", controller.withdrawCandidate(2002L).getData().getLifecycleStatus());
        assertEquals("DRAFT", controller.reopenRejectedCandidate(2002L).getData().getLifecycleStatus());
        assertEquals("DRAFT", controller.cancelCandidate(2002L).getData().getLifecycleStatus());
        assertEquals(Boolean.TRUE, controller.getPublishBlockers(2002L).getData().getPublishable());
    }

    @Test
    void candidateWorkflow_shouldDeclareDedicatedPermissions() throws Exception {
        assertEquals("@ss.hasPermission('mes:pro-route:version-query')",
                MesProRouteVersionController.class.getDeclaredMethod("listByRouteId", Long.class)
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermission('mes:pro-route:version-query')",
                MesProRouteVersionController.class.getDeclaredMethod("getVersion", Long.class)
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermission('mes:pro-route:version-query')",
                MesProRouteVersionController.class.getDeclaredMethod("getPublishBlockers", Long.class)
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermission('mes:pro-route:version-create')",
                MesProRouteVersionController.class.getDeclaredMethod("createCandidate", MesProRouteVersionCreateReqVO.class)
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermission('mes:pro-route:version-submit')",
                MesProRouteVersionController.class.getDeclaredMethod("submitCandidate", Long.class)
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermission('mes:pro-route:version-withdraw')",
                MesProRouteVersionController.class.getDeclaredMethod("withdrawCandidate", Long.class)
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermission('mes:pro-route:version-reopen')",
                MesProRouteVersionController.class.getDeclaredMethod("reopenRejectedCandidate", Long.class)
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermission('mes:pro-route:version-cancel')",
                MesProRouteVersionController.class.getDeclaredMethod("cancelCandidate", Long.class)
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermission('mes:pro-route:version-submit')",
                MesProRouteVersionController.class.getDeclaredMethod("submitAndPublishCandidate", Long.class)
                        .getAnnotation(PreAuthorize.class).value());
    }
}
