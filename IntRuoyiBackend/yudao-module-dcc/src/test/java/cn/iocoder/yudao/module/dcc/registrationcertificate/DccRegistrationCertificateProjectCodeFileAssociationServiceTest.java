package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.association.DccRegistrationCertificateProjectCodeFileAssociationService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import({
        DccRegistrationCertificateProjectCodeFileAssociationService.class,
        DccFileTypeTaxonomyAdminServiceImpl.class,
        DccRegistrationCertificateProjectCodeFileAssociationServiceTest.DbTestConfiguration.class
})
class DccRegistrationCertificateProjectCodeFileAssociationServiceTest extends BaseDbUnitTest {

    @Resource
    private DccRegistrationCertificateProjectCodeFileAssociationService service;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanRegistrationCertificateTaxonomyPath() {
        jdbcTemplate.update("""
                DELETE FROM dcc_file_type_taxonomy
                 WHERE code IN ('REGISTRATION_CERTIFICATE', 'REGISTRATION_DOSSIER', 'TECHNICAL_DOCUMENT')
                """);
    }

    @Test
    void bindVersionRegistrationFileCreatesRegistrationCertificateTaxonomyAndFileProjectClassification() {
        seedProjectCode(501L, "IDI");
        seedCertificate(1001L, 501L, 2001L, 3001L);
        seedVersion(2001L, 1001L, 1, "INITIAL_CERTIFICATE", "CURRENT", "CERT-001", "II");
        seedSnapshot(3001L, 2001L, 1, "Product A");
        seedBoundFile(4001L, "VERSION", 2001L, "REGISTRATION_CERTIFICATE",
                "initial-certificate.pdf", 9001L);

        service.bindVersionRegistrationFile(1L, 2001L, 4001L, 99L);

        assertEquals(501L, longValue("SELECT dcc_project_code_id FROM dcc_registration_certificate_file WHERE id = 4001"));
        assertNotNull(longValue("SELECT file_type_taxonomy_id FROM dcc_registration_certificate_file WHERE id = 4001"));
        assertEquals("技术文档", text("SELECT file_type_level1 FROM dcc_registration_certificate_file WHERE id = 4001"));
        assertEquals("注册资料汇编", text("SELECT file_type_level2 FROM dcc_registration_certificate_file WHERE id = 4001"));
        assertEquals("注册证", text("SELECT file_type_level3 FROM dcc_registration_certificate_file WHERE id = 4001"));
        assertEquals(1, countRegistrationCertificateTaxonomyRows());
    }

    @Test
    void bindChangeAndRenewalFilesReuseExistingRegistrationCertificateTaxonomy() {
        seedProjectCode(502L, "IDU");
        Long taxonomyId = seedRegistrationCertificateTaxonomyPath();
        seedCertificate(1002L, 502L, 2002L, 3002L);
        seedVersion(2002L, 1002L, 1, "INITIAL_CERTIFICATE", "CURRENT", "CERT-002", "III");
        seedSnapshot(3002L, 2002L, 1, "Product B");
        seedVersion(2003L, 1002L, 2, "RENEWAL_CERTIFICATE", "PENDING_EFFECTIVE", "CERT-002-R", "III");
        seedSnapshot(3003L, 2003L, 1, "Product B");
        seedChange(6001L, 1002L, 2002L, 3002L, 3003L);
        seedBoundFile(4002L, "VERSION", 2003L, "REGISTRATION_CERTIFICATE",
                "renewal-certificate.pdf", 9002L);
        seedBoundFile(4003L, "CHANGE", 6001L, "CHANGE_APPROVAL",
                "change-approval.pdf", 9003L);

        service.bindVersionRegistrationFile(1L, 2003L, 4002L, 99L);
        service.bindChangeApprovalFile(1L, 502L, 6001L, 4003L, 99L);

        assertEquals(taxonomyId, longValue("SELECT file_type_taxonomy_id FROM dcc_registration_certificate_file WHERE id = 4002"));
        assertEquals(taxonomyId, longValue("SELECT file_type_taxonomy_id FROM dcc_registration_certificate_file WHERE id = 4003"));
        assertEquals(502L, longValue("SELECT dcc_project_code_id FROM dcc_registration_certificate_file WHERE id = 4002"));
        assertEquals(502L, longValue("SELECT dcc_project_code_id FROM dcc_registration_certificate_file WHERE id = 4003"));
        assertEquals(1, countRegistrationCertificateTaxonomyRows());
    }

