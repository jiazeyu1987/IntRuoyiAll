package cn.iocoder.yudao.module.dcc.signature.service.review;

import java.util.List;

public record SignatureGovernanceReviewBatchEvaluation(SignatureGovernanceReviewBatchStatus status,
                                                        boolean collectable,
                                                        String batchId,
                                                        String snapshotHash,
                                                        List<SignatureGovernanceReviewBlocker> blockers,
                                                        List<SignatureGovernanceReviewSnapshotItem> snapshotItems) {

    public SignatureGovernanceReviewBatchEvaluation {
        if (status == null) {
            throw new IllegalArgumentException("Signature review batch evaluation requires status");
        }
        batchId = blankToEmpty(batchId);
        snapshotHash = blankToEmpty(snapshotHash);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        snapshotItems = snapshotItems == null ? List.of() : List.copyOf(snapshotItems);
    }

    public static SignatureGovernanceReviewBatchEvaluation blocked(
            List<SignatureGovernanceReviewBlocker> blockers) {
        return new SignatureGovernanceReviewBatchEvaluation(SignatureGovernanceReviewBatchStatus.BLOCKED,
                false, "", "", blockers, List.of());
    }

    public static SignatureGovernanceReviewBatchEvaluation collected(String batchId,
                                                                      String snapshotHash,
                                                                      List<SignatureGovernanceReviewSnapshotItem> snapshotItems) {
        return new SignatureGovernanceReviewBatchEvaluation(SignatureGovernanceReviewBatchStatus.COLLECTED,
                true, batchId, snapshotHash, List.of(), snapshotItems);
    }

    private static String blankToEmpty(String value) {
        return value == null || value.trim().isEmpty() ? "" : value.trim();
    }
}
