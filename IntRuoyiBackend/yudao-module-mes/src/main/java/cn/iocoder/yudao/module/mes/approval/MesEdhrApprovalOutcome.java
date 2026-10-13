package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import java.time.LocalDateTime;

/** A read projection of existing facts; task completion is independent of a user decision. */
public record MesEdhrApprovalOutcome(
        String taskOutcome, ApprovalTaskReviewResult approvalResult, String businessStatus,
        String sourceStatus, String decisionState, String decisionSourceStatus,
        String approvalRemark, String sourceRemark, String terminationReason,
        Long actorUserId, LocalDateTime actedAt, String evidenceType, String evidenceId,
        Long taskCompletedByUserId) {
}
