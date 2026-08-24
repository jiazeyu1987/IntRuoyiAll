package cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.time.LocalDate;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_OCCURRENCE_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_THRESHOLD_INVALID;

@Service
public class DccRegistrationCertificateReminderService {

    private static final List<Threshold> THRESHOLDS = List.of(
            new Threshold("T_30", 30, "NORMAL", 1),
            new Threshold("T_8", 8, "LIGHT", 2),
            new Threshold("T_2", 2, "BRIGHT", 3),
            new Threshold("T_1", 1, "BRIGHT", 4));
    private static final String TYPE_CERTIFICATE_EXPIRY = "CERTIFICATE_EXPIRY";
    private static final String STATUS_PENDING = "PENDING_DELIVERY";
    private static final String STATUS_SUPPRESSED = "SUPPRESSED";
    private static final String REASON_MISSED = "MISSED_BY_CATCH_UP";
    private static final String REASON_RENEWAL_CANDIDATE = "RENEWAL_CANDIDATE_EXISTS";

    private final JdbcTemplate jdbcTemplate;

    public DccRegistrationCertificateReminderService(JdbcTemplate jdbcTemplate) {
        if (jdbcTemplate == null) {
            throw new IllegalArgumentException("jdbcTemplate must not be null");
        }
        this.jdbcTemplate = jdbcTemplate;
    }

