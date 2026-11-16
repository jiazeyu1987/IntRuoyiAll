package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MesReportAllocationReviewBindingTest {
    private final LocalDateTime originalTime = LocalDateTime.of(2026, 11, 11, 1, 0);
    private final MesProcessPoolSubmissionReviewDO review = MesProcessPoolSubmissionReviewDO.builder()
            .id(10L).eventId(20L).leaderUserId(30L).reviewedAt(originalTime).build();

    private MesProcessPoolReportAllocationDO allocation() {
        return MesProcessPoolReportAllocationDO.builder().reviewId(10L).eventId(20L)
                .leaderUserId(30L).createdVersion(1).confirmedAt(originalTime).build();
    }

    @Test void originalApprovalKeepsItsExactTime() {
        assertTrue(MesReportAllocationReviewBinding.matches(allocation(), review));
    }
    @Test void signedNewVersionDoesNotNeedToRewriteOriginalApprovalTime() {
        assertTrue(MesReportAllocationReviewBinding.matches(allocation().setCreatedVersion(2)
                .setConfirmedAt(originalTime.plusHours(1)), review));
        assertEquals(originalTime, review.getReviewedAt());
    }
    @Test void unversionedOrInitialAllocationCannotClaimALaterCorrection() {
        assertFalse(MesReportAllocationReviewBinding.matches(allocation()
                .setConfirmedAt(originalTime.plusHours(1)), review));
        assertFalse(MesReportAllocationReviewBinding.matches(allocation().setCreatedVersion(null)
                .setConfirmedAt(originalTime.plusHours(1)), review));
    }
    @Test void correctionCannotPrecedeOriginalApproval() {
        assertFalse(MesReportAllocationReviewBinding.matches(allocation().setCreatedVersion(2)
                .setConfirmedAt(originalTime.minusSeconds(1)), review));
    }
    @Test void originalReviewEventAndLeaderMustRemainBound() {
        assertFalse(MesReportAllocationReviewBinding.matches(allocation().setReviewId(11L), review));
        assertFalse(MesReportAllocationReviewBinding.matches(allocation().setEventId(21L), review));
        assertFalse(MesReportAllocationReviewBinding.matches(allocation().setLeaderUserId(31L), review));
    }
    @Test void missingFormalReviewOrTimesCannotPass() {
        assertFalse(MesReportAllocationReviewBinding.matches(null, review));
        assertFalse(MesReportAllocationReviewBinding.matches(allocation(), null));
        assertFalse(MesReportAllocationReviewBinding.matches(allocation().setConfirmedAt(null), review));
        review.setReviewedAt(null);
        assertFalse(MesReportAllocationReviewBinding.matches(allocation(), review));
    }
}
