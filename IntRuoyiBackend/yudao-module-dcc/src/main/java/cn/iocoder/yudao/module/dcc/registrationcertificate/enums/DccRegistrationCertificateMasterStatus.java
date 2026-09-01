package cn.iocoder.yudao.module.dcc.registrationcertificate.enums;

import java.util.Arrays;

public enum DccRegistrationCertificateMasterStatus {

    DRAFT,
    PENDING_FIRST_EFFECTIVE,
    ACTIVE,
    EXPIRED_UNRENEWED,
    VOIDED;

    public static DccRegistrationCertificateMasterStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的注册证主档状态：" + code));
    }

    public boolean isFormal() {
        return this != DRAFT;
    }
}
