package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.history.DccRegistrationCertificateHistoryItem;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.history.DccRegistrationCertificateHistoryService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.history.DccRegistrationCertificateHistoryServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Import({
        DccRegistrationCertificateHistoryServiceImpl.class,
        DccRegistrationCertificateHistoryServiceTest.DbTestConfiguration.class
})
class DccRegistrationCertificateHistoryServiceTest extends BaseDbUnitTest {

    @Resource
    private DccRegistrationCertificateHistoryService service;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Test
    void listHistoryReturnsOrderedLifecycleAndChangeItems() {
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_lifecycle_event
                  (id, tenant_id, owner_company_id, certificate_id, source_version_id, target_version_id,
                   source_snapshot_id, target_snapshot_id, event_key, event_type, event_sequence,
                   actor_id, detail_json, occurred_at, creator)
                VALUES (7001, 1, 10, 1001, 2001, 2001, 3001, 3002, 'change-1',
                        'CHANGE_APPLIED', 1, 99, '{}', ?, '99')
                """, LocalDateTime.of(2026, 8, 17, 9, 0));
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_change
                  (id, tenant_id, owner_company_id, certificate_id, source_version_id, source_snapshot_id,
                   resulting_snapshot_id, event_id, approval_date, selected_change_types_json,
                   status, actor_id, applied_at)
                VALUES (5001, 1, 10, 1001, 2001, 3001, 3002, 7001, DATE '2026-08-17',
                        '[\"PRODUCT_NAME\"]', 'APPLIED', 99, ?)
                """, LocalDateTime.of(2026, 8, 17, 9, 0));
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_change_item
                  (tenant_id, change_id, item_type, before_value_json, after_value_json, sort_order)
                VALUES (1, 5001, 'PRODUCT_NAME', '{\"value\":\"Product A\"}', '{\"value\":\"Product B\"}', 1)
                """);
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_lifecycle_event
                  (id, tenant_id, owner_company_id, certificate_id, source_version_id, target_version_id,
                   source_snapshot_id, target_snapshot_id, event_key, event_type, event_sequence,
                   actor_id, detail_json, occurred_at, creator)
                VALUES (7002, 1, 10, 1001, 2001, 2001, 3002, 3002, 'void-1',
                        'CERTIFICATE_VOIDED', 2, 100, '{\"voidReason\":\"证书已依法作废\"}', ?, '100')
                """, LocalDateTime.of(2026, 8, 18, 9, 0));

        List<DccRegistrationCertificateHistoryItem> items = assertDoesNotThrow(() -> service.listHistory(1L, 1001L));

        assertEquals(2, items.size());
        assertEquals("CHANGE_APPLIED", items.get(0).eventType());
        assertEquals("PRODUCT_NAME", items.get(0).itemType());
        assertEquals("{\"value\":\"Product A\"}", items.get(0).beforeValueJson());
        assertEquals("{\"value\":\"Product B\"}", items.get(0).afterValueJson());
        assertEquals(99L, items.get(0).actorId());
        assertEquals("CERTIFICATE_VOIDED", items.get(1).eventType());
        assertEquals("CERTIFICATE_VOIDED", items.get(1).itemType());
        assertEquals("{\"value\":\"ACTIVE\"}", items.get(1).beforeValueJson());
        assertEquals("{\"voidReason\":\"证书已依法作废\"}", items.get(1).afterValueJson());
        assertEquals(100L, items.get(1).actorId());
    }

    @Test
    void listHistoryReturnsFormalRenewalParametersAndUploadedFile() {
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_version
                  (id, tenant_id, certificate_id, version_no, version_type, certificate_no,
                   approval_date, effective_date, expiry_date, classification, category_changed,
                   base_snapshot_id, status, formalized_at, formalized_by)
                VALUES (2002, 1, 1001, 2, 'RENEWAL_CERTIFICATE', 'CERT-RENEWED-002',
                        DATE '2026-08-20', DATE '2026-09-01', DATE '2029-08-31', '三类', 1,
                        3001, 'CURRENT', ?, 99)
                """, LocalDateTime.of(2026, 8, 20, 10, 0));
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_file
                  (id, tenant_id, owner_type, owner_id, file_kind, infra_file_id, original_name,
                   mime_type, file_size, sha256, status, bound_file_unique_flag, bound_at, bound_by)
                VALUES (9001, 1, 'VERSION', 2002, 'REGISTRATION_CERTIFICATE', 9101,
                        '延续注册证-2026.pdf', 'application/pdf', 1024,
                        'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                        'BOUND', 9001, ?, 99)
                """, LocalDateTime.of(2026, 8, 20, 10, 0));
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_lifecycle_event
                  (id, tenant_id, owner_company_id, certificate_id, source_version_id, target_version_id,
                   source_snapshot_id, target_snapshot_id, event_key, event_type, event_sequence,
                   actor_id, detail_json, occurred_at, creator)
                VALUES (7003, 1, 10, 1001, 2001, 2002, 3001, 3002, 'renewal-1',
                        'RENEWAL_UPLOADED', 1, 99, '{"payloadHash":"hash"}', ?, '99')
                """, LocalDateTime.of(2026, 8, 20, 10, 0));

        List<DccRegistrationCertificateHistoryItem> items = service.listHistory(1L, 1001L);

        assertEquals(1, items.size());
        DccRegistrationCertificateHistoryItem renewal = items.get(0);
        assertEquals("RENEWAL_UPLOADED", renewal.eventType());
        assertEquals(2002L, renewal.targetVersionId());
        assertEquals(2, renewal.versionNo());
        assertEquals("2026-08-20", renewal.approvalDate().toString());
        assertEquals("2026-09-01", renewal.effectiveDate().toString());
        assertEquals("2029-08-31", renewal.expiryDate().toString());
        assertEquals(true, renewal.categoryChanged());
        assertEquals("CERT-RENEWED-002", renewal.certificateNo());
        assertEquals("三类", renewal.classification());
        assertEquals(9001L, renewal.businessFileId());
        assertEquals("REGISTRATION_CERTIFICATE", renewal.fileKind());
        assertEquals("延续注册证-2026.pdf", renewal.originalFileName());
        assertEquals("BOUND", renewal.fileStatus());
        assertEquals(LocalDateTime.of(2026, 8, 20, 10, 0), renewal.occurredAt());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DbTestConfiguration {

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }
}
