package cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.config.DccRegistrationCertificateConfigService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT;

@Service
public class DccRegistrationCertificateBusinessEventNotificationConfigService {

    private final JdbcTemplate jdbcTemplate;

    public DccRegistrationCertificateBusinessEventNotificationConfigService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = require(jdbcTemplate, "jdbcTemplate");
    }

    public List<Long> resolveRecipientUserIds(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT);
        }
        List<String> recipientJsons = jdbcTemplate.queryForList("""
                SELECT threshold_recipient_user_ids_json
                  FROM dcc_registration_certificate_reminder_config
                 WHERE tenant_id = ?
                   AND deleted = 0
                 ORDER BY id
                """, String.class, tenantId);
        if (recipientJsons.size() != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT);
        }
        Map<String, List<Long>> configuredRecipients =
                DccRegistrationCertificateConfigService.parseThresholdRecipientUserIds(recipientJsons.get(0));
        Set<Long> recipientUserIds = new LinkedHashSet<>();
        configuredRecipients.values().forEach(recipients -> {
            if (recipients != null) {
                recipientUserIds.addAll(recipients);
            }
        });
        recipientUserIds.removeIf(userId -> userId == null || userId <= 0);
        if (recipientUserIds.isEmpty()) {
            return List.of();
        }
        return List.copyOf(new ArrayList<>(recipientUserIds));
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value;
    }
}
