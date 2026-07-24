package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;

import java.util.Objects;

public final class MesProEdhrApprovalStatusMapping {

    public static final int EXECUTION_STATUS_DRAFT = 0;
    public static final int EXECUTION_STATUS_SUBMITTED = 1;
    public static final int EXECUTION_STATUS_REJECTED = 2;
    public static final int EXECUTION_STATUS_APPROVED = 3;
    public static final int EXECUTION_STATUS_FILL_COMPLETED = 4;

    public static final String APPROVAL_STATUS_SUBMITTED = "SUBMITTED";
    public static final String APPROVAL_STATUS_REJECTED = "REJECTED";
    public static final String APPROVAL_STATUS_APPROVED = "APPROVED";

    public static final String ACTION_RESULT_REVIEW_INTERMEDIATE = "REVIEW_INTERMEDIATE";
    public static final String ACTION_RESULT_REVIEW_TO_APPROVE = "REVIEW_TO_APPROVE";
    public static final String ACTION_RESULT_FINAL_APPROVED = "FINAL_APPROVED";
    public static final String ACTION_RESULT_REVIEW_REJECTED_REWORK = "REVIEW_REJECTED_REWORK";

    private MesProEdhrApprovalStatusMapping() {
    }

    public static String resolveReviewApprovalResultType(boolean hasActiveReviewTasks) {
        return hasActiveReviewTasks ? ACTION_RESULT_REVIEW_INTERMEDIATE : ACTION_RESULT_REVIEW_TO_APPROVE;
    }

    public static String resolveApproveResultType() {
        return ACTION_RESULT_FINAL_APPROVED;
    }

    public static String resolveReviewRejectResultType() {
        return ACTION_RESULT_REVIEW_REJECTED_REWORK;
    }

    public static int executionStatusAfterReviewApproval(String completedWorkTaskStatus) {
        if (!Objects.equals(completedWorkTaskStatus, MesProEdhrWorkTaskStatus.DONE)) {
            throw new IllegalArgumentException("REVIEW 工作任务未完成，不能推进审核状态");
        }
        return EXECUTION_STATUS_SUBMITTED;
    }

    public static int executionStatusAfterFinalApproval(String completedWorkTaskStatus) {
        if (!Objects.equals(completedWorkTaskStatus, MesProEdhrWorkTaskStatus.DONE)) {
            throw new IllegalArgumentException("APPROVE 工作任务未完成，不能推进最终批准状态");
        }
        return EXECUTION_STATUS_APPROVED;
    }

    public static int executionStatusAfterReviewReject(String completedWorkTaskStatus) {
        if (!Objects.equals(completedWorkTaskStatus, MesProEdhrWorkTaskStatus.DONE)) {
            throw new IllegalArgumentException("REVIEW 工作任务未完成，不能推进驳回返工状态");
        }
        return EXECUTION_STATUS_REJECTED;
    }
}
