package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApprovalActionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApprovalRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFormReviewSignReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFormReviewSignRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSaveDraftReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSignaturePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSignatureRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionTrackingEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionTrackingPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionTrackingRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProBatchRecordExecutionControllerTest {

    @Mock
    private MesProBatchRecordExecutionService executionService;

    @InjectMocks
    private MesProBatchRecordExecutionController executionController;

    @Test
    void pageGetSaveDraftSubmit_delegateToService() {
        MesProBatchRecordExecutionRespVO detail = new MesProBatchRecordExecutionRespVO();
        when(executionService.getBatchRecordExecution(1L, null)).thenReturn(detail);
        when(executionService.getBatchRecordExecutionPage(any()))
                .thenReturn(new PageResult<>(List.of(detail), 1L));

        assertSame(detail, executionController.getExecution(1L, null).getData());
        assertEquals(1L, executionController.getExecutionPage(new MesProBatchRecordExecutionPageReqVO())
                .getData().getTotal());

        MesProBatchRecordExecutionSaveDraftReqVO saveDraftReqVO = new MesProBatchRecordExecutionSaveDraftReqVO();
        CommonResult<Boolean> saveResult = executionController.saveDraft(saveDraftReqVO);
        assertTrue(saveResult.getData());
        verify(executionService).saveBatchRecordExecutionDraft(saveDraftReqVO);

        MesProBatchRecordExecutionSubmitReqVO submitReqVO = new MesProBatchRecordExecutionSubmitReqVO()
                .setId(1L)
                .setPassword("secret")
                .setComment("submit note");
        CommonResult<Boolean> submitResult = executionController.submit(submitReqVO);
        assertTrue(submitResult.getData());
        verify(executionService).submitBatchRecordExecution(submitReqVO);
    }

    @Test
    void entryContextAndOpen_delegateToService() {
        MesProBatchRecordExecutionEntryContextRespVO entryRespVO = new MesProBatchRecordExecutionEntryContextRespVO()
                .setRouteProcessId(11L)
                .setProcessCode("PROC-11")
                .setProcessName("焊接")
                .setWorkstationCode("WS-11")
                .setWorkstationName("焊接工位")
                .setBatchRecordReportId("report-11")
                .setBatchRecordReportCode("EBR-11")
                .setBatchRecordReportName("执行报表")
                .setBatchCode("BATCH-11")
                .setCanOpen(true)
                .setBindingResolved(true)
                .setActiveExecutionId(88L)
                .setActiveExecutionStatus(1)
                .setActiveContextKey("0:1:2:3:4:BATCH-11");
        when(executionService.getEntryContext(any())).thenReturn(entryRespVO);

        MesProBatchRecordExecutionOpenOrCreateByContextRespVO openRespVO =
                new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(88L).setCreated(false)
                        .setActiveContextKey("0:1:2:3:4:BATCH-11");
        when(executionService.openOrCreateByContext(any())).thenReturn(openRespVO);

        assertSame(entryRespVO, executionController.getEntryContext(new MesProBatchRecordExecutionEntryContextReqVO()).getData());
        assertSame(openRespVO, executionController.openOrCreateByContext(new MesProBatchRecordExecutionOpenOrCreateByContextReqVO()).getData());
    }

    @Test
    void approvalTrackingAndSignature_delegateToMesOwnedServiceContracts() {
        MesProBatchRecordExecutionApprovalActionRespVO approveResp = new MesProBatchRecordExecutionApprovalActionRespVO()
                .setExecutionId(1L)
                .setStatus(3)
                .setProcessInstanceId("process-1")
                .setBpmTaskId("task-1");
        when(executionService.approveBatchRecordExecution(any())).thenReturn(approveResp);
        assertSame(approveResp, executionController.approve(new MesProBatchRecordExecutionApproveReqVO()).getData());

        MesProBatchRecordExecutionApprovalActionRespVO rejectResp = new MesProBatchRecordExecutionApprovalActionRespVO()
                .setExecutionId(2L)
                .setStatus(2)
                .setProcessInstanceId("process-2")
                .setBpmTaskId("task-2");
        when(executionService.rejectBatchRecordExecution(any())).thenReturn(rejectResp);
        assertSame(rejectResp, executionController.reject(new MesProBatchRecordExecutionRejectReqVO()).getData());

        MesProBatchRecordExecutionApprovalRespVO approvalDetail = new MesProBatchRecordExecutionApprovalRespVO()
                .setExecutionId(1L)
                .setExecutionCode("BRE-001")
                .setProcessInstanceId("process-1")
                .setBpmTaskId("task-1")
                .setCanApprove(true)
                .setCanReject(true);
        when(executionService.getApprovalDetail(1L, "task-1", null)).thenReturn(approvalDetail);
        assertSame(approvalDetail, executionController.getApprovalDetail(1L, "task-1", null).getData());

        MesProBatchRecordExecutionTrackingRespVO trackingResp = new MesProBatchRecordExecutionTrackingRespVO()
                .setExecutionId(1L)
                .setExecutionCode("BRE-001")
                .setProcessInstanceId("process-1");
        when(executionService.getTrackingPage(any())).thenReturn(new PageResult<>(List.of(trackingResp), 1L));
        assertSame(trackingResp, executionController.getTrackingPage(new MesProBatchRecordExecutionTrackingPageReqVO())
                .getData().getList().get(0));

        MesProBatchRecordExecutionTrackingEventRespVO eventResp = new MesProBatchRecordExecutionTrackingEventRespVO()
                .setExecutionId(1L)
                .setEventType("APPROVE")
                .setBpmTaskId("task-1");
        when(executionService.getTrackingTimeline(1L)).thenReturn(List.of(eventResp));
        assertSame(eventResp, executionController.getTrackingTimeline(1L).getData().get(0));

        MesProBatchRecordExecutionSignatureRespVO signatureResp = new MesProBatchRecordExecutionSignatureRespVO()
                .setExecutionId(1L)
                .setExecutionCode("BRE-001")
                .setActorNickname("审批人")
                .setMeaningText("审批通过");
        when(executionService.getSignaturePage(any())).thenReturn(new PageResult<>(List.of(signatureResp), 1L));
        assertSame(signatureResp, executionController.getSignaturePage(new MesProBatchRecordExecutionSignaturePageReqVO())
                .getData().getList().get(0));

        MesProBatchRecordExecutionFormReviewSignRespVO cosignResp =
                new MesProBatchRecordExecutionFormReviewSignRespVO()
                        .setExecutionId(1L)
                        .setSignatureId(301L)
                        .setActionType("FORM_REVIEW")
                        .setMeaningText("表单复核");
        when(executionService.cosignBatchRecordExecution(any())).thenReturn(cosignResp);
        MesProBatchRecordExecutionFormReviewSignReqVO cosignReq =
                new MesProBatchRecordExecutionFormReviewSignReqVO()
                        .setExecutionId(1L)
                        .setWorkTaskId(7001L)
                        .setPassword("review-secret")
                        .setComment("复核无异常");
        assertSame(cosignResp, executionController.cosign(cosignReq).getData());
    }

    @Test
    void contractMappings_matchLockedPhaseTwoEndpoints() throws Exception {
        Method pageMethod = MesProBatchRecordExecutionController.class.getDeclaredMethod("getExecutionPage",
                MesProBatchRecordExecutionPageReqVO.class);
        assertArrayEquals(new String[]{"/page"}, pageMethod.getAnnotation(GetMapping.class).value());

        Method getMethod = MesProBatchRecordExecutionController.class.getDeclaredMethod("getExecution",
                Long.class, Long.class);
        assertArrayEquals(new String[]{"/get"}, getMethod.getAnnotation(GetMapping.class).value());
        assertEquals("id", getMethod.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("workTaskId", getMethod.getParameters()[1].getAnnotation(RequestParam.class).value());

        Method saveDraftMethod = MesProBatchRecordExecutionController.class.getDeclaredMethod("saveDraft",
                MesProBatchRecordExecutionSaveDraftReqVO.class);
        assertArrayEquals(new String[]{"/save-draft"}, saveDraftMethod.getAnnotation(PutMapping.class).value());

        Method submitMethod = MesProBatchRecordExecutionController.class.getDeclaredMethod("submit",
                MesProBatchRecordExecutionSubmitReqVO.class);
        assertArrayEquals(new String[]{"/submit"}, submitMethod.getAnnotation(PutMapping.class).value());
        assertEquals("@ss.hasAnyPermissions('mes:pro-batch-record-execution:update', "
                        + "'mes:pro-batch-record-execution:golden-finger')",
                submitMethod.getAnnotation(PreAuthorize.class).value());

        Method cosignMethod = MesProBatchRecordExecutionController.class.getDeclaredMethod("cosign",
                MesProBatchRecordExecutionFormReviewSignReqVO.class);
        assertArrayEquals(new String[]{"/cosign"}, cosignMethod.getAnnotation(PutMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-batch-record-execution:update')",
                cosignMethod.getAnnotation(PreAuthorize.class).value());
        Field cosignWorkTaskIdField = MesProBatchRecordExecutionFormReviewSignReqVO.class.getDeclaredField("workTaskId");
        assertNotNull(cosignWorkTaskIdField.getAnnotation(NotNull.class));

        Method approveMethod = MesProBatchRecordExecutionController.class.getDeclaredMethod("approve",
                MesProBatchRecordExecutionApproveReqVO.class);
        assertArrayEquals(new String[]{"/approve"}, approveMethod.getAnnotation(PutMapping.class).value());

        Method rejectMethod = MesProBatchRecordExecutionController.class.getDeclaredMethod("reject",
                MesProBatchRecordExecutionRejectReqVO.class);
        assertArrayEquals(new String[]{"/reject"}, rejectMethod.getAnnotation(PutMapping.class).value());

        Method approvalDetailMethod = MesProBatchRecordExecutionController.class.getDeclaredMethod("getApprovalDetail",
                Long.class, String.class, Long.class);
        assertArrayEquals(new String[]{"/approval-detail"}, approvalDetailMethod.getAnnotation(GetMapping.class).value());
        assertEquals("id", approvalDetailMethod.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("bpmTaskId", approvalDetailMethod.getParameters()[1].getAnnotation(RequestParam.class).value());
        assertEquals(false, approvalDetailMethod.getParameters()[1].getAnnotation(RequestParam.class).required());
        assertEquals("workTaskId", approvalDetailMethod.getParameters()[2].getAnnotation(RequestParam.class).value());
        assertEquals(false, approvalDetailMethod.getParameters()[2].getAnnotation(RequestParam.class).required());
        assertEquals("@ss.hasPermission('mes:pro-batch-record-execution:approve')",
                approvalDetailMethod.getAnnotation(PreAuthorize.class).value());

        Method trackingTimelineMethod = MesProBatchRecordExecutionController.class.getDeclaredMethod("getTrackingTimeline",
                Long.class);
        assertArrayEquals(new String[]{"/tracking-timeline"}, trackingTimelineMethod.getAnnotation(GetMapping.class).value());
        assertEquals("executionId", trackingTimelineMethod.getParameters()[0].getAnnotation(RequestParam.class).value());
    }

    @Test
    void contractMappings_requireEdhrContextEndpointsAndSignatureAwareSubmit() {
        Class<?> entryReqClass = requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextReqVO");
        Class<?> openReqClass = requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO");

        Method entryContextMethod = requireMethod(MesProBatchRecordExecutionController.class, "getEntryContext", entryReqClass);
        GetMapping entryGetMapping = requireAnnotation(entryContextMethod, GetMapping.class);
        assertArrayEquals(new String[]{"/entry-context"}, entryGetMapping.value());
        assertEquals("@ss.hasPermission('mes:pro-batch-record-execution:create')",
                entryContextMethod.getAnnotation(PreAuthorize.class).value());

        Method openMethod = requireMethod(MesProBatchRecordExecutionController.class, "openOrCreateByContext", openReqClass);
        PostMapping openPostMapping = requireAnnotation(openMethod, PostMapping.class);
        assertArrayEquals(new String[]{"/open-or-create-by-context"}, openPostMapping.value());
        assertEquals("@ss.hasPermission('mes:pro-batch-record-execution:create')",
                openMethod.getAnnotation(PreAuthorize.class).value());

        requireMethod(requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextRespVO"),
                "getCanOpen");
        requireMethod(requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextRespVO"),
                "getBindingResolved");
        requireMethod(requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextRespVO"),
                "getActiveExecutionId");
        requireMethod(requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextRespVO"),
                "getActiveExecutionStatus");
        requireMethod(requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextRespVO"),
                "getActiveContextKey");
        requireMethod(requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextRespVO"),
                "getProcessCode");
        requireMethod(requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextRespVO"),
                "getProcessName");
        requireMethod(requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextRespVO"),
                "getWorkstationCode");
        requireMethod(requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextRespVO"),
                "getWorkstationName");
        requireMethod(requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextRespVO"),
                "getBatchRecordReportCode");
        requireMethod(requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextRespVO"),
                "getBatchRecordReportName");
        requireMethod(requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextRespVO"),
                "getRouteCode");
        requireMethod(requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextRespVO"),
                "getRouteName");
        requireMethod(requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextRespVO"),
                "getCreated");
        requireMethod(requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextRespVO"),
                "getActiveContextKey");
        requireMethod(MesProBatchRecordExecutionSubmitReqVO.class, "getPassword");
        requireMethod(MesProBatchRecordExecutionService.class, "submitBatchRecordExecution",
                MesProBatchRecordExecutionSubmitReqVO.class);
        Class<?> formReviewReqClass = requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFormReviewSignReqVO");
        Class<?> formReviewRespClass = requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFormReviewSignRespVO");
        requireMethod(formReviewReqClass, "getExecutionId");
        requireMethod(formReviewReqClass, "getPassword");
        requireMethod(formReviewReqClass, "getComment");
        requireMethod(formReviewRespClass, "getSignatureId");
        requireMethod(formReviewRespClass, "getActionType");
        requireMethod(MesProBatchRecordExecutionService.class, "cosignBatchRecordExecution", formReviewReqClass);
        requireMethod(MesProBatchRecordExecutionService.class, "getApprovalDetail", Long.class, String.class);
    }

    @Test
    void contractMappings_shouldNotExposeLegacyCreateTemplateEndpoint() {
        boolean legacyEndpointExposed = Arrays.stream(MesProBatchRecordExecutionController.class.getDeclaredMethods())
                .anyMatch(method -> {
                    PostMapping postMapping = method.getAnnotation(PostMapping.class);
                    if (postMapping == null) {
                        return false;
                    }
                    return Arrays.asList(postMapping.value()).contains("/legacy-create-from-template");
                });
        assertEquals(false, legacyEndpointExposed);
    }

    private Class<?> requireClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            return fail("Expected controller contract class to exist: " + className, ex);
        }
    }

    private Method requireMethod(Class<?> type, String methodName, Class<?>... parameterTypes) {
        try {
            return type.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ex) {
            try {
                return type.getMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException nested) {
                return fail("Expected method to exist: " + type.getName() + "#" + methodName, nested);
            }
        }
    }

    private <A extends java.lang.annotation.Annotation> A requireAnnotation(Method method, Class<A> annotationType) {
        A annotation = method.getAnnotation(annotationType);
        assertNotNull(annotation, "Expected annotation " + annotationType.getSimpleName() + " on " + method.getName());
        return annotation;
    }
}
