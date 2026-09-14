package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateOperationAuditService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.history.DccRegistrationCertificateHistoryItem;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.history.DccRegistrationCertificateHistoryService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.history.DccRegistrationCertificateHistoryServiceImpl;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@Import({
        DccRegistrationCertificateHistoryServiceImpl.class,
        DccRegistrationCertificateOperationAuditService.class,
        DccRegistrationCertificateHistoryServiceTest.DbTestConfiguration.class
})
class DccRegistrationCertificateHistoryServiceTest extends BaseDbUnitTest {

    @Resource
    private DccRegistrationCertificateHistoryService service;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @MockitoBean
    private AdminUserApi adminUserApi;

    @BeforeEach
    void setUp() {
        reset(adminUserApi);
        when(adminUserApi.getUserList(any())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            return ids.stream().map(DccRegistrationCertificateHistoryServiceTest::user).toList();
        });
    }

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
    void listHistoryExposesChangeStatusAndApprovalAuditFields() throws Exception {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 8, 17, 9, 0);
        LocalDateTime reviewedAt = LocalDateTime.of(2026, 8, 18, 10, 0);
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_lifecycle_event
                  (id, tenant_id, owner_company_id, certificate_id, source_version_id, target_version_id,
                   source_snapshot_id, target_snapshot_id, event_key, event_type, event_sequence,
                   actor_id, detail_json, occurred_at, creator)
                VALUES (7101, 1, 10, 1001, 2001, 2001, 3001, 3002, 'change-audit-1',
                        'CHANGE_SUBMITTED', 1, 88, '{}', ?, '88')
                """, submittedAt);
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_change
                  (id, tenant_id, owner_company_id, certificate_id, source_version_id, source_snapshot_id,
                   resulting_snapshot_id, event_id, approval_request_id, approval_date,
                   selected_change_types_json, status, actor_id, reviewer_user_id, reviewed_at, applied_at)
                VALUES (5101, 1, 10, 1001, 2001, 3001, 3002, 7101, 8101, DATE '2026-08-17',
                        '["PRODUCT_NAME"]', 'APPLIED', 88, 99, ?, ?)
                """, reviewedAt, reviewedAt);

        List<DccRegistrationCertificateHistoryItem> items = service.listHistory(1L, 1001L);

        DccRegistrationCertificateHistoryItem item = items.get(0);
        assertEquals(7101L, component(item, "eventId"));
        assertEquals(5101L, component(item, "changeId"));
        assertEquals(8101L, component(item, "approvalRequestId"));
        assertEquals("APPLIED", component(item, "changeStatus"));
        assertEquals(88L, component(item, "submittedBy"));
        assertEquals(submittedAt, component(item, "submittedAt"));
        assertEquals(99L, component(item, "reviewedBy"));
        assertEquals(reviewedAt, component(item, "reviewedAt"));
        assertEquals("用户-88", component(item, "submittedByName"));
        assertEquals("用户-99", component(item, "reviewedByName"));
        assertEquals("2026-08-17", component(item, "approvalDate").toString());
    }

    @Test
    void listHistoryReturnsEachRenewalOperatorAndApproverWithoutCrossVersionMixing() {
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
                INSERT INTO dcc_registration_certificate_access_request
                  (id, tenant_id, owner_company_id, certificate_id, requester_user_id,
                   request_type, request_key, purpose, status, requested_at, completed_at,
                   detail_json, creator, deleted)
                VALUES (8001, 1, 10, 1001, 88, 'UPLOAD_CERTIFICATE', 'renewal-audit-8001',
                        '注册证延续审批', 'APPROVED', ?, ?, '{}', '88', 0)
                """, LocalDateTime.of(2026, 8, 20, 9, 0), LocalDateTime.of(2026, 8, 20, 10, 0));
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_access_request_file
                  (id, tenant_id, request_id, business_file_id, file_kind,
                   download_requested, status, detail_json, creator, deleted)
                VALUES (8101, 1, 8001, 9001, 'REGISTRATION_CERTIFICATE',
                        FALSE, 'APPROVED', '{}', '88', 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_access_request
                  (id, tenant_id, owner_company_id, certificate_id, requester_user_id,
                   request_type, request_key, purpose, project_code_id, status, requested_at, completed_at,
                   detail_json, creator, deleted)
                VALUES (8201, 1, 10, 1001, 77, 'DOWNLOAD_FILE', 'download-audit-8201',
                        '下载延续注册证', 20, 'APPROVED', ?, ?, '{}', '77', 0)
                """, LocalDateTime.of(2026, 8, 21, 9, 0), LocalDateTime.of(2026, 8, 21, 10, 0));
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_access_request_file
                  (id, tenant_id, request_id, business_file_id, file_kind,
                   download_requested, status, detail_json, creator, deleted)
                VALUES (8301, 1, 8201, 9001, 'REGISTRATION_CERTIFICATE',
                        TRUE, 'APPROVED', '{}', '77', 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_lifecycle_event
                  (id, tenant_id, owner_company_id, certificate_id, source_version_id, target_version_id,
                   source_snapshot_id, target_snapshot_id, event_key, event_type, event_sequence,
                   actor_id, detail_json, occurred_at, creator)
                VALUES (7003, 1, 10, 1001, 2001, 2002, 3001, 3002, 'renewal-1',
                        'RENEWAL_UPLOADED', 1, 99, '{"payloadHash":"hash"}', ?, '99')
                """, LocalDateTime.of(2026, 8, 20, 10, 0));

        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_version
                  (id, tenant_id, certificate_id, version_no, version_type, certificate_no,
                   approval_date, effective_date, expiry_date, classification, category_changed,
                   base_snapshot_id, status, formalized_at, formalized_by)
                VALUES (2003, 1, 1001, 3, 'RENEWAL_CERTIFICATE', 'CERT-RENEWED-003',
                        DATE '2027-08-20', DATE '2027-09-01', DATE '2030-08-31', '三类', 0,
                        3002, 'CURRENT', ?, 101)
                """, LocalDateTime.of(2027, 8, 20, 12, 0));
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_file
                  (id, tenant_id, owner_type, owner_id, file_kind, infra_file_id, original_name,
                   mime_type, file_size, sha256, status, bound_file_unique_flag, bound_at, bound_by)
                VALUES (9002, 1, 'VERSION', 2003, 'REGISTRATION_CERTIFICATE', 9102,
                        '延续注册证-2027.pdf', 'application/pdf', 2048,
                        'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
                        'BOUND', 9002, ?, 101)
                """, LocalDateTime.of(2027, 8, 20, 12, 0));
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_access_request
                  (id, tenant_id, owner_company_id, certificate_id, requester_user_id,
                   request_type, request_key, purpose, status, requested_at, completed_at,
                   detail_json, creator, deleted)
                VALUES (8002, 1, 10, 1001, 89, 'UPLOAD_CERTIFICATE', 'renewal-audit-8002',
                        '注册证延续审批', 'APPROVED', ?, ?, '{}', '89', 0)
                """, LocalDateTime.of(2027, 8, 20, 11, 0), LocalDateTime.of(2027, 8, 20, 12, 0));
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_access_request_file
                  (id, tenant_id, request_id, business_file_id, file_kind,
                   download_requested, status, detail_json, creator, deleted)
                VALUES (8102, 1, 8002, 9002, 'REGISTRATION_CERTIFICATE',
                        FALSE, 'APPROVED', '{}', '89', 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_lifecycle_event
                  (id, tenant_id, owner_company_id, certificate_id, source_version_id, target_version_id,
                   source_snapshot_id, target_snapshot_id, event_key, event_type, event_sequence,
                   actor_id, detail_json, occurred_at, creator)
                VALUES (7004, 1, 10, 1001, 2002, 2003, 3002, 3003, 'renewal-2',
                        'RENEWAL_UPLOADED', 2, 101, '{"payloadHash":"hash-2"}', ?, '101')
                """, LocalDateTime.of(2027, 8, 20, 12, 0));

        List<DccRegistrationCertificateHistoryItem> items = service.listHistory(1L, 1001L);

        assertEquals(2, items.size());
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
        assertEquals("用户-88", renewal.renewalOperatorName());
        assertEquals(LocalDateTime.of(2026, 8, 20, 9, 0), renewal.renewalOperatedAt());
        assertEquals("用户-99", renewal.renewalApproverName());
        assertEquals(LocalDateTime.of(2026, 8, 20, 10, 0), renewal.renewalApprovedAt());

        DccRegistrationCertificateHistoryItem secondRenewal = items.get(1);
        assertEquals(2003L, secondRenewal.targetVersionId());
        assertEquals("用户-89", secondRenewal.renewalOperatorName());
        assertEquals(LocalDateTime.of(2027, 8, 20, 11, 0), secondRenewal.renewalOperatedAt());
        assertEquals("用户-101", secondRenewal.renewalApproverName());
        assertEquals(LocalDateTime.of(2027, 8, 20, 12, 0), secondRenewal.renewalApprovedAt());
    }

    private static AdminUserRespDTO user(Long id) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setNickname("用户-" + id);
        return user;
    }

    private static Object component(DccRegistrationCertificateHistoryItem item, String name) throws Exception {
        return Objects.requireNonNull(item.getClass().getMethod(name).invoke(item));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DbTestConfiguration {

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }
}
