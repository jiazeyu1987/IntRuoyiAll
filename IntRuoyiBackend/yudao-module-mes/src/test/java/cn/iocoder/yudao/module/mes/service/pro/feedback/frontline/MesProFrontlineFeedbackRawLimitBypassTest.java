package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitAuthorizationService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProFrontlineFeedbackRawLimitBypassTest {

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
    @Mock
    private MesMdAutoCodeRecordService autoCodeRecordService;
    @Mock
    private MesProBatchRecordExecutionSignatureService signatureService;

    private MesProFrontlineFeedbackSubmitService submitService;

    @BeforeEach
    void setUp() {
        submitService = new MesProFrontlineFeedbackSubmitServiceImpl(
                feedbackService,
                recordbookEntryService,
                processPoolSubmitEventService,
                submitAuthorizationService,
                lossReasonValidator,
                new MesProFrontlineFeedbackPayloadSplitter(),
                autoCodeRecordService,
                signatureService);
        org.mockito.Mockito.lenient().when(signatureService.recordProductionSubmitSignature(any(), any(), any()))
                .thenReturn(4001L);
    }

    @Test
    void shouldPreserveRawOutOfLimitEquipmentValuesWithoutClippingOrRejecting() {
        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        Map<String, Object> outOfLimitParameters = new LinkedHashMap<>();
        outOfLimitParameters.put("temperature", new BigDecimal("10"));
        outOfLimitParameters.put("pressure", new BigDecimal("50"));
        reqVO.getRecordbookPayload().setEquipmentParameters(outOfLimitParameters);
        reqVO.getRawPayload().put("equipmentParameters", outOfLimitParameters);

        when(processPoolSubmitEventService.findExistingSubmitEvent(any())).thenReturn(Optional.empty());
        when(feedbackService.createFrontlineFeedback(any())).thenReturn(501L);
        when(recordbookEntryService.createOriginalEntry(any()))
                .thenReturn(new MesProFrontlineRecordbookEntryResult(701L, 702L));
        when(processPoolSubmitEventService.createSubmitEvent(any())).thenReturn(801L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertEquals(801L, submitService.submit(reqVO).getProcessPoolEventId());
        }

        verify(recordbookEntryService).createOriginalEntry(argThat(payload -> {
            Map<?, ?> equipment = (Map<?, ?>) payload.getEntryContent().get("equipmentParameters");
            assertEquals(new BigDecimal("10"), equipment.get("temperature"));
            assertEquals(new BigDecimal("50"), equipment.get("pressure"));
            return true;
        }));
        verify(processPoolSubmitEventService).createSubmitEvent(argThat(payload -> {
            assertEquals(new BigDecimal("10"), payload.getEquipmentParameters().get("temperature"));
            assertEquals(new BigDecimal("50"), payload.getEquipmentParameters().get("pressure"));
            assertEquals(outOfLimitParameters, payload.getRawPayload().get("equipmentParameters"));
            return true;
        }));
    }
}
