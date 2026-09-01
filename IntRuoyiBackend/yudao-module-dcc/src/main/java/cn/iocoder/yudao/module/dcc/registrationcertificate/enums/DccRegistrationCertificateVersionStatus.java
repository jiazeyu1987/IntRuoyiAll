package cn.iocoder.yudao.module.dcc.registrationcertificate.enums;

import java.util.Arrays;

public enum DccRegistrationCertificateVersionStatus {

    DRAFT,
    PENDING_EFFECTIVE,
    CURRENT,
    OLD,
    VOIDED;

    public static DccRegistrationCertificateVersionStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的注册证版本状态：" + code));
    }

    public Integer currentUniqueFlag() {
        return this == CURRENT ? 1 : null;
    }

    public Integer pendingUniqueFlag() {
        return this == PENDING_EFFECTIVE ? 1 : null;
    }
}
