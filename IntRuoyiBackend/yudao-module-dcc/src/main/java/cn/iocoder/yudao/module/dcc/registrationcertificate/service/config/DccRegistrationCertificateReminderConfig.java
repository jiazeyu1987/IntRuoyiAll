package cn.iocoder.yudao.module.dcc.registrationcertificate.service.config;

public record DccRegistrationCertificateReminderConfig(
        Long id,
        Long tenantId,
        Boolean enabled,
        String dailyRunTime,
        String timezone,
        String thresholdDaysJson,
        Integer rowVersion) {
}
