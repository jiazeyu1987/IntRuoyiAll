package cn.iocoder.yudao.module.mes.service.pro.feedback;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.approval.MesFeedbackFormalReviewProjection;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.task.MesProTaskService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.iocoder.yudao.module.mes.service.wm.itemconsume.MesWmItemConsumeService;
import cn.iocoder.yudao.module.mes.service.wm.productproduce.MesWmProductProduceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.approval.MesFeedbackApprovalErrorCodeConstants.FORMAL_FEEDBACK_LEGACY_REVIEW_FORBIDDEN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProFeedbackLegacyFormalGuardTest {
    @Mock MesFeedbackFormalReviewProjection formalReviewProjection;
    @Mock MesProFeedbackMapper feedbackMapper;
    @Mock MesProTaskService taskService;
    @Mock MesProWorkOrderService workOrderService;
    @Mock MesProRouteProcessService routeProcessService;
    @Mock MesWmItemConsumeService itemConsumeService;
    @Mock MesWmProductProduceService productProduceService;
    @Mock MesProEdhrNonconformanceReviewService nonconformanceReviewService;
    @InjectMocks MesProFeedbackServiceImpl service;

    @Test void directApprovalRejectsBeforeAllInventoryTaskAndWorkOrderSideEffects() {
        doThrow(exception(FORMAL_FEEDBACK_LEGACY_REVIEW_FORBIDDEN, 5660L))
                .when(formalReviewProjection).assertLegacyOperationAllowed(5660L);
        ServiceException error = assertThrows(ServiceException.class, () -> service.approveFeedback(5660L));
        assertEquals(FORMAL_FEEDBACK_LEGACY_REVIEW_FORBIDDEN.getCode(), error.getCode());
        verifyNoInteractions(feedbackMapper, taskService, workOrderService, routeProcessService,
                itemConsumeService, productProduceService, nonconformanceReviewService);
    }

    @Test void bothRejectOverloadsRejectBeforeAnyFeedbackStateWrite() {
        doThrow(exception(FORMAL_FEEDBACK_LEGACY_REVIEW_FORBIDDEN, 5660L))
                .when(formalReviewProjection).assertLegacyOperationAllowed(5660L);
        assertThrows(ServiceException.class, () -> service.rejectFeedback(5660L));
        assertThrows(ServiceException.class, () -> service.rejectFeedback(5660L, "不能重复驳回"));
        verify(formalReviewProjection, times(2)).assertLegacyOperationAllowed(5660L);
        verifyNoInteractions(feedbackMapper, taskService, workOrderService, routeProcessService,
                itemConsumeService, productProduceService, nonconformanceReviewService);
    }

    @Test void unlinkedLegacyRejectStillReturnsToDraftWithOriginalReason() {
        MesProFeedbackDO source = MesProFeedbackDO.builder().id(5661L)
                .status(MesProFeedbackStatusEnum.APPROVING.getStatus()).build();
        when(feedbackMapper.selectById(5661L)).thenReturn(source);
        service.rejectFeedback(5661L, "数量错误");
        verify(formalReviewProjection).assertLegacyOperationAllowed(5661L);
        verify(feedbackMapper).updateById(argThat((MesProFeedbackDO update) -> update.getId().equals(5661L)
                && update.getStatus().equals(MesProFeedbackStatusEnum.PREPARE.getStatus())
                && "数量错误".equals(update.getRemark())));
        verifyNoInteractions(taskService, workOrderService, itemConsumeService, productProduceService);
    }
}
