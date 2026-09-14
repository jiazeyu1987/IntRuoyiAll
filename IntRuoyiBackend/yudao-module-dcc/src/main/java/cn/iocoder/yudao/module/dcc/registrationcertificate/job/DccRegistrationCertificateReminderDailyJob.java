package cn.iocoder.yudao.module.dcc.registrationcertificate.job;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.activation.DccRegistrationCertificateActivationCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.activation.DccRegistrationCertificateActivationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.config.DccRegistrationCertificateConfigService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.config.DccRegistrationCertificateReminderConfig;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.dailyrun.DccRegistrationCertificateDailyRunRecord;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.dailyrun.DccRegistrationCertificateDailyRunService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.dailyrun.DccRegistrationCertificateDailyRunStartResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.threshold.DccRegistrationCertificateThresholdDeliveryResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.threshold.DccRegistrationCertificateThresholdNotificationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder.DccRegistrationCertificateRecipient;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder.DccRegistrationCertificateReminderRunResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder.DccRegistrationCertificateReminderService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_DAILY_RUN_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED;

@Component("registrationCertificateReminderDailyJob")
public class DccRegistrationCertificateReminderDailyJob implements JobHandler {

    private static final String DELIVERY_CREATOR = "registration-certificate-reminder-job";
    private static final int MANUAL_BUSINESS_RUN_HOUR = 9;

    private final TenantFrameworkService tenantFrameworkService;
    private final DccRegistrationCertificateConfigService configService;
    private final DccRegistrationCertificateDailyRunService dailyRunService;
    private final DccRegistrationCertificateActivationService activationService;
    private final DccRegistrationCertificateReminderService reminderService;
    private final DccRegistrationCertificateThresholdNotificationService notificationService;
    private final DccRegistrationCertificateBusinessClock businessClock;
    private final JdbcTemplate jdbcTemplate;

    public DccRegistrationCertificateReminderDailyJob(
            TenantFrameworkService tenantFrameworkService,
            DccRegistrationCertificateConfigService configService,
            DccRegistrationCertificateDailyRunService dailyRunService,
            DccRegistrationCertificateActivationService activationService,
            DccRegistrationCertificateReminderService reminderService,
            DccRegistrationCertificateThresholdNotificationService notificationService,
            DccRegistrationCertificateBusinessClock businessClock,
            JdbcTemplate jdbcTemplate) {
        this.tenantFrameworkService = require(tenantFrameworkService, "tenantFrameworkService");
        this.configService = require(configService, "configService");
        this.dailyRunService = require(dailyRunService, "dailyRunService");
        this.activationService = require(activationService, "activationService");
        this.reminderService = require(reminderService, "reminderService");
        this.notificationService = require(notificationService, "notificationService");
        this.businessClock = require(businessClock, "businessClock");
        this.jdbcTemplate = require(jdbcTemplate, "jdbcTemplate");
    }

