package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.accesspolicy.DccRegistrationCertificateAccessPolicyService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateReadAuditService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.DccRegistrationCertificateFilePreviewServiceImpl;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reference.DccRegistrationCertificateFileReference;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reference.DccRegistrationCertificateFileReferenceService;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import cn.iocoder.yudao.module.dcc.service.filepreview.DccOnlineFilePreviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DccRegistrationCertificateFilePreviewServiceTest {

    @Mock
    private DccRegistrationCertificateFileReferenceService referenceService;
    @Mock
    private DccRegistrationCertificateAccessPolicyService accessPolicyService;
    @Mock
    private DccOnlineFilePreviewService onlineFilePreviewService;
    @Mock
    private DccRegistrationCertificateReadAuditService readAuditService;
    @Mock
    private DccRegistrationCertificateBusinessClock businessClock;

    private DccRegistrationCertificateFilePreviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DccRegistrationCertificateFilePreviewServiceImpl(
                referenceService, accessPolicyService, onlineFilePreviewService, readAuditService, businessClock);
    }

    @Test
    void previewMetadataAuthorizesBoundBusinessFileBeforeDelegation() {
        DccRegistrationCertificateFileReference reference = reference();
        DccRequestAuditContext context = context("REQ-PREVIEW-META");
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 9, 0);
        DccControlledFilePreviewMetadataRespVO expected = new DccControlledFilePreviewMetadataRespVO();
        expected.setFileName("renewal.pdf");
        when(referenceService.requireBoundByBusinessFileId(1L, 7001L)).thenReturn(reference);
        when(businessClock.now()).thenReturn(now);
        when(onlineFilePreviewService.getPreviewMetadata(99L, 8001L, context)).thenReturn(expected);

        DccControlledFilePreviewMetadataRespVO actual = service.getPreviewMetadata(1L, 99L, 7001L, context);

        assertEquals("renewal.pdf", actual.getFileName());
        verify(accessPolicyService).assertFilePreviewAllowed(1L, 99L, 1001L, 2002L, now);
        verify(onlineFilePreviewService).getPreviewMetadata(99L, 8001L, context);
    }

    @Test
    void deniedPreviewDoesNotDelegateToGenericPreview() {
        DccRegistrationCertificateFileReference reference = reference();
        DccRequestAuditContext context = context("REQ-PREVIEW-DENIED");
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 9, 0);
        when(referenceService.requireBoundByBusinessFileId(1L, 7001L)).thenReturn(reference);
        when(referenceService.resolveByBusinessFileId(1L, 7001L)).thenReturn(Optional.of(reference));
        when(businessClock.now()).thenReturn(now);
        org.mockito.Mockito.doThrow(new ServiceException(CONTROLLED_FILE_ACCESS_DENIED))
                .when(accessPolicyService).assertFilePreviewAllowed(1L, 99L, 1001L, 2002L, now);

        assertThrows(ServiceException.class,
                () -> service.getPreviewMetadata(1L, 99L, 7001L, context));

        verify(onlineFilePreviewService, never()).getPreviewMetadata(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    private static DccRegistrationCertificateFileReference reference() {
        return new DccRegistrationCertificateFileReference(
                1L, 10L, 1001L, 2002L, 2, 7001L, 8001L,
                "renewal.pdf", "application/pdf");
    }

    private static DccRequestAuditContext context(String requestId) {
        return new DccRequestAuditContext("10.0.0.1", "JUnit", requestId);
    }
}
