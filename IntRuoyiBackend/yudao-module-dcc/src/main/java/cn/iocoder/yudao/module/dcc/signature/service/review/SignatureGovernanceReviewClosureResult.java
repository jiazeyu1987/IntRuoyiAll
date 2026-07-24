package cn.iocoder.yudao.module.dcc.signature.service.review;

import java.util.List;

public record SignatureGovernanceReviewClosureResult(SignatureGovernanceReviewBatchStatus status,
                                                      boolean signed,
                                                      boolean closed,
                                                      List<SignatureGovernanceReviewBlocker> blockers) {

    public SignatureGovernanceReviewClosureResult {
        if (status == null) {
            throw new IllegalArgumentException("Signature review closure result requires status");
        }
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }

    public static SignatureGovernanceReviewClosureResult blocked(
            List<SignatureGovernanceReviewBlocker> blockers) {
        return new SignatureGovernanceReviewClosureResult(SignatureGovernanceReviewBatchStatus.BLOCKED,
                false, false, blockers);
    }

    public static SignatureGovernanceReviewClosureResult signedResult() {
        return new SignatureGovernanceReviewClosureResult(SignatureGovernanceReviewBatchStatus.SIGNED,
                true, false, List.of());
    }

    public static SignatureGovernanceReviewClosureResult closedResult() {
        return new SignatureGovernanceReviewClosureResult(SignatureGovernanceReviewBatchStatus.CLOSED,
                true, true, List.of());
    }
}
