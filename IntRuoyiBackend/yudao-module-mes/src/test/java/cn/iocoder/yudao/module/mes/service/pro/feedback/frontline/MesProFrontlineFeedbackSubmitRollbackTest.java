package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitAuthorizationService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProFrontlineFeedbackSubmitRollbackTest {

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

    private MesProFrontlineFeedbackSubmitService submitService;

    @BeforeEach
    void setUp() {
        submitService = new MesProFrontlineFeedbackSubmitServiceImpl(
                feedbackService,
                recordbookEntryService,
                processPoolSubmitEventService,
                submitAuthorizationService,
                lossReasonValidator,
                new MesProFrontlineFeedbackPayloadSplitter());
    }

    @Test
    void submitShouldDeclareRollbackForAnyException() throws NoSuchMethodException {
        Method submit = MesProFrontlineFeedbackSubmitServiceImpl.class
                .getMethod("submit", cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO.class);

        Transactional transactional = submit.getAnnotation(Transactional.class);
        assertNotNull(transactional);
        assertArrayEquals(new Class[]{Exception.class}, transactional.rollbackFor());
    }

    @Test
    void shouldPropagateProcessPoolFailureToTriggerTransactionRollback() {
        when(feedbackService.createFeedback(any())).thenReturn(501L);
        when(recordbookEntryService.createOriginalEntry(any()))
                .thenReturn(new MesProFrontlineRecordbookEntryResult(701L, 702L));
        when(processPoolSubmitEventService.createSubmitEvent(any()))
                .thenThrow(new IllegalStateException("F1 process pool event service missing"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertThrows(IllegalStateException.class,
                    () -> submitService.submit(MesProFrontlineFeedbackSubmitTestData.buildSubmitReq()));
        }

        verify(feedbackService).createFeedback(any());
        verify(feedbackService).submitFeedback(501L);
        verify(recordbookEntryService).createOriginalEntry(any());
        verify(processPoolSubmitEventService).createSubmitEvent(any());
    }
}
