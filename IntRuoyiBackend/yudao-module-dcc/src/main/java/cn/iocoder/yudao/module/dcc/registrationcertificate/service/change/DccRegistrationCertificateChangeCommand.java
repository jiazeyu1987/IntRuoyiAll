package cn.iocoder.yudao.module.dcc.registrationcertificate.service.change;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

public record DccRegistrationCertificateChangeCommand(
        Long tenantId,
        Long actorId,
        String idempotencyKey,
        String requestTraceId,
        Long certificateId,
        Integer expectedRowVersion,
        LocalDate approvalDate,
        Map<String, String> structuredValues,
        String otherDescription,
        Boolean entrustedProduction,
        Boolean selfProduction,
        String entrustedEnterprisesJson,
        String voidReason,
        MultipartFile file) {

    public DccRegistrationCertificateChangeCommand(Long tenantId, Long actorId, String idempotencyKey,
                                                   String requestTraceId, Long certificateId,
                                                   Integer expectedRowVersion, LocalDate approvalDate,
                                                   Map<String, String> structuredValues, String otherDescription,
                                                   Boolean entrustedProduction, Boolean selfProduction,
                                                   String entrustedEnterprisesJson, String voidReason) {
        this(tenantId, actorId, idempotencyKey, requestTraceId, certificateId, expectedRowVersion,
                approvalDate, structuredValues, otherDescription, entrustedProduction, selfProduction,
                entrustedEnterprisesJson, voidReason, null);
    }
}
