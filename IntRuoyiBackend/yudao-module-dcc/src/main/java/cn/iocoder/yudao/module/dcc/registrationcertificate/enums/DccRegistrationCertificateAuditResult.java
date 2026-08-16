package cn.iocoder.yudao.module.dcc.registrationcertificate.enums;

import java.util.Arrays;

public enum DccRegistrationCertificateAuditResult {

    SUCCESS,
    FAILURE;

    public static DccRegistrationCertificateAuditResult fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown registration certificate audit result: " + code));
    }
}
