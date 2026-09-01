package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessAuditDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateGrantDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessAuditMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateDownloadConsumptionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateGrantMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.accesspolicy.DccRegistrationCertificateAccessPolicyService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.DccRegistrationCertificateFileDeliveryService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.DccRegistrationCertificateFileDeliveryServiceImpl;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.DccRegistrationCertificateFileDownloadResult;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_REVOKED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_DOWNLOAD_ALREADY_CONSUMED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_DOWNLOAD_PROJECT_CODE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_DELIVERY_AUDIT_CONFLICT;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.APPROVER_ROLE_CODE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({
        DccRegistrationCertificateFileDeliveryServiceImpl.class,
        DccRegistrationCertificateAccessPolicyService.class,
        DccRegistrationCertificateBusinessClock.class,
        DccRegistrationCertificateFileDeliveryServiceTest.DbTestConfiguration.class
})
class DccRegistrationCertificateFileDeliveryServiceTest extends BaseDbUnitTest {

    @Resource
    private DccRegistrationCertificateFileDeliveryService deliveryService;
    @Resource
    private DccRegistrationCertificateMapper certificateMapper;
    @Resource
    private DccRegistrationCertificateVersionMapper versionMapper;
    @Resource
    private DccRegistrationCertificateSnapshotMapper snapshotMapper;
    @Resource
    private DccRegistrationCertificateFileMapper registrationFileMapper;
    @Resource
    private DccRegistrationCertificateAccessRequestMapper requestMapper;
    @Resource
    private DccRegistrationCertificateAccessRequestFileMapper requestFileMapper;
    @Resource
    private DccRegistrationCertificateGrantMapper grantMapper;
    @Resource
    private DccRegistrationCertificateDownloadConsumptionMapper consumptionMapper;
    @Resource
    private DccRegistrationCertificateAccessAuditMapper accessAuditMapper;
    @Resource
    private DccRegistrationCertificateBusinessClock businessClock;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private MdmCompanyScopeApi companyScopeApi;
    @MockitoBean
    private DccProjectCodeService projectCodeService;
    @MockitoBean
    private FileService fileService;
    @MockitoBean
    private PermissionApi permissionApi;

    @BeforeEach
    void setUp() {
        reset(companyScopeApi, projectCodeService, fileService, permissionApi);
    }

    @Test
    void downloadReturnsServerOwnedFileNameAndConsumesOnlyOnceBeforeLaterStorageRead() throws Exception {
        FormalFixture fixture = seedGrantedDownload("ACTIVE", "CURRENT", "BOUND", "registration.pdf");
        when(fileService.getFile(fixture.infraFileId())).thenReturn(infraFile(fixture, "registration.pdf"));
        when(projectCodeService.getProjectCode(99L, 40L)).thenReturn(project(DccProjectCodeStatusConstants.ENABLE, 20L));
        when(fileService.getFileContent(fixture.infraConfigId(), fixture.infraPath()))
                .thenReturn("CERT-BYTES".getBytes(StandardCharsets.UTF_8));

        DccRegistrationCertificateFileDownloadResult first = deliveryService.download(
                1L, 99L, fixture.businessFileId(), "attempt-success-1", context("REQ-DOWNLOAD-1"));
        ServiceException alreadyConsumed = assertThrows(ServiceException.class,
                () -> deliveryService.download(1L, 99L, fixture.businessFileId(),
                        "attempt-success-2", context("REQ-DOWNLOAD-2")));

        assertEquals("PRJ-001_20200201_ProductA_CERT-DL-001.pdf", first.fileName());
        assertEquals("application/pdf", first.contentType());
        assertArrayEquals("CERT-BYTES".getBytes(StandardCharsets.UTF_8), first.bytes());
        assertEquals(REGISTRATION_CERTIFICATE_DOWNLOAD_ALREADY_CONSUMED.getCode(), alreadyConsumed.getCode());
        assertEquals(1L, consumptionMapper.countSuccess(1L, fixture.grantId(), fixture.businessFileId()));
        assertNotNull(accessAuditMapper.selectByEventKey(1L, "attempt-success-1:DOWNLOAD:SUCCESS"));
        assertNotNull(accessAuditMapper.selectByEventKey(1L, "attempt-success-2:DOWNLOAD:FAILURE"));
        verify(fileService, times(1)).getFileContent(fixture.infraConfigId(), fixture.infraPath());
    }

