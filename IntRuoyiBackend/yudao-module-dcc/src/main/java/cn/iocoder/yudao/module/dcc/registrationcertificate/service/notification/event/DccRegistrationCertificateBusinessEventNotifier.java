package cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final DccRegistrationCertificateChangeConfirmationService changeConfirmationService;
    private final DccRegistrationCertificateViewerRecipientResolver viewerRecipientResolver;

    public DccRegistrationCertificateBusinessEventNotifier(
            DccRegistrationCertificateBusinessEventNotificationConfigService configService,
            DccRegistrationCertificateBusinessEventNotificationService notificationService,
            DccRegistrationCertificateChangeConfirmationService changeConfirmationService,
            DccRegistrationCertificateViewerRecipientResolver viewerRecipientResolver) {
        this.configService = require(configService, "configService");
        this.notificationService = require(notificationService, "notificationService");
        this.changeConfirmationService = require(changeConfirmationService, "changeConfirmationService");
        this.viewerRecipientResolver = require(viewerRecipientResolver, "viewerRecipientResolver");
    }

    public void validateChangeConfirmationRecipients(Long tenantId, String certificateType) {
        resolveChangeRecipients(tenantId, certificateType);
    }

    public void notifyNewCertificateFormalized(Long tenantId, Long ownerCompanyId, Long certificateId,
                                               Long versionId, Long actorId, String eventKey,
                                               String productName, String certificateNo,
                                               LocalDate effectiveDate, LocalDate expiryDate,
                                               String certificateType, boolean current) {
        send(tenantId, ownerCompanyId, certificateId, versionId, actorId, EVENT_NEW_CERTIFICATE_FORMALIZED,
                eventKey, "已入库", productName, certificateNo, effectiveDate, expiryDate,
                certificateType, isCe(certificateType) && current);
    }

    public void notifyRenewalCandidateUploaded(Long tenantId, Long ownerCompanyId, Long certificateId,
                                               Long versionId, Long actorId, String eventKey,
                                               String productName, String certificateNo,
                                               LocalDate effectiveDate, LocalDate expiryDate, String certificateType) {
        send(tenantId, ownerCompanyId, certificateId, versionId, actorId, EVENT_RENEWAL_CANDIDATE_UPLOADED,
                eventKey, "延续版本已入库", productName, certificateNo, effectiveDate, expiryDate, certificateType, isCe(certificateType));
    }

    public void notifyRenewalCandidateActivated(Long tenantId, Long ownerCompanyId, Long certificateId,
                                                Long versionId, Long actorId, String eventKey,
                                                String productName, String certificateNo,
                                                LocalDate effectiveDate, LocalDate expiryDate, String certificateType) {
        send(tenantId, ownerCompanyId, certificateId, versionId, actorId, EVENT_RENEWAL_CANDIDATE_ACTIVATED,
                eventKey, "延续版本已生效", productName, certificateNo, effectiveDate, expiryDate, certificateType, isCe(certificateType));
    }

    public void notifyChangeApprovalRecorded(Long tenantId, Long ownerCompanyId, Long certificateId,
                                             Long versionId, Long actorId, String eventKey,
                                             String productName, String certificateNo,
                                             LocalDate effectiveDate, LocalDate expiryDate, String certificateType) {
        Map<String,Object> params=detailParams("变更已生效，请确认",productName,certificateNo,effectiveDate,expiryDate,certificateType);
        changeConfirmationService.send(new DccRegistrationCertificateBusinessEventNotificationCommand(
                tenantId,ownerCompanyId,certificateId,versionId,actorId,EVENT_CHANGE_APPROVAL_RECORDED,
                eventKey,List.of(),null,params),resolveChangeRecipients(tenantId, certificateType));
    }

    private List<Long> resolveChangeRecipients(Long tenantId, String certificateType) {
        return isCe(certificateType) ? viewerRecipientResolver.resolve(tenantId)
                : configService.resolveChangeConfirmationRecipientUserIds(tenantId);
    }

    private void send(Long tenantId, Long ownerCompanyId, Long certificateId, Long versionId, Long actorId,
                      String eventType, String eventKey, String eventTitle, String productName,
                      String certificateNo, LocalDate effectiveDate, LocalDate expiryDate,
                      String certificateType, boolean notifyViewers) {
        Map<String, Object> detailParams = detailParams(eventTitle, productName, certificateNo,
                effectiveDate, expiryDate, certificateType);
        var command = new DccRegistrationCertificateBusinessEventNotificationCommand(
                tenantId, ownerCompanyId, certificateId, versionId, actorId, eventType, eventKey,
                List.of(), null, detailParams);
        if (notifyViewers) {
            notificationService.sendToExactUsers(command, viewerRecipientResolver.resolve(tenantId));
        } else {
            notificationService.sendToUsers(command, configService.resolveRecipientUserIds(tenantId));
        }
    }

    private static boolean isCe(String certificateType) {
        if (certificateType == null || !java.util.Set.of("DOMESTIC", "CE", "FDA").contains(certificateType)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_TEMPLATE_PARAM_MISSING);
        }
        return "CE".equals(certificateType);
    }

    private static Map<String, Object> detailParams(String eventTitle, String productName, String certificateNo,
                                                    LocalDate effectiveDate, LocalDate expiryDate, String certificateType) {
        if (isBlank(eventTitle) || isBlank(productName) || isBlank(certificateType)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_TEMPLATE_PARAM_MISSING);
        }
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("certificateTypeName", switch (certificateType) {
            case "DOMESTIC" -> "国内注册证";
            case "CE" -> "CE证书";
            case "FDA" -> "FDA证书";
            default -> throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_TEMPLATE_PARAM_MISSING);
        });
        params.put("eventTitle", eventTitle.trim());
        params.put("productName", productName.trim());
        params.put("certificateNo", isBlank(certificateNo) ? "证件未载明编号" : certificateNo.trim());
        params.put("effectiveDate", effectiveDate == null ? "证件未载明" : effectiveDate.toString());
        params.put("expiryDate", expiryDate == null ? "不适用" : expiryDate.toString());
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
