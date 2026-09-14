package cn.iocoder.yudao.module.dcc.registrationcertificate.service.dailyrun;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DccRegistrationCertificateBusinessTimeSimulationResult(
        Long tenantId,
        LocalDate businessDate,
        LocalDateTime simulatedAt,
        String jobResult) {
}
