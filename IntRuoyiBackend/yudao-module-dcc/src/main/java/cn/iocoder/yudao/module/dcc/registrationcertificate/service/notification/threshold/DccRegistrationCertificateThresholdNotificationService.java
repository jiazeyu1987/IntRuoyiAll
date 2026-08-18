package cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.threshold;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_MESSAGE_ID_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_STATUS_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_NOTIFY_SEND_FAILED;

@Service
public class DccRegistrationCertificateThresholdNotificationService {

    private static final String TEMPLATE_CODE = "DCC_REGISTRATION_CERTIFICATE_THRESHOLD_REMINDER";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SENDING = "SENDING";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";

    private final JdbcTemplate jdbcTemplate;
    private final NotifyMessageSendApi notifyMessageSendApi;

    public DccRegistrationCertificateThresholdNotificationService(
            JdbcTemplate jdbcTemplate, NotifyMessageSendApi notifyMessageSendApi) {
        if (jdbcTemplate == null) {
            throw new IllegalArgumentException("jdbcTemplate must not be null");
        }
        if (notifyMessageSendApi == null) {
            throw new IllegalArgumentException("notifyMessageSendApi must not be null");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.notifyMessageSendApi = notifyMessageSendApi;
    }

    public DccRegistrationCertificateThresholdDeliveryResult sendDelivery(Long tenantId, Long deliveryId) {
        DeliveryRow row = requireDelivery(tenantId, deliveryId);
        if (STATUS_SENT.equals(row.status())) {
            if (row.notifyMessageId() == null || row.notifyMessageId() <= 0) {
                markFailed(row, REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_MESSAGE_ID_REQUIRED,
                        "sent delivery has no message id");
                throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_MESSAGE_ID_REQUIRED);
            }
            return new DccRegistrationCertificateThresholdDeliveryResult(row.deliveryId(), STATUS_SENT,
                    row.notifyMessageId());
        }
        if (!STATUS_PENDING.equals(row.status()) && !STATUS_SENDING.equals(row.status())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_STATUS_INVALID);
        }
        Long messageId;
        try {
            messageId = notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(buildRequest(row));
        } catch (RuntimeException exception) {
            markFailed(row, REGISTRATION_CERTIFICATE_REMINDER_NOTIFY_SEND_FAILED, exception.getMessage());
            ServiceException mapped = new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_NOTIFY_SEND_FAILED);
            mapped.initCause(exception);
            throw mapped;
        }
        if (messageId == null || messageId <= 0) {
            markFailed(row, REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_MESSAGE_ID_REQUIRED,
                    "empty notify message id");
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_MESSAGE_ID_REQUIRED);
        }
        int affected = jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_reminder_delivery
                   SET status = 'SENT', notify_message_id = ?, sent_at = CURRENT_TIMESTAMP,
                       attempt_count = attempt_count + 1, last_failure_code = NULL, last_failure_reason = NULL
                 WHERE id = ? AND tenant_id = ? AND status IN ('PENDING', 'SENDING')
                """, messageId, row.deliveryId(), row.tenantId());
        if (affected != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_STATUS_INVALID);
        }
        return new DccRegistrationCertificateThresholdDeliveryResult(row.deliveryId(), STATUS_SENT, messageId);
    }

    private NotifySendSingleToUserIdempotentReqDTO buildRequest(DeliveryRow row) {
        NotifySendSingleToUserIdempotentReqDTO reqDTO = new NotifySendSingleToUserIdempotentReqDTO();
        reqDTO.setUserId(row.recipientUserId());
        reqDTO.setTemplateCode(TEMPLATE_CODE);
        reqDTO.setBusinessKey(row.deliveryKey());
        reqDTO.setTemplateParams(Map.of(
                "certificateId", row.certificateId(),
                "thresholdLevel", row.thresholdLevel(),
                "dueDate", row.dueDate().toString(),
                "businessDate", row.businessDate().toString()));
        return reqDTO;
    }

    private DeliveryRow requireDelivery(Long tenantId, Long deliveryId) {
        if (tenantId == null || tenantId <= 0 || deliveryId == null || deliveryId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_STATUS_INVALID);
        }
        List<DeliveryRow> rows = jdbcTemplate.query("""
                SELECT d.id AS delivery_id, d.tenant_id, d.occurrence_id, d.recipient_user_id,
                       d.recipient_company_id, d.delivery_key, d.status, d.notify_message_id,
                       o.certificate_id, o.threshold_level, o.business_date, o.due_date
                  FROM dcc_registration_certificate_reminder_delivery d
                  JOIN dcc_registration_certificate_reminder_occurrence o
                    ON o.id = d.occurrence_id
                   AND o.tenant_id = d.tenant_id
                 WHERE d.id = ?
                   AND d.tenant_id = ?
                """, (rs, rowNum) -> new DeliveryRow(
                rs.getLong("delivery_id"),
                rs.getLong("tenant_id"),
                rs.getLong("occurrence_id"),
                rs.getLong("recipient_user_id"),
                rs.getObject("recipient_company_id", Long.class),
                rs.getString("delivery_key"),
                rs.getString("status"),
                rs.getObject("notify_message_id", Long.class),
                rs.getLong("certificate_id"),
                rs.getString("threshold_level"),
                rs.getObject("business_date", LocalDate.class),
                rs.getObject("due_date", LocalDate.class)), deliveryId, tenantId);
        if (rows.size() != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_STATUS_INVALID);
        }
        DeliveryRow row = rows.get(0);
        if (row.deliveryKey() == null || row.deliveryKey().trim().isEmpty()
                || row.recipientUserId() == null || row.recipientUserId() <= 0
                || row.certificateId() == null || row.certificateId() <= 0
                || row.thresholdLevel() == null || row.thresholdLevel().trim().isEmpty()
                || row.businessDate() == null || row.dueDate() == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_STATUS_INVALID);
        }
        return row;
    }

    private void markFailed(DeliveryRow row, ErrorCode errorCode, String reason) {
        jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_reminder_delivery
                   SET status = 'FAILED', attempt_count = attempt_count + 1,
                       last_failure_code = ?, last_failure_reason = ?
                 WHERE id = ? AND tenant_id = ? AND status <> 'SENT'
                """, String.valueOf(errorCode.getCode()), safeReason(reason), row.deliveryId(), row.tenantId());
    }

    private static String safeReason(String reason) {
        String value = reason == null || reason.trim().isEmpty() ? "notification delivery failed" : reason.trim();
        return value.length() <= 1024 ? value : value.substring(0, 1024);
    }

    private record DeliveryRow(Long deliveryId, Long tenantId, Long occurrenceId, Long recipientUserId,
                               Long recipientCompanyId, String deliveryKey, String status, Long notifyMessageId,
                               Long certificateId, String thresholdLevel, LocalDate businessDate, LocalDate dueDate) {
    }
}
