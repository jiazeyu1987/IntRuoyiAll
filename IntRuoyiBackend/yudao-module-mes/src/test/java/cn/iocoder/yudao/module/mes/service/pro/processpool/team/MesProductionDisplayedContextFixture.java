package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import java.util.List;

/** Existing behavior fixtures explicitly model opening a fresh production record before submitting. */
public final class MesProductionDisplayedContextFixture {
    public static void attach(MesReportAllocationCommandService service) {
        var revisions = mock(MesProProcessPoolEventRevisionMapper.class);
        lenient().when(revisions.selectListByEventIdForUpdate(anyLong())).thenReturn(List.of());
        ReflectionTestUtils.setField(service, "revisionMapper", revisions);
    }
    public static MesSubmissionReviewExpectedContext displayed(MesReportAllocationCommandService service, Long id) {
        var events = (MesProProcessPoolEventMapper) ReflectionTestUtils.getField(service, "eventMapper");
        var states = (MesProcessPoolReportAllocationStateMapper) ReflectionTestUtils.getField(service, "stateMapper");
        var reviews = (MesProcessPoolSubmissionReviewMapper) ReflectionTestUtils.getField(service, "reviewMapper");
        var revisions = (MesProProcessPoolEventRevisionMapper) ReflectionTestUtils.getField(service, "revisionMapper");
        var event = events.selectByIdForUpdate(id);
        if (event == null) return null;
        var state = states.selectByEventIdForUpdate(id);
        var review = reviews.selectLatestByEventIdForUpdate(id);
        if (review != null && review.getReviewRound() == null) review.setReviewRound(0);
        var changes = revisions.selectListByEventIdForUpdate(id);
        return new MesSubmissionReviewExpectedContext().setEventId(id)
                .setPayloadHash(event.getRawPayload() == null ? "missing-test-payload" : MesProBatchRecordExecutionFieldAuditHasher.sha256(event.getRawPayload()))
                .setRevisionId(changes.isEmpty() ? 0L : changes.get(0).getId())
                .setReviewId(review == null ? 0L : review.getId()).setReviewRound(review == null ? 0 : review.getReviewRound())
                .setReviewStatus(review == null ? "PENDING" : review.getReviewStatus())
                .setAllocationVersion(state == null ? 0 : state.getCurrentVersion());
    }
    public static MesReportAllocationSnapshot save(MesReportAllocationCommandService service, MesReportAllocationSaveCommand command) {
        if (command.getExpectedReview() == null) command.setExpectedReview(displayed(service, command.getEventId()));
        return service.save(command);
    }
}
