package cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.config.DccRegistrationCertificateConfigService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT;

@Service
public class DccRegistrationCertificateBusinessEventNotificationConfigService {

    private static final String REMINDER_JOB_HANDLER_NAME = "registrationCertificateReminderDailyJob";

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

    public RecipientScope resolveRecipientScope() {
        List<String> params = jdbcTemplate.queryForList("""
                SELECT handler_param
                  FROM infra_job
                 WHERE handler_name = ?
                   AND deleted = 0
                 ORDER BY id
                """, String.class, REMINDER_JOB_HANDLER_NAME);
        if (params.size() != 1 || isBlank(params.get(0))) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED);
        }
        JobParam jobParam;
        try {
            jobParam = JsonUtils.parseObject(params.get(0), JobParam.class);
        } catch (RuntimeException exception) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED);
        }
        if (jobParam == null || jobParam.roleIds() == null || jobParam.roleIds().isEmpty()
                || isBlank(jobParam.permission())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED);
        }
        for (Long roleId : jobParam.roleIds()) {
            if (roleId == null || roleId <= 0) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED);
            }
        }
        return new RecipientScope(List.copyOf(jobParam.roleIds()), jobParam.permission().trim());
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

    private record JobParam(Long actorId, List<Long> roleIds, String permission) {
    }

    public record RecipientScope(List<Long> roleIds, String permission) {
    }
}
