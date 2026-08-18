package cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder;

public record DccRegistrationCertificateReminderEvaluation(
        String thresholdLevel,
        String colorCode,
        int daysUntilDue) {
}
