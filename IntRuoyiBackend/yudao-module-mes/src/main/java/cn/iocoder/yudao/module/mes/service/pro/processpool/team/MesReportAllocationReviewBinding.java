package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;

import java.util.Objects;

/** The original production approval and a later signed allocation are distinct facts. */
final class MesReportAllocationReviewBinding {
    private MesReportAllocationReviewBinding() {}

    static boolean matches(MesProcessPoolReportAllocationDO allocation,
                           MesProcessPoolSubmissionReviewDO review) {
        if (allocation == null || review == null || allocation.getConfirmedAt() == null
                || review.getReviewedAt() == null || review.getId() == null
                || !Objects.equals(allocation.getReviewId(), review.getId())
                || !Objects.equals(allocation.getEventId(), review.getEventId())
                || !Objects.equals(allocation.getLeaderUserId(), review.getLeaderUserId())) return false;
        if (allocation.getConfirmedAt().equals(review.getReviewedAt())) return true;
        // save() creates a new immutable version only after independently authenticating the
        // correction signature. Do not rewrite its time to the original approval's time.
        return allocation.getCreatedVersion() != null && allocation.getCreatedVersion() > 1
                && allocation.getConfirmedAt().isAfter(review.getReviewedAt());
    }
}