    @Test
    void listAssociatedRowsReturnsRegistrationSourceRowsAndCountsProjectFiles() {
        seedProjectCode(503L, "IDV");
        seedProjectCode(504L, "IDW");
        seedCertificate(1003L, 503L, 2004L, 3004L);
        seedVersion(2004L, 1003L, 1, "INITIAL_CERTIFICATE", "CURRENT", "CERT-003", "II");
        seedSnapshot(3004L, 2004L, 1, "Product C");
        seedBoundFile(4004L, "VERSION", 2004L, "REGISTRATION_CERTIFICATE",
                "certificate-c.pdf", 9004L);
        seedCertificate(1004L, 504L, 2005L, 3005L);
        seedVersion(2005L, 1004L, 1, "INITIAL_CERTIFICATE", "CURRENT", "CERT-004", "I");
        seedSnapshot(3005L, 2005L, 1, "Product D");
        seedBoundFile(4005L, "VERSION", 2005L, "REGISTRATION_CERTIFICATE",
                "certificate-d.pdf", 9005L);
        service.bindVersionRegistrationFile(1L, 2004L, 4004L, 99L);
        service.bindVersionRegistrationFile(1L, 2005L, 4005L, 99L);

        List<DccControlledFileRespVO> rows = service.listAssociatedRows(503L, null, null);
        assertEquals(1, rows.size());
        DccControlledFileRespVO row = rows.get(0);
        assertEquals("DCC_REGISTRATION_CERTIFICATE", row.getBusinessSourceType());
        assertEquals(1003L, row.getRegistrationCertificateId());
        assertEquals(2004L, row.getRegistrationCertificateVersionId());
        assertEquals(4004L, row.getRegistrationCertificateBusinessFileId());
        assertEquals("certificate-c.pdf", row.getFileName());
        assertEquals("CERT-003", row.getFileNumber());
        assertEquals("注册资料汇编", row.getFileTypeLevel2());
        assertEquals("注册证", row.getFileTypeLevel3());

        Map<Long, Long> counts = service.countAssociatedFilesByProjectCodeIds(Set.of(503L, 504L, 999L));
        assertEquals(1L, counts.get(503L));
        assertEquals(1L, counts.get(504L));
        assertNull(counts.get(999L));
    }

    private void seedProjectCode(Long id, String projectCode) {
        jdbcTemplate.update("""
                INSERT INTO dcc_project_code
                  (id, product_master_id, doc_control_no, project_name, project_code, status, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, 3000L + id, "DOC-" + id, "项目" + projectCode, projectCode, "ENABLE", 1L);
    }

    private void seedCertificate(Long id, Long projectCodeId, Long currentVersionId, Long currentSnapshotId) {
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate
                  (id, owner_company_id, product_master_id, project_code_id, first_obtained_date,
                   current_version_id, current_snapshot_id, status, row_version, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, 10L, 3000L + id, projectCodeId, LocalDate.of(2021, 3, 1),
                currentVersionId, currentSnapshotId, "ACTIVE", 1, 1L);
    }

    private void seedVersion(Long id, Long certificateId, int versionNo, String versionType,
                             String status, String certificateNo, String classification) {
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_version
                  (id, certificate_id, version_no, version_type, certificate_no, approval_date,
                   effective_date, expiry_date, classification, category_changed, status, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, certificateId, versionNo, versionType, certificateNo,
                LocalDate.of(2021, 3, 1), LocalDate.of(2021, 3, 1),
                LocalDate.of(2028, 3, 1), classification, false, status, 1L);
    }

    private void seedSnapshot(Long id, Long versionId, int revisionNo, String productName) {
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_snapshot
                  (id, version_id, revision_no, product_name, registrant_name, entrusted_production,
                   self_production, entrusted_enterprises_json, effective_at, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, versionId, revisionNo, productName, "Registrant", false, true, "[]",
                LocalDateTime.of(2021, 3, 1, 9, 0), 1L);
    }

    private void seedBoundFile(Long id, String ownerType, Long ownerId, String fileKind,
                               String originalName, Long infraFileId) {
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_file
                  (id, owner_type, owner_id, file_kind, infra_file_id, original_name, mime_type,
                   file_size, sha256, status, bound_at, bound_by, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, ownerType, ownerId, fileKind, infraFileId, originalName,
                "application/pdf", 128L, String.format("%064d", id), "BOUND",
                LocalDateTime.of(2026, 9, 1, 10, 0), 99L, 1L);
    }

    private void seedChange(Long id, Long certificateId, Long sourceVersionId,
                            Long sourceSnapshotId, Long resultingSnapshotId) {
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_change
                  (id, tenant_id, owner_company_id, certificate_id, source_version_id, source_snapshot_id,
                   resulting_snapshot_id, event_id, approval_date, selected_change_types_json,
                   selected_item_count, status, row_version, actor_id, applied_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, 1L, 10L, certificateId, sourceVersionId, sourceSnapshotId, resultingSnapshotId,
                7000L + id, LocalDate.of(2026, 9, 1), "[\"PRODUCT_NAME\"]",
                1, "APPLIED", 1, 99L, LocalDateTime.of(2026, 9, 1, 10, 5));
    }

    private Long seedRegistrationCertificateTaxonomyPath() {
        jdbcTemplate.update("""
                INSERT INTO dcc_file_type_taxonomy
                  (id, parent_id, level_no, code, name, active, sort)
                VALUES
                  (9101, 0, 1, 'TECHNICAL_DOCUMENT', '技术文档', true, 1),
                  (9102, 9101, 2, 'REGISTRATION_DOSSIER', '注册资料汇编', true, 10),
                  (9103, 9102, 3, 'REGISTRATION_CERTIFICATE', '注册证', true, 1)
                """);
        return 9103L;
    }

    private int countRegistrationCertificateTaxonomyRows() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM dcc_file_type_taxonomy t
                  JOIN dcc_file_type_taxonomy s ON s.id = t.parent_id
                  JOIN dcc_file_type_taxonomy r ON r.id = s.parent_id
                 WHERE r.name = '技术文档'
                   AND s.name = '注册资料汇编'
                   AND t.name = '注册证'
                   AND t.active = true
                """, Integer.class);
    }

    private Long longValue(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    private String text(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, String.class, args);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DbTestConfiguration {

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }
}
