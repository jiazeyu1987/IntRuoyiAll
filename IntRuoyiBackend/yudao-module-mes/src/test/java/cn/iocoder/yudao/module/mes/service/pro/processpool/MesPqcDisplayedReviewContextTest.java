package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolTimelineReadMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.ProcessPoolTimelineEventReadDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesPqcDisplayedReviewContextTest {
    private static ProcessPoolTimelineEventReadDO row(long id) {
        return new ProcessPoolTimelineEventReadDO().setId(id).setEventType("PQC_INSPECTION")
                .setPqcSubmissionGroupId("g").setSubmissionReviewStatus("PENDING")
                .setOriginalPayloadJson("{\"pqcSubmissionGroupId\":\"g\",\"value\":"+id+"}")
                .setDisplayedRevisionId(id+100).setDisplayedReviewId(id+200).setDisplayedReviewRound(2);
    }
    @Test void actualTimelinePageFreezesAllMemberContextsBeforeMergingDisplayBody() {
        var mapper=mock(MesProProcessPoolTimelineReadMapper.class);
        var first=row(101);var second=row(102);var request=new ProcessPoolTimelinePageReqVO().setEventType("PQC_INSPECTION");
        request.setPageNo(1);request.setPageSize(20);
        when(mapper.selectTimelineCount(request)).thenReturn(1L);when(mapper.selectTimelinePage(request)).thenReturn(List.of(first));
        when(mapper.selectPqcSubmissionGroupPayloadsByGroupIds(List.of("g"))).thenReturn(List.of(first,second));
        var result=new ProcessPoolTimelineServiceImpl(mapper).getTimelinePage(request).getList().get(0);
        assertEquals(first.getDisplayedRevisionId(), result.getDisplayedRevisionId());
        assertEquals(first.getDisplayedReviewId(), result.getDisplayedReviewId());
        assertEquals(first.getDisplayedReviewRound(), result.getDisplayedReviewRound());
        assertEquals(List.of(101L,102L),result.getExpectedReviews().stream().map(c->c.getEventId()).toList());
        for(int index=0;index<2;index++) {
            var member=List.of(first,second).get(index);var context=result.getExpectedReviews().get(index);
            assertEquals(MesProBatchRecordExecutionFieldAuditHasher.sha256(member.getOriginalPayloadJson()),context.getPayloadHash());
            assertEquals(member.getDisplayedRevisionId(),context.getRevisionId());assertEquals(member.getDisplayedReviewId(),context.getReviewId());assertEquals(2,context.getReviewRound());
        }
    }
    @ParameterizedTest @ValueSource(strings={"raw","revision","review","round"})
    void missingDisplayContextIsExplicitFailureRatherThanFilledFromLatest(String missing) {
        var mapper=mock(MesProProcessPoolTimelineReadMapper.class);var member=row(101);
        switch(missing){case "raw"->member.setOriginalPayloadJson(null);case "revision"->member.setDisplayedRevisionId(null);case "review"->member.setDisplayedReviewId(null);case "round"->member.setDisplayedReviewRound(null);default->throw new IllegalArgumentException(missing);}
        when(mapper.selectTimelineDetailById(101L)).thenReturn(member);
        var failure=assertThrows(IllegalStateException.class,()->new ProcessPoolTimelineServiceImpl(mapper).getTimelineDetail(101L));
        assertTrue(failure.getMessage().contains("刷新"));
        verify(mapper,never()).selectPqcSubmissionGroupPayloadsByGroupIds(any());
    }
}
