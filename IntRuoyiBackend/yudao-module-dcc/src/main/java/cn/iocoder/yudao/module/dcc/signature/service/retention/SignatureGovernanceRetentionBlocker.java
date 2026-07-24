package cn.iocoder.yudao.module.dcc.signature.service.retention;

public class SignatureGovernanceRetentionBlocker {

    private final SignatureGovernanceRetentionBlockerCode code;
    private final String message;
    private final String impact;

    public SignatureGovernanceRetentionBlocker(SignatureGovernanceRetentionBlockerCode code, String message,
            String impact) {
        if (code == null || isBlank(message) || isBlank(impact)) {
            throw new IllegalArgumentException("Retention blocker requires code, message, and impact");
        }
        this.code = code;
        this.message = message.trim();
        this.impact = impact.trim();
    }

    public SignatureGovernanceRetentionBlockerCode getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getImpact() {
        return impact;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
