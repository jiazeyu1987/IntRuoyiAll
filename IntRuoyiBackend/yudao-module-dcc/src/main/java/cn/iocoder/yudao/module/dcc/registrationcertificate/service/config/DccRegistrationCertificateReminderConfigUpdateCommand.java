package cn.iocoder.yudao.module.dcc.registrationcertificate.service.config;

public record DccRegistrationCertificateReminderConfigUpdateCommand(
        Boolean enabled,
        String dailyRunTime,
        Integer expectedRowVersion) {
}
