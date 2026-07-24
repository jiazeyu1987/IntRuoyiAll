package cn.iocoder.yudao.module.dcc.signature.service.review;

public record SignatureGovernanceReviewRemediation(String sourceRef,
                                                    SignatureGovernanceReviewRemediationStatus status,
                                                    String closeEvidenceRef,
                                                    String reviewerRef,
                                                    String exceptionApprovalRef) {

    public SignatureGovernanceReviewRemediation {
        if (isBlank(sourceRef) || status == null) {
            throw new IllegalArgumentException("Signature review remediation requires source reference and status");
        }
        sourceRef = sourceRef.trim();
        closeEvidenceRef = trimToNull(closeEvidenceRef);
        reviewerRef = trimToNull(reviewerRef);
        exceptionApprovalRef = trimToNull(exceptionApprovalRef);
    }

    public boolean resolvesFinding() {
        return SignatureGovernanceReviewRemediationStatus.CLOSED.equals(status)
                || SignatureGovernanceReviewRemediationStatus.EXCEPTION_APPROVED.equals(status);
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
