package cn.iocoder.yudao.module.dcc.registrationcertificate.service.change;

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
        String voidReason) {
}
