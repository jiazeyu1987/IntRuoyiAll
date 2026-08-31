package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitAuthorizationService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.ActiveOrderSnapshotResolver;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProFrontlineFeedbackRouteOrderGateTest {

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
    void shouldKeepRouteAsContextWithoutBlockingOnPredecessorStatusInRawPayload() {
        when(processPoolSubmitEventService.findExistingSubmitEvent(any())).thenReturn(Optional.empty());
        when(feedbackService.createFrontlineFeedback(any())).thenReturn(501L);
        when(processPoolSubmitEventService.createSubmitEvent(any())).thenReturn(801L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertEquals(801L, submitService.submit(MesProFrontlineFeedbackSubmitTestData.buildSubmitReq())
                    .getProcessPoolEventId());
        }

        verify(processPoolSubmitEventService).createSubmitEvent(argThat(payload -> {
            assertEquals(21L, payload.getRouteId());
            assertEquals(71L, payload.getRouteProcessId());
            assertEquals("WAITING",
                    ((java.util.Map<?, ?>) payload.getRawPayload().get("routePredecessorStatuses")).get("P10"));
            return true;
        }));
    }
}
