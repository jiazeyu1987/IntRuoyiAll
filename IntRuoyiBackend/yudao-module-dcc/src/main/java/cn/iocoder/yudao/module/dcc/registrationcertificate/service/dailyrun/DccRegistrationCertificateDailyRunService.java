package cn.iocoder.yudao.module.dcc.registrationcertificate.service.dailyrun;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_DAILY_RUN_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_DAILY_RUN_FAILED;

@Service
public class DccRegistrationCertificateDailyRunService {

    private final JdbcTemplate jdbcTemplate;
    private final DccRegistrationCertificateBusinessClock businessClock;

    public DccRegistrationCertificateDailyRunService(
            JdbcTemplate jdbcTemplate,
            DccRegistrationCertificateBusinessClock businessClock) {
        this.jdbcTemplate = jdbcTemplate;
        this.businessClock = businessClock;
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateDailyRunStartResult startTenantRun(Long tenantId, LocalDate businessDate) {
        requireTenantAndDate(tenantId, businessDate);
        Optional<DccRegistrationCertificateDailyRunRecord> existing = selectByTenantAndDate(tenantId, businessDate);
        if (existing.isEmpty()) {
            try {
                jdbcTemplate.update("""
                        INSERT INTO dcc_registration_certificate_daily_run
                            (tenant_id, business_date, run_key, status, retry_count,
                             failure_reason, started_at, finished_at, detail_json)
                        VALUES (?, ?, ?, 'RUNNING', 0, NULL, ?, NULL, '{}')
                        """, tenantId, businessDate, runKey(tenantId, businessDate), businessClock.now());
            } catch (DuplicateKeyException duplicate) {
                DccRegistrationCertificateDailyRunRecord concurrent =
                        selectRequiredByTenantAndDate(tenantId, businessDate);
                return new DccRegistrationCertificateDailyRunStartResult(concurrent, false);
            }
            return new DccRegistrationCertificateDailyRunStartResult(
                    selectRequiredByTenantAndDate(tenantId, businessDate), true);
        }
        DccRegistrationCertificateDailyRunRecord existingRun = existing.get();
        if ("FAILED".equals(existingRun.status())) {
            int affected = jdbcTemplate.update("""
                    UPDATE dcc_registration_certificate_daily_run
                       SET status = 'RUNNING',
                           retry_count = retry_count + 1,
                           failure_reason = NULL,
                           started_at = ?,
                           finished_at = NULL,
                           detail_json = '{}'
                     WHERE id = ?
                       AND tenant_id = ?
                       AND status = 'FAILED'
                    """, businessClock.now(), existingRun.id(), tenantId);
            if (affected != 1) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_DAILY_RUN_CONFLICT);
            }
            return new DccRegistrationCertificateDailyRunStartResult(
                    selectRequiredById(tenantId, existingRun.id()), true);
        }
        return new DccRegistrationCertificateDailyRunStartResult(existingRun, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateDailyRunRecord markSuccess(Long tenantId, Long runId, String detailJson) {
        requireTenantAndRun(tenantId, runId);
        String detail = validDetailJson(detailJson);
        LocalDateTime finishedAt = businessClock.now();
        int affected = jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_daily_run
                   SET status = 'SUCCESS',
                       failure_reason = NULL,
                       finished_at = ?,
                       detail_json = ?
                 WHERE id = ?
                   AND tenant_id = ?
                   AND status = 'RUNNING'
                """, finishedAt, detail, runId, tenantId);
        if (affected != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_DAILY_RUN_CONFLICT);
        }
        return selectRequiredById(tenantId, runId);
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateDailyRunRecord markFailed(
            Long tenantId, Long runId, String failureReason, String detailJson) {
        requireTenantAndRun(tenantId, runId);
        if (failureReason == null || failureReason.trim().isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_DAILY_RUN_FAILED);
        }
        String detail = validDetailJson(detailJson);
        int affected = jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_daily_run
                   SET status = 'FAILED',
                       failure_reason = ?,
                       finished_at = ?,
                       detail_json = ?
                 WHERE id = ?
                   AND tenant_id = ?
                   AND status = 'RUNNING'
                """, failureReason.trim(), businessClock.now(), detail, runId, tenantId);
        if (affected != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_DAILY_RUN_CONFLICT);
        }
        return selectRequiredById(tenantId, runId);
    }

    private DccRegistrationCertificateDailyRunRecord selectRequiredByTenantAndDate(
            Long tenantId, LocalDate businessDate) {
        Optional<DccRegistrationCertificateDailyRunRecord> record = selectByTenantAndDate(tenantId, businessDate);
        if (record.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_DAILY_RUN_CONFLICT);
        }
        return record.get();
    }

    private Optional<DccRegistrationCertificateDailyRunRecord> selectByTenantAndDate(
            Long tenantId, LocalDate businessDate) {
        List<DccRegistrationCertificateDailyRunRecord> records = jdbcTemplate.query("""
                SELECT id, tenant_id, business_date, run_key, status, retry_count, failure_reason,
                       started_at, finished_at, detail_json
                  FROM dcc_registration_certificate_daily_run
                 WHERE tenant_id = ?
                   AND business_date = ?
                 ORDER BY id
                """, (rs, rowNum) -> mapRecord(rs), tenantId, businessDate);
        if (records.isEmpty()) {
            return Optional.empty();
        }
        if (records.size() != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_DAILY_RUN_CONFLICT);
        }
        return Optional.of(records.get(0));
    }

    private DccRegistrationCertificateDailyRunRecord selectRequiredById(Long tenantId, Long runId) {
        List<DccRegistrationCertificateDailyRunRecord> records = jdbcTemplate.query("""
                SELECT id, tenant_id, business_date, run_key, status, retry_count, failure_reason,
                       started_at, finished_at, detail_json
                  FROM dcc_registration_certificate_daily_run
                 WHERE id = ?
                   AND tenant_id = ?
                """, (rs, rowNum) -> mapRecord(rs), runId, tenantId);
        if (records.size() != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_DAILY_RUN_CONFLICT);
        }
        return records.get(0);
    }

    private static DccRegistrationCertificateDailyRunRecord mapRecord(java.sql.ResultSet rs)
            throws java.sql.SQLException {
        java.sql.Timestamp finishedAt = rs.getTimestamp("finished_at");
        return new DccRegistrationCertificateDailyRunRecord(
                rs.getLong("id"),
                rs.getLong("tenant_id"),
                rs.getDate("business_date").toLocalDate(),
                rs.getString("run_key"),
                rs.getString("status"),
                rs.getInt("retry_count"),
                rs.getString("failure_reason"),
                rs.getTimestamp("started_at").toLocalDateTime(),
                finishedAt == null ? null : finishedAt.toLocalDateTime(),
                rs.getString("detail_json"));
    }

    private static String runKey(Long tenantId, LocalDate businessDate) {
        return "registration-certificate-reminder:%d:%s".formatted(tenantId, businessDate);
    }

    private static void requireTenantAndDate(Long tenantId, LocalDate businessDate) {
        if (tenantId == null || tenantId <= 0 || businessDate == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_DAILY_RUN_CONFLICT);
        }
    }

    private static void requireTenantAndRun(Long tenantId, Long runId) {
        if (tenantId == null || tenantId <= 0 || runId == null || runId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_DAILY_RUN_CONFLICT);
        }
    }

    private static String validDetailJson(String detailJson) {
        if (detailJson == null || detailJson.trim().isEmpty()) {
            return "{}";
        }
        String detail = detailJson.trim();
        if (!(detail.startsWith("{") && detail.endsWith("}"))) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_DAILY_RUN_FAILED);
        }
        return detail;
    }
}
