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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.mes.approval.MesFeedbackFormalReviewProjectionTest.*;

@ExtendWith(MockitoExtension.class)
class MesProFeedbackFormalApprovalTaskAdapterTest {
    @Mock MesProFeedbackMapper feedbackMapper;
    @Mock MesProFeedbackService feedbackService;
    @Mock MesFeedbackFormalReviewProjection projection;
    @Mock cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesFeedbackApprovalQueryMapper approvalQueries;
    @InjectMocks MesProFeedbackApprovalTaskAdapter adapter;

    @org.junit.jupiter.api.BeforeEach
    void injectCandidateQuery() {
        org.springframework.test.util.ReflectionTestUtils.setField(adapter, "approvalQueries", approvalQueries);
    }

    @Test void approvedSourceDisappearsFromTodoWithoutChangingRawStatus() {
        MesProFeedbackDO source = feedback(5660L).setApproveUserId(341L);
        stubCandidates(List.of(source));
        when(projection.read(List.of(source))).thenReturn(Map.of(5660L, approved()));
        var result = adapter.page(query(341L, ApprovalTaskViewType.TODO, 1, 10));
        assertEquals(0L, result.getTotal()); assertTrue(result.getList().isEmpty());
        assertEquals(MesProFeedbackStatusEnum.APPROVING.getStatus(), source.getStatus());
        verifyNoInteractions(feedbackService);
    }

    @Test void designatedApproverDoesNotSeeOtherReviewersDoneAndTimelineIsDenied() {
        MesProFeedbackDO source = feedback(5660L);
        stubCandidates(List.of(source));
        when(projection.read(List.of(source))).thenReturn(Map.of(5660L, approved()));
        assertEquals(0L, adapter.page(query(340L, ApprovalTaskViewType.DONE, 1, 10)).getTotal());
        when(feedbackMapper.selectById(5660L)).thenReturn(source);
        assertThrows(IllegalStateException.class, () -> adapter.listTimeline(timeline(340L)));
    }

    @Test void mixedRowsFilterBeforePaginationAndKeepConsistentTotals() {
        MesProFeedbackDO source = feedback(5660L).setApproveUserId(341L);
        MesProFeedbackDO legacy = feedback(5661L).setApproveUserId(341L);
        stubCandidates(List.of(source, legacy));
        when(projection.read(List.of(source, legacy))).thenReturn(Map.of(5660L, approved()));
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

    @Test void productionReviewNeverEntersAnyApprovalViewOrCount() {
        MesProFeedbackDO source = feedback(5660L).setApproveUserId(341L);
        for (var fact : List.of(approved(),
                new MesFeedbackFormalReviewProjection.Fact(event(282240L, 5660L), null),
                new MesFeedbackFormalReviewProjection.Fact(event(282240L, 5660L), review(282240L, "REJECTED")))) {
            for (var view : List.of(ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE, ApprovalTaskViewType.MY_INITIATED)) {
                for (boolean global : List.of(false, true)) {
                    stubCandidates(List.of(source));
                    when(projection.read(List.of(source))).thenReturn(Map.of(5660L, fact));
                    var context = query(341L, view, 1, 10).setGlobalView(global);
                    var page = adapter.page(context);
                    assertEquals(0L, page.getTotal(), view + " global=" + global);
                    assertTrue(page.getList().isEmpty());
                    assertEquals(0L, adapter.window(context, new ApprovalQueryWindow(0, 0)).getTotal());
                }
            }
        }
        verify(projection, never()).reviewedFeedbackIds(any());
        verifyNoInteractions(feedbackService);
    }

    @Test void proofEligibilityScansFixedWindowsAndCountsExactlyWithoutColumnFilters() {
        var rows = java.util.stream.LongStream.range(1000, 1403)
                .mapToObj(id -> feedback(id).setApproveUserId(341L).setStatus(MesProFeedbackStatusEnum.UNCHECK.getStatus())).toList();
        var byId = rows.stream().collect(java.util.stream.Collectors.toMap(MesProFeedbackDO::getId, row -> row));
        var offsets = new java.util.ArrayList<Integer>();
        when(approvalQueries.query(any())).thenAnswer(call -> new cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX<Long>());
        when(approvalQueries.count(any())).thenReturn(403L);
        when(approvalQueries.window(any())).thenAnswer(call -> {
            var wrapper = (com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<?>) call.getArgument(0);
            var matcher = java.util.regex.Pattern.compile("LIMIT ([0-9]+),200$").matcher(wrapper.getSqlSegment());
            assertTrue(matcher.find(), wrapper.getSqlSegment());
            int offset = Integer.parseInt(matcher.group(1)); offsets.add(offset);
            return rows.subList(offset, Math.min(offset + 200, rows.size())).stream().map(MesProFeedbackDO::getId).toList();
        });
        when(feedbackMapper.selectById(anyLong())).thenAnswer(call -> byId.get(call.getArgument(0)));
        when(projection.read(anyList())).thenAnswer(call -> {
            List<MesProFeedbackDO> batch = call.getArgument(0);
            assertTrue(batch.size() <= 200);
            var facts = new java.util.HashMap<Long, MesFeedbackFormalReviewProjection.Fact>();
            for (var row : batch) {
                long eventId = row.getId() + 10000;
                if ((row.getId() - 1000) % 3 == 0) facts.put(row.getId(),
                        new MesFeedbackFormalReviewProjection.Fact(event(eventId, row.getId()), review(eventId, "APPROVED")));
            }
            return facts;
        });
        var result = adapter.page(query(341L, ApprovalTaskViewType.DONE, 2, 10));
        assertEquals(268L, result.getTotal());
        assertEquals(java.util.stream.LongStream.range(1000, 1403).filter(id -> (id - 1000) % 3 != 0).skip(10).limit(10).mapToObj(String::valueOf).toList(),
                result.getList().stream().map(cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary::getSourceTaskId).toList());
        assertEquals(List.of(0, 200, 400), offsets);
        verify(projection, times(3)).read(anyList());
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
    private void stubCandidates(List<MesProFeedbackDO> rows) {
        when(approvalQueries.query(any())).thenAnswer(call->new cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX<Long>());
        when(approvalQueries.count(any())).thenReturn((long)rows.size());
        when(approvalQueries.window(any())).thenReturn(rows.stream().map(MesProFeedbackDO::getId).toList());
        for(var row:rows) when(feedbackMapper.selectById(row.getId())).thenReturn(row);
    }

}
