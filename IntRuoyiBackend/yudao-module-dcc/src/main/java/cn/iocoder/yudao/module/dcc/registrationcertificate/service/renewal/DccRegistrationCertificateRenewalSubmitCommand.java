package cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public record DccRegistrationCertificateRenewalSubmitCommand(
        Long tenantId,
        Long actorId,
        String idempotencyKey,
        String requestTraceId,
        Long certificateId,
        Integer expectedRowVersion,
        Long currentVersionId,
        LocalDate approvalDate,
        LocalDate effectiveDate,
        LocalDate expiryDate,
        MultipartFile file) {
}
