package cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder;

public record DccRegistrationCertificateReminderRunResult(
        int pendingCount,
        int suppressedCount) {
}
