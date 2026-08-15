package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProFrontlineFeedbackSubmitDetailContractTest {

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
                deviceParameterValidator,
                new MesProFrontlineFeedbackPayloadSplitter(),
                autoCodeRecordService,
                signatureService);
        MesProFrontlineFeedbackSubmitSnapshotTestSupport.stubAuthorization(submitAuthorizationService);
        org.mockito.Mockito.lenient().when(signatureService.recordProductionSubmitSignature(any(), any(), any()))
                .thenReturn(4001L);
    }

    @Test
    void feedbackPayloadMustExposeStructuredProductionSubmitFields() throws Exception {
        assertField(MesProFrontlineFeedbackPayloadReqVO.class, "lossDetails");
        assertField(MesProFrontlineFeedbackPayloadReqVO.class, "selectedDevice");
        assertField(MesProFrontlineFeedbackPayloadReqVO.class, "deviceParameterReadings");
        assertField(MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO.class, "textValue");
    }

    @Test
    void shouldRejectWhenLossQuantityDoesNotEqualLossDetailSumBeforeWritingAnyRecord() {
        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        reqVO.getFeedbackPayload().setLossQuantity(new BigDecimal("5.000"));
        reqVO.getRawPayload().put("lossDetails", List.of(
                Map.of("reasonId", 8301L, "reasonCode", "LOSS-001", "reasonName", "密封件划伤",
                        "quantity", new BigDecimal("3.000")),
                Map.of("reasonId", 8302L, "reasonCode", "LOSS-002", "reasonName", "装配不到位",
                        "quantity", new BigDecimal("1.000"))));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertThrows(RuntimeException.class, () -> submitService.submit(reqVO));
        }

        verifyNoInteractions(submitAuthorizationService);
        verify(lossReasonValidator, never()).requireEnabledLossReason(any(), any(), any());
        verify(feedbackService, never()).createFrontlineFeedback(any());
        verifyNoInteractions(recordbookEntryService, processPoolSubmitEventService);
    }

    @Test
    void shouldPersistStructuredLossDetailsSelectedDeviceAndParameterReadings() {
        when(processPoolSubmitEventService.findExistingSubmitEvent(any())).thenReturn(Optional.empty());
        when(feedbackService.createFrontlineFeedback(any())).thenReturn(501L);
        when(recordbookEntryService.createOriginalEntry(any()))
                .thenReturn(new MesProFrontlineRecordbookEntryResult(701L, 702L));
        when(processPoolSubmitEventService.createSubmitEvent(any())).thenReturn(801L);
        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        reqVO.getFeedbackPayload().setDeviceParameterReadings(List.of(
                new MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO()
                        .setDeviceId(501L)
                        .setDeviceCode("PT-A-03")
                        .setDeviceName("压力泵")
                        .setParameterCode("visualCheck")
                        .setParameterName("外观确认")
                        .setTextValue("符合要求")
                        .setParameterStatus("NORMAL")));
        when(lossReasonValidator.requireSnapshotLossReasons(any(), eq(reqVO.getFeedbackPayload().getLossDetails()),
                eq(new BigDecimal("2.500"))))
                .thenReturn(List.of(new MesFrontlineLossReasonSnapshot(8301L, "LOSS-001", "正常损耗")));

        reqVO.getRawPayload().put("lossDetails", List.of(
                Map.of("reasonId", 8301L, "reasonCode", "LOSS-001", "reasonName", "正常损耗",
                        "quantity", new BigDecimal("2.500"))));
        reqVO.getRawPayload().put("selectedDevice",
                Map.of("deviceId", 501L, "deviceCode", "PT-A-03", "deviceName", "压力泵"));
        reqVO.getRawPayload().put("deviceParameterReadings", List.of(
                Map.of("deviceId", 501L, "parameterCode", "pressure", "parameterName", "压力",
                        "value", new BigDecimal("50"), "unit", "MPa", "lowerLimit", new BigDecimal("20"),
                        "upperLimit", new BigDecimal("40"), "parameterStatus", "ABOVE_UPPER")));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertEquals(801L, submitService.submit(reqVO).getProcessPoolEventId());
        }

        verify(processPoolSubmitEventService).createSubmitEvent(argThat(payload -> {
            assertNotNull(payload.getRawPayload().get("lossDetails"));
            assertNotNull(payload.getRawPayload().get("selectedDevice"));
            assertNotNull(payload.getRawPayload().get("deviceParameterReadings"));
            Object firstReading = ((List<?>) payload.getRawPayload().get("deviceParameterReadings"))
                    .stream().findFirst().orElseThrow();
            Object parameterStatus = firstReading instanceof Map<?, ?> reading
                    ? reading.get("parameterStatus")
                    : ((MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO) firstReading)
                    .getParameterStatus();
            Object textValue = firstReading instanceof Map<?, ?> reading
                    ? reading.get("textValue")
                    : ((MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO) firstReading)
                    .getTextValue();
            assertEquals("NORMAL", parameterStatus);
            assertEquals("符合要求", textValue);
            return true;
        }));
    }

    private static Field assertField(Class<?> targetClass, String fieldName) throws Exception {
        Field field = targetClass.getDeclaredField(fieldName);
        assertNotNull(field);
        return field;
    }
}
