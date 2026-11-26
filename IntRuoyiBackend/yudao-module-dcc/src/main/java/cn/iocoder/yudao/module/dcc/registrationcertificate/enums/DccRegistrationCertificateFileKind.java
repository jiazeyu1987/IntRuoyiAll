package cn.iocoder.yudao.module.dcc.registrationcertificate.enums;

import java.util.Arrays;

public enum DccRegistrationCertificateFileKind {

    REGISTRATION_CERTIFICATE,
    CERTIFICATE_ATTACHMENT,
    FDA_PRODUCT_DESCRIPTION,
    CHANGE_APPROVAL,
    RENEWAL_ACCEPTANCE_RECEIPT,
    RENEWAL_SUPPLEMENT_NOTICE;

    public static DccRegistrationCertificateFileKind fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的注册证文件类别：" + code));
    }
}
