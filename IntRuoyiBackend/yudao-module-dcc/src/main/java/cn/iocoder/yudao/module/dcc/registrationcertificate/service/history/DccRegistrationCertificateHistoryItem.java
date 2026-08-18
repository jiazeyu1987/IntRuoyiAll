package cn.iocoder.yudao.module.dcc.registrationcertificate.service.history;

public record DccRegistrationCertificateHistoryItem(
        String eventType,
        String itemType,
        String beforeValueJson,
        String afterValueJson,
        Long actorId) {
}
