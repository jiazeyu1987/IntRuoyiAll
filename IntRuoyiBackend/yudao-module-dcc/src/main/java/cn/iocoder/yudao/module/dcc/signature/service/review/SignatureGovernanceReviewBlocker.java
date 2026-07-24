package cn.iocoder.yudao.module.dcc.signature.service.review;

public record SignatureGovernanceReviewBlocker(SignatureGovernanceReviewBlockerCode code,
                                                String message,
                                                String impact) {

    public SignatureGovernanceReviewBlocker {
        if (code == null || isBlank(message) || isBlank(impact)) {
            throw new IllegalArgumentException("Signature governance review blocker requires code, message, and impact");
        }
        message = message.trim();
        impact = impact.trim();
    }

    public static SignatureGovernanceReviewBlocker of(SignatureGovernanceReviewBlockerCode code,
                                                       String message,
                                                       String impact) {
        return new SignatureGovernanceReviewBlocker(code, message, impact);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
