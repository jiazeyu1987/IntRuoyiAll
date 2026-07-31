package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitRespVO;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitAuthorizationService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlinePqcResults;
import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlineTemplateCodes;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolEventService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProFrontlineFeedbackSubmitServiceTest {

    @Mock
    private MesProFeedbackService feedbackService;
    @Mock
    private MesProFrontlineRecordbookEntryService recordbookEntryService;
    @Mock
    private MesProcessPoolEventService processPoolEventService;
    @Mock
    private MesProcessPoolSubmitEventService processPoolSubmitEventService;
    @Mock
    private MesFrontlineSubmitAuthorizationService submitAuthorizationService;
    @Mock
    private MesFrontlineSubmitSignatureService submitSignatureService;

    private MesProFrontlineFeedbackSubmitService submitService;

    @BeforeEach
    void setUp() {
        submitService = new MesProFrontlineFeedbackSubmitServiceImpl(
                feedbackService,
                recordbookEntryService,
                processPoolEventService,
                processPoolSubmitEventService,
                submitAuthorizationService,
                submitSignatureService,
                new MesProFrontlineFeedbackPayloadSplitter());
    }

    @Test
    void shouldCreateFeedbackRecordbookAndProcessPoolEventInSingleCommand() {
        when(feedbackService.createFeedback(any())).thenReturn(501L);
        when(recordbookEntryService.createOriginalEntry(any()))
                .thenReturn(new MesProFrontlineRecordbookEntryResult(701L, 702L));
        when(processPoolSubmitEventService.createSubmitEvent(any())).thenReturn(801L);
        when(submitSignatureService.recordSubmitSignature(3001L, "frontline-password", "frontline submit"))
                .thenReturn(4001L);

        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        MesProFrontlineFeedbackSubmitRespVO respVO;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            respVO = submitService.submit(reqVO);
        }

        assertEquals(501L, respVO.getFeedbackId());
        assertEquals(701L, respVO.getRecordbookEntryId());
        assertEquals(702L, respVO.getRecordbookEventId());
        assertEquals(801L, respVO.getProcessPoolEventId());

        InOrder inOrder = inOrder(submitAuthorizationService, submitSignatureService, feedbackService, recordbookEntryService,
                processPoolSubmitEventService);
        inOrder.verify(submitAuthorizationService).authorize(argThat(command -> {
            assertEquals(9001L, command.loginUserId());
            assertEquals(3001L, command.actualEmployeeId());
            assertEquals(3001L, command.signatureEmployeeId());
            assertEquals(501L, command.deviceId());
            assertEquals(11L, command.workstationId());
            assertEquals(21L, command.routeId());
            assertEquals(71L, command.routeProcessId());
            assertEquals(31L, command.processId());
            assertEquals(FrontlineTemplateCodes.PRODUCTION_SIMPLIFIED, command.templateNo());
            return true;
        }));
        inOrder.verify(submitSignatureService).recordSubmitSignature(3001L, "frontline-password", "frontline submit");
        inOrder.verify(feedbackService).createFeedback(argThat(payload -> {
            assertEquals(new BigDecimal("100.500"), payload.getFeedbackQuantity());
            assertEquals(new BigDecimal("2.500"), payload.getUnqualifiedQuantity());
            assertEquals(3001L, payload.getFeedbackUserId());
            return true;
        }));
        inOrder.verify(feedbackService).submitFeedback(501L);
        inOrder.verify(recordbookEntryService).createOriginalEntry(argThat(payload -> {
            assertEquals(501L, payload.getFeedbackId());
            assertEquals(901L, payload.getRecordbookId());
            assertEquals(new BigDecimal("120.000"), payload.getEntryContent().get("previousProcessInputQuantity"));
            return true;
        }));
        inOrder.verify(processPoolSubmitEventService).createSubmitEvent(argThat(payload -> {
            assertEquals(501L, payload.getFeedbackId());
            assertEquals(701L, payload.getRecordbookEntryId());
            assertEquals(702L, payload.getRecordbookEventId());
            assertEquals(4001L, payload.getSignatureId());
            return true;
        }));
        verifyNoInteractions(processPoolEventService);
    }

    @Test
    void shouldCreatePqcInspectionEventForPqcTemplate() {
        when(feedbackService.createFeedback(any())).thenReturn(501L);
        when(recordbookEntryService.createOriginalEntry(any()))
                .thenReturn(new MesProFrontlineRecordbookEntryResult(701L, 702L));
        when(processPoolEventService.createPqcInspectionEvent(any())).thenReturn(802L);
        when(submitSignatureService.recordSubmitSignature(3001L, "frontline-password", "frontline submit"))
                .thenReturn(4001L);

        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        reqVO.getProcessPoolContext().setTemplateType(FrontlineTemplateCodes.PQC_SIMPLIFIED);
        reqVO.getRawPayload().put("templateType", FrontlineTemplateCodes.PQC_SIMPLIFIED);
        reqVO.getRawPayload().put("PQC_RESULT", FrontlinePqcResults.DETECTION_SUCCESS);

        MesProFrontlineFeedbackSubmitRespVO respVO;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            respVO = submitService.submit(reqVO);
        }

        assertEquals(802L, respVO.getProcessPoolEventId());
        verify(processPoolEventService).createPqcInspectionEvent(argThat(payload -> {
            assertEquals(41L, payload.getWorkOrderId());
            assertEquals(71L, payload.getRouteProcessId());
            assertEquals(31L, payload.getProcessId());
            assertEquals(3001L, payload.getActualEmployeeId());
            assertEquals(9001L, payload.getDeviceAccountId());
            assertEquals(4001L, payload.getSignatureId());
            assertEquals("SUCCESS", payload.getInspectionResult());
            return true;
        }));
        verify(processPoolSubmitEventService, never()).createSubmitEvent(any());
    }

    @Test
    void shouldRejectSignatureEmployeeMismatchBeforeWritingAnyRecord() {
        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq()
                .setActualEmployeeId(null);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertThrows(RuntimeException.class, () -> submitService.submit(reqVO));
        }

        verify(feedbackService, never()).createFeedback(any());
        verifyNoInteractions(recordbookEntryService, processPoolSubmitEventService, submitAuthorizationService,
                submitSignatureService);
    }

    @Test
    void shouldRejectUnauthorizedDeviceEmployeeContextBeforeWritingAnyRecord() {
        when(submitAuthorizationService.authorize(any()))
                .thenThrow(new IllegalStateException("route process not authorized"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertThrows(IllegalStateException.class,
                    () -> submitService.submit(MesProFrontlineFeedbackSubmitTestData.buildSubmitReq()));
        }

        verify(submitAuthorizationService).authorize(any());
        verify(feedbackService, never()).createFeedback(any());
        verifyNoInteractions(recordbookEntryService, processPoolSubmitEventService, submitSignatureService);
    }
}