    @Override
    @TenantIgnore
    public String execute(String param) {
        JobParam jobParam = parseParam(param);
        List<Long> tenantIds = tenantFrameworkService.getTenantIds();
        if (tenantIds == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED);
        }
        LocalDate businessDate = businessClock.businessDate();
        List<TenantOutcome> outcomes = new ArrayList<>();
        List<TenantFailure> failures = new ArrayList<>();
        for (Long tenantId : tenantIds) {
            if (tenantId == null || tenantId <= 0) {
                failures.add(new TenantFailure(tenantId, "租户 ID 不合法"));
                continue;
            }
            try {
                TenantOutcome[] holder = new TenantOutcome[1];
                TenantUtils.execute(tenantId, () -> holder[0] = runTenant(tenantId, businessDate, jobParam));
                outcomes.add(holder[0]);
            } catch (RuntimeException exception) {
                failures.add(new TenantFailure(tenantId, safeReason(exception)));
            }
        }
        long successes = outcomes.stream().filter(TenantOutcome::success).count();
        long skipped = outcomes.stream().filter(TenantOutcome::skipped).count();
        if (!failures.isEmpty()) {
            throw new IllegalStateException("注册证提醒任务执行失败：业务日期="
                    + businessDate + ", successes=" + successes + ", skipped=" + skipped
                    + ", failures=" + failures);
        }
        return "注册证提醒每日任务：业务日期=%s，成功=%d，跳过=%d，失败=0"
                .formatted(businessDate, successes, skipped);
    }

    public String executeTenantAtBusinessDate(Long tenantId, LocalDate businessDate, String param) {
        if (tenantId == null || tenantId <= 0 || businessDate == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED);
        }
        JobParam jobParam = parseParam(param);
        try {
            TenantOutcome[] holder = new TenantOutcome[1];
            businessClock.runAt(businessDate.atTime(MANUAL_BUSINESS_RUN_HOUR, 0), () -> {
                TenantUtils.execute(tenantId, () -> holder[0] = runTenant(tenantId, businessDate, jobParam));
                return null;
            });
            TenantOutcome outcome = holder[0];
            long successes = outcome != null && outcome.success() ? 1 : 0;
            long skipped = outcome != null && outcome.skipped() ? 1 : 0;
            return "registrationCertificateReminderDailyJob tenantId=%d businessDate=%s successes=%d skipped=%d failures=0"
                    .formatted(tenantId, businessDate, successes, skipped);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("registration certificate reminder job failed: tenantId="
                    + tenantId + ", businessDate=" + businessDate + ", failures=["
                    + new TenantFailure(tenantId, safeReason(exception)) + "]", exception);
        }
    }

    private TenantOutcome runTenant(Long tenantId, LocalDate businessDate, JobParam jobParam) {
        DccRegistrationCertificateReminderConfig config = configService.getOrCreate(tenantId);
        if (!Boolean.TRUE.equals(config.enabled())) {
            return TenantOutcome.skipped(tenantId);
        }
        DccRegistrationCertificateDailyRunStartResult start = dailyRunService.startTenantRun(tenantId, businessDate);
        DccRegistrationCertificateDailyRunRecord run = start.run();
        if (!start.started()) {
            return TenantOutcome.skipped(tenantId);
        }
        try {
            int activated = activateDueCandidates(tenantId, businessDate, jobParam.actorId());
            DccRegistrationCertificateReminderRunResult occurrences =
                    reminderService.generateOccurrences(tenantId, run.id(), businessDate);
            int createdDeliveries = createDeliveries(tenantId, run.id(), config);
            int sentDeliveries = sendPendingDeliveries(tenantId, run.id());
            int deliveredOccurrences = markDeliveredOccurrences(tenantId, run.id());
            ensureNoPendingOccurrences(tenantId, run.id());
            dailyRunService.markSuccess(tenantId, run.id(), JsonUtils.toJsonString(Map.of(
                    "activated", activated,
                    "pendingOccurrences", occurrences.pendingCount(),
                    "suppressedOccurrences", occurrences.suppressedCount(),
                    "createdDeliveries", createdDeliveries,
                    "sentDeliveries", sentDeliveries,
                    "deliveredOccurrences", deliveredOccurrences)));
            return TenantOutcome.success(tenantId);
        } catch (RuntimeException exception) {
            dailyRunService.markFailed(tenantId, run.id(), safeReason(exception), JsonUtils.toJsonString(Map.of(
                    "error", safeReason(exception))));
            throw exception;
        }
    }

    private void ensureNoPendingOccurrences(Long tenantId, Long runId) {
        Integer pending = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM dcc_registration_certificate_reminder_occurrence
                 WHERE tenant_id = ?
                   AND run_id = ?
                   AND status = 'PENDING_DELIVERY'
                """, Integer.class, tenantId, runId);
        if (pending != null && pending > 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_CONFLICT);
        }
    }

    private int activateDueCandidates(Long tenantId, LocalDate businessDate, Long actorId) {
        List<DueCandidate> candidates = jdbcTemplate.query("""
                SELECT c.id AS certificate_id,
                       c.current_version_id,
                       c.pending_version_id,
                       c.row_version,
                       v.effective_date
                  FROM dcc_registration_certificate c
                  JOIN dcc_registration_certificate_version v
                    ON v.id = c.pending_version_id
                   AND v.tenant_id = c.tenant_id
                   AND v.certificate_id = c.id
                   AND v.status = 'PENDING_EFFECTIVE'
                   AND v.deleted = 0
                 WHERE c.tenant_id = ?
                   AND c.status = 'ACTIVE'
                   AND c.pending_version_id IS NOT NULL
                   AND c.deleted = 0
                   AND v.effective_date <= ?
                 ORDER BY c.id
                """, (rs, rowNum) -> new DueCandidate(
                rs.getLong("certificate_id"),
                rs.getLong("current_version_id"),
                rs.getLong("pending_version_id"),
                rs.getInt("row_version"),
                rs.getObject("effective_date", LocalDate.class)), tenantId, businessDate);
        int activated = 0;
        for (DueCandidate candidate : candidates) {
            activationService.activateDueCandidate(new DccRegistrationCertificateActivationCommand(
                    tenantId,
                    actorId,
                    "registration-certificate-reminder-activation:%d:%d:%s"
                            .formatted(candidate.certificateId(), candidate.pendingVersionId(), businessDate),
                    "registration-certificate-reminder-job:%d:%s:%d:%d"
                            .formatted(tenantId, businessDate, candidate.certificateId(), candidate.pendingVersionId()),
                    candidate.certificateId(),
                    candidate.rowVersion(),
                    candidate.currentVersionId(),
                    candidate.pendingVersionId()));
            activated++;
        }
        return activated;
    }

    private int createDeliveries(Long tenantId, Long runId,
                                 DccRegistrationCertificateReminderConfig config) {
        List<OccurrenceRow> occurrences = jdbcTemplate.query("""
                SELECT id, owner_company_id, threshold_level
                  FROM dcc_registration_certificate_reminder_occurrence
                 WHERE tenant_id = ?
                   AND run_id = ?
                   AND status = 'PENDING_DELIVERY'
                 ORDER BY id
        """, (rs, rowNum) -> new OccurrenceRow(
                rs.getLong("id"),
                rs.getLong("owner_company_id"),
                rs.getString("threshold_level")), tenantId, runId);
        int created = 0;
        for (OccurrenceRow occurrence : occurrences) {
            List<DccRegistrationCertificateRecipient> recipients = configService.getRecipientUserIds(
                    config, occurrence.thresholdLevel()).stream()
                    .map(userId -> new DccRegistrationCertificateRecipient(userId, occurrence.ownerCompanyId()))
                    .toList();
            for (DccRegistrationCertificateRecipient recipient : recipients) {
                if (insertDeliveryIfAbsent(tenantId, occurrence.id(), recipient)) {
                    created++;
                }
            }
        }
        return created;
    }

    private boolean insertDeliveryIfAbsent(Long tenantId, Long occurrenceId,
                                           DccRegistrationCertificateRecipient recipient) {
        String deliveryKey = "registration-certificate-reminder-delivery:%d:%d"
                .formatted(occurrenceId, recipient.userId());
        Long existingId = findDeliveryId(tenantId, deliveryKey);
        if (existingId != null) {
            return false;
        }
        try {
            int affected = jdbcTemplate.update("""
                    INSERT INTO dcc_registration_certificate_reminder_delivery
                      (tenant_id, occurrence_id, recipient_user_id, recipient_company_id,
                       delivery_key, status, attempt_count, detail_json, creator)
                    VALUES (?, ?, ?, ?, ?, 'PENDING', 0, '{}', ?)
                    """, tenantId, occurrenceId, recipient.userId(), recipient.companyId(), deliveryKey,
                    DELIVERY_CREATOR);
            if (affected != 1) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_CONFLICT);
            }
            return true;
        } catch (DuplicateKeyException duplicate) {
            if (findDeliveryId(tenantId, deliveryKey) != null) {
                return false;
            }
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_CONFLICT);
        }
    }

    private int sendPendingDeliveries(Long tenantId, Long runId) {
        List<Long> deliveryIds = jdbcTemplate.queryForList("""
                SELECT d.id
                  FROM dcc_registration_certificate_reminder_delivery d
                  JOIN dcc_registration_certificate_reminder_occurrence o
                    ON o.id = d.occurrence_id
                   AND o.tenant_id = d.tenant_id
                 WHERE d.tenant_id = ?
                   AND o.run_id = ?
                   AND d.status IN ('PENDING', 'SENDING')
                 ORDER BY d.id
                """, Long.class, tenantId, runId);
        int sent = 0;
        for (Long deliveryId : deliveryIds) {
            DccRegistrationCertificateThresholdDeliveryResult result =
                    notificationService.sendDelivery(tenantId, deliveryId);
            if (!"SENT".equals(result.status())) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_CONFLICT);
            }
            sent++;
        }
        return sent;
    }

    private int markDeliveredOccurrences(Long tenantId, Long runId) {
        return jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_reminder_occurrence
                   SET status = 'DELIVERED'
                 WHERE tenant_id = ?
                   AND run_id = ?
                   AND status = 'PENDING_DELIVERY'
                   AND EXISTS (
                         SELECT 1
                           FROM dcc_registration_certificate_reminder_delivery d
                          WHERE d.tenant_id = dcc_registration_certificate_reminder_occurrence.tenant_id
                            AND d.occurrence_id = dcc_registration_certificate_reminder_occurrence.id
                            AND d.status = 'SENT'
                       )
                   AND NOT EXISTS (
                         SELECT 1
                           FROM dcc_registration_certificate_reminder_delivery d
                          WHERE d.tenant_id = dcc_registration_certificate_reminder_occurrence.tenant_id
                            AND d.occurrence_id = dcc_registration_certificate_reminder_occurrence.id
                            AND d.status <> 'SENT'
                       )
                """, tenantId, runId);
    }

    private Long findDeliveryId(Long tenantId, String deliveryKey) {
        List<Long> ids = jdbcTemplate.queryForList("""
                SELECT id
                  FROM dcc_registration_certificate_reminder_delivery
                 WHERE tenant_id = ?
                   AND delivery_key = ?
                 ORDER BY id
                """, Long.class, tenantId, deliveryKey);
        if (ids.isEmpty()) {
            return null;
        }
        if (ids.size() != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_CONFLICT);
        }
        return ids.get(0);
    }

    private static JobParam parseParam(String param) {
        if (isBlank(param)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED);
        }
        JobParam jobParam;
        try {
            jobParam = JsonUtils.parseObject(param, JobParam.class);
        } catch (RuntimeException exception) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED);
        }
        if (jobParam == null || jobParam.actorId() == null || jobParam.actorId() <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED);
        }
        return new JobParam(jobParam.actorId());
    }

    private static String safeReason(Throwable exception) {
        String reason = exception == null ? null : exception.getMessage();
        if (isBlank(reason) && exception != null && exception.getCause() != null) {
            reason = exception.getCause().getMessage();
        }
        if (isBlank(reason)) {
            reason = "注册证提醒任务执行失败";
        }
        String trimmed = reason.trim();
        return trimmed.length() <= 512 ? trimmed : trimmed.substring(0, 512);
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

    private record JobParam(Long actorId) {
    }

    private record TenantOutcome(Long tenantId, boolean success, boolean skipped) {
        static TenantOutcome success(Long tenantId) {
            return new TenantOutcome(tenantId, true, false);
        }

        static TenantOutcome skipped(Long tenantId) {
            return new TenantOutcome(tenantId, false, true);
        }
    }

    private record TenantFailure(Long tenantId, String reason) {
    }

    private record DueCandidate(Long certificateId, Long currentVersionId, Long pendingVersionId,
                                Integer rowVersion, LocalDate effectiveDate) {
    }

    private record OccurrenceRow(Long id, Long ownerCompanyId, String thresholdLevel) {
    }
}
