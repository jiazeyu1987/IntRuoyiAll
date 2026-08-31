package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitRespVO;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitAuthorizationService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.ActiveOrderSnapshotResolver;
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
import java.util.Map;
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
    private MesProFeedbackMaterialService feedbackMaterialService;
    @Mock
    private MesProcessPoolSubmitEventService processPoolSubmitEventService;
    @Mock
    private MesFrontlineSubmitAuthorizationService submitAuthorizationService;
    @Mock
    private MesFrontlineLossReasonValidator lossReasonValidator;
    @Mock
    private MesFrontlineDeviceParameterValidator deviceParameterValidator;
    @Mock
    private MesFrontlineParameterAuditService parameterAuditService;
    @Mock
    private MesMdAutoCodeRecordService autoCodeRecordService;
    @Mock
    private MesProBatchRecordExecutionSignatureService signatureService;
    @Mock
    private ActiveOrderSnapshotResolver activeOrderSnapshotResolver;

    private MesProFrontlineFeedbackSubmitService submitService;

    @BeforeEach
    void setUp() {
        submitService = new MesProFrontlineFeedbackSubmitServiceImpl(
                feedbackService,
                feedbackMaterialService,
                processPoolSubmitEventService,
                submitAuthorizationService,
                parameterAuditService,
                new MesProFrontlineFeedbackMaterialSubmissionValidator(lossReasonValidator),
                new MesProFrontlineFeedbackPayloadSplitter(),
                autoCodeRecordService,
                signatureService,
                activeOrderSnapshotResolver);
        MesProFrontlineFeedbackSubmitSnapshotTestSupport.stubAuthorization(submitAuthorizationService);
        MesProFrontlineFeedbackSubmitTestData.stubLossReasonValidator(lossReasonValidator);
        MesProFrontlineFeedbackSubmitTestData.stubActiveOrderSnapshot(activeOrderSnapshotResolver);
        org.mockito.Mockito.lenient().when(parameterAuditService.resolveAndApply(any()))
                .thenReturn(MesFrontlineParameterAuditResult.empty());
        org.mockito.Mockito.lenient().when(signatureService.recordProductionSubmitSignature(any(), any(), any()))
                .thenReturn(4001L);
    }

    @Test
    void shouldCreateFeedbackSourceAndProcessPoolEventInOneTransaction() throws Exception {
        assertSubmitMethodHasRollbackBoundary();
        when(processPoolSubmitEventService.findExistingSubmitEvent(any())).thenReturn(Optional.empty());
        when(feedbackService.createFrontlineFeedback(any())).thenReturn(501L);
        when(processPoolSubmitEventService.createSubmitEvent(any())).thenReturn(801L);

        MesProFrontlineFeedbackSubmitRespVO respVO;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            respVO = submitService.submit(MesProFrontlineFeedbackSubmitTestData.buildSubmitReq());
        }

        assertEquals(501L, respVO.getFeedbackId());
        assertEquals(null, respVO.getRecordbookEntryId());
        assertEquals(null, respVO.getRecordbookEventId());
        assertEquals(801L, respVO.getProcessPoolEventId());

        InOrder inOrder = inOrder(submitAuthorizationService, feedbackService, processPoolSubmitEventService);
        inOrder.verify(submitAuthorizationService).authorize(argThat(command -> {
            assertEquals(9001L, command.loginUserId());
            assertEquals(9001L, command.actualEmployeeId());
            assertEquals(9001L, command.signatureEmployeeId());
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
        inOrder.verify(feedbackService).createFrontlineFeedback(argThat(payload -> {
            assertEquals(new BigDecimal("103.000"), payload.getFeedbackQuantity());
            assertEquals(new BigDecimal("100.500"), payload.getQualifiedQuantity());
            assertEquals(new BigDecimal("2.500"), payload.getUnqualifiedQuantity());
            assertEquals(9001L, payload.getFeedbackUserId());
            return true;
        }));
        inOrder.verify(feedbackService).submitFeedback(501L);
        inOrder.verify(processPoolSubmitEventService).createSubmitEvent(argThat(payload -> {
            assertEquals("P0-SUBMIT-F2-20260730-001", payload.getProcessPoolSubmissionIdempotencyKey());
            assertEquals(501L, payload.getFeedbackId());
            assertEquals(null, payload.getRecordbookEntryId());
            assertEquals(null, payload.getRecordbookEventId());
            assertEquals(901L, ((Map<?, ?>) payload.getRawPayload().get("recordbookSourceSnapshot")).get("recordbookId"));
            assertEquals(4001L, payload.getSignatureId());
            return true;
        }));
    }

    @Test
    void shouldNotCallFormalRecordbookInsideSubmitTransaction() {
        when(processPoolSubmitEventService.findExistingSubmitEvent(any())).thenReturn(Optional.empty());
        when(feedbackService.createFrontlineFeedback(any())).thenReturn(501L);
        when(processPoolSubmitEventService.createSubmitEvent(any())).thenReturn(801L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertEquals(801L, submitService.submit(MesProFrontlineFeedbackSubmitTestData.buildSubmitReq())
                    .getProcessPoolEventId());
        }

        verify(feedbackService).submitFeedback(501L);
        verify(processPoolSubmitEventService).createSubmitEvent(any());
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
