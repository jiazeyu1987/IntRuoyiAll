package cn.iocoder.yudao.module.dcc.registrationcertificate.enums;

import java.util.Arrays;

public enum DccRegistrationCertificateVersionType {

    INITIAL_CERTIFICATE,
    RENEWAL_CERTIFICATE;

    public static DccRegistrationCertificateVersionType fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的注册证版本类型：" + code));
    }
}
