package cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID;

@Service
public class DccRegistrationCertificateChangeConfirmationService {
    private final JdbcTemplate jdbc;
    private final DccRegistrationCertificateBusinessEventNotificationService notifications;
    private final DccRegistrationCertificateBusinessClock clock;

    public DccRegistrationCertificateChangeConfirmationService(JdbcTemplate jdbc,
            DccRegistrationCertificateBusinessEventNotificationService notifications,
            DccRegistrationCertificateBusinessClock clock) {
        this.jdbc=jdbc;this.notifications=notifications;this.clock=clock;
    }

    @Transactional(rollbackFor=Exception.class)
    public void send(DccRegistrationCertificateBusinessEventNotificationCommand command,List<Long> recipients) {
        if (!Objects.equals(TenantContextHolder.getRequiredTenantId(),command.tenantId())
                || !"CHANGE_APPROVAL_RECORDED".equals(command.eventType())
                || recipients==null || recipients.isEmpty()
                || recipients.stream().anyMatch(id -> id==null || id<=0)) throw denied();
        var result=notifications.sendToUsers(command,recipients);
        LocalDateTime sentAt=clock.now();
        for (Long userId:recipients.stream().distinct().toList()) {
            Long messageId=result.recipientMessageIds().get(userId);
            if (messageId==null || messageId<=0) throw new IllegalStateException("变更通知缺少正式发送记录");
            try {
                jdbc.update("""
                    INSERT INTO dcc_registration_certificate_change_confirmation
                      (tenant_id,certificate_id,version_id,event_key,recipient_user_id,notify_message_id,sent_at)
                    VALUES (?,?,?,?,?,?,?)
                    """,command.tenantId(),command.certificateId(),command.versionId(),command.eventKey(),userId,messageId,sentAt);
            } catch (DuplicateKeyException duplicate) {
                var existing=getForRecipient(command.tenantId(),userId,messageId);
                if (!Objects.equals(existing.certificateId(),command.certificateId())
                        || !Objects.equals(existing.versionId(),command.versionId())
                        || !Objects.equals(existing.eventKey(),command.eventKey())) throw duplicate;
            }
        }
    }

    public List<Confirmation> list(Long tenantId,Long certificateId) {
        return jdbc.query("""
            SELECT * FROM dcc_registration_certificate_change_confirmation
             WHERE tenant_id=? AND certificate_id=? ORDER BY id DESC
            """,(rs,n)->new Confirmation(rs.getLong("id"),rs.getLong("certificate_id"),rs.getLong("version_id"),
                rs.getString("event_key"),rs.getLong("recipient_user_id"),rs.getLong("notify_message_id"),
                rs.getTimestamp("sent_at").toLocalDateTime(),rs.getTimestamp("confirmed_at")==null?null:
                rs.getTimestamp("confirmed_at").toLocalDateTime()),tenantId,certificateId);
    }

    public Confirmation getForRecipient(Long tenantId,Long actorId,Long messageId) {
        var rows=jdbc.query("""
            SELECT * FROM dcc_registration_certificate_change_confirmation
             WHERE tenant_id=? AND recipient_user_id=? AND notify_message_id=?
            """,(rs,n)->new Confirmation(rs.getLong("id"),rs.getLong("certificate_id"),rs.getLong("version_id"),
                rs.getString("event_key"),rs.getLong("recipient_user_id"),rs.getLong("notify_message_id"),
                rs.getTimestamp("sent_at").toLocalDateTime(),rs.getTimestamp("confirmed_at")==null?null:
                rs.getTimestamp("confirmed_at").toLocalDateTime()),tenantId,actorId,messageId);
        if (rows.size()!=1) throw denied();
        return rows.get(0);
    }

    @Transactional(rollbackFor=Exception.class)
    public Confirmation confirm(Long tenantId,Long actorId,Long messageId) {
        var record=getForRecipient(tenantId,actorId,messageId);
        if (record.confirmedAt()==null) jdbc.update("""
            UPDATE dcc_registration_certificate_change_confirmation SET confirmed_at=?
             WHERE id=? AND tenant_id=? AND recipient_user_id=? AND confirmed_at IS NULL
            """,clock.now(),record.id(),tenantId,actorId);
        return getForRecipient(tenantId,actorId,messageId);
    }

    private static ServiceException denied() {return new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);}

    public record Confirmation(Long id,Long certificateId,Long versionId,String eventKey,Long recipientUserId,
                               Long notifyMessageId,LocalDateTime sentAt,LocalDateTime confirmedAt) {}
}
