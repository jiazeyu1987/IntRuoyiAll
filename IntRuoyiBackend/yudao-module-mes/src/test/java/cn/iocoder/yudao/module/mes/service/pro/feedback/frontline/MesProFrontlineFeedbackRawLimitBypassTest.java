package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesProFrontlineFeedbackRawLimitBypassTest {

    @Test
    void submit_preservesRawOverLimitValuesForRecordbookAndProcessPoolEvent() {
        MesProFeedbackService feedbackService = mock(MesProFeedbackService.class);
        MesProFrontlineRecordbookEntryService recordbookEntryService = mock(MesProFrontlineRecordbookEntryService.class);
        MesProcessPoolSubmitEventService processPoolSubmitEventService = mock(MesProcessPoolSubmitEventService.class);
        MesProFrontlineFeedbackSubmitService submitService = new MesProFrontlineFeedbackSubmitServiceImpl(
                feedbackService, recordbookEntryService, processPoolSubmitEventService,
                new MesProFrontlineFeedbackPayloadSplitter());
        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackPayloadSplitterTest.buildReq();
        reqVO.getRecordbookPayload().setEquipmentParameters(Map.of(
                "temperature", new BigDecimal("10"),
                "pressure", new BigDecimal("50")));
        reqVO.setRawPayload(Map.of("temperature", new BigDecimal("10"), "pressure", new BigDecimal("50")));
        when(feedbackService.createFeedback(any())).thenReturn(501L);
        when(recordbookEntryService.createOriginalEntry(any()))
                .thenReturn(new MesProFrontlineRecordbookEntryResult(701L, 702L));
        when(processPoolSubmitEventService.createSubmitEvent(any())).thenReturn(801L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            submitService.submit(reqVO);
        }

        ArgumentCaptor<MesProFrontlineRecordbookEntryPayload> recordbookCaptor =
                ArgumentCaptor.forClass(MesProFrontlineRecordbookEntryPayload.class);
        verify(recordbookEntryService).createOriginalEntry(recordbookCaptor.capture());
        assertEquals(new BigDecimal("10"),
                ((Map<?, ?>) recordbookCaptor.getValue().getEntryContent().get("equipmentParameters")).get("temperature"));
        assertEquals(new BigDecimal("50"),
                ((Map<?, ?>) recordbookCaptor.getValue().getEntryContent().get("equipmentParameters")).get("pressure"));

        verify(processPoolSubmitEventService).createSubmitEvent(org.mockito.ArgumentMatchers.argThat(event -> {
            assertEquals(new BigDecimal("10"), event.getEquipmentParameters().get("temperature"));
            assertEquals(new BigDecimal("50"), event.getEquipmentParameters().get("pressure"));
            assertSame(reqVO.getRawPayload(), event.getRawPayload());
            return true;
        }));
    }
}
