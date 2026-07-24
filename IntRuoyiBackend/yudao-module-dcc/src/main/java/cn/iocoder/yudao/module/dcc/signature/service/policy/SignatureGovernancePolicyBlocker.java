package cn.iocoder.yudao.module.dcc.signature.service.policy;

public record SignatureGovernancePolicyBlocker(SignatureGovernancePolicyBlockerCode code,
                                                String message,
                                                String impact) {

    public SignatureGovernancePolicyBlocker {
        if (code == null || isBlank(message) || isBlank(impact)) {
            throw new IllegalArgumentException("Signature governance policy blocker requires code, message, and impact");
        }
        message = message.trim();
        impact = impact.trim();
    }

    public static SignatureGovernancePolicyBlocker of(SignatureGovernancePolicyBlockerCode code,
                                                       String message,
                                                       String impact) {
        return new SignatureGovernancePolicyBlocker(code, message, impact);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
