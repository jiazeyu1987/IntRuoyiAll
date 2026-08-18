package cn.iocoder.yudao.module.dcc.registrationcertificate.service.config;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_CONFIG_TIME_INVALID;

@Service
public class DccRegistrationCertificateConfigService {

    private static final String DEFAULT_DAILY_RUN_TIME = "09:00";
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
    private static final String DEFAULT_THRESHOLDS = "[30,8,2,1]";

    private final JdbcTemplate jdbcTemplate;

    public DccRegistrationCertificateConfigService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateReminderConfig getOrCreate(Long tenantId) {
        requireTenant(tenantId);
        Optional<DccRegistrationCertificateReminderConfig> existing = selectActive(tenantId);
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            jdbcTemplate.update("""
                    INSERT INTO dcc_registration_certificate_reminder_config
                        (tenant_id, enabled, daily_run_time, timezone, threshold_days_json, row_version)
                    VALUES (?, ?, ?, ?, ?, 1)
                    """, tenantId, true, DEFAULT_DAILY_RUN_TIME, DEFAULT_TIMEZONE, DEFAULT_THRESHOLDS);
        } catch (DuplicateKeyException duplicate) {
            Optional<DccRegistrationCertificateReminderConfig> concurrent = selectActive(tenantId);
            if (concurrent.isPresent()) {
                return concurrent.get();
            }
            throw duplicate;
        }
        return selectRequired(tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateReminderConfig update(
            Long tenantId, Long actorId, DccRegistrationCertificateReminderConfigUpdateCommand command) {
        requireTenant(tenantId);
        if (actorId == null || actorId <= 0 || command == null || command.enabled() == null
                || command.expectedRowVersion() == null || command.expectedRowVersion() <= 0
                || !validDailyRunTime(command.dailyRunTime())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_CONFIG_TIME_INVALID);
        }
        getOrCreate(tenantId);
        int affected = jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_reminder_config
                   SET enabled = ?,
                       daily_run_time = ?,
                       timezone = ?,
                       row_version = row_version + 1
                 WHERE tenant_id = ?
                   AND deleted = 0
                   AND row_version = ?
                """, command.enabled(), command.dailyRunTime().trim(), DEFAULT_TIMEZONE,
                tenantId, command.expectedRowVersion());
        if (affected != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT);
        }
        return selectRequired(tenantId);
    }

    private DccRegistrationCertificateReminderConfig selectRequired(Long tenantId) {
        Optional<DccRegistrationCertificateReminderConfig> config = selectActive(tenantId);
        if (config.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT);
        }
        return config.get();
    }

    private Optional<DccRegistrationCertificateReminderConfig> selectActive(Long tenantId) {
        List<DccRegistrationCertificateReminderConfig> configs = jdbcTemplate.query("""
                SELECT id, tenant_id, enabled, daily_run_time, timezone, threshold_days_json, row_version
                  FROM dcc_registration_certificate_reminder_config
                 WHERE tenant_id = ?
                   AND deleted = 0
                 ORDER BY id
                """, (rs, rowNum) -> new DccRegistrationCertificateReminderConfig(
                rs.getLong("id"),
                rs.getLong("tenant_id"),
                rs.getBoolean("enabled"),
                rs.getString("daily_run_time"),
                rs.getString("timezone"),
                rs.getString("threshold_days_json"),
                rs.getInt("row_version")), tenantId);
        if (configs.isEmpty()) {
            return Optional.empty();
        }
        if (configs.size() != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT);
        }
        return Optional.of(configs.get(0));
    }

    private static void requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT);
        }
    }

    private static boolean validDailyRunTime(String value) {
        if (value == null || !value.equals(value.trim()) || value.length() != 5
                || !value.matches("^[0-2][0-9]:[0-5][0-9]$")) {
            return false;
        }
        return value.compareTo("23:59") <= 0;
    }
}
