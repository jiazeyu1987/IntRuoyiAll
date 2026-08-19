package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateGrantDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateGrantMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.grant.DccRegistrationCertificateGrantService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_REQUEST_STATUS_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(DccRegistrationCertificateGrantService.class)
class DccRegistrationCertificateGrantServiceTest extends BaseDbUnitTest {

    @Resource
    private DccRegistrationCertificateGrantService grantService;
    @Resource
    private DccRegistrationCertificateAccessRequestMapper requestMapper;
    @Resource
    private DccRegistrationCertificateAccessRequestFileMapper requestFileMapper;
    @Resource
    private DccRegistrationCertificateGrantMapper grantMapper;
    @Resource
    private DccRegistrationCertificateMapper certificateMapper;
    @Resource
    private DccRegistrationCertificateVersionMapper versionMapper;
    @Autowired
    private DccRegistrationCertificateFileMapper fileMapper;

    @Test
    void approvedOldViewRequestCreatesTwentyFourHourViewGrantAndReplays() {
        FormalFixture fixture = seedFormal("EXPIRED_UNRENEWED", "OLD", "BOUND");
        Long requestId = seedRequest(fixture.certificateId(), "VIEW_OLD_CERTIFICATE", "APPROVED", null);
        LocalDateTime approvedAt = LocalDateTime.of(2026, 8, 19, 9, 0);

        List<DccRegistrationCertificateGrantDO> first = grantService.createGrantsForApprovedRequest(
                1L, 77L, requestId, "approval-view-1", approvedAt);
        List<DccRegistrationCertificateGrantDO> replay = grantService.createGrantsForApprovedRequest(
                1L, 77L, requestId, "approval-view-1", approvedAt);

        assertEquals(1, first.size());
        assertEquals(first.get(0).getId(), replay.get(0).getId());
        DccRegistrationCertificateGrantDO grant = first.get(0);
        assertEquals("VIEW_OLD_CERTIFICATE", grant.getGrantType());
        assertEquals("ACTIVE", grant.getStatus());
        assertEquals(approvedAt.plusHours(24), grant.getExpiresAt());
        assertEquals(null, grant.getRequestFileId());
        assertEquals(1, grantMapper.selectByRequest(1L, requestId).size());
    }

    @Test
    void approvedDownloadRequestCreatesOnlyFileScopedGrantsAndMarksRequestFilesGranted() {
        FormalFixture fixture = seedFormal("ACTIVE", "CURRENT", "BOUND");
        Long requestId = seedRequest(fixture.certificateId(), "DOWNLOAD_FILE", "APPROVED", 40L);
        Long requestFileId = seedRequestFile(requestId, fixture.fileId(), true, "APPROVED");
        LocalDateTime approvedAt = LocalDateTime.of(2026, 8, 19, 9, 0);

        List<DccRegistrationCertificateGrantDO> grants = grantService.createGrantsForApprovedRequest(
                1L, 77L, requestId, "approval-download-1", approvedAt);

        assertEquals(1, grants.size());
        DccRegistrationCertificateGrantDO grant = grants.get(0);
        assertEquals("DOWNLOAD", grant.getGrantType());
        assertEquals(requestFileId, grant.getRequestFileId());
        assertEquals(fixture.fileId(), grant.getBusinessFileId());
        assertEquals(approvedAt.plusHours(24), grant.getExpiresAt());
        assertEquals("GRANTED", requestFileMapper.selectById(requestFileId).getStatus());
    }

    @Test
    void approvedDownloadRequestPromotesRequestedFilesFromNativeApprovalToGranted() {
        FormalFixture fixture = seedFormal("ACTIVE", "CURRENT", "BOUND");
        Long requestId = seedRequest(fixture.certificateId(), "DOWNLOAD_FILE", "APPROVED", 40L);
        Long requestFileId = seedRequestFile(requestId, fixture.fileId(), true, "REQUESTED");
        LocalDateTime approvedAt = LocalDateTime.of(2026, 8, 19, 9, 0);

        List<DccRegistrationCertificateGrantDO> grants = assertDoesNotThrow(() ->
                grantService.createGrantsForApprovedRequest(
                        1L, 77L, requestId, "approval-download-requested-file", approvedAt));

        assertEquals(1, grants.size());
        DccRegistrationCertificateGrantDO grant = grants.get(0);
        assertEquals(requestFileId, grant.getRequestFileId());
        assertEquals(fixture.fileId(), grant.getBusinessFileId());
        assertEquals("GRANTED", requestFileMapper.selectById(requestFileId).getStatus());
    }

