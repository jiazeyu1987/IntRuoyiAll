package cn.iocoder.yudao.module.dcc.signature.service.policy;

public record SignatureGovernancePolicyDrift(String fieldName,
                                             String expectedValue,
                                             String actualValue,
                                             String impact) {

    public SignatureGovernancePolicyDrift {
        if (fieldName == null || fieldName.trim().isEmpty() || impact == null || impact.trim().isEmpty()) {
            throw new IllegalArgumentException("Signature governance drift requires field and impact");
        }
        fieldName = fieldName.trim();
        impact = impact.trim();
    }

    public static SignatureGovernancePolicyDrift of(String fieldName, String expectedValue, String actualValue) {
        return new SignatureGovernancePolicyDrift(fieldName, expectedValue, actualValue,
                "Policy drift requires review before cross-module signature governance can be released");
    }
}
