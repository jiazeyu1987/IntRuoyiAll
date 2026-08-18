package cn.iocoder.yudao.module.dcc.registrationcertificate.service.dailyrun;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DccRegistrationCertificateDailyRunRecord(
        Long id,
        Long tenantId,
        LocalDate businessDate,
        String runKey,
        String status,
        Integer retryCount,
        String failureReason,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String detailJson) {
}
