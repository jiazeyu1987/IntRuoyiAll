package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.supportingdocument.DccRegistrationCertificateSupportingDocumentCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.supportingdocument.DccRegistrationCertificateSupportingDocumentResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.supportingdocument.DccRegistrationCertificateSupportingDocumentService;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_NOT_STAGED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@Import({
        DccRegistrationCertificateSupportingDocumentService.class,
        DccRegistrationCertificateSupportingDocumentServiceTest.DbTestConfiguration.class
})
class DccRegistrationCertificateSupportingDocumentServiceTest extends BaseDbUnitTest {

    @jakarta.annotation.Resource
    private DccRegistrationCertificateSupportingDocumentService service;
    @jakarta.annotation.Resource
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private MdmCompanyScopeApi companyScopeApi;

    @Test
    void uploadTakesEffectImmediatelyAndClearsEightMonthLightReminder() {
        Fixture fixture = seedCurrentCertificate();

        DccRegistrationCertificateSupportingDocumentResult result = assertDoesNotThrow(
                () -> service.upload(uploadCommand(fixture)));

        assertNotNull(result.supportingDocumentId());
        assertEquals("EFFECTIVE", result.status());
        assertFalse(result.lightRequired());
        assertEquals("EFFECTIVE", text("SELECT status FROM dcc_registration_certificate_supporting_document WHERE id = ?", result.supportingDocumentId()));
        assertEquals("SUPPORTING_DOCUMENT", text("SELECT owner_type FROM dcc_registration_certificate_file WHERE id = ?", fixture.businessFileId()));
        assertEquals(result.supportingDocumentId(), longValue("SELECT owner_id FROM dcc_registration_certificate_file WHERE id = ?", fixture.businessFileId()));
        assertEquals("BOUND", text("SELECT status FROM dcc_registration_certificate_file WHERE id = ?", fixture.businessFileId()));
        assertEquals(null, objectValue("SELECT open_unique_flag FROM dcc_registration_certificate_supporting_document WHERE id = ?", result.supportingDocumentId()));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_lifecycle_event WHERE tenant_id = 1 AND certificate_id = ? AND event_type = 'SUPPORTING_DOCUMENT_EFFECTIVE'", fixture.certificateId()));
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_lifecycle_event WHERE tenant_id = 1 AND certificate_id = ? AND event_type IN ('SUPPORTING_DOCUMENT_CONFIRMED', 'SUPPORTING_DOCUMENT_REJECTED')", fixture.certificateId()));
        verify(companyScopeApi).validateUserCompanyAccess(99L, 10L);
    }

