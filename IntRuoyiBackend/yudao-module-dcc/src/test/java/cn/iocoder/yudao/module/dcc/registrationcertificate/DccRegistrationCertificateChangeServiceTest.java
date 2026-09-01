package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.association.DccRegistrationCertificateProjectCodeFileAssociationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.change.DccRegistrationCertificateChangeCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.change.DccRegistrationCertificateChangeResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.change.DccRegistrationCertificateChangeService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotifier;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_CHANGE_PRODUCTION_RELATION_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_CHANGE_VALUE_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REVISION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_TOP_LEVEL_VOID_REASON_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({
        DccRegistrationCertificateChangeService.class,
        DccRegistrationCertificateChangeServiceTest.DbTestConfiguration.class
})
class DccRegistrationCertificateChangeServiceTest extends BaseDbUnitTest {

    @Resource
    private DccRegistrationCertificateChangeService service;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @MockitoBean
    private FileService fileService;
    @MockitoBean
    private DccRegistrationCertificateBusinessEventNotifier businessEventNotifier;
    @MockitoBean
    private DccRegistrationCertificateProjectCodeFileAssociationService projectCodeFileAssociationService;

    @BeforeEach
    void setUpFileService() {
        when(fileService.createFileAndReturnId(any(byte[].class), anyString(), anyString(), anyString()))
                .thenReturn(9002L);
    }