    public DccRegistrationCertificateReminderEvaluation evaluateThreshold(
            LocalDate businessDate, LocalDate dueDate, boolean cleared) {
        if (businessDate == null || dueDate == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_THRESHOLD_INVALID);
        }
        int daysUntilDue = Math.toIntExact(ChronoUnit.DAYS.between(businessDate, dueDate));
        if (cleared) {
            return new DccRegistrationCertificateReminderEvaluation("CLEARED", "CLEARED", daysUntilDue);
        }
        Threshold threshold = selectThreshold(businessDate, dueDate);
        if (threshold == null) {
            return new DccRegistrationCertificateReminderEvaluation("NONE", "NORMAL", daysUntilDue);
        }
        return new DccRegistrationCertificateReminderEvaluation(threshold.level(), threshold.colorCode(),
                daysUntilDue);
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateReminderRunResult generateOccurrences(
            Long tenantId, Long runId, LocalDate businessDate) {
        if (tenantId == null || tenantId <= 0 || runId == null || runId <= 0 || businessDate == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_OCCURRENCE_CONFLICT);
        }
        int pending = 0;
        int suppressed = 0;
        for (CertificateExpiry expiry : selectCertificateExpiries(tenantId)) {
            int daysUntilDue = Math.toIntExact(ChronoUnit.DAYS.between(businessDate, expiry.dueDate()));
            List<Threshold> crossed = crossedThresholds(businessDate, expiry.dueDate());
            if (crossed.isEmpty()) {
                continue;
            }
            Threshold active = crossed.stream()
                    .max(Comparator.comparingInt(Threshold::urgency))
                    .orElseThrow();
            Long activeOccurrenceId;
            if (expiry.hasRenewalCandidate()) {
                InsertOutcome activeOutcome = insertOccurrence(tenantId, runId, expiry, active,
                        STATUS_SUPPRESSED, 0L, REASON_RENEWAL_CANDIDATE, businessDate);
                activeOccurrenceId = activeOutcome.occurrenceId();
                if (activeOutcome.inserted()) {
                    suppressed++;
                }
            } else {
                InsertOutcome activeOutcome = insertOccurrence(tenantId, runId, expiry, active,
                        STATUS_PENDING, null, null, businessDate);
                activeOccurrenceId = activeOutcome.occurrenceId();
                if (activeOutcome.inserted()) {
                    pending++;
                }
            }
            for (Threshold lower : crossed.stream()
                    .filter(threshold -> threshold.urgency() < active.urgency())
                    .toList()) {
                InsertOutcome lowerOutcome = insertOccurrence(tenantId, runId, expiry, lower,
                        STATUS_SUPPRESSED, activeOccurrenceId == null ? 0L : activeOccurrenceId,
                        REASON_MISSED, businessDate);
                if (lowerOutcome.inserted()) {
                    suppressed++;
                }
            }
        }
        return new DccRegistrationCertificateReminderRunResult(pending, suppressed);
    }

    public boolean isSupportingDocumentCleared(Long tenantId, Long certificateId, String documentType) {
        if (tenantId == null || tenantId <= 0 || certificateId == null || certificateId <= 0
                || documentType == null || documentType.trim().isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_THRESHOLD_INVALID);
        }
        Integer confirmed = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM dcc_registration_certificate_supporting_document
                 WHERE tenant_id = ?
                   AND certificate_id = ?
                   AND document_type = ?
                   AND status = 'EFFECTIVE'
                   AND deleted = 0
                """, Integer.class, tenantId, certificateId, documentType.trim());
        return confirmed != null && confirmed > 0;
    }

    private List<CertificateExpiry> selectCertificateExpiries(Long tenantId) {
        return jdbcTemplate.query("""
                SELECT c.id AS certificate_id,
                       c.owner_company_id,
                       c.current_version_id,
                       c.pending_version_id,
                       v.expiry_date
                  FROM dcc_registration_certificate c
                  JOIN dcc_registration_certificate_version v
                    ON v.id = c.current_version_id
                   AND v.tenant_id = c.tenant_id
                   AND v.certificate_id = c.id
                   AND v.status = 'CURRENT'
                   AND v.deleted = 0
                 WHERE c.tenant_id = ?
                   AND c.status = 'ACTIVE'
                   AND c.deleted = 0
                   AND v.expiry_date IS NOT NULL
                 ORDER BY c.id
                """, (rs, rowNum) -> new CertificateExpiry(
                rs.getLong("certificate_id"),
                rs.getLong("owner_company_id"),
                rs.getLong("current_version_id"),
                rs.getObject("pending_version_id", Long.class),
                rs.getObject("expiry_date", LocalDate.class)), tenantId);
    }

    private InsertOutcome insertOccurrence(Long tenantId, Long runId, CertificateExpiry expiry,
                                           Threshold threshold, String status, Long suppressedBy,
                                           String suppressReason, LocalDate businessDate) {
        ExistingOccurrence existing = findExistingOccurrence(tenantId, expiry.certificateId(),
                TYPE_CERTIFICATE_EXPIRY, threshold.level(), expiry.dueDate());
        if (existing != null) {
            return new InsertOutcome(existing.id(), false);
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            int affected = jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO dcc_registration_certificate_reminder_occurrence
                          (tenant_id, run_id, owner_company_id, certificate_id, version_id,
                           supporting_document_id, reminder_type, threshold_level, business_date,
                           due_date, event_key, status, suppressed_by_occurrence_id, suppress_reason,
                           detail_json, creator)
                        VALUES (?, ?, ?, ?, ?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, tenantId);
                ps.setLong(2, runId);
                ps.setLong(3, expiry.ownerCompanyId());
                ps.setLong(4, expiry.certificateId());
                ps.setLong(5, expiry.versionId());
                ps.setString(6, TYPE_CERTIFICATE_EXPIRY);
                ps.setString(7, threshold.level());
                ps.setObject(8, businessDate);
                ps.setObject(9, expiry.dueDate());
                ps.setString(10, eventKey(expiry.certificateId(), TYPE_CERTIFICATE_EXPIRY,
                        threshold.level(), expiry.dueDate()));
                ps.setString(11, status);
                if (suppressedBy == null) {
                    ps.setObject(12, null);
                } else {
                    ps.setLong(12, suppressedBy);
                }
                ps.setString(13, suppressReason);
                ps.setString(14, detailJson(threshold, status, suppressReason));
                ps.setString(15, "registration-certificate-reminder");
                return ps;
            }, keyHolder);
            if (affected != 1) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_OCCURRENCE_CONFLICT);
            }
        } catch (DuplicateKeyException duplicate) {
            ExistingOccurrence concurrent = findExistingOccurrence(tenantId, expiry.certificateId(),
                    TYPE_CERTIFICATE_EXPIRY, threshold.level(), expiry.dueDate());
            if (concurrent != null) {
                return new InsertOutcome(concurrent.id(), false);
            }
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_OCCURRENCE_CONFLICT);
        }
        Number key = keyHolder.getKeyList().isEmpty() ? null
                : (Number) keyHolder.getKeyList().get(0).get("id");
        if (key == null) {
            ExistingOccurrence inserted = findExistingOccurrence(tenantId, expiry.certificateId(),
                    TYPE_CERTIFICATE_EXPIRY, threshold.level(), expiry.dueDate());
            if (inserted != null) {
                return new InsertOutcome(inserted.id(), true);
            }
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_OCCURRENCE_CONFLICT);
        }
        return new InsertOutcome(key.longValue(), true);
    }

    private ExistingOccurrence findExistingOccurrence(Long tenantId, Long certificateId, String type,
                                                       String level, LocalDate dueDate) {
        List<ExistingOccurrence> rows = jdbcTemplate.query("""
                SELECT id, status
                  FROM dcc_registration_certificate_reminder_occurrence
                 WHERE tenant_id = ?
                   AND event_key = ?
                """, (rs, rowNum) -> new ExistingOccurrence(
                rs.getLong("id"),
                rs.getString("status")), tenantId, eventKey(certificateId, type, level, dueDate));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static String eventKey(Long certificateId, String type, String level, LocalDate dueDate) {
        return "registration-certificate-reminder:" + certificateId + ":" + type + ":" + level + ":" + dueDate;
    }

    private static String detailJson(Threshold threshold, String status, String reason) {
        return "{\"threshold\":\"" + threshold.level() + "\",\"status\":\"" + status
                + "\",\"reason\":\"" + Objects.toString(reason, "") + "\"}";
    }

    private static Threshold selectThreshold(LocalDate businessDate, LocalDate dueDate) {
        return crossedThresholds(businessDate, dueDate).stream()
                .max(Comparator.comparingInt(Threshold::urgency))
                .orElse(null);
    }

    private static List<Threshold> crossedThresholds(LocalDate businessDate, LocalDate dueDate) {
        List<Threshold> crossed = new ArrayList<>();
        for (Threshold threshold : THRESHOLDS) {
            if (!businessDate.isBefore(dueDate.minusMonths(threshold.months()))) {
                crossed.add(threshold);
            }
        }
        return crossed;
    }

    private record Threshold(String level, int months, String colorCode, int urgency) {
    }

    private record CertificateExpiry(Long certificateId, Long ownerCompanyId, Long versionId,
                                     Long pendingVersionId, LocalDate dueDate) {
        boolean hasRenewalCandidate() {
            return pendingVersionId != null && pendingVersionId > 0;
        }
    }

    private record ExistingOccurrence(Long id, String status) {
    }

    private record InsertOutcome(Long occurrenceId, boolean inserted) {
    }
}