    @Test
    void uploadReplayReturnsTheSameEffectiveDocumentWithoutSecondLifecycleEvent() {
        Fixture fixture = seedCurrentCertificate();
        DccRegistrationCertificateSupportingDocumentResult first = assertDoesNotThrow(
                () -> service.upload(uploadCommand(fixture)));
        DccRegistrationCertificateSupportingDocumentResult replay = assertDoesNotThrow(
                () -> service.upload(uploadCommand(fixture)));

        assertEquals(first.supportingDocumentId(), replay.supportingDocumentId());
        assertEquals("EFFECTIVE", replay.status());
        assertFalse(replay.lightRequired());
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_supporting_document WHERE tenant_id = 1 AND certificate_id = ?", fixture.certificateId()));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_lifecycle_event WHERE tenant_id = 1 AND certificate_id = ? AND event_type = 'SUPPORTING_DOCUMENT_EFFECTIVE'", fixture.certificateId()));
    }

    @Test
    void replayWithTheSameKeyButDifferentBusinessFileIsRejected() {
        Fixture fixture = seedCurrentCertificate();
        assertDoesNotThrow(() -> service.upload(uploadCommand(fixture)));

        ServiceException error = assertServiceException(() -> service.upload(
                new DccRegistrationCertificateSupportingDocumentCommand(
                        1L, 99L, "upload-1", "trace-upload-1", fixture.certificateId(), fixture.versionId(),
                        9002L, null, null, "RENEWAL_ACCEPTANCE_RECEIPT", null)));

        assertEquals(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT.getCode(), error.getCode());
    }

    @Test
    void crossCompanyUploadFailureDoesNotCreateAnEffectiveDocument() {
        Fixture fixture = seedCurrentCertificate();
        ServiceException denied = new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
        doThrow(denied).when(companyScopeApi).validateUserCompanyAccess(99L, 10L);

        ServiceException error = assertServiceException(
                () -> service.upload(uploadCommand(fixture)));

        assertEquals(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED.getCode(), error.getCode());
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_supporting_document WHERE tenant_id = 1 AND certificate_id = ?", fixture.certificateId()));
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_lifecycle_event WHERE tenant_id = 1 AND certificate_id = ?", fixture.certificateId()));
    }

    @Test
    void uploadRejectsAFileThatIsNotStagedForTheCurrentVersion() {
        Fixture fixture = seedCurrentCertificate();
        jdbcTemplate.update("UPDATE dcc_registration_certificate_file SET status = 'BOUND' WHERE id = ?",
                fixture.businessFileId());

        ServiceException error = assertServiceException(() -> service.upload(new DccRegistrationCertificateSupportingDocumentCommand(
                1L, 99L, "upload-invalid-file", "trace-upload-invalid-file", fixture.certificateId(),
                fixture.versionId(), fixture.businessFileId(), null, null,
                "RENEWAL_ACCEPTANCE_RECEIPT", null)));

        assertEquals(REGISTRATION_CERTIFICATE_FILE_NOT_STAGED.getCode(), error.getCode());
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_supporting_document WHERE tenant_id = 1 AND certificate_id = ?", fixture.certificateId()));
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_lifecycle_event WHERE tenant_id = 1 AND certificate_id = ?", fixture.certificateId()));
    }

    private Fixture seedCurrentCertificate() {
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate
                  (id, tenant_id, owner_company_id, product_master_id, first_obtained_date,
                   current_version_id, pending_version_id, current_snapshot_id, status, row_version)
                VALUES (1001, 1, 10, 20, ?, 2001, NULL, 3001, 'ACTIVE', 3)
                """, LocalDate.of(2021, 1, 1));
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_version
                  (id, tenant_id, certificate_id, version_no, version_type, certificate_no,
                   approval_date, effective_date, expiry_date, classification, category_changed,
                   base_snapshot_id, status, formalized_at, formalized_by)
                VALUES (2001, 1, 1001, 1, 'RENEWAL_CERTIFICATE', 'CERT-002',
                        ?, ?, ?, 'II', FALSE, 3001, 'CURRENT', ?, 99)
                """, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10),
                LocalDate.of(2031, 8, 10), LocalDateTime.of(2026, 8, 10, 9, 0));
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_snapshot
                  (id, tenant_id, version_id, revision_no, product_name, registrant_name,
                   model_specification, structure_composition, intended_use, technical_requirements,
                   residence_address, production_address, entrusted_production, self_production,
                   entrusted_enterprises_json, effective_at)
                VALUES (3001, 1, 2001, 1, 'Product A', 'Registrant', 'Model', 'Structure',
                        'Use', 'Requirements', 'Residence', 'Production', FALSE, TRUE, '[]', ?)
                """, LocalDateTime.of(2026, 8, 10, 9, 0));
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_file
                  (id, owner_type, owner_id, file_kind, infra_file_id, original_name, mime_type,
                   file_size, sha256, status, bound_at, bound_by, tenant_id)
                VALUES (9001, 'VERSION', 2001, 'RENEWAL_ACCEPTANCE_RECEIPT', 9101,
                        'renewal-acceptance.pdf', 'application/pdf', 128,
                        'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                        'STAGED', NULL, NULL, 1)
                """);
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_file
                  (id, owner_type, owner_id, file_kind, infra_file_id, original_name, mime_type,
                   file_size, sha256, status, bound_at, bound_by, tenant_id)
                VALUES (9002, 'VERSION', 2001, 'RENEWAL_ACCEPTANCE_RECEIPT', 9102,
                        'renewal-acceptance-2.pdf', 'application/pdf', 128,
                        'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
                        'STAGED', NULL, NULL, 1)
                """);
        return new Fixture(1001L, 2001L, 9001L);
    }

    private DccRegistrationCertificateSupportingDocumentCommand uploadCommand(Fixture fixture) {
        return new DccRegistrationCertificateSupportingDocumentCommand(
                1L, 99L, "upload-1", "trace-upload-1", fixture.certificateId(), fixture.versionId(),
                fixture.businessFileId(), null, null, "RENEWAL_ACCEPTANCE_RECEIPT", null);
    }

    private ServiceException assertServiceException(Runnable runnable) {
        try {
            runnable.run();
        } catch (ServiceException exception) {
            return exception;
        } catch (RuntimeException exception) {
            fail("expected stable ServiceException, got " + exception);
        }
        fail("expected stable ServiceException");
        return null;
    }

    private String text(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, String.class, args);
    }

    private Long longValue(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    private Object objectValue(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Object.class, args);
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private record Fixture(Long certificateId, Long versionId, Long businessFileId) {
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
