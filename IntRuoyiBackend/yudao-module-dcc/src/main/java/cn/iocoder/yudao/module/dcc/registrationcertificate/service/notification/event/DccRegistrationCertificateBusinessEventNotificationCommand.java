package cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event;

import java.util.List;
import java.util.Map;

public record DccRegistrationCertificateBusinessEventNotificationCommand(
        Long tenantId,
        Long ownerCompanyId,
        Long certificateId,
        Long versionId,
        Long actorId,
        String eventType,
        String eventKey,
        List<Long> documentControlRoleIds,
        String recipientPermission,
        Map<String, Object> detailParams) {
}
