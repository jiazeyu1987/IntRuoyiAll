package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.accessrequest.DccRegistrationCertificateAccessRequestController;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.accessrequest.vo.DccRegistrationCertificateAccessRequestSubmitReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.accessrequest.DccRegistrationCertificateAccessRequestCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.accessrequest.DccRegistrationCertificateAccessRequestResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.accessrequest.DccRegistrationCertificateAccessRequestService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeService;
import jakarta.annotation.Resource;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_NOT_STAGED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_PRODUCT_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({
        DccRegistrationCertificateAccessRequestService.class,
        DccRegistrationCertificateAccessRequestServiceTest.DbTestConfiguration.class
})
class DccRegistrationCertificateAccessRequestServiceTest extends BaseDbUnitTest {

    @Resource
    private DccRegistrationCertificateAccessRequestService service;
    @Resource
    private DccRegistrationCertificateAccessRequestMapper requestMapper;
    @Resource
    private DccRegistrationCertificateAccessRequestFileMapper requestFileMapper;
    @Resource
    private DccRegistrationCertificateMapper certificateMapper;
    @Resource
    private DccRegistrationCertificateVersionMapper versionMapper;
    @Resource
    private DccRegistrationCertificateFileMapper registrationCertificateFileMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private DccProjectCodeService projectCodeService;

    @BeforeEach
    void setUp() {
        reset(projectCodeService);
    }

