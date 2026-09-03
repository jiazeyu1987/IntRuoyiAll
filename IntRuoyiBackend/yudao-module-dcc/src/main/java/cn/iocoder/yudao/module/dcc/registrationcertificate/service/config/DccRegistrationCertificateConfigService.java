package cn.iocoder.yudao.module.dcc.registrationcertificate.service.config;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.dto.SystemEntitlementSyncReqDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_CONFIG_TIME_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_THRESHOLD_INVALID;

@Service
public class DccRegistrationCertificateConfigService {

    private static final String DEFAULT_DAILY_RUN_TIME = "09:00";
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
    private static final String DEFAULT_THRESHOLDS = "[30,8,2,1]";
    private static final String DEFAULT_THRESHOLD_RECIPIENTS = "{}";
    private static final List<String> REQUIRED_THRESHOLD_LEVELS = List.of("T_30", "T_8", "T_2", "T_1");
    private static final String ENTITLEMENT_POLICY_CODE = "DCC_REGISTRATION_CERTIFICATE_REMINDER_VIEW";
    private static final String ENTITLEMENT_SOURCE_TYPE = "DCC_REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT";
    private static final String ENTITLEMENT_SOURCE_KEY = "REGISTRATION_CERTIFICATE_REMINDER_CONFIG";

    private final JdbcTemplate jdbcTemplate;
    private final AdminUserApi adminUserApi;
    private final PermissionApi permissionApi;

    public DccRegistrationCertificateConfigService(JdbcTemplate jdbcTemplate, AdminUserApi adminUserApi,
                                                    PermissionApi permissionApi) {
        this.jdbcTemplate = jdbcTemplate;
        this.adminUserApi = adminUserApi;
        this.permissionApi = permissionApi;
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
                        (tenant_id, enabled, daily_run_time, timezone, threshold_days_json,
                         threshold_recipient_user_ids_json, row_version)
                    VALUES (?, ?, ?, ?, ?, ?, 1)
                    """, tenantId, true, DEFAULT_DAILY_RUN_TIME, DEFAULT_TIMEZONE, DEFAULT_THRESHOLDS,
                    DEFAULT_THRESHOLD_RECIPIENTS);
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
        Map<String, List<Long>> thresholdRecipientUserIds = normalizeThresholdRecipientUserIds(
                command.thresholdRecipientUserIds());
        Set<Long> allRecipientUserIds = new LinkedHashSet<>();
        thresholdRecipientUserIds.values().forEach(allRecipientUserIds::addAll);
        adminUserApi.validateUserList(allRecipientUserIds);
        String recipientJson = JsonUtils.toJsonString(thresholdRecipientUserIds);
        getOrCreate(tenantId);
        int affected = jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_reminder_config
                   SET enabled = ?,
                       daily_run_time = ?,
                       timezone = ?,
                       threshold_recipient_user_ids_json = ?,
                       row_version = row_version + 1
                 WHERE tenant_id = ?
                   AND deleted = 0
                   AND row_version = ?
                """, command.enabled(), command.dailyRunTime().trim(), DEFAULT_TIMEZONE, recipientJson,
                tenantId, command.expectedRowVersion());
        if (affected != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT);
        }
        DccRegistrationCertificateReminderConfig updated = selectRequired(tenantId);
        permissionApi.syncEntitlementClaims(SystemEntitlementSyncReqDTO.builder()
                .tenantId(tenantId)
                .sourceType(ENTITLEMENT_SOURCE_TYPE)
                .sourceKey(ENTITLEMENT_SOURCE_KEY)
                .sourceVersion(String.valueOf(updated.rowVersion()))
                .sourceDigest(DigestUtil.sha256Hex(recipientJson))
                .policyCode(ENTITLEMENT_POLICY_CODE)
                .resolvedUserIds(Set.copyOf(allRecipientUserIds))
                .operatorUserId(actorId)
                .operatorUsername(String.valueOf(actorId))
                .build());
        return updated;
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
                SELECT id, tenant_id, enabled, daily_run_time, timezone, threshold_days_json,
                       threshold_recipient_user_ids_json, row_version
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
                rs.getString("threshold_recipient_user_ids_json"),
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

    public List<Long> getRecipientUserIds(DccRegistrationCertificateReminderConfig config, String thresholdLevel) {
        if (config == null || !REQUIRED_THRESHOLD_LEVELS.contains(thresholdLevel)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_THRESHOLD_INVALID);
        }
        List<Long> recipientUserIds = parseThresholdRecipientUserIds(
                config.thresholdRecipientUserIdsJson()).get(thresholdLevel);
        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT_REQUIRED);
        }
        return recipientUserIds;
    }

    public static Map<String, List<Long>> parseThresholdRecipientUserIds(String json) {
        try {
            Map<String, List<Long>> parsed = JsonUtils.parseObject(json,
                    new TypeReference<LinkedHashMap<String, List<Long>>>() { });
            return parsed == null ? Map.of() : Map.copyOf(parsed);
        } catch (RuntimeException exception) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_THRESHOLD_INVALID);
        }
    }

    private static Map<String, List<Long>> normalizeThresholdRecipientUserIds(
            Map<String, List<Long>> rawRecipients) {
        if (rawRecipients == null || !rawRecipients.keySet().equals(new LinkedHashSet<>(REQUIRED_THRESHOLD_LEVELS))) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_THRESHOLD_INVALID);
        }
        LinkedHashMap<String, List<Long>> normalized = new LinkedHashMap<>();
        for (String thresholdLevel : REQUIRED_THRESHOLD_LEVELS) {
            List<Long> rawUserIds = rawRecipients.get(thresholdLevel);
            if (rawUserIds == null || rawUserIds.isEmpty() || rawUserIds.size() > 100) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT_REQUIRED);
            }
            LinkedHashSet<Long> userIds = new LinkedHashSet<>();
            for (Long userId : rawUserIds) {
                if (userId == null || userId <= 0) {
                    throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT_REQUIRED);
                }
                userIds.add(userId);
            }
            normalized.put(thresholdLevel, List.copyOf(new ArrayList<>(userIds)));
        }
        return Map.copyOf(normalized);
    }
}