    @Test
    void nonApprovedRequestOrRejectedFileDoesNotCreatePartialGrant() {
        FormalFixture fixture = seedFormal("ACTIVE", "CURRENT", "BOUND");
        Long submittedRequest = seedRequest(fixture.certificateId(), "VIEW_OLD_CERTIFICATE", "SUBMITTED", null);
        ServiceException requestError = assertThrows(ServiceException.class,
                () -> grantService.createGrantsForApprovedRequest(1L, 77L, submittedRequest,
                        "approval-submitted", LocalDateTime.of(2026, 8, 19, 9, 0)));
        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_STATUS_INVALID.getCode(), requestError.getCode());

        Long requestId = seedRequest(fixture.certificateId(), "DOWNLOAD_FILE", "APPROVED", 40L);
        seedRequestFile(requestId, fixture.fileId(), true, "REJECTED");
        ServiceException fileError = assertThrows(ServiceException.class,
                () -> grantService.createGrantsForApprovedRequest(1L, 77L, requestId,
                        "approval-rejected-file", LocalDateTime.of(2026, 8, 19, 9, 0)));
        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_STATUS_INVALID.getCode(), fileError.getCode());
        assertEquals(0, grantMapper.selectByRequest(1L, requestId).size());
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
                .certificateNo("CERT-GRANT-001").approvalDate(java.time.LocalDate.of(2020, 2, 1))
                .effectiveDate(java.time.LocalDate.of(2020, 3, 1)).expiryDate(java.time.LocalDate.of(2030, 3, 1))
                .classification("II").categoryChanged(false).status(versionStatus).build();
        version.setTenantId(1L);
        assertEquals(1, versionMapper.insert(version));
        DccRegistrationCertificateFileDO file = DccRegistrationCertificateFileDO.builder()
                .ownerType("VERSION").ownerId(version.getId()).fileKind("REGISTRATION_CERTIFICATE")
                .infraFileId(8001L).originalName("grant.pdf").mimeType("application/pdf")
                .fileSize(128L).sha256("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                .status(fileStatus).boundAt(LocalDateTime.of(2020, 3, 1, 8, 0)).boundBy(77L).build();
        file.setTenantId(1L);
        assertEquals(1, fileMapper.insert(file));
        return new FormalFixture(certificate.getId(), version.getId(), file.getId());
    }

    private Long seedRequest(Long certificateId, String type, String status, Long projectCodeId) {
        DccRegistrationCertificateAccessRequestDO request = DccRegistrationCertificateAccessRequestDO.builder()
                .ownerCompanyId(10L).certificateId(certificateId).requesterUserId(99L)
                .requestType(type).requestKey(type + "-" + status + "-" + certificateId + "-" + System.nanoTime())
                .purpose("approved access").projectCodeId(projectCodeId).status(status)
                .requestedAt(LocalDateTime.of(2026, 8, 19, 8, 0)).completedAt(LocalDateTime.of(2026, 8, 19, 8, 30))
                .detailJson("{}").build();
        request.setTenantId(1L);
        assertEquals(1, requestMapper.insert(request));
        return request.getId();
    }

    private Long seedRequestFile(Long requestId, Long businessFileId, boolean downloadRequested, String status) {
        DccRegistrationCertificateAccessRequestFileDO requestFile = DccRegistrationCertificateAccessRequestFileDO.builder()
                .requestId(requestId).businessFileId(businessFileId).fileKind("REGISTRATION_CERTIFICATE")
                .downloadRequested(downloadRequested).status(status).detailJson("{}").build();
        requestFile.setTenantId(1L);
        assertEquals(1, requestFileMapper.insert(requestFile));
        return requestFile.getId();
    }

    private record FormalFixture(Long certificateId, Long versionId, Long fileId) {
    }
}
