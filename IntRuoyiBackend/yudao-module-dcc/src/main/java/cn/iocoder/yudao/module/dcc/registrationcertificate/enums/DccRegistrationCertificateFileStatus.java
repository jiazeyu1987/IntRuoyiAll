package cn.iocoder.yudao.module.dcc.registrationcertificate.enums;

import java.util.Arrays;

public enum DccRegistrationCertificateFileStatus {

    STAGED,
    BOUND,
    CLEANUP_REQUIRED,
    VOIDED;

    public static DccRegistrationCertificateFileStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "未知的注册证文件状态：" + code));
    }
}
