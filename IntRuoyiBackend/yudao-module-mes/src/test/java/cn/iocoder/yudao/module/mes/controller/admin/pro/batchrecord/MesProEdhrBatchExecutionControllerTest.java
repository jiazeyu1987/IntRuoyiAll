package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesActiveOrderDossierFileService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionCloseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionOpenOrCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionReviewTimelineRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionTaskOpenReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionTaskOpenRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionTaskPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchWorkbenchRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrLocalStateSampleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrLocalStateSampleRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchActiveOrderDetailService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchWorkbenchService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLocalStateSampleService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRehearsalReadinessCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRehearsalReadinessResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRehearsalReadinessService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrBatchExecutionControllerTest {

    @Mock
    private MesProEdhrBatchExecutionService batchExecutionService;
    @Mock
    private MesProEdhrBatchActiveOrderDetailService batchActiveOrderDetailService;
    @Mock
    private MesProEdhrBatchWorkbenchService batchWorkbenchService;
    @Mock
    private MesProEdhrRehearsalReadinessService rehearsalReadinessService;
    @Mock
    private MesProEdhrLocalStateSampleService localStateSampleService;
    @Mock
    private MesActiveOrderDossierFileService dossierFileService;
    @InjectMocks
    private MesProEdhrBatchExecutionController controller;

    @Test
    void dossierReaderDelegatesExactScopeWithAuthenticatedActor() {
        LoginUser actor = new LoginUser().setId(347L).setTenantId(1L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null, List.of()));
        try {
            controller.getActiveOrderDossierFiles(1225L, null);
            controller.getActiveOrderDossierFiles(null, 1009L);
            verify(dossierFileService).listForBatchScope(347L, 1225L, null);
            verify(dossierFileService).listForBatchScope(347L, null, 1009L);
            org.mockito.Mockito.verifyNoInteractions(batchExecutionService, batchActiveOrderDetailService);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void dossierRouteRequiresBatchPermissionAndPreservesOptionalExactScopes() throws Exception {
        Method method = MesProEdhrBatchExecutionController.class.getDeclaredMethod("getActiveOrderDossierFiles", Long.class, Long.class);
        assertArrayEquals(new String[]{"/active-order-dossier-files"}, method.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-batch-execution:query')", method.getAnnotation(PreAuthorize.class).value());
        assertEquals("batchExecutionId", method.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("activeOrderId", method.getParameters()[1].getAnnotation(RequestParam.class).value());
        assertFalse(method.getParameters()[0].getAnnotation(RequestParam.class).required());
        assertFalse(method.getParameters()[1].getAnnotation(RequestParam.class).required());
    }

    @Test
    void listGetOpenTaskAndClose_delegateToService() {
        EdhrBatchExecutionRespVO batch = new EdhrBatchExecutionRespVO().setId(1L).setBatchCode("BATCH-1");
        when(batchExecutionService.getPage(any())).thenReturn(new PageResult<>(List.of(batch), 1L));
        when(batchExecutionService.get(1L)).thenReturn(batch);
        EdhrBatchWorkbenchRespVO workbench = new EdhrBatchWorkbenchRespVO()
                .setBatchExecutionId(1L)
                .setMainStage("IN_FILLING");
        when(batchWorkbenchService.getWorkbench(1L)).thenReturn(workbench);
        when(batchExecutionService.openOrCreate(any())).thenReturn(batch);
        when(batchExecutionService.close(any())).thenReturn(batch);
        when(batchExecutionService.getReviewTimeline(1L)).thenReturn(new EdhrBatchExecutionReviewTimelineRespVO()
                .setBatchExecutionId(1L));
        EdhrBatchExecutionTaskOpenRespVO taskOpen = new EdhrBatchExecutionTaskOpenRespVO()
                .setTaskId(11L)
                .setExecutionId(22L);
        when(batchExecutionService.openTask(any())).thenReturn(taskOpen);
        EdhrBatchExecutionTaskPreviewRespVO taskPreview = new EdhrBatchExecutionTaskPreviewRespVO()
                .setBatchExecutionId(1L)
                .setTaskId(11L)
                .setExecutionCreated(false);
        when(batchExecutionService.previewTask(1L, 11L)).thenReturn(taskPreview);

        assertSame(batch, controller.getPage(new EdhrBatchExecutionPageReqVO()).getData().getList().get(0));
        assertSame(batch, controller.get(1L).getData());
        assertSame(workbench, controller.getWorkbench(1L).getData());
        assertSame(batch, controller.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()).getData());
        assertSame(taskOpen, controller.openTask(new EdhrBatchExecutionTaskOpenReqVO()).getData());
        assertSame(taskPreview, controller.previewTask(1L, 11L).getData());
        assertSame(batch, controller.close(new EdhrBatchExecutionCloseReqVO()).getData());
        assertEquals(1L, controller.reviewTimeline(1L).getData().getBatchExecutionId());

        verify(batchExecutionService).close(any());
        verify(batchExecutionService).getReviewTimeline(1L);
    }

    @Test
    void localStateSample_delegatesToSampleService() {
        EdhrLocalStateSampleRespVO response = new EdhrLocalStateSampleRespVO()
                .setBatchExecutionId(101L)
                .setBatchExecutionCode("EDHR-UI-SAMPLE-CLOSE-20260720123000")
                .setSampleState("CLOSE");
        when(localStateSampleService.createLocalStateSample(any())).thenReturn(response);

        assertSame(response, controller.createLocalStateSample(new EdhrLocalStateSampleReqVO()
                .setState("CLOSE")).getData());

        verify(localStateSampleService).createLocalStateSample(argThat(req ->
                "CLOSE".equals(req.getState())));
    }

    @Test
    void activeOrderDetail_delegatesToBatchScopedDetailService() {
        MesTeamLeaderActiveOrderDetail detail = new MesTeamLeaderActiveOrderDetail()
                .setActiveOrderId(300L);
        when(batchActiveOrderDetailService.getDetail(100L)).thenReturn(detail);

        assertEquals(300L, controller.getActiveOrderDetail(100L, null).getData().getActiveOrderId());

        verify(batchActiveOrderDetailService).getDetail(100L);
    }

    @Test
    void contractMappings_matchBatchExecutionEndpointsAndPermissions() throws Exception {
        Method page = MesProEdhrBatchExecutionController.class.getDeclaredMethod("getPage",
                EdhrBatchExecutionPageReqVO.class);
        assertArrayEquals(new String[]{"/page"}, page.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-batch-execution:query')",
                page.getAnnotation(PreAuthorize.class).value());

        Method get = MesProEdhrBatchExecutionController.class.getDeclaredMethod("get", Long.class);
        assertArrayEquals(new String[]{"/get"}, get.getAnnotation(GetMapping.class).value());
        assertEquals("id", get.getParameters()[0].getAnnotation(RequestParam.class).value());

        Method activeOrderDetail = MesProEdhrBatchExecutionController.class.getDeclaredMethod(
                "getActiveOrderDetail", Long.class, Long.class);
        assertArrayEquals(new String[]{"/active-order-detail"},
                activeOrderDetail.getAnnotation(GetMapping.class).value());
        assertEquals("batchExecutionId",
                activeOrderDetail.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertFalse(activeOrderDetail.getParameters()[0].getAnnotation(RequestParam.class).required());
        assertEquals("activeOrderId",
                activeOrderDetail.getParameters()[1].getAnnotation(RequestParam.class).value());
        assertFalse(activeOrderDetail.getParameters()[1].getAnnotation(RequestParam.class).required());
        assertEquals("@ss.hasPermission('mes:pro-edhr-batch-execution:query')",
                activeOrderDetail.getAnnotation(PreAuthorize.class).value());

        Method workbench = MesProEdhrBatchExecutionController.class.getDeclaredMethod("getWorkbench", Long.class);
        assertArrayEquals(new String[]{"/workbench"}, workbench.getAnnotation(GetMapping.class).value());
        assertEquals("id", workbench.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-batch-execution:query')",
                workbench.getAnnotation(PreAuthorize.class).value());

        Method open = MesProEdhrBatchExecutionController.class.getDeclaredMethod("openOrCreate",
                EdhrBatchExecutionOpenOrCreateReqVO.class);
        assertArrayEquals(new String[]{"/open-or-create"}, open.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-batch-execution:create')",
                open.getAnnotation(PreAuthorize.class).value());

        Method openTask = MesProEdhrBatchExecutionController.class.getDeclaredMethod("openTask",
                EdhrBatchExecutionTaskOpenReqVO.class);
        assertArrayEquals(new String[]{"/task/open"}, openTask.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-batch-execution:update')",
                openTask.getAnnotation(PreAuthorize.class).value());

        Method previewTask = MesProEdhrBatchExecutionController.class.getDeclaredMethod("previewTask",
                Long.class, Long.class);
        assertArrayEquals(new String[]{"/task/preview"}, previewTask.getAnnotation(GetMapping.class).value());
        assertEquals("batchExecutionId",
                previewTask.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("taskId",
                previewTask.getParameters()[1].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-batch-execution:query')",
                previewTask.getAnnotation(PreAuthorize.class).value());

        Method close = MesProEdhrBatchExecutionController.class.getDeclaredMethod("close",
                EdhrBatchExecutionCloseReqVO.class);
        assertArrayEquals(new String[]{"/close"}, close.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-batch-execution:close')",
                close.getAnnotation(PreAuthorize.class).value());

        Method localStateSample = MesProEdhrBatchExecutionController.class.getDeclaredMethod("createLocalStateSample",
                EdhrLocalStateSampleReqVO.class);
        assertArrayEquals(new String[]{"/local-state-sample"},
                localStateSample.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-batch-execution:create')",
                localStateSample.getAnnotation(PreAuthorize.class).value());

        Method reviewTimeline = MesProEdhrBatchExecutionController.class.getDeclaredMethod("reviewTimeline",
                Long.class);
        assertArrayEquals(new String[]{"/review-timeline"}, reviewTimeline.getAnnotation(GetMapping.class).value());
        assertEquals("id", reviewTimeline.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-batch-execution:query')",
                reviewTimeline.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void reviewTimelineVo_exposesExecutionSignatureRecords() throws Exception {
        EdhrBatchExecutionReviewTimelineRespVO.ExecutionReview.class.getDeclaredMethod("getSignatureRecords");
        EdhrBatchExecutionReviewTimelineRespVO.ExecutionReview.class.getDeclaredMethod("setSignatureRecords",
                List.class);
        EdhrBatchExecutionReviewTimelineRespVO.SignatureRecord.class.getDeclaredMethod("getExecutionId");
        EdhrBatchExecutionReviewTimelineRespVO.SignatureRecord.class.getDeclaredMethod("getExecutionCode");
    }

    @Test
    void reviewTimelineVo_exposesSignatureCellMarkers() throws Exception {
        EdhrBatchExecutionReviewTimelineRespVO.FormViewModel.class.getDeclaredMethod("getSignatureCellMarkers");
        EdhrBatchExecutionReviewTimelineRespVO.FormViewModel.class.getDeclaredMethod("setSignatureCellMarkers",
                List.class);
        EdhrBatchExecutionReviewTimelineRespVO.SignatureCellMarker.class.getDeclaredMethod("getRowIndex");
        EdhrBatchExecutionReviewTimelineRespVO.SignatureCellMarker.class.getDeclaredMethod("getColumnIndex");
        EdhrBatchExecutionReviewTimelineRespVO.SignatureCellMarker.class.getDeclaredMethod("getActionType");
    }

    @Test
    void rehearsalReadinessEndpoint_delegatesToReadinessService() throws Exception {
        MesProEdhrRehearsalReadinessResult readiness = new MesProEdhrRehearsalReadinessResult()
                .setOverallStatus(MesProEdhrRehearsalReadinessResult.STATUS_PASS);
        when(rehearsalReadinessService.check(any())).thenReturn(readiness);

        assertSame(readiness, controller.rehearsalReadiness(922045L, 611L, 916L, 1161L).getData());

        Method method = MesProEdhrBatchExecutionController.class.getDeclaredMethod("rehearsalReadiness",
                Long.class, Long.class, Long.class, Long.class);
        assertArrayEquals(new String[]{"/rehearsal-readiness"}, method.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-batch-execution:query')",
                method.getAnnotation(PreAuthorize.class).value());
        verify(rehearsalReadinessService).check(argThat(command ->
                Long.valueOf(922045L).equals(command.getRouteId())
                        && Long.valueOf(611L).equals(command.getExecutorUserId())
                        && Long.valueOf(916L).equals(command.getApproverUserId())
                        && Long.valueOf(1161L).equals(command.getArchiverUserId())));
    }
    @org.junit.jupiter.api.Test
    void doesNotExposeManualBatchCreation() {
        for (java.lang.reflect.Method method : MesProEdhrBatchExecutionController.class.getDeclaredMethods()) {
            PostMapping mapping = method.getAnnotation(PostMapping.class);
            if (mapping != null) {
                for (String route : mapping.value()) {
                    org.junit.jupiter.api.Assertions.assertNotEquals("/open-or-create-manual", route);
                }
            }
        }
    }
}
