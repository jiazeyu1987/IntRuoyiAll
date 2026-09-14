package cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.threshold;

public record DccRegistrationCertificateThresholdDeliveryResult(
        Long deliveryId,
        String status,
        Long notifyMessageId) {
}
