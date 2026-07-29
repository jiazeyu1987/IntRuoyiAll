package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class MesProFrontlineFeedbackRouteOrderGateTest {

    @Test
    void submit_usesRouteOnlyAsContextAndDoesNotRequirePredecessorCompletion() {
        MesProFeedbackService feedbackService = mock(MesProFeedbackService.class);
        MesProFrontlineRecordbookEntryService recordbookEntryService = mock(MesProFrontlineRecordbookEntryService.class);
        MesProcessPoolSubmitEventService processPoolSubmitEventService = mock(MesProcessPoolSubmitEventService.class);
        MesProFrontlineFeedbackSubmitService submitService = new MesProFrontlineFeedbackSubmitServiceImpl(
                feedbackService, recordbookEntryService, processPoolSubmitEventService,
                new MesProFrontlineFeedbackPayloadSplitter());
        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackPayloadSplitterTest.buildReq();
        when(feedbackService.createFeedback(any())).thenReturn(501L);
        when(recordbookEntryService.createOriginalEntry(any()))
                .thenReturn(new MesProFrontlineRecordbookEntryResult(701L, 702L));
        when(processPoolSubmitEventService.createSubmitEvent(any())).thenReturn(801L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertEquals(801L, submitService.submit(reqVO).getProcessPoolEventId());
        }
    }
}
