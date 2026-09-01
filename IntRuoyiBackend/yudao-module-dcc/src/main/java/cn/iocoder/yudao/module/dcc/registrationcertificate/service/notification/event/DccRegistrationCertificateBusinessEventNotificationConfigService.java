package cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED;

@Service
public class DccRegistrationCertificateBusinessEventNotificationConfigService {

    private static final String REMINDER_JOB_HANDLER_NAME = "registrationCertificateReminderDailyJob";

    private final JdbcTemplate jdbcTemplate;

    public DccRegistrationCertificateBusinessEventNotificationConfigService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = require(jdbcTemplate, "jdbcTemplate");
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
