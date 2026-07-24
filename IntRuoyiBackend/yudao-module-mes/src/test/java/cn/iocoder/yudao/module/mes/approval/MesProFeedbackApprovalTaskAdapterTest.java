package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineEntry;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProFeedbackApprovalTaskAdapterTest {

    @Mock
    private MesProFeedbackMapper feedbackMapper;
    @Mock
    private MesProFeedbackService feedbackService;
    @InjectMocks
    private MesProFeedbackApprovalTaskAdapter adapter;

    @Test
    void metadataDeclaresMesFeedbackRolloutContract() {
        assertEquals(ApprovalModuleCode.MES_FEEDBACK, adapter.getModuleCode());
        assertEquals("MES 报工审批", adapter.getModuleName());
        assertEquals("mes-feedback-approval", adapter.getProviderCode());
        assertEquals("phase8", adapter.getProviderVersion());
        assertTrue(adapter.getSupportedViewTypes().containsAll(Set.of(
                ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE, ApprovalTaskViewType.MY_INITIATED)));
        assertTrue(adapter.getCapabilities().contains(ApprovalTaskCapability.TIMELINE));
        assertTrue(adapter.getCapabilities().contains(ApprovalTaskCapability.AUDIT));
        assertFalse(adapter.getCapabilities().contains(ApprovalTaskCapability.REMINDER));
        assertFalse(adapter.getCapabilities().contains(ApprovalTaskCapability.SIGNATURE_AUTHORIZATION));
    }

    @Test
    void pageTodoMapsApprovingFeedbackAssignedToCurrentApprover() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 24, 8, 30);
        MesProFeedbackDO feedback = buildFeedback(9001L, "FB-9001",
                MesProFeedbackStatusEnum.APPROVING.getStatus(), createdAt);
        when(feedbackMapper.selectUnifiedApprovalList(100L, null,
                List.of(MesProFeedbackStatusEnum.APPROVING.getStatus()), "FB"))
                .thenReturn(List.of(feedback));

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.MES_FEEDBACK, "FB", 1, 10));

        assertEquals(1L, page.getTotal());
        ApprovalTaskSummary summary = page.getList().get(0);
        assertEquals("MES_FEEDBACK:MES_PRO_FEEDBACK:9001", summary.getId());
        assertEquals(ApprovalModuleCode.MES_FEEDBACK, summary.getModuleCode());
        assertEquals("MES_PRO_FEEDBACK", summary.getSourceTaskType());
        assertEquals("9001", summary.getSourceTaskId());
        assertEquals("9001", summary.getBusinessKey());
        assertEquals("生产报工 FB-9001", summary.getBusinessTitle());
        assertEquals("FB-9001", summary.getBusinessCode());
        assertEquals("审批中", summary.getBusinessStatus());
        assertEquals("APPROVING", summary.getCurrentNodeCode());
        assertEquals("当前审批人", summary.getCurrentNodeName());
        assertEquals(200L, summary.getInitiatorUserId());
        assertEquals(100L, summary.getAssigneeUserId());
        assertEquals(Boolean.TRUE, summary.getRequiresSignature());
        assertEquals("/mes/pro/feedback", summary.getDetailRoute());
        assertEquals("9001", summary.getDetailQuery().get("feedbackId"));
        assertTrue(summary.getAvailableActions().contains("PROCESS_IN_MODULE"));
        assertTrue(summary.getAvailableActions().contains("APPROVE"));
        assertTrue(summary.getAvailableActions().contains("REJECT"));
        assertTrue(summary.getCapabilities().contains(ApprovalTaskCapability.TIMELINE));
        assertTrue(summary.getCapabilities().contains(ApprovalTaskCapability.AUDIT));
    }

    @Test
    void listTimelineRequiresRealFeedbackAndMapsDomainAuditEvidence() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 24, 8, 30);
        LocalDateTime approvedAt = LocalDateTime.of(2026, 6, 24, 9, 5);
        MesProFeedbackDO feedback = buildFeedback(9001L, "FB-9001",
                MesProFeedbackStatusEnum.FINISHED.getStatus(), createdAt);
        feedback.setUpdateTime(approvedAt);
        when(feedbackMapper.selectById(9001L)).thenReturn(feedback);

        List<ApprovalTaskTimelineEntry> timeline = adapter.listTimeline(ApprovalTaskTimelineQueryContext.of(
                100L, ApprovalModuleCode.MES_FEEDBACK, "MES_PRO_FEEDBACK", "9001", "9001", null));

        assertEquals(2, timeline.size());
        ApprovalTaskTimelineEntry submitted = timeline.get(0);
        assertEquals("MES_FEEDBACK:9001:SUBMITTED", submitted.getId());
        assertEquals("提交报工审批", submitted.getNodeName());
        assertEquals("SUBMITTED", submitted.getAction());
        assertEquals(200L, submitted.getActorUserId());
        assertEquals("MES_PRO_FEEDBACK", submitted.getEvidenceType());

        ApprovalTaskTimelineEntry approved = timeline.get(1);
        assertEquals("MES_FEEDBACK:9001:APPROVED", approved.getId());
        assertEquals("报工审批", approved.getNodeName());
        assertEquals("APPROVED", approved.getAction());
        assertEquals("审批通过", approved.getActionLabel());
        assertEquals(100L, approved.getActorUserId());
        assertEquals(approvedAt, approved.getActedAt());
        assertEquals("MES_PRO_FEEDBACK_APPROVAL", approved.getEvidenceType());
    }

    @Test
    void listTimelineFailsFastWhenUserIsNotParticipant() {
        MesProFeedbackDO feedback = buildFeedback(9001L, "FB-9001",
                MesProFeedbackStatusEnum.APPROVING.getStatus(), LocalDateTime.of(2026, 6, 24, 8, 30));
        when(feedbackMapper.selectById(9001L)).thenReturn(feedback);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> adapter.listTimeline(ApprovalTaskTimelineQueryContext.of(
                        999L, ApprovalModuleCode.MES_FEEDBACK, "MES_PRO_FEEDBACK", "9001", "9001", null)));

        assertEquals("MES_FEEDBACK_TIMELINE_ACCESS_DENIED: 9001", ex.getMessage());
    }

    @Test
    void pageTodoUsesNullApproverFilterWhenGlobalViewEnabled() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 24, 8, 30);
        MesProFeedbackDO feedback = buildFeedback(9002L, "FB-9002",
                MesProFeedbackStatusEnum.APPROVING.getStatus(), createdAt);
        when(feedbackMapper.selectUnifiedApprovalList(null, null,
                List.of(MesProFeedbackStatusEnum.APPROVING.getStatus()), null))
                .thenReturn(List.of(feedback));

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.MES_FEEDBACK, null, 1, 10, true));

        assertEquals(1L, page.getTotal());
        assertEquals("MES_FEEDBACK:MES_PRO_FEEDBACK:9002", page.getList().get(0).getId());
    }

    @Test
    void listTimelineAllowsGlobalViewForNonParticipant() {
        MesProFeedbackDO feedback = buildFeedback(9003L, "FB-9003",
                MesProFeedbackStatusEnum.FINISHED.getStatus(), LocalDateTime.of(2026, 6, 24, 8, 30));
        when(feedbackMapper.selectById(9003L)).thenReturn(feedback);

        List<ApprovalTaskTimelineEntry> timeline = adapter.listTimeline(ApprovalTaskTimelineQueryContext.of(
                999L, ApprovalModuleCode.MES_FEEDBACK, "MES_PRO_FEEDBACK", "9003", "9003", null, true));

        assertEquals(2, timeline.size());
    }

    @Test
    void reviewApproveDelegatesToFormalFeedbackService() {
        MesProFeedbackDO feedback = buildFeedback(9001L, "FB-9001",
                MesProFeedbackStatusEnum.APPROVING.getStatus(), LocalDateTime.of(2026, 6, 24, 8, 30));
        when(feedbackMapper.selectById(9001L)).thenReturn(feedback);

        adapter.review(ApprovalTaskReviewContext.of(100L, ApprovalModuleCode.MES_FEEDBACK, "MES_PRO_FEEDBACK",
                "9001", "9001", null, ApprovalTaskReviewResult.APPROVE, null, "secret", false));

        verify(feedbackService).approveFeedback(9001L);
    }

    @Test
    void reviewRejectDelegatesToFormalFeedbackService() {
        MesProFeedbackDO feedback = buildFeedback(9001L, "FB-9001",
                MesProFeedbackStatusEnum.APPROVING.getStatus(), LocalDateTime.of(2026, 6, 24, 8, 30));
        when(feedbackMapper.selectById(9001L)).thenReturn(feedback);

        adapter.review(ApprovalTaskReviewContext.of(100L, ApprovalModuleCode.MES_FEEDBACK, "MES_PRO_FEEDBACK",
                "9001", "9001", null, ApprovalTaskReviewResult.REJECT, "数量不一致", "secret", false));

        verify(feedbackService).rejectFeedback(9001L, "数量不一致");
    }

    @Test
    void pageDoneMapsRejectedFeedbackReturnedToDraftWithReason() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 24, 8, 30);
        LocalDateTime rejectedAt = LocalDateTime.of(2026, 6, 24, 8, 55);
        MesProFeedbackDO feedback = buildFeedback(9004L, "FB-9004",
                MesProFeedbackStatusEnum.PREPARE.getStatus(), createdAt);
        feedback.setUpdateTime(rejectedAt);
        feedback.setRemark("数量不一致");
        when(feedbackMapper.selectUnifiedApprovalList(100L, null,
                List.of(MesProFeedbackStatusEnum.UNCHECK.getStatus(),
                        MesProFeedbackStatusEnum.FINISHED.getStatus(),
                        MesProFeedbackStatusEnum.PREPARE.getStatus()), "FB"))
                .thenReturn(List.of(feedback));

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.DONE, ApprovalModuleCode.MES_FEEDBACK, "FB", 1, 10));

        assertEquals(1L, page.getTotal());
        ApprovalTaskSummary summary = page.getList().get(0);
        assertEquals(ApprovalTaskReviewResult.REJECT, summary.getApprovalResult());
        assertEquals("数量不一致", summary.getApprovalRemark());
        assertEquals(rejectedAt, summary.getTaskCompletedAt());
    }

    @Test
    void reviewFailsFastWhenCurrentUserIsNotApprover() {
        MesProFeedbackDO feedback = buildFeedback(9001L, "FB-9001",
                MesProFeedbackStatusEnum.APPROVING.getStatus(), LocalDateTime.of(2026, 6, 24, 8, 30));
        when(feedbackMapper.selectById(9001L)).thenReturn(feedback);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> adapter.review(ApprovalTaskReviewContext.of(999L, ApprovalModuleCode.MES_FEEDBACK,
                        "MES_PRO_FEEDBACK", "9001", "9001", null, ApprovalTaskReviewResult.APPROVE, null, "secret", false)));

        assertEquals("MES_FEEDBACK_REVIEW_ACCESS_DENIED: 9001", ex.getMessage());
    }

    private static MesProFeedbackDO buildFeedback(Long id, String code, Integer status, LocalDateTime createdAt) {
        MesProFeedbackDO feedback = new MesProFeedbackDO();
        feedback.setId(id);
        feedback.setCode(code);
        feedback.setStatus(status);
        feedback.setFeedbackUserId(200L);
        feedback.setApproveUserId(100L);
        feedback.setFeedbackQuantity(new BigDecimal("12.5"));
        feedback.setCreateTime(createdAt);
        feedback.setUpdateTime(createdAt.plusMinutes(5));
        return feedback;
    }
}
