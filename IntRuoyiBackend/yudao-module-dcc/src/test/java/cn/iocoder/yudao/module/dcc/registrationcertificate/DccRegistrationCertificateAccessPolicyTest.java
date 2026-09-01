package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateGrantDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateGrantMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.accesspolicy.DccRegistrationCertificateAccessPolicyService;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_EXPIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_REVOKED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.APPROVER_ROLE_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@Import({DccRegistrationCertificateAccessPolicyService.class,
        DccRegistrationCertificateAccessPolicyTest.JdbcTestConfiguration.class})
class DccRegistrationCertificateAccessPolicyTest extends BaseDbUnitTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class JdbcTestConfiguration {
        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }

    @Resource
    private DccRegistrationCertificateAccessPolicyService accessPolicyService;
    @Resource
    private DccRegistrationCertificateMapper certificateMapper;
    @Resource
    private DccRegistrationCertificateVersionMapper versionMapper;
    @Autowired
    private DccRegistrationCertificateFileMapper fileMapper;
    @Resource
    private DccRegistrationCertificateGrantMapper grantMapper;
    @MockitoBean
    private MdmCompanyScopeApi companyScopeApi;
    @MockitoBean
    private PermissionApi permissionApi;

    @BeforeEach
    void setUp() {
        reset(companyScopeApi, permissionApi);
    }

    @Test
    void currentPreviewRequiresCompanyScopeButNoGrant() {
        FormalFixture fixture = seedFormal("ACTIVE", "CURRENT", "BOUND");

        accessPolicyService.assertCurrentPreviewAllowed(1L, 99L, fixture.certificateId());

        verify(companyScopeApi).validateUserCompanyAccess(99L, 10L);
    }

    @Test
    void currentPreviewRejectsOldCertificateWithoutGrant() {
        FormalFixture fixture = seedFormal("EXPIRED_UNRENEWED", "OLD", "BOUND");

        ServiceException denied = assertThrows(ServiceException.class,
                () -> accessPolicyService.assertCurrentPreviewAllowed(1L, 99L, fixture.certificateId()));

        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID.getCode(), denied.getCode());
    }

    @Test
    void filePreviewAllowsCurrentAndPendingVersionsWithinCompanyScope() {
        FormalFixture current = seedFormal("ACTIVE", "CURRENT", "BOUND");
        FormalFixture pending = seedFormal("ACTIVE", "PENDING_EFFECTIVE", "BOUND");
        LocalDateTime viewedAt = LocalDateTime.of(2026, 8, 31, 9, 0);

        accessPolicyService.assertFilePreviewAllowed(
                1L, 99L, current.certificateId(), current.versionId(), viewedAt);
        accessPolicyService.assertFilePreviewAllowed(
                1L, 99L, pending.certificateId(), pending.versionId(), viewedAt);

        verify(companyScopeApi, times(2)).validateUserCompanyAccess(99L, 10L);
    }

    @Test
    void filePreviewRequiresOldViewGrantForOldVersion() {
        FormalFixture fixture = seedFormal("EXPIRED_UNRENEWED", "OLD", "BOUND");
        LocalDateTime grantedAt = LocalDateTime.of(2026, 8, 31, 9, 0);

        assertThrows(ServiceException.class, () -> accessPolicyService.assertFilePreviewAllowed(
                1L, 99L, fixture.certificateId(), fixture.versionId(), grantedAt));
        seedGrant(fixture, null, "VIEW_OLD_CERTIFICATE", "ACTIVE", grantedAt, grantedAt.plusHours(24));

        accessPolicyService.assertFilePreviewAllowed(
                1L, 99L, fixture.certificateId(), fixture.versionId(), grantedAt.plusHours(1));
        verify(companyScopeApi).validateUserCompanyAccess(99L, 10L);
    }

    @Test
    void filePreviewRejectsVoidedVersion() {
        FormalFixture fixture = seedFormal("ACTIVE", "VOIDED", "BOUND");

        ServiceException denied = assertThrows(ServiceException.class,
                () -> accessPolicyService.assertFilePreviewAllowed(
                        1L, 99L, fixture.certificateId(), fixture.versionId(),
                        LocalDateTime.of(2026, 8, 31, 9, 0)));

        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID.getCode(), denied.getCode());
    }

    @Test
    void oldViewGrantIsValidBeforeTwentyFourHoursAndInvalidAtExactExpiry() {
        FormalFixture fixture = seedFormal("EXPIRED_UNRENEWED", "OLD", "BOUND");
        LocalDateTime grantedAt = LocalDateTime.of(2026, 8, 19, 9, 0);
        seedGrant(fixture, null, "VIEW_OLD_CERTIFICATE", "ACTIVE", grantedAt, grantedAt.plusHours(24));

        accessPolicyService.assertOldViewAllowed(1L, 99L, fixture.certificateId(), grantedAt.plusHours(24).minusSeconds(1));
        ServiceException expired = assertThrows(ServiceException.class,
                () -> accessPolicyService.assertOldViewAllowed(1L, 99L, fixture.certificateId(), grantedAt.plusHours(24)));

        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_GRANT_EXPIRED.getCode(), expired.getCode());
    }

    @Test
    void oldViewAllowsRegistrationManagerRoleWithoutGrant() {
        FormalFixture fixture = seedFormal("EXPIRED_UNRENEWED", "OLD", "BOUND");
        LocalDateTime viewedAt = LocalDateTime.of(2026, 8, 30, 10, 0);
        when(permissionApi.hasAnyRolesOrSuperAdmin(99L, APPROVER_ROLE_CODE)).thenReturn(true);

        accessPolicyService.assertOldViewAllowed(1L, 99L, fixture.certificateId(), viewedAt);

        verify(permissionApi).hasAnyRolesOrSuperAdmin(99L, APPROVER_ROLE_CODE);
        verify(companyScopeApi).validateUserCompanyAccess(99L, 10L);
    }

    @Test
    void oldViewRegistrationManagerRoleStillRequiresCompanyScope() {
        FormalFixture fixture = seedFormal("EXPIRED_UNRENEWED", "OLD", "BOUND");
        LocalDateTime viewedAt = LocalDateTime.of(2026, 8, 30, 10, 0);
        when(permissionApi.hasAnyRolesOrSuperAdmin(99L, APPROVER_ROLE_CODE)).thenReturn(true);
        doThrow(new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID))
                .when(companyScopeApi).validateUserCompanyAccess(99L, 10L);

        ServiceException denied = assertThrows(ServiceException.class,
                () -> accessPolicyService.assertOldViewAllowed(1L, 99L, fixture.certificateId(), viewedAt));

        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID.getCode(), denied.getCode());
        verify(permissionApi).hasAnyRolesOrSuperAdmin(99L, APPROVER_ROLE_CODE);
        verify(companyScopeApi).validateUserCompanyAccess(99L, 10L);
    }

    @Test
    void downloadGrantMustMatchFileUserAndLiveCompanyScope() {
        FormalFixture fixture = seedFormal("ACTIVE", "CURRENT", "BOUND");
        LocalDateTime grantedAt = LocalDateTime.of(2026, 8, 19, 9, 0);
        seedGrant(fixture, fixture.fileId(), "DOWNLOAD", "ACTIVE", grantedAt, grantedAt.plusHours(24));

        accessPolicyService.assertDownloadAllowed(1L, 99L, fixture.fileId(), grantedAt.plusHours(1));
        doThrow(new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID))
                .when(companyScopeApi).validateUserCompanyAccess(99L, 10L);
        ServiceException denied = assertThrows(ServiceException.class,
                () -> accessPolicyService.assertDownloadAllowed(1L, 99L, fixture.fileId(), grantedAt.plusHours(1)));

        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID.getCode(), denied.getCode());
    }

    @Test
    void newestActiveDownloadGrantIsSelectedBeforeOlderGrant() {
        FormalFixture fixture = seedFormal("ACTIVE", "CURRENT", "BOUND");
        LocalDateTime firstGrantedAt = LocalDateTime.of(2026, 8, 19, 9, 0);
        seedGrant(fixture, fixture.fileId(), "DOWNLOAD", "ACTIVE", firstGrantedAt, firstGrantedAt.plusHours(24));
        DccRegistrationCertificateGrantDO newestGrant = seedGrant(
                fixture, fixture.fileId(), "DOWNLOAD", "ACTIVE", firstGrantedAt.plusHours(1),
                firstGrantedAt.plusHours(25));

        DccRegistrationCertificateGrantDO selected = accessPolicyService.requireDownloadGrant(
                1L, 99L, fixture.fileId(), firstGrantedAt.plusHours(2));

        assertEquals(newestGrant.getId(), selected.getId());
    }

    @Test
    void revokedOrVoidedFactsInvalidateGrantImmediately() {
        FormalFixture revokedFixture = seedFormal("ACTIVE", "CURRENT", "BOUND");
        LocalDateTime grantedAt = LocalDateTime.of(2026, 8, 19, 9, 0);
        seedGrant(revokedFixture, revokedFixture.fileId(), "DOWNLOAD", "REVOKED", grantedAt, grantedAt.plusHours(24));
        ServiceException revoked = assertThrows(ServiceException.class,
                () -> accessPolicyService.assertDownloadAllowed(1L, 99L, revokedFixture.fileId(), grantedAt.plusHours(1)));
        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_GRANT_REVOKED.getCode(), revoked.getCode());

        FormalFixture voidedFixture = seedFormal("VOIDED", "CURRENT", "BOUND");
        seedGrant(voidedFixture, voidedFixture.fileId(), "DOWNLOAD", "ACTIVE", grantedAt, grantedAt.plusHours(24));
        ServiceException voided = assertThrows(ServiceException.class,
                () -> accessPolicyService.assertDownloadAllowed(1L, 99L, voidedFixture.fileId(), grantedAt.plusHours(1)));
        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID.getCode(), voided.getCode());
    }

    private FormalFixture seedFormal(String masterStatus, String versionStatus, String fileStatus) {
        DccRegistrationCertificateDO certificate = DccRegistrationCertificateDO.builder()
                .ownerCompanyId(10L).productMasterId(20L).projectCodeId(40L)
                .firstObtainedDate(java.time.LocalDate.of(2020, 1, 1))
                .status(masterStatus).rowVersion(1).build();
        certificate.setTenantId(1L);
        assertEquals(1, certificateMapper.insert(certificate));
        DccRegistrationCertificateVersionDO version = DccRegistrationCertificateVersionDO.builder()
                .certificateId(certificate.getId()).versionNo(1).versionType("INITIAL_CERTIFICATE")
                .certificateNo("CERT-POLICY-001").approvalDate(java.time.LocalDate.of(2020, 2, 1))
                .effectiveDate(java.time.LocalDate.of(2020, 3, 1)).expiryDate(java.time.LocalDate.of(2030, 3, 1))
                .classification("II").categoryChanged(false).status(versionStatus).build();
        version.setTenantId(1L);
        assertEquals(1, versionMapper.insert(version));
        if ("CURRENT".equals(versionStatus)) {
            certificate.setCurrentVersionId(version.getId());
            assertEquals(1, certificateMapper.updateById(certificate));
        } else if ("PENDING_EFFECTIVE".equals(versionStatus)) {
            certificate.setPendingVersionId(version.getId());
            assertEquals(1, certificateMapper.updateById(certificate));
        }
        DccRegistrationCertificateFileDO file = DccRegistrationCertificateFileDO.builder()
                .ownerType("VERSION").ownerId(version.getId()).fileKind("REGISTRATION_CERTIFICATE")
                .infraFileId(8101L).originalName("policy.pdf").mimeType("application/pdf")
                .fileSize(128L).sha256("dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd")
                .status(fileStatus).boundAt(LocalDateTime.of(2020, 3, 1, 8, 0)).boundBy(77L).build();
        file.setTenantId(1L);
        assertEquals(1, fileMapper.insert(file));
        return new FormalFixture(certificate.getId(), version.getId(), file.getId());
    }

    private DccRegistrationCertificateGrantDO seedGrant(FormalFixture fixture, Long businessFileId, String type,
                                                        String status, LocalDateTime grantedAt,
                                                        LocalDateTime expiresAt) {
        long requestId = 9001L + Math.abs(System.nanoTime() % 1_000_000L);
        Long requestFileId = businessFileId == null ? null : requestId;
        DccRegistrationCertificateGrantDO grant = DccRegistrationCertificateGrantDO.builder()
                .requestId(requestId).requestFileId(requestFileId)
                .ownerCompanyId(10L).certificateId(fixture.certificateId()).businessFileId(businessFileId)
                .granteeUserId(99L).grantType(type).grantKey(type + "-" + fixture.certificateId() + "-" + System.nanoTime())
                .status(status).grantedAt(grantedAt).expiresAt(expiresAt)
                .revokedAt("REVOKED".equals(status) ? grantedAt.plusHours(1) : null)
                .revokedBy("REVOKED".equals(status) ? 77L : null)
                .revokeReason("REVOKED".equals(status) ? "manual revoke" : null)
                .detailJson("{}").build();
        grant.setTenantId(1L);
        assertEquals(1, grantMapper.insert(grant));
        return grant;
    }

    private record FormalFixture(Long certificateId, Long versionId, Long fileId) {
    }
}