    @Test
    void submitDownloadRequestValidatesProjectAndPersistsRequestAndFilesAtomically() {
        FormalFixture fixture = seedFormalCertificate("ACTIVE", "CURRENT", "BOUND");
        when(projectCodeService.getProjectCode(40L)).thenReturn(projectCode(1L, 20L,
                DccProjectCodeStatusConstants.ENABLE));

        DccRegistrationCertificateAccessRequestResult result = service.submit(1L, 99L, "download-1",
                download(fixture.certificateId(), 40L, List.of(fixture.fileId())));

        assertEquals(fixture.certificateId(), result.certificateId());
        assertEquals(10L, result.ownerCompanyId());
        assertEquals("SUBMITTED", result.status());
        assertEquals(List.of(fixture.fileId()), result.businessFileIds());
        verify(projectCodeService, never()).getProjectCode(99L, 40L);
        DccRegistrationCertificateAccessRequestDO request = requestMapper.selectById(result.requestId());
        assertEquals("DOWNLOAD_FILE", request.getRequestType());
        assertEquals(40L, request.getProjectCodeId());
        assertTrue(request.getDetailJson().contains("payloadHash"));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_access_request_file "
                + "WHERE tenant_id = 1 AND request_id = ? AND business_file_id = ? "
                + "AND download_requested = TRUE AND status = 'REQUESTED'",
                result.requestId(), fixture.fileId()));
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_bpm_binding"));
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_grant"));
    }

    @Test
    void submitDownloadRequestResolvesProjectAndFileIdsFromTheCertificateWhenOmitted() {
        FormalFixture fixture = seedFormalCertificate("ACTIVE", "CURRENT", "BOUND");
        when(projectCodeService.getProjectCode(40L)).thenReturn(projectCode(1L, 20L,
                DccProjectCodeStatusConstants.ENABLE));

        DccRegistrationCertificateAccessRequestResult result = service.submit(1L, 99L, "download-auto-ids",
                download(fixture.certificateId(), null, List.of()));

        assertEquals(List.of(fixture.fileId()), result.businessFileIds());
        assertEquals(40L, requestMapper.selectById(result.requestId()).getProjectCodeId());
    }

    @Test
    void submitDownloadRequestAcceptsSpecifiedOldCertificateAndChangeApprovalFiles() {
        FormalFixture current = seedFormalCertificate("ACTIVE", "CURRENT", "BOUND");
        Long oldVersionId = seedAdditionalVersion(current.certificateId(), 2, "CERT-ACCESS-OLD", "OLD");
        Long oldFileId = seedBusinessFile("VERSION", oldVersionId, "REGISTRATION_CERTIFICATE", "old.pdf");
        Long changeId = seedAppliedChange(current.certificateId(), current.versionId());
        Long changeFileId = seedBusinessFile("CHANGE", changeId, "CHANGE_APPROVAL", "change.pdf");
        when(projectCodeService.getProjectCode(40L)).thenReturn(projectCode(1L, 20L,
                DccProjectCodeStatusConstants.ENABLE));

        DccRegistrationCertificateAccessRequestResult result = service.submit(1L, 99L, "download-old-change",
                download(current.certificateId(), 40L, List.of(oldFileId, changeFileId)));

        assertEquals(List.of(oldFileId, changeFileId), result.businessFileIds());
        assertEquals(2, count("SELECT COUNT(*) FROM dcc_registration_certificate_access_request_file "
                + "WHERE tenant_id = 1 AND request_id = ? AND download_requested = TRUE AND status = 'REQUESTED'",
                result.requestId()));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_access_request_file "
                + "WHERE tenant_id = 1 AND request_id = ? AND business_file_id = ? AND file_kind = 'CHANGE_APPROVAL'",
                result.requestId(), changeFileId));
    }

    @Test
    void viewOldCertificateRequestDoesNotRequireProjectOrFileRows() {
        FormalFixture fixture = seedFormalCertificate("EXPIRED_UNRENEWED", "OLD", "BOUND");

        DccRegistrationCertificateAccessRequestResult result = service.submit(1L, 99L, "view-old-1",
                new DccRegistrationCertificateAccessRequestCommand(
                        fixture.certificateId(), "VIEW_OLD_CERTIFICATE", "legacy lookup", null, List.of()));

        assertEquals(fixture.certificateId(), result.certificateId());
        assertEquals(List.of(), result.businessFileIds());
        verify(projectCodeService, never()).getProjectCode(40L);
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_access_request "
                + "WHERE request_key = 'view-old-1' AND project_code_id IS NULL"));
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_access_request_file "
                + "WHERE request_id = ?", result.requestId()));
    }

    @Test
    void sameKeySamePayloadReplaysExistingRequestAndDoesNotDuplicateRows() {
        FormalFixture fixture = seedFormalCertificate("ACTIVE", "CURRENT", "BOUND");
        when(projectCodeService.getProjectCode(40L)).thenReturn(projectCode(1L, 20L,
                DccProjectCodeStatusConstants.ENABLE));
        DccRegistrationCertificateAccessRequestCommand command =
                download(fixture.certificateId(), 40L, List.of(fixture.fileId()));

        Long first = service.submit(1L, 99L, "download-replay", command).requestId();
        Long second = service.submit(1L, 99L, "download-replay", command).requestId();

        assertEquals(first, second);
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_access_request "
                + "WHERE request_key = 'download-replay'"));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_access_request_file "
                + "WHERE request_id = ?", first));
    }

    @Test
    void sameKeyDifferentPayloadConflictsAndDoesNotAppendRows() {
        FormalFixture fixture = seedFormalCertificate("ACTIVE", "CURRENT", "BOUND");
        when(projectCodeService.getProjectCode(40L)).thenReturn(projectCode(1L, 20L,
                DccProjectCodeStatusConstants.ENABLE));
        service.submit(1L, 99L, "download-conflict",
                download(fixture.certificateId(), 40L, List.of(fixture.fileId())));

        ServiceException error = assertThrows(ServiceException.class, () -> service.submit(1L, 99L,
                "download-conflict", new DccRegistrationCertificateAccessRequestCommand(
                        fixture.certificateId(), "DOWNLOAD_FILE", "changed purpose", 40L, List.of(fixture.fileId()))));

        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT.getCode(), error.getCode());
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_access_request "
                + "WHERE request_key = 'download-conflict'"));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_access_request_file"));
    }

    @Test
    void replayRequiresPayloadHashFieldToMatchExactlyRatherThanSubstring() {
        FormalFixture fixture = seedFormalCertificate("ACTIVE", "CURRENT", "BOUND");
        when(projectCodeService.getProjectCode(40L)).thenReturn(projectCode(1L, 20L,
                DccProjectCodeStatusConstants.ENABLE));
        DccRegistrationCertificateAccessRequestCommand command =
                download(fixture.certificateId(), 40L, List.of(fixture.fileId()));
        Long source = service.submit(1L, 99L, "hash-source", command).requestId();
        String hash = extractPayloadHash(source);

        DccRegistrationCertificateAccessRequestDO tampered = DccRegistrationCertificateAccessRequestDO.builder()
                .ownerCompanyId(10L)
                .certificateId(fixture.certificateId())
                .requesterUserId(99L)
                .requestType("DOWNLOAD_FILE")
                .requestKey("hash-substring")
                .purpose("download for regulated business")
                .projectCodeId(40L)
                .status("SUBMITTED")
                .requestedAt(java.time.LocalDateTime.of(2026, 8, 19, 9, 0))
                .detailJson("{\"payloadHash\":\"wrong\",\"shadow\":\"" + hash + "\"}")
                .build();
        tampered.setTenantId(1L);
        assertEquals(1, requestMapper.insert(tampered));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.submit(1L, 99L, "hash-substring", command));

        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT.getCode(), error.getCode());
    }

    @Test
    void downloadProjectCodeMayBeMissingButExplicitProjectMustStayEnabledSameTenantAndSameProductBeforeInsert() {
        FormalFixture missingProjectFixture = seedFormalCertificate("ACTIVE", "CURRENT", "BOUND", null);
        DccRegistrationCertificateAccessRequestResult missingProjectResult = service.submit(1L, 99L,
                "missing-project", download(missingProjectFixture.certificateId(), null,
                        List.of(missingProjectFixture.fileId())));
        assertEquals(List.of(missingProjectFixture.fileId()), missingProjectResult.businessFileIds());
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_access_request "
                + "WHERE tenant_id = 1 AND id = ? AND project_code_id IS NULL", missingProjectResult.requestId()));
        verify(projectCodeService, never()).getProjectCode(40L);

        FormalFixture fixture = seedFormalCertificate("ACTIVE", "CURRENT", "BOUND");
        when(projectCodeService.getProjectCode(40L)).thenReturn(projectCode(1L, 20L,
                DccProjectCodeStatusConstants.DISABLE));
        ServiceException disabled = assertThrows(ServiceException.class, () -> service.submit(1L, 99L,
                "disabled-project", download(fixture.certificateId(), 40L, List.of(fixture.fileId()))));

        when(projectCodeService.getProjectCode(40L)).thenReturn(projectCode(1L, 21L,
                DccProjectCodeStatusConstants.ENABLE));
        ServiceException mismatch = assertThrows(ServiceException.class, () -> service.submit(1L, 99L,
                "product-mismatch", download(fixture.certificateId(), 40L, List.of(fixture.fileId()))));

        assertEquals(REGISTRATION_CERTIFICATE_PROJECT_CODE_DISABLED.getCode(), disabled.getCode());
        assertEquals(REGISTRATION_CERTIFICATE_PROJECT_CODE_PRODUCT_MISMATCH.getCode(), mismatch.getCode());
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_access_request "
                + "WHERE request_key IN ('disabled-project', 'product-mismatch')"));
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_access_request_file "
                + "WHERE request_id NOT IN (?)", missingProjectResult.requestId()));
    }

    @Test
    void downloadRequestAllowsUploadedCertificateWithoutProductMasterBinding() {
        FormalFixture fixture = seedFormalCertificate("ACTIVE", "CURRENT", "BOUND", 40L, null);
        when(projectCodeService.getProjectCode(40L)).thenReturn(projectCode(1L, 21L,
                DccProjectCodeStatusConstants.ENABLE));

        DccRegistrationCertificateAccessRequestResult result = service.submit(1L, 99L, "download-uploaded",
                download(fixture.certificateId(), 40L, List.of(fixture.fileId())));

        assertEquals(fixture.certificateId(), result.certificateId());
        assertEquals(List.of(fixture.fileId()), result.businessFileIds());
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_access_request "
                + "WHERE tenant_id = 1 AND id = ? AND project_code_id = 40", result.requestId()));
    }

    @Test
    void downloadFileMustBeBoundRegistrationCertificateFileForTheSameCertificate() {
        FormalFixture fixture = seedFormalCertificate("ACTIVE", "CURRENT", "STAGED");
        when(projectCodeService.getProjectCode(40L)).thenReturn(projectCode(1L, 20L,
                DccProjectCodeStatusConstants.ENABLE));

        ServiceException notBound = assertThrows(ServiceException.class, () -> service.submit(1L, 99L,
                "file-not-bound", download(fixture.certificateId(), 40L, List.of(fixture.fileId()))));

        assertEquals(REGISTRATION_CERTIFICATE_FILE_NOT_STAGED.getCode(), notBound.getCode());
        assertNoRequestRows();
    }

    @Test
    void controllerExposesApprovedSubmissionRoutePermissionAndIdempotencyHeader() throws Exception {
        RequestMapping root = DccRegistrationCertificateAccessRequestController.class.getAnnotation(RequestMapping.class);
        assertEquals("/dcc/registration-certificates/access-requests", root.value()[0]);
        Method submit = DccRegistrationCertificateAccessRequestController.class.getDeclaredMethod(
                "submit", String.class, DccRegistrationCertificateAccessRequestSubmitReqVO.class);
        assertEquals(0, submit.getAnnotation(PostMapping.class).value().length);
        PreAuthorize preAuthorize = submit.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertTrue(preAuthorize.value().contains("'dcc:registration-certificate:access-request:create'"));
        RequestHeader header = findAnnotation(submit.getParameterAnnotations()[0], RequestHeader.class);
        assertNotNull(header);
        assertEquals("Idempotency-Key", header.value());
        assertTrue(header.required());
    }

    private DccRegistrationCertificateAccessRequestCommand download(Long certificateId, Long projectCodeId,
                                                                     List<Long> businessFileIds) {
        return new DccRegistrationCertificateAccessRequestCommand(
                certificateId, "DOWNLOAD_FILE", "download for regulated business", projectCodeId, businessFileIds);
    }

    private FormalFixture seedFormalCertificate(String masterStatus, String versionStatus, String fileStatus) {
        return seedFormalCertificate(masterStatus, versionStatus, fileStatus, 40L);
    }

    private FormalFixture seedFormalCertificate(String masterStatus, String versionStatus, String fileStatus,
                                                Long projectCodeId) {
        return seedFormalCertificate(masterStatus, versionStatus, fileStatus, projectCodeId, 20L);
    }

    private FormalFixture seedFormalCertificate(String masterStatus, String versionStatus, String fileStatus,
                                                Long projectCodeId, Long productMasterId) {
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
                .certificateNo("CERT-ACCESS-001")
                .approvalDate(LocalDate.of(2020, 2, 1))
                .effectiveDate(LocalDate.of(2020, 3, 1))
                .expiryDate(LocalDate.of(2030, 3, 1))
                .classification("II")
                .categoryChanged(false)
                .status(versionStatus)
                .build();
        version.setTenantId(1L);
        assertEquals(1, versionMapper.insert(version));
        certificate.setCurrentVersionId(version.getId());
        assertEquals(1, certificateMapper.updateById(certificate));

        DccRegistrationCertificateFileDO file = DccRegistrationCertificateFileDO.builder()
                .ownerType("VERSION")
                .ownerId(version.getId())
                .fileKind("REGISTRATION_CERTIFICATE")
                .infraFileId(7001L)
                .originalName("certificate.pdf")
                .mimeType("application/pdf")
                .fileSize(128L)
                .sha256("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .status(fileStatus)
                .boundAt("BOUND".equals(fileStatus) ? java.time.LocalDateTime.of(2020, 3, 1, 8, 0) : null)
                .boundBy("BOUND".equals(fileStatus) ? 99L : null)
                .build();
        file.setTenantId(1L);
        assertEquals(1, registrationCertificateFileMapper.insert(file));
        return new FormalFixture(certificate.getId(), version.getId(), file.getId());
    }

    private Long seedAdditionalVersion(Long certificateId, int versionNo, String certificateNo, String status) {
        DccRegistrationCertificateVersionDO version = DccRegistrationCertificateVersionDO.builder()
                .certificateId(certificateId)
                .versionNo(versionNo)
                .versionType("RENEWAL_CERTIFICATE")
                .certificateNo(certificateNo)
                .approvalDate(LocalDate.of(2021, 2, 1))
                .effectiveDate(LocalDate.of(2021, 3, 1))
                .expiryDate(LocalDate.of(2026, 3, 1))
                .classification("II")
                .categoryChanged(false)
                .status(status)
                .build();
        version.setTenantId(1L);
        assertEquals(1, versionMapper.insert(version));
        return version.getId();
    }

    private Long seedAppliedChange(Long certificateId, Long sourceVersionId) {
        assertEquals(1, jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_change
                  (tenant_id, owner_company_id, certificate_id, source_version_id, source_snapshot_id,
                   resulting_snapshot_id, event_id, approval_date, selected_change_types_json,
                   selected_item_count, status, actor_id, applied_at)
                VALUES (1, 10, ?, ?, 91001, 91002, 97001, ?, '[\"PRODUCT_NAME\"]', 1, 'APPLIED', 99, ?)
                """, certificateId, sourceVersionId, LocalDate.of(2026, 8, 17),
                java.time.LocalDateTime.of(2026, 8, 17, 9, 0)));
        Long changeId = jdbcTemplate.queryForObject("""
                SELECT id FROM dcc_registration_certificate_change
                 WHERE tenant_id = 1 AND event_id = 97001
                """, Long.class);
        assertNotNull(changeId);
        return changeId;
    }

    private Long seedBusinessFile(String ownerType, Long ownerId, String fileKind, String originalName) {
        DccRegistrationCertificateFileDO file = DccRegistrationCertificateFileDO.builder()
                .ownerType(ownerType)
                .ownerId(ownerId)
                .fileKind(fileKind)
                .infraFileId(7_100L + Math.abs(System.nanoTime() % 1000L))
                .originalName(originalName)
                .mimeType("application/pdf")
                .fileSize(128L)
                .sha256("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                .status("BOUND")
                .boundAt(java.time.LocalDateTime.of(2026, 8, 17, 9, 5))
                .boundBy(99L)
                .build();
        file.setTenantId(1L);
        assertEquals(1, registrationCertificateFileMapper.insert(file));
        return file.getId();
    }

    private DccProjectCodeDO projectCode(Long tenantId, Long productMasterId, String status) {
        DccProjectCodeDO projectCode = DccProjectCodeDO.builder()
                .id(40L)
                .productMasterId(productMasterId)
                .projectCode("PRJ-001")
                .status(status)
                .build();
        projectCode.setTenantId(tenantId);
        return projectCode;
    }

    private void assertNoRequestRows() {
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_access_request"));
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_access_request_file"));
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private String extractPayloadHash(Long requestId) {
        String detailJson = requestMapper.selectById(requestId).getDetailJson();
        String marker = "\"payloadHash\":\"";
        int start = detailJson.indexOf(marker);
        assertTrue(start >= 0);
        int valueStart = start + marker.length();
        int valueEnd = detailJson.indexOf('"', valueStart);
        assertTrue(valueEnd > valueStart);
        return detailJson.substring(valueStart, valueEnd);
    }

    private static <T extends Annotation> T findAnnotation(Annotation[] annotations, Class<T> type) {
        for (Annotation annotation : annotations) {
            if (type.isInstance(annotation)) {
                return type.cast(annotation);
            }
        }
        throw new AssertionError("missing annotation " + type.getName());
    }

    private record FormalFixture(Long certificateId, Long versionId, Long fileId) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DbTestConfiguration {

        @Bean
        DccRegistrationCertificateBusinessClock registrationCertificateBusinessClock() {
            return new DccRegistrationCertificateBusinessClock(
                    Clock.fixed(Instant.parse("2026-08-19T01:00:00Z"), ZoneId.of("Asia/Shanghai")));
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }
}
