package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder.DccRegistrationCertificateReminderEvaluation;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder.DccRegistrationCertificateReminderRunResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder.DccRegistrationCertificateReminderService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({
        DccRegistrationCertificateReminderService.class,
        DccRegistrationCertificateReminderServiceTest.DbTestConfiguration.class
})
class DccRegistrationCertificateReminderServiceTest extends BaseDbUnitTest {

    @Resource
    private DccRegistrationCertificateReminderService service;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Test
    void thresholdMatrixUsesCalendarBoundariesAndExpiredRemainsBright() {
        List<DccRegistrationCertificateReminderEvaluation> cases = List.of(
                service.evaluateThreshold(LocalDate.of(2026, 1, 29), LocalDate.of(2026, 2, 28), false),
                service.evaluateThreshold(LocalDate.of(2028, 1, 30), LocalDate.of(2028, 2, 29), false),
                service.evaluateThreshold(LocalDate.of(2026, 3, 23), LocalDate.of(2026, 3, 31), false),
                service.evaluateThreshold(LocalDate.of(2026, 3, 29), LocalDate.of(2026, 3, 31), false),
                service.evaluateThreshold(LocalDate.of(2026, 3, 30), LocalDate.of(2026, 3, 31), false),
                service.evaluateThreshold(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 3, 31), false),
                service.evaluateThreshold(LocalDate.of(2026, 3, 30), LocalDate.of(2026, 3, 31), true));

        assertEquals(List.of("T_30", "T_30", "T_8", "T_2", "T_1", "T_1", "CLEARED"),
                cases.stream().map(DccRegistrationCertificateReminderEvaluation::thresholdLevel).toList());
        assertEquals(List.of("YELLOW", "YELLOW", "ORANGE", "RED", "BRIGHT_RED", "BRIGHT_RED", "CLEARED"),
                cases.stream().map(DccRegistrationCertificateReminderEvaluation::colorCode).toList());
        assertEquals(-1, cases.get(5).daysUntilDue());
    }

    @Test
    void catchUpSuppressesMissedLowerThresholdsAndLaterEscalatesOnce() {
        insertActiveCertificate(1L, 101L, 201L, null, 301L, LocalDate.of(2026, 3, 31));

        DccRegistrationCertificateReminderRunResult first = service.generateOccurrences(
                1L, 9001L, LocalDate.of(2026, 3, 23));
        assertEquals(1, first.pendingCount());
        assertEquals(1, first.suppressedCount());
        assertOccurrence(101L, "T_8", "PENDING_DELIVERY", null);
        assertOccurrence(101L, "T_30", "SUPPRESSED", "MISSED_BY_CATCH_UP");

        DccRegistrationCertificateReminderRunResult replay = service.generateOccurrences(
                1L, 9001L, LocalDate.of(2026, 3, 23));
        assertEquals(0, replay.pendingCount());
        assertEquals(0, replay.suppressedCount());
        assertEquals(2, countOccurrences(101L));

        DccRegistrationCertificateReminderRunResult later = service.generateOccurrences(
                1L, 9002L, LocalDate.of(2026, 3, 29));
        assertEquals(1, later.pendingCount());
        assertEquals(0, later.suppressedCount());
        assertOccurrence(101L, "T_2", "PENDING_DELIVERY", null);
        assertEquals(3, countOccurrences(101L));
    }

    @Test
    void renewalCandidateSuppressesExpiryWithoutDelivery() {
        insertActiveCertificate(1L, 102L, 202L, 203L, 302L, LocalDate.of(2026, 4, 30));

        DccRegistrationCertificateReminderRunResult result = service.generateOccurrences(
                1L, 9003L, LocalDate.of(2026, 4, 22));

        assertEquals(0, result.pendingCount());
        assertEquals(2, result.suppressedCount());
        assertOccurrence(102L, "T_8", "SUPPRESSED", "RENEWAL_CANDIDATE_EXISTS");
        assertOccurrence(102L, "T_30", "SUPPRESSED", "MISSED_BY_CATCH_UP");
    }

    @Test
    void confirmedSupportingDocumentClearsSupportingDocumentReminder() {
        insertActiveCertificate(1L, 103L, 203L, null, 303L, LocalDate.of(2026, 5, 31));
        insertSupportingDocument(1L, 7001L, 103L, 203L, "PENDING_CONFIRMATION");
        assertFalse(service.isSupportingDocumentCleared(1L, 103L, "RENEWAL_ACCEPTANCE_RECEIPT"));

        jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_supporting_document
                   SET status = 'CONFIRMED', open_unique_flag = NULL, confirmed_at = CURRENT_TIMESTAMP, confirmed_by = 9
                 WHERE id = 7001
                """);

        assertTrue(service.isSupportingDocumentCleared(1L, 103L, "RENEWAL_ACCEPTANCE_RECEIPT"));
    }

    private void insertActiveCertificate(Long tenantId, Long certificateId, Long currentVersionId,
                                         Long pendingVersionId, Long snapshotId, LocalDate expiryDate) {
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate
                  (id, tenant_id, owner_company_id, product_master_id, first_obtained_date,
                   current_version_id, pending_version_id, current_snapshot_id, status, row_version)
                VALUES (?, ?, 501, 601, DATE '2023-01-01', ?, ?, ?, 'ACTIVE', 1)
                """, certificateId, tenantId, currentVersionId, pendingVersionId, snapshotId);
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_version
                  (id, tenant_id, certificate_id, version_no, version_type, certificate_no,
                   approval_date, effective_date, expiry_date, classification, category_changed,
                   status, current_unique_flag, formalized_at, formalized_by)
                VALUES (?, ?, ?, 1, 'INITIAL_CERTIFICATE', ?, DATE '2023-01-01',
                        DATE '2023-01-02', ?, 'CLASS-III', 0, 'CURRENT', 1, CURRENT_TIMESTAMP, 9)
                """, currentVersionId, tenantId, certificateId, "CERT-" + certificateId, expiryDate);
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_snapshot
                  (id, tenant_id, version_id, revision_no, product_name, registrant_name,
                   model_specification, structure_composition, intended_use, technical_requirements,
                   residence_address, production_address, entrusted_production, self_production,
                   entrusted_enterprises_json, effective_at)
                VALUES (?, ?, ?, 1, '产品', '注册人', '型号', '结构', '用途', '要求',
                        '住所', '生产地址', 0, 1, '[]', CURRENT_TIMESTAMP)
                """, snapshotId, tenantId, currentVersionId);
    }

    private void insertSupportingDocument(Long tenantId, Long id, Long certificateId,
                                          Long versionId, String status) {
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_supporting_document
                  (id, tenant_id, owner_company_id, certificate_id, version_id, business_file_id,
                   document_type, status, open_unique_flag, uploaded_at, uploaded_by)
                VALUES (?, ?, 501, ?, ?, 8001, 'RENEWAL_ACCEPTANCE_RECEIPT', ?, 1, CURRENT_TIMESTAMP, 9)
                """, id, tenantId, certificateId, versionId, status);
    }

    private Integer countOccurrences(Long certificateId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM dcc_registration_certificate_reminder_occurrence
                 WHERE certificate_id = ?
                """, Integer.class, certificateId);
    }

    private void assertOccurrence(Long certificateId, String level, String status, String reason) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT status, suppress_reason
                  FROM dcc_registration_certificate_reminder_occurrence
                 WHERE certificate_id = ? AND threshold_level = ?
                """, certificateId, level);
        assertEquals(status, row.get("STATUS"));
        assertEquals(reason, row.get("SUPPRESS_REASON"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DbTestConfiguration {

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }
}
