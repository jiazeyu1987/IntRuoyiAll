package cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_TEMPLATE_PARAM_MISSING;

@Service
@Slf4j
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
                                               String productName, String certificateNo,
                                               LocalDate effectiveDate, LocalDate expiryDate) {
        send(tenantId, ownerCompanyId, certificateId, versionId, actorId, EVENT_NEW_CERTIFICATE_FORMALIZED,
                eventKey, "已入库", productName, certificateNo, effectiveDate, expiryDate);
    }

    public void notifyRenewalCandidateUploaded(Long tenantId, Long ownerCompanyId, Long certificateId,
                                               Long versionId, Long actorId, String eventKey,
                                               String productName, String certificateNo,
                                               LocalDate effectiveDate, LocalDate expiryDate) {
        send(tenantId, ownerCompanyId, certificateId, versionId, actorId, EVENT_RENEWAL_CANDIDATE_UPLOADED,
                eventKey, "延续版本已入库", productName, certificateNo, effectiveDate, expiryDate);
    }

    public void notifyRenewalCandidateActivated(Long tenantId, Long ownerCompanyId, Long certificateId,
                                                Long versionId, Long actorId, String eventKey,
                                                String productName, String certificateNo,
                                                LocalDate effectiveDate, LocalDate expiryDate) {
        send(tenantId, ownerCompanyId, certificateId, versionId, actorId, EVENT_RENEWAL_CANDIDATE_ACTIVATED,
                eventKey, "延续版本已生效", productName, certificateNo, effectiveDate, expiryDate);
    }

    public void notifyChangeApprovalRecorded(Long tenantId, Long ownerCompanyId, Long certificateId,
                                             Long versionId, Long actorId, String eventKey,
                                             String productName, String certificateNo,
                                             LocalDate effectiveDate, LocalDate expiryDate) {
        send(tenantId, ownerCompanyId, certificateId, versionId, actorId, EVENT_CHANGE_APPROVAL_RECORDED,
                eventKey, "变更批件已记录", productName, certificateNo, effectiveDate, expiryDate);
    }

    private void send(Long tenantId, Long ownerCompanyId, Long certificateId, Long versionId, Long actorId,
                      String eventType, String eventKey, String eventTitle, String productName,
                      String certificateNo, LocalDate effectiveDate, LocalDate expiryDate) {
        Map<String, Object> detailParams = detailParams(eventTitle, productName, certificateNo,
                effectiveDate, expiryDate);
        DccRegistrationCertificateBusinessEventNotificationConfigService.RecipientScope recipientScope;
        try {
            recipientScope = configService.resolveRecipientScope();
        } catch (ServiceException exception) {
            if (Objects.equals(exception.getCode(), REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED.getCode())) {
                log.warn("[send][注册证业务事件通知跳过，提醒任务未配置][tenantId({}) certificateId({}) eventType({}) eventKey({})]",
                        tenantId, certificateId, eventType, eventKey);
                return;
            }
            throw exception;
        }
        notificationService.send(new DccRegistrationCertificateBusinessEventNotificationCommand(
                tenantId, ownerCompanyId, certificateId, versionId, actorId, eventType, eventKey,
                recipientScope.roleIds(), recipientScope.permission(),
                detailParams));
    }

    private static Map<String, Object> detailParams(String eventTitle, String productName, String certificateNo,
                                                    LocalDate effectiveDate, LocalDate expiryDate) {
        if (isBlank(eventTitle) || isBlank(productName) || isBlank(certificateNo)
                || effectiveDate == null || expiryDate == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_TEMPLATE_PARAM_MISSING);
        }
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("eventTitle", eventTitle.trim());
        params.put("productName", productName.trim());
        params.put("certificateNo", certificateNo.trim());
        params.put("effectiveDate", effectiveDate.toString());
        params.put("expiryDate", expiryDate.toString());
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
