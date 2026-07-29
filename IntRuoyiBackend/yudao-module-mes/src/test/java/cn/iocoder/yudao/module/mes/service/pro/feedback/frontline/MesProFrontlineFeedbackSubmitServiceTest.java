package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitRespVO;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventCreateReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesProFrontlineFeedbackSubmitServiceTest {

    private final MesProFeedbackService feedbackService = mock(MesProFeedbackService.class);
    private final MesProFrontlineRecordbookEntryService recordbookEntryService =
            mock(MesProFrontlineRecordbookEntryService.class);
    private final MesProcessPoolSubmitEventService processPoolSubmitEventService =
            mock(MesProcessPoolSubmitEventService.class);
    private final MesProFrontlineFeedbackPayloadSplitter payloadSplitter =
            new MesProFrontlineFeedbackPayloadSplitter();
    private final MesProFrontlineFeedbackSubmitService submitService =
            new MesProFrontlineFeedbackSubmitServiceImpl(feedbackService, recordbookEntryService,
                    processPoolSubmitEventService, payloadSplitter);

    @Test
    void submit_createsFeedbackRecordbookEntryAndProcessPoolEventInOrder() {
        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackPayloadSplitterTest.buildReq();
        when(feedbackService.createFeedback(any())).thenReturn(501L);
        when(recordbookEntryService.createOriginalEntry(any()))
                .thenReturn(new MesProFrontlineRecordbookEntryResult(701L, 702L));
        when(processPoolSubmitEventService.createSubmitEvent(any())).thenReturn(801L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            MesProFrontlineFeedbackSubmitRespVO respVO = submitService.submit(reqVO);

            assertEquals(501L, respVO.getFeedbackId());
            assertEquals(701L, respVO.getRecordbookEntryId());
            assertEquals(702L, respVO.getRecordbookEventId());
            assertEquals(801L, respVO.getProcessPoolEventId());
        }

        InOrder inOrder = inOrder(feedbackService, recordbookEntryService, processPoolSubmitEventService);
        inOrder.verify(feedbackService).createFeedback(any(MesProFeedbackSaveReqVO.class));
        inOrder.verify(feedbackService).submitFeedback(501L);
        inOrder.verify(recordbookEntryService).createOriginalEntry(any());
        inOrder.verify(processPoolSubmitEventService).createSubmitEvent(any());
    }

    @Test
    void submit_passesFormalSourceIdsAndIdentityToProcessPoolEvent() {
        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackPayloadSplitterTest.buildReq();
        when(feedbackService.createFeedback(any())).thenReturn(501L);
        when(recordbookEntryService.createOriginalEntry(any()))
                .thenReturn(new MesProFrontlineRecordbookEntryResult(701L, 702L));
        when(processPoolSubmitEventService.createSubmitEvent(any())).thenReturn(801L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            submitService.submit(reqVO);
        }

        verify(processPoolSubmitEventService).createSubmitEvent(org.mockito.ArgumentMatchers.argThat(event ->
                event.getFeedbackId().equals(501L)
                        && event.getRecordbookEntryId().equals(701L)
                        && event.getRecordbookEventId().equals(702L)
                        && event.getActualEmployeeId().equals(1001L)
                        && event.getSignatureEmployeeId().equals(1001L)
                        && event.getDeviceAccountUserId().equals(9001L)
                        && event.getOutputQuantity().compareTo(new BigDecimal("12.500")) == 0
                        && event.getLossQuantity().compareTo(new BigDecimal("1.250")) == 0));
    }

    @Test
    void submit_rejectsSignatureEmployeeMismatchBeforeWritingAnything() {
        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackPayloadSplitterTest.buildReq()
                .setSignatureEmployeeId(2002L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> submitService.submit(reqVO));
        }

        org.mockito.Mockito.verifyNoInteractions(feedbackService, recordbookEntryService, processPoolSubmitEventService);
    }
}
