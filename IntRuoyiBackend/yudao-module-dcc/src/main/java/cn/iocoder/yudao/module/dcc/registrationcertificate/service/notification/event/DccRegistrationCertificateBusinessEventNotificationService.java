package cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder.DccRegistrationCertificateRecipient;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder.DccRegistrationCertificateRecipientResolver;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_EVENT_NOTIFICATION_SCOPE_UNAPPROVED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_MESSAGE_ID_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_NOTIFY_SEND_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_TEMPLATE_PARAM_MISSING;

@Service
public class DccRegistrationCertificateBusinessEventNotificationService {

    public static final String TEMPLATE_CODE = "DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT";

    private static final Set<String> APPROVED_EVENTS = Set.of(
            "NEW_CERTIFICATE_FORMALIZED",
            "RENEWAL_CANDIDATE_UPLOADED",
            "RENEWAL_CANDIDATE_ACTIVATED",
            "CHANGE_APPROVAL_RECORDED",
            "SUPPORTING_FILE_CONFIRMED");

    private final DccRegistrationCertificateRecipientResolver recipientResolver;
    private final NotifyMessageSendApi notifyMessageSendApi;

    public DccRegistrationCertificateBusinessEventNotificationService(
            DccRegistrationCertificateRecipientResolver recipientResolver,
            NotifyMessageSendApi notifyMessageSendApi) {
        this.recipientResolver = recipientResolver;
        this.notifyMessageSendApi = notifyMessageSendApi;
    }

    public DccRegistrationCertificateBusinessEventNotificationResult send(
            DccRegistrationCertificateBusinessEventNotificationCommand command) {
        ValidatedCommand validated = validate(command);
        List<DccRegistrationCertificateRecipient> companyRecipients = recipientResolver.resolve(
                validated.ownerCompanyId(), command.documentControlRoleIds(), validated.recipientPermission());
        LinkedHashMap<Long, Long> messageIds = new LinkedHashMap<>();
        for (DccRegistrationCertificateRecipient recipient : companyRecipients) {
            sendToRecipient(validated, recipient.userId(), messageIds);
        }
        sendToRecipient(validated, validated.actorId(), messageIds);
        return new DccRegistrationCertificateBusinessEventNotificationResult(
                validated.eventType(), Map.copyOf(messageIds));
    }

    private void sendToRecipient(ValidatedCommand command, Long userId, Map<Long, Long> messageIds) {
        if (userId == null || userId <= 0 || messageIds.containsKey(userId)) {
            return;
        }
        Long messageId;
        try {
            messageId = notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(buildRequest(command, userId));
        } catch (RuntimeException exception) {
            ServiceException mapped = new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_NOTIFY_SEND_FAILED);
            mapped.initCause(exception);
            throw mapped;
        }
        if (messageId == null || messageId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_MESSAGE_ID_REQUIRED);
        }
        messageIds.put(userId, messageId);
    }

    private NotifySendSingleToUserIdempotentReqDTO buildRequest(ValidatedCommand command, Long userId) {
        String businessKey = "REG_CERT:" + command.eventType() + ":" + command.eventKey() + ":USER:" + userId;
        if (businessKey.length() > 255) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_TEMPLATE_PARAM_MISSING);
        }
        NotifySendSingleToUserIdempotentReqDTO reqDTO = new NotifySendSingleToUserIdempotentReqDTO();
        reqDTO.setUserId(userId);
        reqDTO.setTemplateCode(TEMPLATE_CODE);
        reqDTO.setBusinessKey(businessKey);
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        if (command.detailParams() != null) {
            params.putAll(command.detailParams());
        }
        params.put("eventType", command.eventType());
        params.put("eventKey", command.eventKey());
        params.put("tenantId", command.tenantId());
        params.put("ownerCompanyId", command.ownerCompanyId());
        params.put("certificateId", command.certificateId());
        params.put("versionId", command.versionId());
        params.put("actorId", command.actorId());
        reqDTO.setTemplateParams(Map.copyOf(params));
        return reqDTO;
    }

    private static ValidatedCommand validate(DccRegistrationCertificateBusinessEventNotificationCommand command) {
        if (command == null
                || !positive(command.tenantId())
                || !positive(command.ownerCompanyId())
                || !positive(command.certificateId())
                || !positive(command.versionId())
                || !positive(command.actorId())
                || StrUtil.isBlank(command.eventType())
                || StrUtil.isBlank(command.eventKey())
                || StrUtil.isBlank(command.recipientPermission())
                || command.documentControlRoleIds() == null
                || command.documentControlRoleIds().isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_TEMPLATE_PARAM_MISSING);
        }
        String eventType = command.eventType().trim();
        if (!APPROVED_EVENTS.contains(eventType)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_EVENT_NOTIFICATION_SCOPE_UNAPPROVED);
        }
        requireReadableTemplateParams(command.detailParams());
        return new ValidatedCommand(command.tenantId(), command.ownerCompanyId(), command.certificateId(),
                command.versionId(), command.actorId(), eventType, command.eventKey().trim(),
                command.recipientPermission().trim(), command.detailParams());
    }

    private static void requireReadableTemplateParams(Map<String, Object> detailParams) {
        if (detailParams == null
                || missing(detailParams, "eventTitle")
                || missing(detailParams, "productName")
                || missing(detailParams, "certificateNo")
                || missing(detailParams, "effectiveDate")
                || missing(detailParams, "expiryDate")) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_TEMPLATE_PARAM_MISSING);
        }
    }

    private static boolean missing(Map<String, Object> detailParams, String key) {
        Object value = detailParams.get(key);
        return value == null || value instanceof String text && StrUtil.isBlank(text);
    }

    private static boolean positive(Long value) {
        return value != null && value > 0;
    }

    private record ValidatedCommand(Long tenantId, Long ownerCompanyId, Long certificateId, Long versionId,
                                    Long actorId, String eventType, String eventKey, String recipientPermission,
                                    Map<String, Object> detailParams) {
    }
}
