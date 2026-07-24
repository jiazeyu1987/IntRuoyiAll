package cn.iocoder.yudao.module.dcc.signature.service.review;

import java.util.List;

public record SignatureGovernanceReviewClosureCommand(String batchId,
                                                       String snapshotHash,
                                                       boolean reviewSignatureStrategyConfigured,
                                                       boolean reviewSigned,
                                                       List<SignatureGovernanceReviewSnapshotItem> snapshotItems,
                                                       List<SignatureGovernanceReviewRemediation> remediations) {

    public SignatureGovernanceReviewClosureCommand {
        batchId = trimToNull(batchId);
        snapshotHash = trimToNull(snapshotHash);
        snapshotItems = snapshotItems == null ? List.of() : List.copyOf(snapshotItems);
        remediations = remediations == null ? List.of() : List.copyOf(remediations);
    }

    private static String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
