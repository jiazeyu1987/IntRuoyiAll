package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.mes.approval.MesFeedbackFormalReviewProjectionTest.*;

@ExtendWith(MockitoExtension.class)
class MesProFeedbackFormalApprovalTaskAdapterTest {
    @Mock MesProFeedbackMapper feedbackMapper;
    @Mock MesProFeedbackService feedbackService;
    @Mock MesFeedbackFormalReviewProjection projection;
    @InjectMocks MesProFeedbackApprovalTaskAdapter adapter;

    @Test void approvedSourceDisappearsFromTodoWithoutChangingRawStatus() {
        MesProFeedbackDO source = feedback(5660L).setApproveUserId(341L);
        when(feedbackMapper.selectUnifiedApprovalList(eq(341L), isNull(), anyList(), isNull())).thenReturn(List.of(source));
        when(projection.read(List.of(source))).thenReturn(Map.of(5660L, approved()));
        var result = adapter.page(query(341L, ApprovalTaskViewType.TODO, 1, 10));
        assertEquals(0L, result.getTotal()); assertTrue(result.getList().isEmpty());
        assertEquals(MesProFeedbackStatusEnum.APPROVING.getStatus(), source.getStatus());
        verifyNoInteractions(feedbackService);
    }

    @Test void actualReviewerSeesDoneAndFormalActorTimeReasonSignatureEvenWhenOriginalApproverDiffers() {
        MesProFeedbackDO source = feedback(5660L);
        when(projection.reviewedFeedbackIds(341L)).thenReturn(List.of(5660L));
        when(feedbackMapper.selectUnifiedFormalApprovalSources(List.of(5660L), null)).thenReturn(List.of(source));
        when(projection.read(List.of(source))).thenReturn(Map.of(5660L, approved()));
        var page = adapter.page(query(341L, ApprovalTaskViewType.DONE, 1, 10));
        assertEquals(1L, page.getTotal());
        var row = page.getList().get(0);
        assertEquals(341L, row.getAssigneeUserId()); assertEquals(ApprovalTaskReviewResult.APPROVE, row.getApprovalResult());
        assertEquals(approved().review().getReviewedAt(), row.getTaskCompletedAt());
        assertEquals("旧冻结事件正式复核", row.getApprovalRemark());
        assertEquals(Set.of("PROCESS_IN_MODULE"), row.getAvailableActions());
        assertEquals("/mes/pro/process-pool/production-leader", row.getDetailRoute());
        assertEquals(Map.of("eventId", "282240"), row.getDetailQuery());
        assertTrue(row.getBusinessContextTags().contains("复核签名 12743"));
        assertEquals(MesProFeedbackStatusEnum.APPROVING.getStatus(), source.getStatus());
    }

    @Test void designatedApproverDoesNotSeeOtherReviewersDoneAndTimelineIsDenied() {
        MesProFeedbackDO source = feedback(5660L);
        when(feedbackMapper.selectUnifiedApprovalList(eq(340L), isNull(), anyList(), isNull())).thenReturn(List.of(source));
        when(projection.read(List.of(source))).thenReturn(Map.of(5660L, approved()));
        assertEquals(0L, adapter.page(query(340L, ApprovalTaskViewType.DONE, 1, 10)).getTotal());
        when(feedbackMapper.selectById(5660L)).thenReturn(source);
        assertThrows(IllegalStateException.class, () -> adapter.listTimeline(timeline(340L)));
    }

    @Test void pendingFormalTaskOnlyAllowsProcessInModule() {
        MesProFeedbackDO source = feedback(5660L);
        when(feedbackMapper.selectUnifiedApprovalList(eq(340L), isNull(), anyList(), isNull())).thenReturn(List.of(source));
        when(projection.read(List.of(source))).thenReturn(Map.of(5660L,
                new MesFeedbackFormalReviewProjection.Fact(event(282240L, 5660L), null)));
        var row = adapter.page(query(340L, ApprovalTaskViewType.TODO, 1, 10)).getList().get(0);
        assertEquals("PENDING", row.getCurrentNodeCode()); assertNull(row.getTaskCompletedAt());
        assertEquals(Set.of("PROCESS_IN_MODULE"), row.getAvailableActions());
        assertNull(row.getApprovalResult());
    }

    @Test void rejectedFormalDecisionUsesSignedReviewInsteadOfLegacyDraftHeuristic() {
        MesProFeedbackDO source = feedback(5660L).setApproveUserId(341L);
        when(feedbackMapper.selectUnifiedApprovalList(eq(341L), isNull(), anyList(), isNull())).thenReturn(List.of(source));
        when(projection.read(List.of(source))).thenReturn(Map.of(5660L,
                new MesFeedbackFormalReviewProjection.Fact(event(282240L, 5660L), review(282240L, "REJECTED"))));
        var row = adapter.page(query(341L, ApprovalTaskViewType.DONE, 1, 10)).getList().get(0);
        assertEquals(ApprovalTaskReviewResult.REJECT, row.getApprovalResult());
        assertEquals("REJECTED", row.getCurrentNodeCode());
        assertEquals(MesProFeedbackStatusEnum.APPROVING.getStatus(), source.getStatus());
    }

