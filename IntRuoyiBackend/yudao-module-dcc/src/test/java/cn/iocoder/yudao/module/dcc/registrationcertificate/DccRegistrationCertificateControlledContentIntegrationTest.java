package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.file.DccRegistrationCertificateFilePreviewController;
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
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.DccRegistrationCertificateFilePreviewService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.DccRegistrationCertificateFilePreviewServiceImpl;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reference.DccRegistrationCertificateFileReferenceServiceImpl;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import cn.iocoder.yudao.module.dcc.service.filepreview.DccOnlineFilePreviewService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({
        DccRegistrationCertificateFilePreviewServiceImpl.class,
        DccRegistrationCertificateFileReferenceServiceImpl.class,
        DccRegistrationCertificateReadAuditService.class,
        DccRegistrationCertificateBusinessClock.class
})
class DccRegistrationCertificateControlledContentIntegrationTest extends BaseDbUnitTest {

    @Resource
    private DccRegistrationCertificateFilePreviewService previewService;
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
    private DccOnlineFilePreviewService onlineFilePreviewService;

    @Test
    void controllerFreezesApi018BusinessFileRoutesAndQueryCurrentPermission() throws Exception {
        RequestMapping mapping = DccRegistrationCertificateFilePreviewController.class.getAnnotation(RequestMapping.class);
        assertEquals("/dcc/registration-certificates/files", mapping.value()[0]);
        Method metadata = DccRegistrationCertificateFilePreviewController.class
                .getMethod("getPreviewMetadata", Long.class, jakarta.servlet.http.HttpServletRequest.class);
        assertEquals("/{businessFileId}/preview-metadata", metadata.getAnnotation(GetMapping.class).value()[0]);
        assertTrue(metadata.getAnnotation(PreAuthorize.class).value()
                .contains("dcc:registration-certificate:query-current"));
        Method preview = DccRegistrationCertificateFilePreviewController.class
                .getMethod("previewFile", Long.class, String.class, String.class, String.class, String.class,
                        String.class, jakarta.servlet.http.HttpServletRequest.class);
        assertEquals("/{businessFileId}/preview", preview.getAnnotation(GetMapping.class).value()[0]);
        assertTrue(preview.getAnnotation(PreAuthorize.class).value()
                .contains("dcc:registration-certificate:query-current"));
    }

    @Test
    void previewEndpointUsesBusinessFileIdAndDelegatesOnlyResolvedInfraFile() {
        FormalFile file = seedFormalFile(1L, 10L, "ACTIVE", "CURRENT", "BOUND", 92001L);
        DccControlledFilePreviewMetadataRespVO metadata = new DccControlledFilePreviewMetadataRespVO();
        metadata.setFileName("registration.pdf");
        when(onlineFilePreviewService.getPreviewMetadata(99L, 92001L,
                context("REQ-RC-META-001"))).thenReturn(metadata);

        DccControlledFilePreviewMetadataRespVO response = previewService.getPreviewMetadata(
                1L, 99L, file.businessFileId(), context("REQ-RC-META-001"));

        assertEquals("registration.pdf", response.getFileName());
        verify(onlineFilePreviewService).getPreviewMetadata(99L, 92001L, context("REQ-RC-META-001"));
        assertThrows(ServiceException.class,
                () -> previewService.getPreviewMetadata(1L, 99L, 92001L, context("REQ-NAKED-INFRA")));
    }

    @Test
    void nonCurrentBusinessFileIsDeniedBeforeGenericPreviewService() {
        FormalFile file = seedFormalFile(1L, 10L, "ACTIVE", "OLD", "BOUND", 92002L);

        ServiceException error = assertThrows(ServiceException.class,
                () -> previewService.getPreviewMetadata(1L, 99L, file.businessFileId(), context("REQ-OLD-FILE")));

        assertEquals(CONTROLLED_FILE_ACCESS_DENIED.getCode(), error.getCode());
        assertNotNull(auditMapper.selectByTenantIdAndEventKey(
                1L, "REQ-OLD-FILE:PREVIEW:REQUESTED:" + file.certificateId() + ":FAILURE"));
        verify(onlineFilePreviewService, never()).getPreviewMetadata(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
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
                .certificateNo("CERT-CONTROLLER-" + infraFileId)
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
                .sha256("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                .status(fileStatus)
                .boundAt(LocalDateTime.of(2026, 8, 1, 9, 5))
                .boundBy(99L)
                .build();
        file.setTenantId(tenantId);
        assertEquals(1, dbFileMapper.insert(file));

        certificate.setCurrentVersionId("CURRENT".equals(versionStatus) ? version.getId() : null);
        certificate.setCurrentSnapshotId("CURRENT".equals(versionStatus) ? snapshot.getId() : null);
        assertEquals(1, certificateMapper.updateById(certificate));
        return new FormalFile(certificate.getId(), version.getId(), file.getId(), infraFileId);
    }

    private static DccRequestAuditContext context(String requestId) {
        return new DccRequestAuditContext("10.0.0.1", "JUnit", requestId);
    }

    private record FormalFile(Long certificateId, Long versionId, Long businessFileId, Long infraFileId) {
    }
}
