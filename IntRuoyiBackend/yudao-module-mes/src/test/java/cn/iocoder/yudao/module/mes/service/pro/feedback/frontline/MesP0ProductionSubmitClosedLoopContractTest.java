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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesP0ProductionSubmitClosedLoopContractTest {

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
    void shouldCreateFeedbackRecordbookAndProcessPoolEventInOneTransaction() throws Exception {
        assertSubmitMethodHasRollbackBoundary();
        when(processPoolSubmitEventService.findExistingSubmitEvent(any())).thenReturn(Optional.empty());
        when(feedbackService.createFeedback(any())).thenReturn(501L);
        when(recordbookEntryService.createOriginalEntry(any()))
                .thenReturn(new MesProFrontlineRecordbookEntryResult(701L, 702L));
        when(processPoolSubmitEventService.createSubmitEvent(any())).thenReturn(801L);

        MesProFrontlineFeedbackSubmitRespVO respVO;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            respVO = submitService.submit(MesProFrontlineFeedbackSubmitTestData.buildSubmitReq());
        }

        assertEquals(501L, respVO.getFeedbackId());
        assertEquals(701L, respVO.getRecordbookEntryId());
        assertEquals(702L, respVO.getRecordbookEventId());
        assertEquals(801L, respVO.getProcessPoolEventId());

        InOrder inOrder = inOrder(submitAuthorizationService, feedbackService, recordbookEntryService,
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
            return true;
        }));
        inOrder.verify(processPoolSubmitEventService).findExistingSubmitEvent(argThat(payload -> {
            assertEquals("P0-SUBMIT-F2-20260730-001", payload.getProcessPoolSubmissionIdempotencyKey());
            assertEquals(41L, payload.getWorkOrderId());
            assertEquals(71L, payload.getRouteProcessId());
            assertEquals(31L, payload.getProcessId());
            return true;
        }));
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
            return true;
        }));
        inOrder.verify(processPoolSubmitEventService).createSubmitEvent(argThat(payload -> {
            assertEquals("P0-SUBMIT-F2-20260730-001", payload.getProcessPoolSubmissionIdempotencyKey());
            assertEquals(501L, payload.getFeedbackId());
            assertEquals(701L, payload.getRecordbookEntryId());
            assertEquals(702L, payload.getRecordbookEventId());
            assertEquals(4001L, payload.getSignatureId());
            return true;
        }));
    }

    @Test
    void shouldPropagateRecordbookFailureInsideSameSubmitTransaction() {
        when(processPoolSubmitEventService.findExistingSubmitEvent(any())).thenReturn(Optional.empty());
        when(feedbackService.createFeedback(any())).thenReturn(501L);
        when(recordbookEntryService.createOriginalEntry(any()))
                .thenThrow(new IllegalStateException("recordbook write failed"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertThrows(IllegalStateException.class,
                    () -> submitService.submit(MesProFrontlineFeedbackSubmitTestData.buildSubmitReq()));
        }

        verify(feedbackService).submitFeedback(501L);
        verify(processPoolSubmitEventService, never()).createSubmitEvent(any());
    }

    private static void assertSubmitMethodHasRollbackBoundary() throws Exception {
        Transactional transactional = MesProFrontlineFeedbackSubmitServiceImpl.class
                .getMethod("submit", MesProFrontlineFeedbackSubmitReqVO.class)
                .getAnnotation(Transactional.class);
        assertNotNull(transactional, "P0 submit must stay in one transactional boundary");
        assertTrue(Arrays.asList(transactional.rollbackFor()).contains(Exception.class),
                "P0 submit must roll back feedback and recordbook writes if event creation fails");
    }
}
