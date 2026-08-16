package cn.iocoder.yudao.module.dcc.registrationcertificate.enums;

import java.util.Arrays;

public enum DccRegistrationCertificateFileKind {

    REGISTRATION_CERTIFICATE,
    CHANGE_APPROVAL,
    RENEWAL_ACCEPTANCE_RECEIPT,
    RENEWAL_SUPPLEMENT_NOTICE;

    public static DccRegistrationCertificateFileKind fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown registration certificate file kind: " + code));
    }
}
