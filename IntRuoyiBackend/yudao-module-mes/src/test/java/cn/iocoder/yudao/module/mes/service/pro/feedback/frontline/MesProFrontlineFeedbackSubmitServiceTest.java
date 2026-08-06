package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitRespVO;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitAuthorizationService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private MesProcessPoolSubmitEventService processPoolSubmitEventService;
    @Mock
    private MesFrontlineSubmitAuthorizationService submitAuthorizationService;
    @Mock
    private MesFrontlineLossReasonValidator lossReasonValidator;
    @Mock
    private MesFrontlineDeviceParameterValidator deviceParameterValidator;

    private MesProFrontlineFeedbackSubmitService submitService;

    @BeforeEach
    void setUp() {
        submitService = new MesProFrontlineFeedbackSubmitServiceImpl(
                feedbackService,
                recordbookEntryService,
                processPoolSubmitEventService,
                submitAuthorizationService,
                lossReasonValidator,
                deviceParameterValidator,
                new MesProFrontlineFeedbackPayloadSplitter());
    }

    @Test
    void shouldCreateFeedbackRecordbookAndProcessPoolEventInSingleCommand() {
        when(processPoolSubmitEventService.findExistingSubmitEvent(any())).thenReturn(Optional.empty());
        when(feedbackService.createFeedback(any())).thenReturn(501L);
        when(recordbookEntryService.createOriginalEntry(any()))
                .thenReturn(new MesProFrontlineRecordbookEntryResult(701L, 702L));
        when(processPoolSubmitEventService.createSubmitEvent(any())).thenReturn(801L);
        stubValidLossReason();

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

        InOrder inOrder = inOrder(submitAuthorizationService, lossReasonValidator, feedbackService,
                recordbookEntryService, processPoolSubmitEventService);
        inOrder.verify(submitAuthorizationService).authorize(argThat(command -> {
            assertEquals(9001L, command.loginUserId());
            assertEquals(3001L, command.actualEmployeeId());
            assertEquals(3001L, command.signatureEmployeeId());
            assertEquals(501L, command.deviceId());
            assertEquals(11L, command.workstationId());
            assertEquals(21L, command.routeId());
            assertEquals(71L, command.routeProcessId());
            assertEquals(31L, command.processId());
            assertEquals("PRODUCTION_SIMPLE", command.templateNo());
            return true;
        }));
        inOrder.verify(lossReasonValidator).requireEnabledLossReasons(
                71L,
                MesProFrontlineFeedbackSubmitTestData.buildSubmitReq().getFeedbackPayload().getLossDetails(),
                new BigDecimal("2.500"));
        inOrder.verify(feedbackService).createFeedback(argThat(payload -> {
            assertEquals(new BigDecimal("100.500"), payload.getFeedbackQuantity());
            assertEquals(new BigDecimal("2.500"), payload.getUnqualifiedQuantity());
            assertEquals(8301L, payload.getLossReasonId());
            assertEquals("LOSS-001", payload.getLossReasonCodeSnapshot());
            assertEquals("正常损耗", payload.getLossReasonNameSnapshot());
            assertEquals(3001L, payload.getFeedbackUserId());
            return true;
        }));
        inOrder.verify(feedbackService).submitFeedback(501L);
        inOrder.verify(recordbookEntryService).createOriginalEntry(argThat(payload -> {
            assertEquals(501L, payload.getFeedbackId());
            assertEquals(901L, payload.getRecordbookId());
            assertFalse(payload.getEntryContent().containsKey("previousProcessInputQuantity"));
            return true;
        }));
        inOrder.verify(processPoolSubmitEventService).createSubmitEvent(argThat(payload -> {
            assertEquals("P0-SUBMIT-F2-20260730-001", payload.getProcessPoolSubmissionIdempotencyKey());
            assertEquals(501L, payload.getFeedbackId());
            assertEquals(701L, payload.getRecordbookEntryId());
            assertEquals(702L, payload.getRecordbookEventId());
            assertEquals(4001L, payload.getSignatureId());
            assertEquals(8301L, payload.getRawPayload().get("lossReasonId"));
            assertEquals("正常损耗", payload.getRawPayload().get("lossReasonNameSnapshot"));
            return true;
        }));
    }

    @Test
    void shouldReturnExistingSubmitResultBeforeWritingDuplicateFeedback() {
        when(processPoolSubmitEventService.findExistingSubmitEvent(any()))
                .thenReturn(Optional.of(new cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventResult()
                        .setFeedbackId(501L)
                        .setRecordbookEntryId(701L)
                        .setRecordbookEventId(702L)
                        .setProcessPoolEventId(801L)));
        stubValidLossReason();

        MesProFrontlineFeedbackSubmitRespVO respVO;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            respVO = submitService.submit(MesProFrontlineFeedbackSubmitTestData.buildSubmitReq());
        }

        assertEquals(501L, respVO.getFeedbackId());
        assertEquals(701L, respVO.getRecordbookEntryId());
        assertEquals(702L, respVO.getRecordbookEventId());
        assertEquals(801L, respVO.getProcessPoolEventId());
        verify(processPoolSubmitEventService).findExistingSubmitEvent(argThat(payload -> {
            assertEquals("P0-SUBMIT-F2-20260730-001", payload.getProcessPoolSubmissionIdempotencyKey());
            assertEquals(41L, payload.getWorkOrderId());
            assertEquals(71L, payload.getRouteProcessId());
            assertEquals(31L, payload.getProcessId());
            return true;
        }));
        verify(feedbackService, never()).createFeedback(any());
        verifyNoInteractions(recordbookEntryService);
        verify(processPoolSubmitEventService, never()).createSubmitEvent(any());
    }

    @Test
    void shouldRejectDisabledOrCrossRouteProcessLossReasonBeforeWritingAnyRecord() {
        when(lossReasonValidator.requireEnabledLossReasons(
                71L,
                MesProFrontlineFeedbackSubmitTestData.buildSubmitReq().getFeedbackPayload().getLossDetails(),
                new BigDecimal("2.500")))
                .thenThrow(new IllegalStateException("损耗原因不属于当前工序或已禁用"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertThrows(IllegalStateException.class,
                    () -> submitService.submit(MesProFrontlineFeedbackSubmitTestData.buildSubmitReq()));
        }

        verify(submitAuthorizationService).authorize(any());
        verify(lossReasonValidator).requireEnabledLossReasons(
                71L,
                MesProFrontlineFeedbackSubmitTestData.buildSubmitReq().getFeedbackPayload().getLossDetails(),
                new BigDecimal("2.500"));
        verify(feedbackService, never()).createFeedback(any());
        verifyNoInteractions(recordbookEntryService, processPoolSubmitEventService);
    }

    @Test
    void shouldRejectSignatureEmployeeMismatchBeforeWritingAnyRecord() {
        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq()
                .setSignatureEmployeeId(3999L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertThrows(RuntimeException.class, () -> submitService.submit(reqVO));
        }

        verify(feedbackService, never()).createFeedback(any());
        verifyNoInteractions(recordbookEntryService, processPoolSubmitEventService, submitAuthorizationService);
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
        verifyNoInteractions(recordbookEntryService, processPoolSubmitEventService);
    }

    @Test
    void shouldRejectLossQuantityGreaterThanOutputBeforeWritingAnyRecord() {
        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        reqVO.getFeedbackPayload()
                .setOutputQuantity(new BigDecimal("10.000"))
                .setLossQuantity(new BigDecimal("11.000"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertThrows(RuntimeException.class, () -> submitService.submit(reqVO));
        }

        verifyNoInteractions(submitAuthorizationService, feedbackService, recordbookEntryService,
                processPoolSubmitEventService);
    }

    @Test
    void shouldRejectNegativeProductionQuantityBeforeWritingAnyRecord() {
        MesProFrontlineFeedbackSubmitReqVO negativeOutputReq = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        negativeOutputReq.getFeedbackPayload()
                .setOutputQuantity(new BigDecimal("-1.000"))
                .setLossQuantity(BigDecimal.ZERO);
        MesProFrontlineFeedbackSubmitReqVO negativeLossReq = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        negativeLossReq.getFeedbackPayload()
                .setOutputQuantity(new BigDecimal("10.000"))
                .setLossQuantity(new BigDecimal("-0.001"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertThrows(RuntimeException.class, () -> submitService.submit(negativeOutputReq));
            assertThrows(RuntimeException.class, () -> submitService.submit(negativeLossReq));
        }

        verifyNoInteractions(submitAuthorizationService, feedbackService, recordbookEntryService,
                processPoolSubmitEventService);
    }

    private void stubValidLossReason() {
        when(lossReasonValidator.requireEnabledLossReasons(
                71L,
                MesProFrontlineFeedbackSubmitTestData.buildSubmitReq().getFeedbackPayload().getLossDetails(),
                new BigDecimal("2.500")))
                .thenReturn(List.of(new MesFrontlineLossReasonSnapshot(8301L, "LOSS-001", "正常损耗")));
    }
}
