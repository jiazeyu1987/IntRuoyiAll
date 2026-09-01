package cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_TEMPLATE_PARAM_MISSING;

@Service
public class DccRegistrationCertificateBusinessEventNotifier {

    private static final String EVENT_NEW_CERTIFICATE_FORMALIZED = "NEW_CERTIFICATE_FORMALIZED";
    private static final String EVENT_RENEWAL_CANDIDATE_UPLOADED = "RENEWAL_CANDIDATE_UPLOADED";
    private static final String EVENT_RENEWAL_CANDIDATE_ACTIVATED = "RENEWAL_CANDIDATE_ACTIVATED";
    private static final String EVENT_CHANGE_APPROVAL_RECORDED = "CHANGE_APPROVAL_RECORDED";

    private final DccRegistrationCertificateBusinessEventNotificationConfigService configService;
    private final DccRegistrationCertificateBusinessEventNotificationService notificationService;

    public DccRegistrationCertificateBusinessEventNotifier(
            DccRegistrationCertificateBusinessEventNotificationConfigService configService,
            DccRegistrationCertificateBusinessEventNotificationService notificationService) {
        this.configService = require(configService, "configService");
        this.notificationService = require(notificationService, "notificationService");
    }

    public void notifyNewCertificateFormalized(Long tenantId, Long ownerCompanyId, Long certificateId,
                                               Long versionId, Long actorId, String eventKey,
                                               String certificateNo) {
        send(tenantId, ownerCompanyId, certificateId, versionId, actorId, EVENT_NEW_CERTIFICATE_FORMALIZED,
                eventKey, "注册证上传审批通过", certificateNo);
    }

    public void notifyRenewalCandidateUploaded(Long tenantId, Long ownerCompanyId, Long certificateId,
                                               Long versionId, Long actorId, String eventKey,
                                               String certificateNo) {
        send(tenantId, ownerCompanyId, certificateId, versionId, actorId, EVENT_RENEWAL_CANDIDATE_UPLOADED,
                eventKey, "延续注册证审批通过", certificateNo);
    }

    public void notifyRenewalCandidateActivated(Long tenantId, Long ownerCompanyId, Long certificateId,
                                                Long versionId, Long actorId, String eventKey,
                                                String certificateNo) {
        send(tenantId, ownerCompanyId, certificateId, versionId, actorId, EVENT_RENEWAL_CANDIDATE_ACTIVATED,
                eventKey, "延续注册证已生效", certificateNo);
    }

    public void notifyChangeApprovalRecorded(Long tenantId, Long ownerCompanyId, Long certificateId,
                                             Long versionId, Long actorId, String eventKey,
                                             String certificateNo) {
        send(tenantId, ownerCompanyId, certificateId, versionId, actorId, EVENT_CHANGE_APPROVAL_RECORDED,
                eventKey, "变更批件已记录", certificateNo);
    }

    private void send(Long tenantId, Long ownerCompanyId, Long certificateId, Long versionId, Long actorId,
                      String eventType, String eventKey, String eventTitle, String certificateNo) {
        DccRegistrationCertificateBusinessEventNotificationConfigService.RecipientScope recipientScope =
                configService.resolveRecipientScope();
        notificationService.send(new DccRegistrationCertificateBusinessEventNotificationCommand(
                tenantId, ownerCompanyId, certificateId, versionId, actorId, eventType, eventKey,
                recipientScope.roleIds(), recipientScope.permission(), detailParams(eventTitle, certificateNo)));
    }

    private static Map<String, Object> detailParams(String eventTitle, String certificateNo) {
        if (isBlank(eventTitle) || isBlank(certificateNo)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_TEMPLATE_PARAM_MISSING);
        }
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("eventTitle", eventTitle.trim());
        params.put("certificateNo", certificateNo.trim());
        return Map.copyOf(params);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value;
    }
}
