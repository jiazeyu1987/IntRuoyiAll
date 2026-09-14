package cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval;

import java.time.LocalDateTime;

public record DccRegistrationCertificateGrantStatus(
        Long grantId,
        Long requestFileId,
        Long businessFileId,
        String grantType,
        String status,
        LocalDateTime grantedAt,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt,
        String revokeReason) {
}
