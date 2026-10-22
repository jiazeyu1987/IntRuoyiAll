package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineEntry;
import java.util.Objects;

/** List and timeline expose exactly the same formally verified outcome. */
final class MesEdhrApprovalOutcomeProjection {
    private MesEdhrApprovalOutcomeProjection() { }

    static ApprovalTaskSummary apply(ApprovalTaskSummary row, MesEdhrApprovalOutcome outcome) {
        Objects.requireNonNull(outcome, "EDHR_APPROVAL_OUTCOME_REQUIRED");
        row.setTaskOutcome(outcome.taskOutcome()).setBusinessStatus(outcome.businessStatus())
                .setSourceStatus(outcome.sourceStatus()).setApprovalResult(outcome.approvalResult())
                .setDecisionState(outcome.decisionState()).setDecisionSourceStatus(outcome.decisionSourceStatus())
                .setApprovalRemark(outcome.approvalRemark()).setSourceRemark(outcome.sourceRemark())
                .setTerminationReason(outcome.terminationReason()).setDecisionActorUserId(outcome.actorUserId())
                .setDecisionAt(outcome.actedAt()).setEvidenceType(outcome.evidenceType()).setEvidenceId(outcome.evidenceId())
                .setTaskCompletedByUserId(outcome.taskCompletedByUserId());
        if ("DONE".equals(outcome.sourceStatus())) row.setAssigneeUserId(outcome.taskCompletedByUserId());
        return row;
    }

    static ApprovalTaskTimelineEntry apply(ApprovalTaskTimelineEntry row, MesEdhrApprovalOutcome outcome) {
        Objects.requireNonNull(outcome, "EDHR_APPROVAL_OUTCOME_REQUIRED");
        return row.setTaskOutcome(outcome.taskOutcome()).setBusinessStatus(outcome.businessStatus())
                .setSourceStatus(outcome.sourceStatus()).setApprovalResult(outcome.approvalResult())
                .setDecisionState(outcome.decisionState()).setDecisionSourceStatus(outcome.decisionSourceStatus())
                .setApprovalRemark(outcome.approvalRemark()).setSourceRemark(outcome.sourceRemark())
                .setTerminationReason(outcome.terminationReason()).setDecisionActorUserId(outcome.actorUserId())
                .setDecisionAt(outcome.actedAt()).setEvidenceType(outcome.evidenceType()).setEvidenceId(outcome.evidenceId())
                .setTaskCompletedByUserId(outcome.taskCompletedByUserId())
                .setActorUserId(outcome.taskCompletedByUserId())
                .setActedAt(outcome.approvalResult() != null ? outcome.actedAt()
                        : outcome.taskCompletedByUserId() != null ? row.getTaskCompletedAt() : null)
                .setComment(outcome.approvalRemark());
    }

    static String action(MesEdhrApprovalOutcome outcome) {
        return outcome.approvalResult() == null ? outcome.taskOutcome() : outcome.approvalResult().name();
    }

    static String actionLabel(MesEdhrApprovalOutcome outcome) {
        if (outcome.approvalResult() != null) {
            return outcome.approvalResult() == cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult.APPROVE
                    ? "审批通过" : "审批驳回";
        }
        return switch (outcome.taskOutcome()) {
            case "COMPLETED" -> "任务已完成";
            case "CANCELLED" -> "已取消";
            case "WAIT" -> "待处理";
            case "RUNNING" -> "处理中";
            case "OVERDUE" -> "已逾期";
            default -> "任务状态未识别";
        };
    }
}