    @Test
    void registrationManagerDownloadsCurrentFileWithoutGrantOrProjectCode() throws Exception {
        FormalFixture fixture = seedDownloadCandidate("ACTIVE", "CURRENT", "BOUND",
                "manager-registration.pdf", 20L, null);
        when(permissionApi.hasAnyRolesOrSuperAdmin(99L, APPROVER_ROLE_CODE)).thenReturn(true);
        when(fileService.getFile(fixture.infraFileId())).thenReturn(infraFile(fixture, "manager-registration.pdf"));
        when(fileService.getFileContent(fixture.infraConfigId(), fixture.infraPath()))
                .thenReturn("MANAGER-CERT".getBytes(StandardCharsets.UTF_8));

        DccRegistrationCertificateFileDownloadResult result = deliveryService.download(
                1L, 99L, fixture.businessFileId(), "attempt-manager-direct", context("REQ-MANAGER-DIRECT"));

        assertEquals("20200201_ProductA_CERT-DL-001.pdf", result.fileName());
        assertArrayEquals("MANAGER-CERT".getBytes(StandardCharsets.UTF_8), result.bytes());
        assertNull(consumptionMapper.selectByAttemptKey(1L, "attempt-manager-direct"));
        DccRegistrationCertificateAccessAuditDO audit =
                accessAuditMapper.selectByEventKey(1L, "attempt-manager-direct:DOWNLOAD:SUCCESS");
        assertNotNull(audit);
        assertNull(audit.getGrantId());
        assertTrue(audit.getDetailJson().contains("REGISTRATION_MANAGER_ROLE"));
        verify(projectCodeService, never()).getProjectCode(99L, 40L);
    }

