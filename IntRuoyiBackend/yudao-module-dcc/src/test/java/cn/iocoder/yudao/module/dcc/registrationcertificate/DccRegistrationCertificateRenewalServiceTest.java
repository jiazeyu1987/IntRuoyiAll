package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotEntrustedDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotEntrustedMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.activation.DccRegistrationCertificateActivationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotifier;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal.DccRegistrationCertificateRenewalCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal.DccRegistrationCertificateRenewalResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal.DccRegistrationCertificateRenewalService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal.DccRegistrationCertificateRenewalSubmitCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal.DccRegistrationCertificateRenewalSubmitResult;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mdm.api.enterprise.MdmEnterpriseApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.dto.MdmEnterpriseRespDTO;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseTypeEnum;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentProjectionSnapshot;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentRegistrationProjectionService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_RENEWAL_CATEGORY_CHANGE_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_APPROVAL_DATE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_DATE_ORDER_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_RENEWAL_FIELD_FORBIDDEN;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@Import({
        DccRegistrationCertificateRenewalService.class,
        DccRegistrationCertificateActivationService.class,
        DccRegistrationCertificateRenewalServiceTest.DbTestConfiguration.class
})
class DccRegistrationCertificateRenewalServiceTest extends BaseDbUnitTest {

    @Resource
    private DccRegistrationCertificateRenewalService service;
    @Resource
    private DccRegistrationCertificateMapper certificateMapper;
    @Resource
    private DccRegistrationCertificateVersionMapper versionMapper;
    @Resource
    private DccRegistrationCertificateSnapshotMapper snapshotMapper;
    @Resource
    private DccRegistrationCertificateSnapshotEntrustedMapper entrustedMapper;
    @Autowired
    private DccRegistrationCertificateFileMapper fileMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ControlledContentRegistrationProjectionService projectionService;
    @MockitoBean
    private FileService fileService;
    @MockitoBean
    private DccRegistrationCertificateBusinessEventNotifier businessEventNotifier;
    @MockitoBean
    private MdmEnterpriseApi enterpriseApi;