    @Test void mixedRowsFilterBeforePaginationAndKeepConsistentTotals() {
        MesProFeedbackDO source = feedback(5660L).setApproveUserId(341L);
        MesProFeedbackDO legacy = feedback(5661L).setApproveUserId(341L);
        when(feedbackMapper.selectUnifiedApprovalList(eq(341L), isNull(), anyList(), isNull())).thenReturn(List.of(source, legacy));
        when(projection.read(List.of(legacy, source))).thenReturn(Map.of(5660L, approved()));
        var first = adapter.page(query(341L, ApprovalTaskViewType.TODO, 1, 1));
        var second = adapter.page(query(341L, ApprovalTaskViewType.TODO, 2, 1));
        assertEquals(1L, first.getTotal()); assertEquals("5661", first.getList().get(0).getSourceTaskId());
        assertEquals(1L, second.getTotal()); assertTrue(second.getList().isEmpty());
    }

    @Test void timelineUsesOriginalSubmitAndActualReviewFactsForParticipant() {
        MesProFeedbackDO source = feedback(5660L);
        when(feedbackMapper.selectById(5660L)).thenReturn(source);
        when(projection.read(List.of(source))).thenReturn(Map.of(5660L, approved()));
        var entries = adapter.listTimeline(timeline(341L));
        assertEquals(2, entries.size()); assertEquals(342L, entries.get(0).getActorUserId());
        assertEquals(event(282240L, 5660L).getServerSubmitTime(), entries.get(0).getActedAt());
        assertEquals("event:282240/signature:12742", entries.get(0).getDomainReferenceId());
        assertEquals(341L, entries.get(1).getActorUserId());
        assertEquals(approved().review().getReviewedAt(), entries.get(1).getActedAt());
        assertEquals("旧冻结事件正式复核", entries.get(1).getComment());
        assertEquals("event:282240/review:281240/signature:12743", entries.get(1).getDomainReferenceId());
        assertEquals(2, adapter.listTimeline(timeline(342L)).size());
    }

    @Test void adapterAlsoGuardsForgedLegacyReviewRequests() {
        MesProFeedbackDO source = feedback(5660L).setApproveUserId(341L);
        when(feedbackMapper.selectById(5660L)).thenReturn(source);
        doThrow(new IllegalStateException("formal only")).when(projection).assertLegacyOperationAllowed(5660L);
        assertThrows(IllegalStateException.class, () -> adapter.review(ApprovalTaskReviewContext.of(341L,
                ApprovalModuleCode.MES_FEEDBACK, "MES_PRO_FEEDBACK", "5660", "5660", null,
                ApprovalTaskReviewResult.APPROVE, "reason", "test", false)));
        verifyNoInteractions(feedbackService);
    }

    @Test void reviewerDiscoveryKeepsKeywordFilteringAndDeduplicatesBeforePaging() {
        MesProFeedbackDO source = feedback(5660L).setApproveUserId(341L);
        var context = query(341L, ApprovalTaskViewType.DONE, 1, 1).setKeyword("FB-5660");
        when(feedbackMapper.selectUnifiedApprovalList(eq(341L), isNull(), anyList(), eq("FB-5660")))
                .thenReturn(List.of(source));
        when(projection.reviewedFeedbackIds(341L)).thenReturn(List.of(5660L));
        when(feedbackMapper.selectUnifiedFormalApprovalSources(List.of(5660L), "FB-5660"))
                .thenReturn(List.of(source));
        when(projection.read(List.of(source))).thenReturn(Map.of(5660L, approved()));
        assertEquals(1L, adapter.page(context).getTotal());
        assertTrue(adapter.page(context.setPageNo(2)).getList().isEmpty());
        verify(projection, times(2)).read(List.of(source));
    }

    @Test void initiatorGetsFormalDecisionAndGlobalDoneKeepsActualReviewerWithoutScopeExpansion() {
        MesProFeedbackDO source = feedback(5660L);
        when(feedbackMapper.selectUnifiedApprovalList(isNull(), eq(342L), anyList(), isNull()))
                .thenReturn(List.of(source));
        when(projection.read(List.of(source))).thenReturn(Map.of(5660L, approved()));
        var initiated = adapter.page(query(342L, ApprovalTaskViewType.MY_INITIATED, 1, 10));
        assertEquals(1L, initiated.getTotal());
        assertEquals(ApprovalTaskReviewResult.APPROVE, initiated.getList().get(0).getApprovalResult());
        when(feedbackMapper.selectUnifiedApprovalList(isNull(), isNull(), anyList(), isNull()))
                .thenReturn(List.of(source));
        var global = adapter.page(query(999L, ApprovalTaskViewType.DONE, 1, 10).setGlobalView(true));
        assertEquals(1L, global.getTotal()); assertEquals(341L, global.getList().get(0).getAssigneeUserId());
        verify(projection, never()).reviewedFeedbackIds(any());
    }

    private static MesFeedbackFormalReviewProjection.Fact approved() {
        return new MesFeedbackFormalReviewProjection.Fact(event(282240L, 5660L), review(282240L, "APPROVED"));
    }
    private static ApprovalTaskQueryContext query(Long actor, ApprovalTaskViewType view, int page, int size) {
        return ApprovalTaskQueryContext.of(actor, view, ApprovalModuleCode.MES_FEEDBACK, null, page, size);
    }
    private static ApprovalTaskTimelineQueryContext timeline(Long actor) {
        return ApprovalTaskTimelineQueryContext.of(actor, ApprovalModuleCode.MES_FEEDBACK,
                "MES_PRO_FEEDBACK", "5660", "5660", null);
    }
}
