package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;

import java.util.Objects;

public final class ApprovalTaskResultSupport {

    private ApprovalTaskResultSupport() {
    }

    public static ApprovalTaskReviewResult fromBpmTaskStatus(Integer status, String source) {
        if (Objects.equals(status, BpmTaskStatusEnum.APPROVE.getStatus())
                || Objects.equals(status, BpmTaskStatusEnum.APPROVING.getStatus())) {
            return ApprovalTaskReviewResult.APPROVE;
        }
        if (Objects.equals(status, BpmTaskStatusEnum.REJECT.getStatus())
                || Objects.equals(status, BpmTaskStatusEnum.RETURN.getStatus())
                || Objects.equals(status, BpmTaskStatusEnum.CANCEL.getStatus())) {
            return ApprovalTaskReviewResult.REJECT;
        }
        throw new IllegalStateException("APPROVAL_RESULT_UNSUPPORTED: " + source + " status=" + status);
    }

    public static ApprovalTaskReviewResult fromApprovedRejectedStatus(String status, String approvedStatus,
                                                                      String rejectedStatus) {
        if (Objects.equals(status, approvedStatus)) {
            return ApprovalTaskReviewResult.APPROVE;
        }
        if (Objects.equals(status, rejectedStatus)) {
            return ApprovalTaskReviewResult.REJECT;
        }
        return null;
    }

    public static String rejectRemark(ApprovalTaskReviewResult result, String remark) {
        return result == ApprovalTaskReviewResult.REJECT ? trimToNull(remark) : null;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