    @BeforeEach
    void stubOwnerCompany() {
        lenient().when(enterpriseApi.getEnabledEnterprises(
                        List.of(10L), Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())))
                .thenReturn(List.of(MdmEnterpriseRespDTO.builder()
                        .id(10L)
                        .tenantId(1L)
                        .type(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())
                        .name("Company A")
                        .build()));
    }

    @Test
    void sameCategoryRenewalCreatesPendingCandidateAndKeepsCurrentActive() {
        CurrentFixture current = seedCurrentCertificate();
        DccRegistrationCertificateRenewalCommand command = sameCategoryCommand(
                current.certificateId(), current.currentVersionId(), current.stagedFileId(), "renewal-1");

        DccRegistrationCertificateRenewalResult result = uploadOrFail(command);

        assertEquals(current.certificateId(), result.certificateId());
        assertEquals("ACTIVE", result.masterStatus());
        assertEquals("PENDING_EFFECTIVE", result.renewalVersionStatus());
        assertFalse(result.renewalUploadMissing());

        DccRegistrationCertificateDO certificate = certificateMapper.selectById(current.certificateId());
        assertEquals("ACTIVE", certificate.getStatus());
        assertEquals(current.currentVersionId(), certificate.getCurrentVersionId());
        assertEquals(result.renewalVersionId(), certificate.getPendingVersionId());
        assertEquals(current.currentSnapshotId(), certificate.getCurrentSnapshotId());
        assertEquals(2, certificate.getRowVersion());

        DccRegistrationCertificateVersionDO renewal = versionMapper.selectById(result.renewalVersionId());
        assertEquals("RENEWAL_CERTIFICATE", renewal.getVersionType());
        assertEquals("PENDING_EFFECTIVE", renewal.getStatus());
        assertEquals(2, renewal.getVersionNo());
        assertEquals(current.currentSnapshotId(), renewal.getBaseSnapshotId());
        assertEquals("CERT-001", renewal.getCertificateNo());
        assertEquals("II", renewal.getClassification());
        assertEquals(false, renewal.getCategoryChanged());
        assertEquals(LocalDate.of(2026, 8, 1), renewal.getApprovalDate());
        assertEquals(LocalDate.of(2026, 9, 1), renewal.getEffectiveDate());
        assertEquals(LocalDate.of(2031, 9, 1), renewal.getExpiryDate());

        DccRegistrationCertificateSnapshotDO snapshot = snapshotMapper.selectById(result.renewalSnapshotId());
        assertEquals(renewal.getId(), snapshot.getVersionId());
        assertEquals("Product A", snapshot.getProductName());
        assertEquals("Registrant", snapshot.getRegistrantName());
        assertEquals("Model", snapshot.getModelSpecification());
        assertEquals("Structure", snapshot.getStructureComposition());
        assertEquals("Use", snapshot.getIntendedUse());
        assertEquals("Requirements", snapshot.getTechnicalRequirements());
        assertEquals("Residence", snapshot.getResidenceAddress());
        assertEquals("Production", snapshot.getProductionAddress());
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_snapshot_entrusted "
                + "WHERE tenant_id = 1 AND snapshot_id = ?", snapshot.getId()));

        DccRegistrationCertificateFileDO file = fileMapper.selectById(current.stagedFileId());
        assertEquals(result.renewalVersionId(), file.getOwnerId());
        assertEquals("BOUND", file.getStatus());
        assertNotNull(file.getBoundAt());
        assertEquals(99L, file.getBoundBy());

        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_lifecycle_event "
                + "WHERE tenant_id = 1 AND certificate_id = ? AND event_type = 'RENEWAL_UPLOADED'",
                current.certificateId()));
        assertFalse(service.isRenewalUploadMissing(1L, current.certificateId()));

        verify(projectionService).registerReadyCandidate(any(), any(), any(),
                eq(current.certificateId()), eq(result.renewalVersionId()), eq("2"),
                eq("PENDING_EFFECTIVE"), eq(99L), anyString());
        verify(businessEventNotifier).notifyRenewalCandidateUploaded(
                eq(1L), eq(10L), eq(current.certificateId()), eq(result.renewalVersionId()),
                eq(99L), eq("renewal-1"), eq("CERT-001"));
    }

    @Test
    void categoryChangedRenewalCreatesPendingCandidateWithNewCertificateNoAndCategory() {
        CurrentFixture current = seedCurrentCertificate();

        DccRegistrationCertificateRenewalResult result = uploadOrFail(categoryChangedCommand(
                current.certificateId(), current.currentVersionId(), current.stagedFileId(), "renewal-category-1"));

        DccRegistrationCertificateVersionDO renewal = versionMapper.selectById(result.renewalVersionId());
        assertEquals("CERT-002", renewal.getCertificateNo());
        assertEquals("III", renewal.getClassification());
        assertEquals(true, renewal.getCategoryChanged());
        assertEquals(current.currentVersionId(),
                certificateMapper.selectById(current.certificateId()).getCurrentVersionId());
        assertEquals(result.renewalVersionId(),
                certificateMapper.selectById(current.certificateId()).getPendingVersionId());
        verify(businessEventNotifier).notifyRenewalCandidateUploaded(
                eq(1L), eq(10L), eq(current.certificateId()), eq(result.renewalVersionId()),
                eq(99L), eq("renewal-category-1"), eq("CERT-002"));
    }

    @Test
    void dueRenewalActivatesImmediatelyAndAllowsAnotherRenewal() {
        CurrentFixture current = seedCurrentCertificate();

        DccRegistrationCertificateRenewalResult firstRenewal = uploadOrFail(
                new DccRegistrationCertificateRenewalCommand(
                        1L, 99L, "renewal-due-now-1", "trace-renewal-due-now-1",
                        current.certificateId(), 1, current.currentVersionId(), current.stagedFileId(),
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 17),
                        LocalDate.of(2031, 8, 17), false, null, null));

        assertEquals("CURRENT", firstRenewal.renewalVersionStatus());
        DccRegistrationCertificateDO activated = certificateMapper.selectById(current.certificateId());
        assertEquals(firstRenewal.renewalVersionId(), activated.getCurrentVersionId());
        assertNull(activated.getPendingVersionId());
        assertEquals(3, activated.getRowVersion());
        assertEquals("OLD", versionMapper.selectById(current.currentVersionId()).getStatus());
        assertEquals("CURRENT", versionMapper.selectById(firstRenewal.renewalVersionId()).getStatus());

        Long nextFileId = seedStagedFile(firstRenewal.renewalVersionId(), 7002L);
        DccRegistrationCertificateRenewalResult nextRenewal = uploadOrFail(
                new DccRegistrationCertificateRenewalCommand(
                        1L, 99L, "renewal-after-due-2", "trace-renewal-after-due-2",
                        current.certificateId(), 3, firstRenewal.renewalVersionId(), nextFileId,
                        LocalDate.of(2026, 8, 17), LocalDate.of(2026, 9, 1),
                        LocalDate.of(2032, 9, 1), false, null, null));

        assertEquals("PENDING_EFFECTIVE", nextRenewal.renewalVersionStatus());
        assertEquals(firstRenewal.renewalVersionId(),
                certificateMapper.selectById(current.certificateId()).getCurrentVersionId());
        assertEquals(nextRenewal.renewalVersionId(),
                certificateMapper.selectById(current.certificateId()).getPendingVersionId());
    }

    @Test
    void categoryChangedRenewalRequiresBothCertificateNoAndClassification() {
        CurrentFixture current = seedCurrentCertificate();

        ServiceException missingCertificateNo = assertThrows(ServiceException.class,
                () -> service.uploadRenewalCandidate(new DccRegistrationCertificateRenewalCommand(
                        1L, 99L, "renewal-category-missing-no", "trace-renewal-category-missing-no",
                        current.certificateId(), 1, current.currentVersionId(), current.stagedFileId(),
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1),
                        LocalDate.of(2031, 9, 1), true, " ", "III")));
        assertEquals(REGISTRATION_CERTIFICATE_RENEWAL_CATEGORY_CHANGE_REQUIRED.getCode(),
                missingCertificateNo.getCode());

        ServiceException missingClassification = assertThrows(ServiceException.class,
                () -> service.uploadRenewalCandidate(new DccRegistrationCertificateRenewalCommand(
                        1L, 99L, "renewal-category-missing-class", "trace-renewal-category-missing-class",
                        current.certificateId(), 1, current.currentVersionId(), current.stagedFileId(),
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1),
                        LocalDate.of(2031, 9, 1), true, "CERT-002", " ")));
        assertEquals(REGISTRATION_CERTIFICATE_RENEWAL_CATEGORY_CHANGE_REQUIRED.getCode(),
                missingClassification.getCode());
    }

    @Test
    void categoryUnchangedRenewalRejectsCategoryFields() {
        CurrentFixture current = seedCurrentCertificate();

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.uploadRenewalCandidate(new DccRegistrationCertificateRenewalCommand(
                        1L, 99L, "renewal-category-forbidden", "trace-renewal-category-forbidden",
                        current.certificateId(), 1, current.currentVersionId(), current.stagedFileId(),
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1),
                        LocalDate.of(2031, 9, 1), false, "CERT-002", null)));

        assertEquals(REGISTRATION_CERTIFICATE_RENEWAL_FIELD_FORBIDDEN.getCode(), error.getCode());
    }

    @Test
    void sameCategoryRenewalAutomaticallyUsesTheOnlyStagedRegistrationFileWhenIdIsOmitted() {
        CurrentFixture current = seedCurrentCertificate();

        DccRegistrationCertificateRenewalResult result = uploadOrFail(sameCategoryCommand(
                current.certificateId(), current.currentVersionId(), null, "renewal-auto-file-1"));

        assertEquals(current.stagedFileId(), result.businessFileId());
        assertEquals(result.renewalVersionId(), fileMapper.selectById(current.stagedFileId()).getOwnerId());
        assertEquals("BOUND", fileMapper.selectById(current.stagedFileId()).getStatus());
    }

    @Test
    void submittedRenewalApprovalMakesRenewalUploadNotMissingBeforeManagerApproval() {
        CurrentFixture current = seedCurrentCertificate();
        when(fileService.createFileAndReturnId(any(byte[].class), eq("renewal.pdf"), anyString(), eq("application/pdf")))
                .thenReturn(9001L);

        DccRegistrationCertificateRenewalSubmitResult result = service.submitRenewalForApproval(
                new DccRegistrationCertificateRenewalSubmitCommand(
                        1L, 99L, "renewal-approval-pending-1", "trace-renewal-approval-pending-1",
                        current.certificateId(), 1, current.currentVersionId(),
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1),
                        LocalDate.of(2031, 9, 1), false, null, null, renewalApprovalFile()));

        assertEquals("SUBMITTED", result.requestStatus());
        assertFalse(service.isRenewalUploadMissing(1L, current.certificateId()));
        assertNull(certificateMapper.selectById(current.certificateId()).getPendingVersionId());
        String detailJson = jdbcTemplate.queryForObject("""
                SELECT detail_json
                  FROM dcc_registration_certificate_access_request
                 WHERE tenant_id = 1
                   AND request_key = 'renewal-approval-pending-1'
                """, String.class);
        Map<?, ?> detail = JsonUtils.parseObject(detailJson, Map.class);
        assertEquals("CERT-001", detail.get("certificateNo"));
        assertEquals("II", detail.get("classification"));
        assertEquals("Product A", detail.get("productName"));
        assertEquals("Company A", detail.get("ownerCompanyName"));
        assertEquals(1, count("""
                SELECT COUNT(*)
                  FROM dcc_registration_certificate_access_request r
                  JOIN dcc_registration_certificate_access_request_file rf
                    ON rf.tenant_id = r.tenant_id
                   AND rf.request_id = r.id
                   AND rf.file_kind = 'REGISTRATION_CERTIFICATE'
                   AND rf.status = 'REQUESTED'
                  JOIN dcc_registration_certificate_file f
                    ON f.tenant_id = r.tenant_id
                   AND f.id = rf.business_file_id
                   AND f.owner_type = 'VERSION'
                   AND f.owner_id = ?
                   AND f.file_kind = 'REGISTRATION_CERTIFICATE'
                   AND f.status = 'STAGED'
                 WHERE r.tenant_id = 1
                   AND r.certificate_id = ?
                   AND r.request_type = 'UPLOAD_CERTIFICATE'
                   AND r.status IN ('SUBMITTED', 'BPM_BOUND')
                   AND r.deleted = 0
                """, current.currentVersionId(), current.certificateId()));
    }

    @Test
    void concurrentRenewalSubmissionsCreateOnlyOneOpenApproval() throws Exception {
        CurrentFixture current = seedCurrentCertificate();
        CountDownLatch uploadsEntered = new CountDownLatch(2);
        AtomicLong infraFileId = new AtomicLong(9100L);
        when(fileService.createFileAndReturnId(any(byte[].class), eq("renewal.pdf"), anyString(),
                eq("application/pdf"))).thenAnswer(invocation -> {
            uploadsEntered.countDown();
            uploadsEntered.await(500, TimeUnit.MILLISECONDS);
            return infraFileId.incrementAndGet();
        });

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Object>> calls = List.of(
                    () -> submitOutcome(current, "renewal-submit-race-1"),
                    () -> submitOutcome(current, "renewal-submit-race-2"));
            List<Future<Object>> futures = pool.invokeAll(calls);
            List<Object> outcomes = new ArrayList<>();
            for (Future<Object> future : futures) {
                outcomes.add(future.get());
            }

            assertEquals(1, outcomes.stream()
                    .filter(DccRegistrationCertificateRenewalSubmitResult.class::isInstance).count());
            assertEquals(1, outcomes.stream()
                    .filter(ServiceException.class::isInstance)
                    .map(ServiceException.class::cast)
                    .filter(error -> error.getCode().equals(REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT.getCode()))
                    .count());
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_access_request "
                + "WHERE tenant_id = 1 AND certificate_id = ? AND status = 'SUBMITTED'",
                current.certificateId()));
    }

    @Test
    void submittedRenewalApprovalRejectsFutureApprovalDateAsApprovalDateInvalid() {
        CurrentFixture current = seedCurrentCertificate();

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.submitRenewalForApproval(new DccRegistrationCertificateRenewalSubmitCommand(
                        1L, 99L, "renewal-future-approval", "trace-renewal-future-approval",
                        current.certificateId(), 1, current.currentVersionId(),
                        LocalDate.of(2026, 8, 18), LocalDate.of(2026, 9, 1),
                        LocalDate.of(2031, 9, 1), false, null, null, renewalApprovalFile())));

        assertEquals(REGISTRATION_CERTIFICATE_APPROVAL_DATE_INVALID.getCode(), error.getCode());
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_access_request "
                + "WHERE tenant_id = 1 AND request_key = ?", "renewal-future-approval"));
    }

    @Test
    void submittedRenewalApprovalRejectsInvalidDateOrderAsDateOrderInvalid() {
        CurrentFixture current = seedCurrentCertificate();

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.submitRenewalForApproval(new DccRegistrationCertificateRenewalSubmitCommand(
                        1L, 99L, "renewal-date-order", "trace-renewal-date-order",
                        current.certificateId(), 1, current.currentVersionId(),
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 1), false, null, null, renewalApprovalFile())));

        assertEquals(REGISTRATION_CERTIFICATE_DATE_ORDER_INVALID.getCode(), error.getCode());
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_access_request "
                + "WHERE tenant_id = 1 AND request_key = ?", "renewal-date-order"));
    }

    @Test
    void submittedRenewalApprovalStillRejectsRealCurrentVersionMismatchAsBaseConflict() {
        CurrentFixture current = seedCurrentCertificate();

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.submitRenewalForApproval(new DccRegistrationCertificateRenewalSubmitCommand(
                        1L, 99L, "renewal-wrong-base", "trace-renewal-wrong-base",
                        current.certificateId(), 1, current.currentVersionId() + 1,
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1),
                        LocalDate.of(2031, 9, 1), false, null, null, renewalApprovalFile())));

        assertEquals(REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT.getCode(), error.getCode());
    }

    @Test
    void renewalCommandCarriesDatesFileAndCategoryChangeIdentity() {
        List<String> componentNames = java.util.Arrays.stream(
                        DccRegistrationCertificateRenewalCommand.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList();

        assertEquals(List.of("tenantId", "actorId", "idempotencyKey", "requestTraceId",
                "certificateId", "expectedRowVersion", "currentVersionId", "businessFileId",
                "approvalDate", "effectiveDate", "expiryDate",
                "categoryChanged", "certificateNo", "classification"), componentNames);
    }

    @Test
    void concurrentRenewalUploadsCannotCreateASecondPendingCandidate() throws Exception {
        CurrentFixture current = seedCurrentCertificate();
        Long secondFile = seedStagedFile(current.currentVersionId(), 7002L);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Object>> calls = List.of(
                    () -> outcome(() -> service.uploadRenewalCandidate(sameCategoryCommand(
                            current.certificateId(), current.currentVersionId(), current.stagedFileId(), "renewal-race-1"))),
                    () -> outcome(() -> service.uploadRenewalCandidate(sameCategoryCommand(
                            current.certificateId(), current.currentVersionId(), secondFile, "renewal-race-2"))));
            List<Future<Object>> futures = pool.invokeAll(calls);
            List<Object> outcomes = new ArrayList<>();
            for (Future<Object> future : futures) {
                outcomes.add(future.get());
            }
            long successes = outcomes.stream()
                    .filter(DccRegistrationCertificateRenewalResult.class::isInstance)
                    .count();
            long pendingConflicts = outcomes.stream()
                    .filter(ServiceException.class::isInstance)
                    .map(ServiceException.class::cast)
                    .filter(error -> error.getCode().equals(REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT.getCode()))
                    .count();
            assertEquals(1, successes, "exactly one concurrent renewal upload should win");
            assertEquals(1, pendingConflicts, "the loser must fail as pending conflict");
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_version "
                + "WHERE tenant_id = 1 AND certificate_id = ? AND status = 'PENDING_EFFECTIVE'",
                current.certificateId()));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_lifecycle_event "
                + "WHERE tenant_id = 1 AND certificate_id = ? AND event_type = 'RENEWAL_UPLOADED'",
                current.certificateId()));
    }

    @Test
    void voidingPendingCandidateRecomputesRenewalUploadMissingFromRemainingFacts() {
        CurrentFixture current = seedCurrentCertificate();
        DccRegistrationCertificateRenewalResult uploaded = uploadOrFail(sameCategoryCommand(
                current.certificateId(), current.currentVersionId(), current.stagedFileId(), "renewal-void-1"));
        assertFalse(service.isRenewalUploadMissing(1L, current.certificateId()));

        DccRegistrationCertificateRenewalResult voided = voidOrFail(
                1L, 99L, "renewal-void-2", "trace-void-2",
                current.certificateId(), 2, uploaded.renewalVersionId(), "obsolete renewal file");

        assertEquals(current.certificateId(), voided.certificateId());
        assertEquals("ACTIVE", voided.masterStatus());
        assertTrue(voided.renewalUploadMissing());
        DccRegistrationCertificateDO certificate = certificateMapper.selectById(current.certificateId());
        assertEquals(current.currentVersionId(), certificate.getCurrentVersionId());
        assertNull(certificate.getPendingVersionId());
        assertEquals(3, certificate.getRowVersion());
        assertEquals("VOIDED", versionMapper.selectById(uploaded.renewalVersionId()).getStatus());
        assertEquals("VOIDED", fileMapper.selectById(uploaded.businessFileId()).getStatus());
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_lifecycle_event "
                + "WHERE tenant_id = 1 AND certificate_id = ? AND event_type = 'CANDIDATE_VOIDED'",
                current.certificateId()));
        assertTrue(service.isRenewalUploadMissing(1L, current.certificateId()));
    }

    private DccRegistrationCertificateRenewalResult uploadOrFail(DccRegistrationCertificateRenewalCommand command) {
        try {
            return service.uploadRenewalCandidate(command);
        } catch (RuntimeException exception) {
            fail("renewal upload should create a formal pending candidate, but failed with " + exception);
            throw exception;
        }
    }

    private DccRegistrationCertificateRenewalResult voidOrFail(
            Long tenantId, Long actorId, String idempotencyKey, String requestTraceId,
            Long certificateId, Integer expectedRowVersion, Long pendingVersionId, String reason) {
        try {
            return service.voidPendingCandidate(tenantId, actorId, idempotencyKey, requestTraceId,
                    certificateId, expectedRowVersion, pendingVersionId, reason);
        } catch (RuntimeException exception) {
            fail("renewal candidate void should recompute pending state, but failed with " + exception);
            throw exception;
        }
    }

    private static Object outcome(Callable<DccRegistrationCertificateRenewalResult> call) throws Exception {
        try {
            return call.call();
        } catch (ServiceException exception) {
            return exception;
        } catch (RuntimeException exception) {
            return exception;
        }
    }

    private Object submitOutcome(CurrentFixture current, String key) {
        try {
            return service.submitRenewalForApproval(new DccRegistrationCertificateRenewalSubmitCommand(
                    1L, 99L, key, "trace-" + key,
                    current.certificateId(), 1, current.currentVersionId(),
                    LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1),
                    LocalDate.of(2031, 9, 1), false, null, null, renewalApprovalFile()));
        } catch (ServiceException exception) {
            return exception;
        }
    }

    private MockMultipartFile renewalApprovalFile() {
        return new MockMultipartFile(
                "file", "renewal.pdf", "application/pdf",
                "renewal-content".getBytes(StandardCharsets.UTF_8));
    }

    private CurrentFixture seedCurrentCertificate() {
        DccRegistrationCertificateDO certificate = DccRegistrationCertificateDO.builder()
                .ownerCompanyId(10L)
                .productMasterId(20L)
                .projectCodeId(40L)
                .firstObtainedDate(LocalDate.of(2021, 1, 1))
                .status("ACTIVE")
                .rowVersion(1)
                .build();
        certificate.setTenantId(1L);
        assertEquals(1, certificateMapper.insert(certificate));

        DccRegistrationCertificateVersionDO currentVersion = DccRegistrationCertificateVersionDO.builder()
                .certificateId(certificate.getId())
                .versionNo(1)
                .versionType("INITIAL_CERTIFICATE")
                .certificateNo("CERT-001")
                .approvalDate(LocalDate.of(2021, 2, 1))
                .effectiveDate(LocalDate.of(2021, 3, 1))
                .expiryDate(LocalDate.of(2026, 8, 20))
                .classification("II")
                .categoryChanged(false)
                .status("CURRENT")
                .formalizedAt(java.time.LocalDateTime.of(2021, 3, 1, 9, 0))
                .formalizedBy(88L)
                .build();
        currentVersion.setTenantId(1L);
        assertEquals(1, versionMapper.insert(currentVersion));

        DccRegistrationCertificateSnapshotDO snapshot = DccRegistrationCertificateSnapshotDO.builder()
                .versionId(currentVersion.getId())
                .revisionNo(1)
                .productName("Product A")
                .registrantName("Registrant")
                .modelSpecification("Model")
                .structureComposition("Structure")
                .intendedUse("Use")
                .technicalRequirements("Requirements")
                .residenceAddress("Residence")
                .productionAddress("Production")
                .entrustedProduction(true)
                .selfProduction(false)
                .entrustedEnterprisesJson("[{\"enterpriseId\":30,\"enterpriseName\":\"Factory A\"}]")
                .effectiveAt(java.time.LocalDateTime.of(2021, 3, 1, 0, 0))
                .build();
        snapshot.setTenantId(1L);
        assertEquals(1, snapshotMapper.insert(snapshot));

        DccRegistrationCertificateSnapshotEntrustedDO entrusted =
                DccRegistrationCertificateSnapshotEntrustedDO.builder()
                        .snapshotId(snapshot.getId())
                        .enterpriseId(30L)
                        .enterpriseNameSnapshot("Factory A")
                        .sortOrder(1)
                        .build();
        entrusted.setTenantId(1L);
        assertEquals(1, entrustedMapper.insert(entrusted));

        certificate.setCurrentVersionId(currentVersion.getId());
        certificate.setCurrentSnapshotId(snapshot.getId());
        assertEquals(1, certificateMapper.updateById(certificate));

        Long fileId = seedStagedFile(currentVersion.getId(), 7001L);
        return new CurrentFixture(certificate.getId(), currentVersion.getId(), snapshot.getId(), fileId);
    }

    private Long seedStagedFile(Long currentVersionId, Long infraFileId) {
        DccRegistrationCertificateFileDO file = DccRegistrationCertificateFileDO.builder()
                .ownerType("VERSION")
                .ownerId(currentVersionId)
                .fileKind("REGISTRATION_CERTIFICATE")
                .infraFileId(infraFileId)
                .originalName("renewal-" + infraFileId + ".pdf")
                .mimeType("application/pdf")
                .fileSize(256L)
                .sha256("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .status("STAGED")
                .build();
        file.setTenantId(1L);
        assertEquals(1, fileMapper.insert(file));
        return file.getId();
    }

    private static DccRegistrationCertificateRenewalCommand sameCategoryCommand(
            Long certificateId, Long currentVersionId, Long businessFileId, String key) {
        return new DccRegistrationCertificateRenewalCommand(
                1L, 99L, key, "trace-" + key, certificateId, 1, currentVersionId, businessFileId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1), LocalDate.of(2031, 9, 1),
                false, null, null);
    }

    private static DccRegistrationCertificateRenewalCommand categoryChangedCommand(
            Long certificateId, Long currentVersionId, Long businessFileId, String key) {
        return new DccRegistrationCertificateRenewalCommand(
                1L, 99L, key, "trace-" + key, certificateId, 1, currentVersionId, businessFileId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1), LocalDate.of(2031, 9, 1),
                true, "CERT-002", "III");
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private record CurrentFixture(
            Long certificateId,
            Long currentVersionId,
            Long currentSnapshotId,
            Long stagedFileId) {
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