    @Test
    void registrationManagerDownloadUsesFirstObtainedDateWhenUploadedCertificateHasNoApprovalDate() throws Exception {
        FormalFixture fixture = seedDownloadCandidate("ACTIVE", "CURRENT", "BOUND",
                "uploaded-manager-registration.pdf", null, null);
        jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_version
                   SET approval_date = NULL
                 WHERE id = ?
                """, fixture.versionId());
        when(permissionApi.hasAnyRolesOrSuperAdmin(99L, APPROVER_ROLE_CODE)).thenReturn(true);
        when(fileService.getFile(fixture.infraFileId())).thenReturn(
                infraFile(fixture, "uploaded-manager-registration.pdf"));
        when(fileService.getFileContent(fixture.infraConfigId(), fixture.infraPath()))
                .thenReturn("UPLOADED-MANAGER-CERT".getBytes(StandardCharsets.UTF_8));

        DccRegistrationCertificateFileDownloadResult result = deliveryService.download(
                1L, 99L, fixture.businessFileId(), "attempt-manager-uploaded-no-approval",
                context("REQ-MANAGER-UPLOADED-NO-APPROVAL"));

        assertEquals("20200101_ProductA_CERT-DL-001.pdf", result.fileName());
        assertArrayEquals("UPLOADED-MANAGER-CERT".getBytes(StandardCharsets.UTF_8), result.bytes());
        assertNotNull(accessAuditMapper.selectByEventKey(
                1L, "attempt-manager-uploaded-no-approval:DOWNLOAD:SUCCESS"));
    }

    @Test
    void nonRegistrationManagerWithoutDownloadGrantStillFailsBeforeStorageIo() throws Exception {
        FormalFixture fixture = seedDownloadCandidate("ACTIVE", "CURRENT", "BOUND",
                "registration.pdf", 20L, 40L);
        when(permissionApi.hasAnyRolesOrSuperAdmin(99L, APPROVER_ROLE_CODE)).thenReturn(false);

        ServiceException failure = assertThrows(ServiceException.class,
                () -> deliveryService.download(1L, 99L, fixture.businessFileId(),
                        "attempt-no-grant", context("REQ-NO-GRANT")));

        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID.getCode(), failure.getCode());
        assertNotNull(accessAuditMapper.selectByEventKey(1L, "attempt-no-grant:DOWNLOAD:FAILURE"));
        verify(fileService, never()).getFileContent(fixture.infraConfigId(), fixture.infraPath());
    }

    @Test
    void registrationManagerDownloadStillRequiresCompanyScopeBeforeStorageIo() throws Exception {
        FormalFixture fixture = seedDownloadCandidate("ACTIVE", "CURRENT", "BOUND",
                "manager-denied.pdf", 20L, null);
        when(permissionApi.hasAnyRolesOrSuperAdmin(99L, APPROVER_ROLE_CODE)).thenReturn(true);
        doThrow(new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID))
                .when(companyScopeApi).validateUserCompanyAccess(99L, 10L);

        ServiceException failure = assertThrows(ServiceException.class,
                () -> deliveryService.download(1L, 99L, fixture.businessFileId(),
                        "attempt-manager-company-denied", context("REQ-MANAGER-COMPANY-DENIED")));

        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID.getCode(), failure.getCode());
        assertNotNull(accessAuditMapper.selectByEventKey(
                1L, "attempt-manager-company-denied:DOWNLOAD:FAILURE"));
        verify(fileService, never()).getFileContent(fixture.infraConfigId(), fixture.infraPath());
    }

    @Test
    void changeAndExpiredFilesUseTheRequiredDownloadNameSuffixes() throws Exception {
        FormalFixture changeFixture = seedGrantedDownload("ACTIVE", "CURRENT", "BOUND", "change.pdf");
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_change
                  (id, tenant_id, owner_company_id, certificate_id, source_version_id, source_snapshot_id,
                   resulting_snapshot_id, event_id, approval_date, selected_change_types_json,
                   selected_item_count, status, actor_id, applied_at)
                VALUES (7001, 1, 10, ?, ?, ?, ?, 97001, ?, '[\"PRODUCT_NAME\"]', 1, 'APPLIED', 99, ?)
                """, changeFixture.certificateId(), changeFixture.versionId(), changeFixture.snapshotId(),
                changeFixture.snapshotId(), LocalDate.of(2026, 8, 17), businessClock.now());
        jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_file
                   SET owner_type = 'CHANGE', owner_id = 7001, file_kind = 'CHANGE_APPROVAL'
                 WHERE id = ?
                """, changeFixture.businessFileId());
        when(fileService.getFile(changeFixture.infraFileId())).thenReturn(infraFile(changeFixture, "change.pdf"));
        when(projectCodeService.getProjectCode(99L, 40L)).thenReturn(project(DccProjectCodeStatusConstants.ENABLE, 20L));
        when(fileService.getFileContent(changeFixture.infraConfigId(), changeFixture.infraPath()))
                .thenReturn("CHANGE".getBytes(StandardCharsets.UTF_8));

        DccRegistrationCertificateFileDownloadResult changeResult = deliveryService.download(
                1L, 99L, changeFixture.businessFileId(), "attempt-change-name", context("REQ-CHANGE-NAME"));
        assertEquals("PRJ-001_20200201_ProductA_变更文件_CERT-DL-001.pdf", changeResult.fileName());

        FormalFixture oldFixture = seedGrantedDownload("ACTIVE", "OLD", "BOUND", "old.pdf");
        when(fileService.getFile(oldFixture.infraFileId())).thenReturn(infraFile(oldFixture, "old.pdf"));
        when(fileService.getFileContent(oldFixture.infraConfigId(), oldFixture.infraPath()))
                .thenReturn("OLD".getBytes(StandardCharsets.UTF_8));
        DccRegistrationCertificateFileDownloadResult oldResult = deliveryService.download(
                1L, 99L, oldFixture.businessFileId(), "attempt-old-name", context("REQ-OLD-NAME"));
        assertEquals("PRJ-001_20200201_ProductA_CERT-DL-001_已失效.pdf", oldResult.fileName());
    }

    @Test
    void preStartStorageFailureIsAuditedButDoesNotConsume() throws Exception {
        FormalFixture fixture = seedGrantedDownload("ACTIVE", "CURRENT", "BOUND", "registration.pdf");
        when(fileService.getFile(fixture.infraFileId())).thenReturn(infraFile(fixture, "registration.pdf"));
        when(projectCodeService.getProjectCode(99L, 40L)).thenReturn(project(DccProjectCodeStatusConstants.ENABLE, 20L));
        IllegalStateException storageFailure = new IllegalStateException("storage unavailable");
        when(fileService.getFileContent(fixture.infraConfigId(), fixture.infraPath()))
                .thenThrow(storageFailure)
                .thenReturn("RECOVERED".getBytes(StandardCharsets.UTF_8));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> deliveryService.download(1L, 99L, fixture.businessFileId(),
                        "attempt-storage-fail", context("REQ-DOWNLOAD-STORAGE-FAIL")));
        DccRegistrationCertificateFileDownloadResult recovered = deliveryService.download(
                1L, 99L, fixture.businessFileId(), "attempt-storage-retry", context("REQ-DOWNLOAD-STORAGE-RETRY"));

        assertEquals(storageFailure, failure);
        assertEquals(1L, consumptionMapper.countSuccess(1L, fixture.grantId(), fixture.businessFileId()));
        assertNotNull(consumptionMapper.selectByAttemptKey(1L, "attempt-storage-fail"));
        assertEquals("FAILED_BEFORE_START", consumptionMapper.selectByAttemptKey(1L, "attempt-storage-fail").getResult());
        assertArrayEquals("RECOVERED".getBytes(StandardCharsets.UTF_8), recovered.bytes());
        assertNotNull(accessAuditMapper.selectByEventKey(1L, "attempt-storage-fail:DOWNLOAD:FAILURE"));
        assertNotNull(accessAuditMapper.selectByEventKey(1L, "attempt-storage-retry:DOWNLOAD:SUCCESS"));
    }

    @Test
    void liveProjectCodeDriftFailsBeforeStorageIoAndAuditsFailure() throws Exception {
        FormalFixture fixture = seedGrantedDownload("ACTIVE", "CURRENT", "BOUND", "registration.pdf");
        when(fileService.getFile(fixture.infraFileId())).thenReturn(infraFile(fixture, "registration.pdf"));
        when(projectCodeService.getProjectCode(99L, 40L)).thenReturn(project("DISABLED", 20L));

        ServiceException failure = assertThrows(ServiceException.class,
                () -> deliveryService.download(1L, 99L, fixture.businessFileId(),
                        "attempt-project-drift", context("REQ-DOWNLOAD-PROJECT-DRIFT")));

        assertEquals(REGISTRATION_CERTIFICATE_DOWNLOAD_PROJECT_CODE_INVALID.getCode(), failure.getCode());
        assertEquals(0L, consumptionMapper.countSuccess(1L, fixture.grantId(), fixture.businessFileId()));
        assertNotNull(accessAuditMapper.selectByEventKey(1L, "attempt-project-drift:DOWNLOAD:FAILURE"));
        verify(fileService, never()).getFileContent(fixture.infraConfigId(), fixture.infraPath());
    }

    @Test
    void downloadAllowsUploadedCertificateWithoutProductMasterBinding() throws Exception {
        FormalFixture fixture = seedGrantedDownload("ACTIVE", "CURRENT", "BOUND",
                "uploaded-registration.pdf", null);
        when(fileService.getFile(fixture.infraFileId())).thenReturn(infraFile(fixture, "uploaded-registration.pdf"));
        when(projectCodeService.getProjectCode(99L, 40L)).thenReturn(project(DccProjectCodeStatusConstants.ENABLE, 21L));
        when(fileService.getFileContent(fixture.infraConfigId(), fixture.infraPath()))
                .thenReturn("UPLOADED-CERT".getBytes(StandardCharsets.UTF_8));

        DccRegistrationCertificateFileDownloadResult result = deliveryService.download(
                1L, 99L, fixture.businessFileId(), "attempt-uploaded-download", context("REQ-UPLOADED-DOWNLOAD"));

        assertEquals("PRJ-001_20200201_ProductA_CERT-DL-001.pdf", result.fileName());
        assertArrayEquals("UPLOADED-CERT".getBytes(StandardCharsets.UTF_8), result.bytes());
        assertEquals(1L, consumptionMapper.countSuccess(1L, fixture.grantId(), fixture.businessFileId()));
    }

    @Test
    void projectCodeInfrastructureFailureIsAuditedBeforeStorageIoAndPropagatesOriginalException() throws Exception {
        FormalFixture fixture = seedGrantedDownload("ACTIVE", "CURRENT", "BOUND", "registration.pdf");
        when(fileService.getFile(fixture.infraFileId())).thenReturn(infraFile(fixture, "registration.pdf"));
        IllegalStateException outage = new IllegalStateException("project code service outage");
        when(projectCodeService.getProjectCode(99L, 40L)).thenThrow(outage);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> deliveryService.download(1L, 99L, fixture.businessFileId(),
                        "attempt-project-outage", context("REQ-DOWNLOAD-PROJECT-OUTAGE")));

        assertSame(outage, failure);
        assertEquals(0L, consumptionMapper.countSuccess(1L, fixture.grantId(), fixture.businessFileId()));
        assertNotNull(accessAuditMapper.selectByEventKey(1L, "attempt-project-outage:DOWNLOAD:FAILURE"));
        verify(fileService, never()).getFileContent(fixture.infraConfigId(), fixture.infraPath());
    }

    @Test
    void revokedGrantFailsBeforeStorageIoAndAuditsFailure() throws Exception {
        FormalFixture fixture = seedGrantedDownload("ACTIVE", "CURRENT", "BOUND", "registration.pdf");
        DccRegistrationCertificateGrantDO grant = grantMapper.selectById(fixture.grantId());
        grant.setStatus("REVOKED");
        grant.setRevokedAt(businessClock.now().minusMinutes(10));
        grant.setRevokedBy(101L);
        grant.setRevokeReason("scope revoked");
        assertEquals(1, grantMapper.updateById(grant));
        when(fileService.getFile(fixture.infraFileId())).thenReturn(infraFile(fixture, "registration.pdf"));

        ServiceException failure = assertThrows(ServiceException.class,
                () -> deliveryService.download(1L, 99L, fixture.businessFileId(),
                        "attempt-revoked-grant", context("REQ-DOWNLOAD-REVOKED-GRANT")));

        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_GRANT_REVOKED.getCode(), failure.getCode());
        assertEquals(0L, consumptionMapper.countSuccess(1L, fixture.grantId(), fixture.businessFileId()));
        assertNotNull(accessAuditMapper.selectByEventKey(1L, "attempt-revoked-grant:DOWNLOAD:FAILURE"));
        verify(fileService, never()).getFileContent(fixture.infraConfigId(), fixture.infraPath());
    }

    @Test
    void successAuditConflictRollsBackSuccessfulConsumptionAndRecordsFailure() throws Exception {
        FormalFixture fixture = seedGrantedDownload("ACTIVE", "CURRENT", "BOUND", "registration.pdf");
        when(fileService.getFile(fixture.infraFileId())).thenReturn(infraFile(fixture, "registration.pdf"));
        when(projectCodeService.getProjectCode(99L, 40L)).thenReturn(project(DccProjectCodeStatusConstants.ENABLE, 20L));
        when(fileService.getFileContent(fixture.infraConfigId(), fixture.infraPath()))
                .thenReturn("AUDIT-CONFLICT".getBytes(StandardCharsets.UTF_8));
        DccRegistrationCertificateAccessAuditDO existingAudit =
                DccRegistrationCertificateAccessAuditDO.builder()
                        .requestId(9001L)
                        .grantId(fixture.grantId())
                        .businessFileId(fixture.businessFileId())
                        .actorUserId(99L)
                        .eventType("DOWNLOAD")
                        .eventKey("attempt-audit-conflict:DOWNLOAD:SUCCESS")
                        .result("SUCCESS")
                        .occurredAt(LocalDateTime.of(2026, 8, 19, 9, 30))
                        .detailJson("{\"requestId\":\"preexisting\"}")
                        .build();
        existingAudit.setTenantId(1L);
        assertEquals(1, accessAuditMapper.insert(existingAudit));

        ServiceException failure = assertThrows(ServiceException.class,
                () -> deliveryService.download(1L, 99L, fixture.businessFileId(),
                        "attempt-audit-conflict", context("REQ-DOWNLOAD-AUDIT-CONFLICT")));

        assertEquals(REGISTRATION_CERTIFICATE_FILE_DELIVERY_AUDIT_CONFLICT.getCode(), failure.getCode());
        assertEquals(0L, consumptionMapper.countSuccess(1L, fixture.grantId(), fixture.businessFileId()));
        assertNotNull(accessAuditMapper.selectByEventKey(1L, "attempt-audit-conflict:DOWNLOAD:FAILURE"));
        verify(fileService, times(1)).getFileContent(fixture.infraConfigId(), fixture.infraPath());
    }

    @Test
    void missingServerOwnedFilenameFactsFailBeforeStorageIoAndConsumption() throws Exception {
        FormalFixture fixture = seedGrantedDownload("ACTIVE", "CURRENT", "BOUND", "registration");
        when(fileService.getFile(fixture.infraFileId())).thenReturn(infraFile(fixture, "registration"));
        when(projectCodeService.getProjectCode(99L, 40L)).thenReturn(project(DccProjectCodeStatusConstants.ENABLE, 20L));

        ServiceException failure = assertThrows(ServiceException.class,
                () -> deliveryService.download(1L, 99L, fixture.businessFileId(),
                        "attempt-bad-filename", context("REQ-DOWNLOAD-BAD-FILENAME")));

        assertTrue(failure.getMessage().contains("扩展名"));
        assertEquals(0L, consumptionMapper.countSuccess(1L, fixture.grantId(), fixture.businessFileId()));
        assertNotNull(accessAuditMapper.selectByEventKey(1L, "attempt-bad-filename:DOWNLOAD:FAILURE"));
        verify(fileService, never()).getFileContent(fixture.infraConfigId(), fixture.infraPath());
    }

    @Test
    void concurrentDownloadRaceReadsStorageOnceAndReturnsOneSuccess() throws Exception {
        FormalFixture fixture = seedGrantedDownload("ACTIVE", "CURRENT", "BOUND", "registration.pdf");
        when(fileService.getFile(fixture.infraFileId())).thenReturn(infraFile(fixture, "registration.pdf"));
        when(projectCodeService.getProjectCode(99L, 40L)).thenReturn(project(DccProjectCodeStatusConstants.ENABLE, 20L));
        CountDownLatch firstReadEntered = new CountDownLatch(1);
        CountDownLatch releaseStorage = new CountDownLatch(1);
        AtomicInteger storageReads = new AtomicInteger();
        when(fileService.getFileContent(fixture.infraConfigId(), fixture.infraPath())).thenAnswer(invocation -> {
            storageReads.incrementAndGet();
            firstReadEntered.countDown();
            assertTrue(releaseStorage.await(5, TimeUnit.SECONDS));
            return "RACE-BYTES".getBytes(StandardCharsets.UTF_8);
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<Object> first = CompletableFuture.supplyAsync(() -> downloadOutcome(
                    fixture, "attempt-race-1", "REQ-DOWNLOAD-RACE-1"), executor);
            CompletableFuture<Object> second = CompletableFuture.supplyAsync(() -> downloadOutcome(
                    fixture, "attempt-race-2", "REQ-DOWNLOAD-RACE-2"), executor);
            assertTrue(firstReadEntered.await(5, TimeUnit.SECONDS));
            Thread.sleep(150);
            releaseStorage.countDown();

            List<Object> outcomes = List.of(first.join(), second.join());

            assertEquals(1, outcomes.stream()
                    .filter(DccRegistrationCertificateFileDownloadResult.class::isInstance).count());
            assertEquals(1, outcomes.stream()
                    .filter(ServiceException.class::isInstance)
                    .map(ServiceException.class::cast)
                    .filter(ex -> ex.getCode() == REGISTRATION_CERTIFICATE_DOWNLOAD_ALREADY_CONSUMED.getCode())
                    .count());
            assertEquals(1, storageReads.get());
            assertEquals(1L, consumptionMapper.countSuccess(1L, fixture.grantId(), fixture.businessFileId()));
        } finally {
            releaseStorage.countDown();
            executor.shutdownNow();
        }
    }

    private Object downloadOutcome(FormalFixture fixture, String attemptKey, String requestId) {
        try {
            return deliveryService.download(1L, 99L, fixture.businessFileId(), attemptKey, context(requestId));
        } catch (ServiceException ex) {
            return ex;
        }
    }

    private FormalFixture seedGrantedDownload(String masterStatus, String versionStatus, String fileStatus,
                                              String infraOriginalName) {
        return seedGrantedDownload(masterStatus, versionStatus, fileStatus, infraOriginalName, 20L);
    }

    private FormalFixture seedGrantedDownload(String masterStatus, String versionStatus, String fileStatus,
                                              String infraOriginalName, Long productMasterId) {
        FormalFixture fixture = seedDownloadCandidate(masterStatus, versionStatus, fileStatus,
                infraOriginalName, productMasterId, 40L);
        DccRegistrationCertificateFileDO file = registrationFileMapper.selectById(fixture.businessFileId());

        DccRegistrationCertificateAccessRequestDO request = DccRegistrationCertificateAccessRequestDO.builder()
                .ownerCompanyId(10L)
                .certificateId(fixture.certificateId())
                .requesterUserId(99L)
                .requestType("DOWNLOAD_FILE")
                .requestKey("request-download-" + System.nanoTime())
                .purpose("approved file delivery")
                .projectCodeId(40L)
                .status("APPROVED")
                .requestedAt(businessClock.now().minusHours(2))
                .completedAt(businessClock.now().minusHours(1))
                .detailJson("{}")
                .build();
        request.setTenantId(1L);
        assertEquals(1, requestMapper.insert(request));

        DccRegistrationCertificateAccessRequestFileDO requestFile =
                DccRegistrationCertificateAccessRequestFileDO.builder()
                        .requestId(request.getId())
                        .businessFileId(file.getId())
                        .fileKind("REGISTRATION_CERTIFICATE")
                        .downloadRequested(true)
                        .status("GRANTED")
                        .detailJson("{}")
                        .build();
        requestFile.setTenantId(1L);
        assertEquals(1, requestFileMapper.insert(requestFile));

        DccRegistrationCertificateGrantDO grant = DccRegistrationCertificateGrantDO.builder()
                .requestId(request.getId())
                .requestFileId(requestFile.getId())
                .ownerCompanyId(10L)
                .certificateId(fixture.certificateId())
                .businessFileId(file.getId())
                .granteeUserId(99L)
                .grantType("DOWNLOAD")
                .grantKey("grant-download-" + System.nanoTime())
                .status("ACTIVE")
                .grantedAt(businessClock.now().minusMinutes(30))
                .expiresAt(businessClock.now().plusHours(24))
                .detailJson("{}")
                .build();
        grant.setTenantId(1L);
        assertEquals(1, grantMapper.insert(grant));
        return new FormalFixture(fixture.certificateId(), fixture.versionId(), fixture.snapshotId(), file.getId(),
                fixture.infraFileId(), fixture.infraConfigId(), fixture.infraPath(), grant.getId());
    }

    private FormalFixture seedDownloadCandidate(String masterStatus, String versionStatus, String fileStatus,
                                                String infraOriginalName, Long productMasterId, Long projectCodeId) {
        Long infraFileId = 930_000L + Math.abs(System.nanoTime() % 10_000L);
        Long infraConfigId = 7001L;
        String infraPath = "registration/" + infraOriginalName;

        DccRegistrationCertificateDO certificate = DccRegistrationCertificateDO.builder()
                .ownerCompanyId(10L)
                .productMasterId(productMasterId)
                .projectCodeId(projectCodeId)
                .firstObtainedDate(LocalDate.of(2020, 1, 1))
                .status(masterStatus)
                .rowVersion(1)
                .build();
        certificate.setTenantId(1L);
        assertEquals(1, certificateMapper.insert(certificate));

        DccRegistrationCertificateVersionDO version = DccRegistrationCertificateVersionDO.builder()
                .certificateId(certificate.getId())
                .versionNo(1)
                .versionType("INITIAL_CERTIFICATE")
                .certificateNo("CERT-DL-001")
                .approvalDate(LocalDate.of(2020, 2, 1))
                .effectiveDate(LocalDate.of(2020, 3, 1))
                .expiryDate(LocalDate.of(2030, 3, 1))
                .classification("II")
                .categoryChanged(false)
                .status(versionStatus)
                .build();
        version.setTenantId(1L);
        assertEquals(1, versionMapper.insert(version));

        DccRegistrationCertificateSnapshotDO snapshot = DccRegistrationCertificateSnapshotDO.builder()
                .versionId(version.getId())
                .revisionNo(1)
                .productName("ProductA")
                .registrantName("Registrant")
                .entrustedProduction(false)
                .selfProduction(true)
                .entrustedEnterprisesJson("[]")
                .effectiveAt(LocalDateTime.of(2020, 3, 1, 0, 0))
                .build();
        snapshot.setTenantId(1L);
        assertEquals(1, snapshotMapper.insert(snapshot));

        DccRegistrationCertificateFileDO file = DccRegistrationCertificateFileDO.builder()
                .ownerType("VERSION")
                .ownerId(version.getId())
                .fileKind("REGISTRATION_CERTIFICATE")
                .infraFileId(infraFileId)
                .originalName(infraOriginalName)
                .mimeType("application/pdf")
                .fileSize(1024L)
                .sha256("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")
                .status(fileStatus)
                .boundAt(LocalDateTime.of(2020, 3, 1, 8, 0))
                .boundBy(77L)
                .build();
        file.setTenantId(1L);
        assertEquals(1, registrationFileMapper.insert(file));
        certificate.setCurrentVersionId("CURRENT".equals(versionStatus) ? version.getId() : null);
        certificate.setCurrentSnapshotId("CURRENT".equals(versionStatus) ? snapshot.getId() : null);
        certificate.setPendingVersionId("PENDING_EFFECTIVE".equals(versionStatus) ? version.getId() : null);
        assertEquals(1, certificateMapper.updateById(certificate));
        return new FormalFixture(certificate.getId(), version.getId(), snapshot.getId(), file.getId(),
                infraFileId, infraConfigId, infraPath, null);
    }

    private FileDO infraFile(FormalFixture fixture, String name) {
        return FileDO.builder()
                .id(fixture.infraFileId())
                .configId(fixture.infraConfigId())
                .name(name)
                .path(fixture.infraPath())
                .type("application/pdf")
                .size(1024L)
                .build();
    }

    private DccProjectCodeDO project(String status, Long productMasterId) {
        DccProjectCodeDO projectCode = DccProjectCodeDO.builder()
                .id(40L)
                .productMasterId(productMasterId)
                .projectCode("PRJ-001")
                .status(status)
                .build();
        projectCode.setTenantId(1L);
        return projectCode;
    }

    private static DccRequestAuditContext context(String requestId) {
        return new DccRequestAuditContext("10.0.0.1", "JUnit", requestId);
    }

    private record FormalFixture(Long certificateId, Long versionId, Long snapshotId, Long businessFileId,
                                 Long infraFileId, Long infraConfigId, String infraPath, Long grantId) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DbTestConfiguration {

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }
}
