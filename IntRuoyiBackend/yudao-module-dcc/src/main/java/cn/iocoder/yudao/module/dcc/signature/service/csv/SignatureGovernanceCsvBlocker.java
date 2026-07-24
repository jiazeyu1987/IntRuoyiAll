package cn.iocoder.yudao.module.dcc.signature.service.csv;

public record SignatureGovernanceCsvBlocker(SignatureGovernanceCsvBlockerCode code,
                                            String message,
                                            String impact) {

    public SignatureGovernanceCsvBlocker {
        if (code == null || isBlank(message) || isBlank(impact)) {
            throw new IllegalArgumentException("CSV governance blocker requires code, message, and impact");
        }
        message = message.trim();
        impact = impact.trim();
    }

    public static SignatureGovernanceCsvBlocker of(SignatureGovernanceCsvBlockerCode code,
                                                   String message,
                                                   String impact) {
        return new SignatureGovernanceCsvBlocker(code, message, impact);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
