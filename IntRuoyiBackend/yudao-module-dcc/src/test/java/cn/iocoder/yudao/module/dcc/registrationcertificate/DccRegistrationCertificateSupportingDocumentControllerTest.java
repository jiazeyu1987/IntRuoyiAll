package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.supportingdocument.DccRegistrationCertificateSupportingDocumentController;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.supportingdocument.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccRegistrationCertificateSupportingDocumentControllerTest {
    @Test
    void delegatesAuthenticatedContextAndMultipartFieldsAndRequiresRenewalPermission() throws Exception {
        var service = mock(DccRegistrationCertificateSupportingDocumentService.class);
        var controller = new DccRegistrationCertificateSupportingDocumentController(service);
        var file = new MockMultipartFile("file", "contract.pdf", "application/pdf", new byte[]{1,2,3});
        var expected = new DccRegistrationCertificateSupportingDocumentResult(5L, 2L, 3L, "CE_RENEWAL_CONTRACT", "EFFECTIVE", false);
        when(service.uploadFile(7L, 9L, "key", 2L, 3L, "CE_RENEWAL_CONTRACT", file)).thenReturn(expected);
        TenantContextHolder.setTenantId(7L);
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9L);
            assertSame(expected, controller.upload(2L, "key", 3L, "CE_RENEWAL_CONTRACT", file).getData());
            verify(service).uploadFile(7L, 9L, "key", 2L, 3L, "CE_RENEWAL_CONTRACT", file);
        } finally { TenantContextHolder.clear(); }
        var method = DccRegistrationCertificateSupportingDocumentController.class.getMethod("upload", Long.class, String.class, Long.class, String.class, org.springframework.web.multipart.MultipartFile.class);
        assertEquals("@ss.hasPermission('dcc:registration-certificate:renewal:upload')", method.getAnnotation(PreAuthorize.class).value());
    }
}
