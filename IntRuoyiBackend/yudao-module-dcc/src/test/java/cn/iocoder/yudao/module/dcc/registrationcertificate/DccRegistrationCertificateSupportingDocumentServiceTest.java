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
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_REJECT_REASON_REQUIRED;
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
    void uploadKeepsPendingConfirmationLightAndNeverAutoConfirms() {
        Fixture fixture = seedCurrentCertificate();

        DccRegistrationCertificateSupportingDocumentResult result = assertDoesNotThrow(
                () -> service.upload(uploadCommand(fixture)));

        assertNotNull(result.supportingDocumentId());
        assertEquals("PENDING_CONFIRMATION", result.status());
        assertEquals(true, result.lightRequired());
        assertEquals("PENDING_CONFIRMATION", text("SELECT status FROM dcc_registration_certificate_supporting_document WHERE id = ?", result.supportingDocumentId()));
        assertEquals(1L, longValue("SELECT open_unique_flag FROM dcc_registration_certificate_supporting_document WHERE id = ?", result.supportingDocumentId()));
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_supporting_document WHERE id = ? AND confirmed_at IS NOT NULL", result.supportingDocumentId()));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_lifecycle_event WHERE tenant_id = 1 AND certificate_id = ? AND event_type = 'SUPPORTING_DOCUMENT_UPLOADED'", fixture.certificateId()));
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_lifecycle_event WHERE tenant_id = 1 AND certificate_id = ? AND event_type = 'SUPPORTING_DOCUMENT_CONFIRMED'", fixture.certificateId()));
        verify(companyScopeApi).validateUserCompanyAccess(99L, 10L);
    }

    @Test
    void confirmBySameCompanyDocControlClearsLightAndWritesManualAudit() {
        Fixture fixture = seedCurrentCertificate();
        Long supportId = seedPendingSupportingDocument(fixture);

        DccRegistrationCertificateSupportingDocumentResult result = assertDoesNotThrow(
                () -> service.confirm(reviewCommand(fixture, supportId, "confirm-1", null)));

        assertEquals(supportId, result.supportingDocumentId());
        assertEquals("CONFIRMED", result.status());
        assertFalse(result.lightRequired());
        assertEquals(null, objectValue("SELECT open_unique_flag FROM dcc_registration_certificate_supporting_document WHERE id = ?", supportId));
        assertEquals(99L, longValue("SELECT confirmed_by FROM dcc_registration_certificate_supporting_document WHERE id = ?", supportId));
        assertEquals(2, count("SELECT row_version FROM dcc_registration_certificate_supporting_document WHERE id = ?", supportId));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_lifecycle_event WHERE tenant_id = 1 AND certificate_id = ? AND event_type = 'SUPPORTING_DOCUMENT_CONFIRMED'", fixture.certificateId()));
        verify(companyScopeApi).validateUserCompanyAccess(99L, 10L);
    }

    @Test
    void rejectRequiresReasonAndKeepsLightForManualFollowUp() {
        Fixture fixture = seedCurrentCertificate();
        Long supportId = seedPendingSupportingDocument(fixture);

        ServiceException missingReason = assertServiceException(
                () -> service.reject(reviewCommand(fixture, supportId, "reject-blank", "  ")));
        assertEquals(REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_REJECT_REASON_REQUIRED.getCode(), missingReason.getCode());
        assertEquals("PENDING_CONFIRMATION", text("SELECT status FROM dcc_registration_certificate_supporting_document WHERE id = ?", supportId));

        DccRegistrationCertificateSupportingDocumentResult result =
                assertDoesNotThrow(() -> service.reject(
                        reviewCommand(fixture, supportId, "reject-valid", "补充材料不清晰")));

        assertEquals("REJECTED", result.status());
        assertEquals(true, result.lightRequired());
        assertEquals(1L, longValue("SELECT open_unique_flag FROM dcc_registration_certificate_supporting_document WHERE id = ?", supportId));
        assertEquals("补充材料不清晰", text("SELECT reject_reason FROM dcc_registration_certificate_supporting_document WHERE id = ?", supportId));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_lifecycle_event WHERE tenant_id = 1 AND certificate_id = ? AND event_type = 'SUPPORTING_DOCUMENT_REJECTED'", fixture.certificateId()));
    }

    @Test
    void crossCompanyDocControlFailureDoesNotMutatePendingDocument() {
        Fixture fixture = seedCurrentCertificate();
        Long supportId = seedPendingSupportingDocument(fixture);
        ServiceException denied = new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
        doThrow(denied).when(companyScopeApi).validateUserCompanyAccess(99L, 10L);

        ServiceException error = assertServiceException(
                () -> service.confirm(reviewCommand(fixture, supportId, "confirm-denied", null)));

        assertEquals(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED.getCode(), error.getCode());
        assertEquals("PENDING_CONFIRMATION", text("SELECT status FROM dcc_registration_certificate_supporting_document WHERE id = ?", supportId));
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_lifecycle_event WHERE tenant_id = 1 AND event_key = 'confirm-denied'"));
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
        return new Fixture(1001L, 2001L, 9001L);
    }

    private Long seedPendingSupportingDocument(Fixture fixture) {
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_supporting_document
                  (id, tenant_id, owner_company_id, certificate_id, version_id, business_file_id,
                   document_type, status, open_unique_flag, row_version, uploaded_at, uploaded_by)
                VALUES (4001, 1, 10, ?, ?, ?, 'RENEWAL_SUPPLEMENT_NOTICE',
                        'PENDING_CONFIRMATION', 1, 1, ?, 88)
                """, fixture.certificateId(), fixture.versionId(), fixture.businessFileId(),
                LocalDateTime.of(2026, 8, 17, 9, 0));
        return 4001L;
    }

    private DccRegistrationCertificateSupportingDocumentCommand uploadCommand(Fixture fixture) {
        return new DccRegistrationCertificateSupportingDocumentCommand(
                1L, 99L, "upload-1", "trace-upload-1", fixture.certificateId(), fixture.versionId(),
                fixture.businessFileId(), null, null, "RENEWAL_ACCEPTANCE_RECEIPT", null);
    }

    private DccRegistrationCertificateSupportingDocumentCommand reviewCommand(Fixture fixture, Long supportId,
                                                                              String key, String reason) {
        return new DccRegistrationCertificateSupportingDocumentCommand(
                1L, 99L, key, "trace-" + key, fixture.certificateId(), fixture.versionId(),
                fixture.businessFileId(), supportId, 1, "RENEWAL_SUPPLEMENT_NOTICE", reason);
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