    @Test
    void structuredChangeUpdatesOnlySelectedFieldsAndKeepsHistoryImmutable() {
        seedCurrentCertificate();

        DccRegistrationCertificateChangeResult result = assertDoesNotThrow(() -> service.applyChange(command(
                "change-product", 3, Map.of("PRODUCT_NAME", "Product B"), null, null, null, null)));

        assertEquals("APPLIED", result.status());
        assertEquals("Product A", text("SELECT product_name FROM dcc_registration_certificate_snapshot WHERE id = 3001"));
        assertEquals("Product B", text("SELECT product_name FROM dcc_registration_certificate_snapshot WHERE id = ?", result.resultingSnapshotId()));
        assertEquals("Registrant", text("SELECT registrant_name FROM dcc_registration_certificate_snapshot WHERE id = ?", result.resultingSnapshotId()));
        assertEquals(result.resultingSnapshotId(), longValue("SELECT current_snapshot_id FROM dcc_registration_certificate WHERE id = 1001"));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_change_item WHERE change_id = ? AND item_type = 'PRODUCT_NAME'", result.changeId()));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_lifecycle_event WHERE event_type = 'CHANGE_APPLIED' AND certificate_id = 1001"));
        verify(businessEventNotifier).notifyChangeApprovalRecorded(
                eq(1L), eq(10L), eq(1001L), eq(2001L), eq(99L),
                eq("change-product"), eq("Product B"), eq("CERT-001"),
                eq(LocalDate.of(2021, 3, 1)), eq(LocalDate.of(2026, 8, 20)));
    }

    @Test
    void submittedChangeWaitsForApprovalBeforeUpdatingOnlyMvpDisplayFields() {
        seedCurrentCertificate();

        Long requestId = assertDoesNotThrow(() -> service.submitChangeForApproval(command(
                "change-mvp-pending", 3, Map.of(
                        "PRODUCT_NAME", "Product B",
                        "REGISTRANT_NAME", "Registrant B"),
                null, null, null, null)));

        Long changeId = longValue("SELECT id FROM dcc_registration_certificate_change WHERE approval_request_id = ?", requestId);
        assertEquals("PENDING_APPROVAL", text("SELECT status FROM dcc_registration_certificate_change WHERE id = ?", changeId));
        assertEquals(3001L, longValue("SELECT current_snapshot_id FROM dcc_registration_certificate WHERE id = 1001"));

        jdbcTemplate.update("UPDATE dcc_registration_certificate_access_request SET status = 'APPROVED' WHERE id = ?", requestId);
        assertDoesNotThrow(() -> service.approveChangeRequest(1L, 55L, requestId, "change-mvp-approval"));

        Long approvedSnapshotId = longValue("SELECT current_snapshot_id FROM dcc_registration_certificate WHERE id = 1001");
        assertEquals("APPLIED", text("SELECT status FROM dcc_registration_certificate_change WHERE id = ?", changeId));
        assertEquals(55L, longValue("SELECT reviewer_user_id FROM dcc_registration_certificate_change WHERE id = ?", changeId));
        assertEquals("Product B", text("SELECT product_name FROM dcc_registration_certificate_snapshot WHERE id = ?", approvedSnapshotId));
        assertEquals("Registrant B", text("SELECT registrant_name FROM dcc_registration_certificate_snapshot WHERE id = ?", approvedSnapshotId));
    }

    @Test
    void changeApprovalFileIsUploadedAndBoundToTheAppliedChange() {
        seedCurrentCertificate();

        DccRegistrationCertificateChangeCommand command = new DccRegistrationCertificateChangeCommand(
                1L, 99L, "change-with-file", "trace-change-with-file", 1001L, 3,
                LocalDate.of(2026, 8, 17), Map.of("PRODUCT_NAME", "Product B"), null,
                null, null, null, null, changeFile("change-with-file"));

        DccRegistrationCertificateChangeResult result = assertDoesNotThrow(() -> service.applyChange(command));

        Long businessFileId = longValue("SELECT id FROM dcc_registration_certificate_file "
                + "WHERE owner_type = 'CHANGE' AND owner_id = ? AND file_kind = 'CHANGE_APPROVAL'",
                result.changeId());
        assertEquals(9002L, longValue("SELECT infra_file_id FROM dcc_registration_certificate_file WHERE id = ?",
                businessFileId));
        assertEquals("BOUND", text("SELECT status FROM dcc_registration_certificate_file WHERE id = ?",
                businessFileId));
        verify(projectCodeFileAssociationService).bindChangeApprovalFile(
                1L, null, result.changeId(), businessFileId, 99L);
    }

    @Test
    void multiStructuredChangeUpdatesEverySelectedFieldAndProductionRelation() {
        seedCurrentCertificate();

        DccRegistrationCertificateChangeResult result = assertDoesNotThrow(() -> service.applyChange(command(
                "change-all-structured", 3, Map.of(
                        "PRODUCT_NAME", "Product B",
                        "REGISTRANT_NAME", "Registrant B",
                        "MODEL_SPECIFICATION", "Model B",
                        "STRUCTURE_COMPOSITION", "Structure B",
                        "INTENDED_USE", "Use B",
                        "TECHNICAL_REQUIREMENTS", "Requirements B",
                        "RESIDENCE_ADDRESS", "Residence B",
                        "PRODUCTION_ADDRESS", "Production B"),
                null, true, "[{\"enterpriseId\":30,\"enterpriseName\":\"Entrusted B\"}]", null)));

        Long snapshotId = result.resultingSnapshotId();
        assertEquals("Product B", text("SELECT product_name FROM dcc_registration_certificate_snapshot WHERE id = ?", snapshotId));
        assertEquals("Registrant B", text("SELECT registrant_name FROM dcc_registration_certificate_snapshot WHERE id = ?", snapshotId));
        assertEquals("Model B", text("SELECT model_specification FROM dcc_registration_certificate_snapshot WHERE id = ?", snapshotId));
        assertEquals("Structure B", text("SELECT structure_composition FROM dcc_registration_certificate_snapshot WHERE id = ?", snapshotId));
        assertEquals("Use B", text("SELECT intended_use FROM dcc_registration_certificate_snapshot WHERE id = ?", snapshotId));
        assertEquals("Requirements B", text("SELECT technical_requirements FROM dcc_registration_certificate_snapshot WHERE id = ?", snapshotId));
        assertEquals("Residence B", text("SELECT residence_address FROM dcc_registration_certificate_snapshot WHERE id = ?", snapshotId));
        assertEquals("Production B", text("SELECT production_address FROM dcc_registration_certificate_snapshot WHERE id = ?", snapshotId));
        assertEquals(Boolean.TRUE, jdbcTemplate.queryForObject(
                "SELECT entrusted_production FROM dcc_registration_certificate_snapshot WHERE id = ?", Boolean.class, snapshotId));
        assertEquals(Boolean.FALSE, jdbcTemplate.queryForObject(
                "SELECT self_production FROM dcc_registration_certificate_snapshot WHERE id = ?", Boolean.class, snapshotId));
        assertEquals("[{\"enterpriseId\":30,\"enterpriseName\":\"Entrusted B\"}]",
                text("SELECT entrusted_enterprises_json FROM dcc_registration_certificate_snapshot WHERE id = ?", snapshotId));
        assertEquals(8, count("SELECT COUNT(*) FROM dcc_registration_certificate_change_item WHERE change_id = ?", result.changeId()));
        assertEquals("Product A", text("SELECT product_name FROM dcc_registration_certificate_snapshot WHERE id = 3001"));
    }

    @Test
    void invalidSelectionFailsAtomically() {
        seedCurrentCertificate();

        assertCode(REGISTRATION_CERTIFICATE_CHANGE_VALUE_REQUIRED.getCode(), () -> service.applyChange(command(
                "missing-product", 3, Map.of("PRODUCT_NAME", " "), null, null, null, null)));
        assertCode(REGISTRATION_CERTIFICATE_CHANGE_PRODUCTION_RELATION_REQUIRED.getCode(), () -> service.applyChange(command(
                "production-no-relation", 3, Map.of("PRODUCTION_ADDRESS", "New Production"), null, null, null, null)));

        assertEquals(3001L, longValue("SELECT current_snapshot_id FROM dcc_registration_certificate WHERE id = 1001"));
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_change"));
    }

    @Test
    void otherContentCanBeSubmittedWithStructuredChangesInTheSameApproval() {
        seedCurrentCertificate();

        DccRegistrationCertificateChangeResult result = assertDoesNotThrow(() -> service.applyChange(command(
                "other-and-structured", 3, Map.of("PRODUCT_NAME", "Product B"),
                "补充变更说明", null, null, null)));

        assertEquals("Product B", text("SELECT product_name FROM dcc_registration_certificate_snapshot WHERE id = ?",
                result.resultingSnapshotId()));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_change_item "
                + "WHERE change_id = ? AND item_type = 'PRODUCT_NAME'", result.changeId()));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_change_item "
                + "WHERE change_id = ? AND item_type = 'OTHER_CONTENT'", result.changeId()));
    }

    @Test
    void otherContentRecordsHistoryWithoutChangingProjection() {
        seedCurrentCertificate();

        DccRegistrationCertificateChangeResult result = assertDoesNotThrow(() -> service.applyChange(command(
                "other-only", 3, Map.of(), "包装标签说明更新", null, null, null)));
        DccRegistrationCertificateChangeResult replay = assertDoesNotThrow(() -> service.applyChange(command(
                "other-only", 3, Map.of(), "包装标签说明更新", null, null, null)));

        assertEquals(3001L, longValue("SELECT current_snapshot_id FROM dcc_registration_certificate WHERE id = 1001"));
        assertEquals(3001L, result.resultingSnapshotId());
        assertEquals(result.changeId(), replay.changeId());
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_change_item WHERE change_id = ? AND item_type = 'OTHER_CONTENT'", result.changeId()));
        ServiceException conflict = assertThrows(ServiceException.class, () -> service.applyChange(command(
                "other-only", 3, Map.of(), "另一条说明", null, null, null)));
        assertEquals(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT.getCode(), conflict.getCode());
    }

    @Test
    void staleRevisionAndTopLevelVoidAreFailClosed() {
        seedCurrentCertificate();

        assertCode(REGISTRATION_CERTIFICATE_REVISION_CONFLICT.getCode(), () -> service.applyChange(command(
                "stale", 2, Map.of("PRODUCT_NAME", "Product B"), null, null, null, null)));
        assertCode(REGISTRATION_CERTIFICATE_TOP_LEVEL_VOID_REASON_REQUIRED.getCode(), () -> service.voidCertificate(command(
                "void-no-reason", 3, Map.of(), null, null, null, " ")));

        DccRegistrationCertificateChangeResult result = assertDoesNotThrow(() -> service.voidCertificate(command(
                "void-ok", 3, Map.of(), null, null, null, "证书已依法作废")));

        assertEquals("VOIDED", result.status());
        assertEquals("VOIDED", text("SELECT status FROM dcc_registration_certificate WHERE id = 1001"));
        assertEquals("VOIDED", text("SELECT status FROM dcc_registration_certificate_version WHERE id = 2001"));
        assertEquals("VOIDED", text("SELECT status FROM dcc_registration_certificate_file WHERE id = 4001"));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_lifecycle_event WHERE event_type = 'CERTIFICATE_VOIDED' AND certificate_id = 1001"));
    }

    private DccRegistrationCertificateChangeCommand command(String key, Integer rowVersion,
                                                            Map<String, String> values, String other,
                                                            Boolean entrusted, String entrustedJson,
                                                            String voidReason) {
        return new DccRegistrationCertificateChangeCommand(1L, 99L, key, "trace-" + key, 1001L, rowVersion,
                LocalDate.of(2026, 8, 17), values, other, entrusted, entrusted == null ? null : !entrusted,
                entrustedJson, voidReason, changeFile(key));
    }

    private MockMultipartFile changeFile(String key) {
        return new MockMultipartFile("file", key + ".pdf", "application/pdf",
                ("change approval " + key).getBytes(StandardCharsets.UTF_8));
    }

    private void seedCurrentCertificate() {
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate
                  (id, tenant_id, owner_company_id, product_master_id, project_code_id, first_obtained_date,
                   current_version_id, pending_version_id, current_snapshot_id, status, row_version)
                VALUES (1001, 1, 10, 20, 40, ?, 2001, NULL, 3001, 'ACTIVE', 3)
                """, LocalDate.of(2021, 1, 1));
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_version
                  (id, tenant_id, certificate_id, version_no, version_type, certificate_no,
                   approval_date, effective_date, expiry_date, classification, category_changed,
                   base_snapshot_id, status, formalized_at, formalized_by)
                VALUES (2001, 1, 1001, 1, 'INITIAL_CERTIFICATE', 'CERT-001',
                        ?, ?, ?, 'II', FALSE, NULL, 'CURRENT', ?, 99)
                """, LocalDate.of(2021, 2, 1), LocalDate.of(2021, 3, 1),
                LocalDate.of(2026, 8, 20), LocalDateTime.of(2021, 3, 1, 9, 0));
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_snapshot
                  (id, tenant_id, version_id, revision_no, product_name, registrant_name,
                   model_specification, structure_composition, intended_use, technical_requirements,
                   residence_address, production_address, entrusted_production, self_production,
                   entrusted_enterprises_json, effective_at)
                VALUES (3001, 1, 2001, 1, 'Product A', 'Registrant', 'Model', 'Structure',
                        'Use', 'Requirements', 'Residence', 'Production', FALSE, TRUE, '[]', ?)
                """, LocalDateTime.of(2021, 3, 1, 9, 0));
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_file
                  (id, tenant_id, owner_type, owner_id, file_kind, infra_file_id, original_name, mime_type,
                   file_size, sha256, status, bound_file_unique_flag, bound_at, bound_by)
                VALUES (4001, 1, 'VERSION', 2001, 'REGISTRATION_CERTIFICATE', 9001, 'cert.pdf',
                        'application/pdf', 128, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                        'BOUND', 9001, ?, 99)
                """, LocalDateTime.of(2021, 3, 1, 9, 0));
    }

    private void assertCode(int code, Runnable runnable) {
        try {
            runnable.run();
        } catch (ServiceException exception) {
            assertEquals(code, exception.getCode());
            return;
        } catch (RuntimeException exception) {
            fail("expected ServiceException, got " + exception);
        }
        fail("expected ServiceException");
    }

    private String text(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, String.class, args);
    }

    private Long longValue(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DbTestConfiguration {

        @Bean
        DccRegistrationCertificateBusinessClock registrationCertificateBusinessClock() {
            return new DccRegistrationCertificateBusinessClock(
                    Clock.fixed(Instant.parse("2026-08-17T01:00:00Z"), ZoneId.of("Asia/Shanghai")));
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }
}
