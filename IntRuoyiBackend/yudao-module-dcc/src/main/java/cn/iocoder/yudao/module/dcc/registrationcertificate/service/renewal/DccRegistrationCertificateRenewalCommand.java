package cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal;

import java.time.LocalDate;

public record DccRegistrationCertificateRenewalCommand(
        Long tenantId,
        Long actorId,
        String idempotencyKey,
        String requestTraceId,
        Long certificateId,
        Integer expectedRowVersion,
        Long currentVersionId,
        Long businessFileId,
        Boolean categoryChanged,
        String certificateNo,
        String classification,
        LocalDate approvalDate,
        LocalDate effectiveDate,
        LocalDate expiryDate) {
}
