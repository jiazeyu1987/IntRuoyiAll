package cn.iocoder.yudao.module.bpm.approval.core;

import java.util.Objects;

public final class ApprovalReviewReason {

    private ApprovalReviewReason() {
    }

    public static String normalize(String reason) {
        return reason == null ? "" : reason.replaceAll(
                "^[\\p{javaWhitespace}\\p{javaSpaceChar}\\x00-\\x20\\uFEFF]+|[\\p{javaWhitespace}\\p{javaSpaceChar}\\x00-\\x20\\uFEFF]+$", "");
    }

    public static String requireValid(ApprovalTaskReviewResult result, String reason) {
        Objects.requireNonNull(result, "APPROVAL_REVIEW_RESULT_REQUIRED");
        String normalized = normalize(reason);
        if (result == ApprovalTaskReviewResult.REJECT && normalized.isEmpty()) {
            throw new IllegalArgumentException("APPROVAL_REVIEW_REASON_REQUIRED");
        }
        return normalized;
    }
}
