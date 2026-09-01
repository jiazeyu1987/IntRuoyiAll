package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAuditDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAuditMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateReadAuditService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.accesspolicy.DccRegistrationCertificateAccessPolicyService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.access.DccRegistrationCertificateBusinessFileAccessProvider;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reference.DccRegistrationCertificateFileReferenceService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reference.DccRegistrationCertificateFileReferenceServiceImpl;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessDeniedException;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessRequest;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessService;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.access.DccRegistrationCertificateBusinessFileAccessProvider.BUSINESS_TYPE;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.access.DccRegistrationCertificateBusinessFileAccessProvider.PROVIDER_ID;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.access.DccRegistrationCertificateBusinessFileAccessProvider.QUERY_CURRENT_PERMISSION;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.APPROVER_ROLE_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({
        DccRegistrationCertificateBusinessFileAccessProvider.class,
        DccRegistrationCertificateFileReferenceServiceImpl.class,
        DccRegistrationCertificateAccessPolicyService.class,
        DccRegistrationCertificateReadAuditService.class,
        DccRegistrationCertificateBusinessClock.class,
        DccRegistrationCertificateBusinessFileAccessProviderTest.JdbcTestConfiguration.class
})
class DccRegistrationCertificateBusinessFileAccessProviderTest extends BaseDbUnitTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class JdbcTestConfiguration {
        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }

    @Resource
    private DccRegistrationCertificateBusinessFileAccessProvider provider;
    @Resource
    private DccRegistrationCertificateFileReferenceService referenceService;
    @Resource
    private DccRegistrationCertificateAccessPolicyService accessPolicyService;
    @Resource
    private DccRegistrationCertificateBusinessClock businessClock;
    @Resource
    private DccRegistrationCertificateMapper certificateMapper;
    @Resource
    private DccRegistrationCertificateVersionMapper versionMapper;
    @Resource
    private DccRegistrationCertificateSnapshotMapper snapshotMapper;
    @Resource
    private DccRegistrationCertificateFileMapper dbFileMapper;
    @Resource
    private DccRegistrationCertificateAuditMapper auditMapper;

    @MockitoBean
    private MdmCompanyScopeApi companyScopeApi;
    @MockitoBean
    private PermissionApi permissionApi;

    private BusinessFileAccessService accessService;

    @BeforeEach
    void setUp() {
        reset(companyScopeApi, permissionApi);
        accessService = new BusinessFileAccessService(List.of(provider));
    }

    @Test
    void previewAllowsOnlyCurrentBusinessFileAndWritesSuccessAudit() {
        FormalFile file = seedFormalFile(1L, 10L, "ACTIVE", "CURRENT", "BOUND", 91001L);
        when(permissionApi.hasAnyPermissions(99L, QUERY_CURRENT_PERMISSION)).thenReturn(true);

        BusinessFileAccessReference reference = accessService.assertAllowed(new BusinessFileAccessRequest(
                BusinessFileAccessOperation.PREVIEW, file.infraFileId(), 1L, 99L, null,
                "REQ-RC-PREVIEW-001", null, "10.0.0.1", "JUnit")).orElseThrow();

        assertEquals(PROVIDER_ID, reference.providerId());
        assertEquals(BUSINESS_TYPE, reference.businessType());
        assertEquals(file.businessFileId(), reference.businessId());
        assertEquals(10L, reference.companyId());
        verify(companyScopeApi).validateUserCompanyAccess(99L, 10L);
        DccRegistrationCertificateAuditDO audit = auditMapper.selectByTenantIdAndEventKey(
                1L, "REQ-RC-PREVIEW-001:PREVIEW:CERTIFICATE:" + file.certificateId() + ":SUCCESS");
        assertNotNull(audit);
        assertEquals(file.businessFileId(), audit.getBusinessFileId());
        assertEquals("SUCCESS", audit.getResult());
    }

    @Test
    void previewAllowsPendingEffectiveBusinessFileSelectedByTheMasterProjection() {
        FormalFile pendingInitial = seedFormalFile(
                1L, 10L, "PENDING_FIRST_EFFECTIVE", "PENDING_EFFECTIVE", "BOUND", 91006L);
        FormalFile pendingRenewal = seedFormalFile(
                1L, 10L, "ACTIVE", "PENDING_EFFECTIVE", "BOUND", 91007L);
        when(permissionApi.hasAnyPermissions(99L, QUERY_CURRENT_PERMISSION)).thenReturn(true);

        for (FormalFile file : List.of(pendingInitial, pendingRenewal)) {
            BusinessFileAccessReference reference = accessService.assertAllowed(new BusinessFileAccessRequest(
                    BusinessFileAccessOperation.PREVIEW, file.infraFileId(), 1L, 99L, null,
                    "REQ-RC-PENDING-" + file.infraFileId(), null, "10.0.0.1", "JUnit")).orElseThrow();

            assertEquals(file.businessFileId(), reference.businessId());
            assertEquals(file.certificateId(), reference.versionKey() == null ? null : file.certificateId());
        }
    }

    @Test
    void onlyOfficePreviewAllowsOldVersionForRegistrationManagerWithinCompanyScope() {
        FormalFile file = seedFormalFile(1L, 10L, "EXPIRED_UNRENEWED", "OLD", "BOUND", 91008L);
        when(permissionApi.hasAnyPermissions(99L, QUERY_CURRENT_PERMISSION)).thenReturn(true);
        when(permissionApi.hasAnyRolesOrSuperAdmin(99L, APPROVER_ROLE_CODE)).thenReturn(true);

        BusinessFileAccessReference reference = accessService.assertAllowed(new BusinessFileAccessRequest(
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, file.infraFileId(), 1L, 99L, null,
                "REQ-RC-OLD-OFFICE-001", null, "10.0.0.1", "JUnit")).orElseThrow();

        assertEquals(file.businessFileId(), reference.businessId());
        verify(companyScopeApi).validateUserCompanyAccess(99L, 10L);
        assertNotNull(auditMapper.selectByTenantIdAndEventKey(
                1L, "REQ-RC-OLD-OFFICE-001:ONLYOFFICE_PREVIEW:CERTIFICATE:"
                        + file.certificateId() + ":SUCCESS"));
    }

    @Test
    void permissionDeniedIsAuditedBeforeFileIo() {
        FormalFile file = seedFormalFile(1L, 10L, "ACTIVE", "CURRENT", "BOUND", 91004L);
        when(permissionApi.hasAnyPermissions(99L, QUERY_CURRENT_PERMISSION)).thenReturn(false);

        assertThrows(BusinessFileAccessDeniedException.class,
                () -> accessService.assertAllowed(new BusinessFileAccessRequest(
                        BusinessFileAccessOperation.PREVIEW, file.infraFileId(), 1L, 99L, null,
                        "REQ-RC-NO-PERM-001", null, "10.0.0.1", "JUnit")));

        assertNotNull(auditMapper.selectByTenantIdAndEventKey(
                1L, "REQ-RC-NO-PERM-001:PREVIEW:REQUESTED:" + file.certificateId() + ":FAILURE"));
    }

    @Test
    void auditFailureBlocksAllowedPreview() {
        FormalFile file = seedFormalFile(1L, 10L, "ACTIVE", "CURRENT", "BOUND", 91005L);
        when(permissionApi.hasAnyPermissions(99L, QUERY_CURRENT_PERMISSION)).thenReturn(true);
        DccRegistrationCertificateReadAuditService brokenAuditService =
                mock(DccRegistrationCertificateReadAuditService.class);
        IllegalStateException auditFailure = new IllegalStateException("audit unavailable");
        doThrow(auditFailure).when(brokenAuditService).record(any());
        BusinessFileAccessService guardedService = new BusinessFileAccessService(List.of(
                new DccRegistrationCertificateBusinessFileAccessProvider(referenceService, accessPolicyService,
                        permissionApi, brokenAuditService, businessClock)));

        BusinessFileAccessDeniedException denied = assertThrows(BusinessFileAccessDeniedException.class,
                () -> guardedService.assertAllowed(new BusinessFileAccessRequest(
                        BusinessFileAccessOperation.PREVIEW, file.infraFileId(), 1L, 99L, null,
                        "REQ-RC-AUDIT-DOWN-001", null, "10.0.0.1", "JUnit")));

        assertSame(auditFailure, denied.getCause());
    }

    @Test
    void directLinkAndPreSp06OperationsAreDeniedAndAuditedBeforeStorage() {
        FormalFile file = seedFormalFile(1L, 10L, "ACTIVE", "CURRENT", "BOUND", 91002L);

        assertThrows(BusinessFileAccessDeniedException.class,
                () -> accessService.assertAllowed(BusinessFileAccessRequest.publicDirectLink(
                        file.infraFileId(), "REQ-RC-DIRECT-001", "10.0.0.1", "JUnit")));
        assertNotNull(auditMapper.selectByTenantIdAndEventKey(
                1L, "REQ-RC-DIRECT-001:DIRECT_LINK:REQUESTED:" + file.certificateId() + ":FAILURE"));

        for (BusinessFileAccessOperation operation : List.of(BusinessFileAccessOperation.DOWNLOAD,
                BusinessFileAccessOperation.PRINT, BusinessFileAccessOperation.CONVERT)) {
            String requestId = "REQ-RC-" + operation.name() + "-001";
            assertThrows(BusinessFileAccessDeniedException.class,
                    () -> accessService.assertAllowed(new BusinessFileAccessRequest(
                            operation, file.infraFileId(), 1L, 99L, null,
                            requestId, null, "10.0.0.1", "JUnit")));
            assertNotNull(auditMapper.selectByTenantIdAndEventKey(
                    1L, requestId + ":" + operation.name() + ":REQUESTED:" + file.certificateId() + ":FAILURE"));
        }
    }

    @Test
    void referenceDriftIsDeniedAndAuditedBeforeFileIo() {
        FormalFile file = seedFormalFile(1L, 10L, "ACTIVE", "CURRENT", "BOUND", 91003L);
        BusinessFileAccessReference staleClaim = provider.resolve(file.infraFileId()).orElseThrow();
        DccRegistrationCertificateVersionDO version = versionMapper.selectById(file.versionId());
        version.setStatus("OLD");
        assertEquals(1, versionMapper.updateById(version));

        assertThrows(BusinessFileAccessDeniedException.class,
                () -> accessService.assertAllowed(BusinessFileAccessRequest.tokenCallback(
                        BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, file.infraFileId(), 1L, 99L, null,
                        "REQ-RC-DRIFT-001", staleClaim, "10.0.0.1", "JUnit")));

        assertNotNull(auditMapper.selectByTenantIdAndEventKey(
                1L, "REQ-RC-DRIFT-001:ONLYOFFICE_PREVIEW:REQUESTED:" + file.certificateId() + ":FAILURE"));
    }

    private FormalFile seedFormalFile(Long tenantId, Long ownerCompanyId, String masterStatus, String versionStatus,
                                      String fileStatus, Long infraFileId) {
        DccRegistrationCertificateDO certificate = DccRegistrationCertificateDO.builder()
                .ownerCompanyId(ownerCompanyId)
                .productMasterId(20L)
                .firstObtainedDate(LocalDate.of(2026, 1, 1))
                .status(masterStatus)
                .rowVersion(1)
                .build();
        certificate.setTenantId(tenantId);
        assertEquals(1, certificateMapper.insert(certificate));

        DccRegistrationCertificateVersionDO version = DccRegistrationCertificateVersionDO.builder()
                .certificateId(certificate.getId())
                .versionNo(1)
                .versionType("INITIAL_CERTIFICATE")
                .certificateNo("CERT-FILE-" + infraFileId)
                .approvalDate(LocalDate.of(2026, 2, 1))
                .effectiveDate(LocalDate.of(2026, 8, 1))
                .expiryDate(LocalDate.of(2031, 8, 1))
                .classification("II")
                .categoryChanged(false)
                .status(versionStatus)
                .formalizedAt(LocalDateTime.of(2026, 8, 1, 9, 0))
                .formalizedBy(99L)
                .build();
        version.setTenantId(tenantId);
        assertEquals(1, versionMapper.insert(version));

        DccRegistrationCertificateSnapshotDO snapshot = DccRegistrationCertificateSnapshotDO.builder()
                .versionId(version.getId())
                .revisionNo(1)
                .productName("Product")
                .registrantName("Registrant")
                .entrustedProduction(false)
                .selfProduction(true)
                .entrustedEnterprisesJson("[]")
                .effectiveAt(LocalDateTime.of(2026, 8, 1, 0, 0))
                .build();
        snapshot.setTenantId(tenantId);
        assertEquals(1, snapshotMapper.insert(snapshot));

        DccRegistrationCertificateFileDO file = DccRegistrationCertificateFileDO.builder()
                .ownerType("VERSION")
                .ownerId(version.getId())
                .fileKind("REGISTRATION_CERTIFICATE")
                .infraFileId(infraFileId)
                .originalName("registration.pdf")
                .mimeType("application/pdf")
                .fileSize(1024L)
                .sha256("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .status(fileStatus)
                .boundAt(LocalDateTime.of(2026, 8, 1, 9, 5))
                .boundBy(99L)
                .build();
        file.setTenantId(tenantId);
        assertEquals(1, dbFileMapper.insert(file));

        certificate.setCurrentVersionId("CURRENT".equals(versionStatus) ? version.getId() : null);
        certificate.setCurrentSnapshotId("CURRENT".equals(versionStatus) ? snapshot.getId() : null);
        certificate.setPendingVersionId("PENDING_EFFECTIVE".equals(versionStatus) ? version.getId() : null);
        assertEquals(1, certificateMapper.updateById(certificate));
        return new FormalFile(certificate.getId(), version.getId(), file.getId(), infraFileId);
    }

    private record FormalFile(Long certificateId, Long versionId, Long businessFileId, Long infraFileId) {
    }
}
