package cn.iocoder.yudao.module.dcc.registrationcertificate.enums;

import java.util.Arrays;

public enum DccRegistrationCertificateFileOwnerType {

    VERSION,
    CHANGE,
    SUPPORTING_DOCUMENT;

    public static DccRegistrationCertificateFileOwnerType fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的注册证文件归属类型：" + code));
    }
}
