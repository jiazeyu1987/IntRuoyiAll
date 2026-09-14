package cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event;

import java.util.Map;

public record DccRegistrationCertificateBusinessEventNotificationResult(
        String eventType,
        Map<Long, Long> recipientMessageIds) {
}
