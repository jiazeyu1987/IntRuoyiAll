package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.file.DccRegistrationCertificateFilePreviewController;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.DccRegistrationCertificateFileDeliveryService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.DccRegistrationCertificateFileDownloadResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.DccRegistrationCertificateFilePreviewService;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccRegistrationCertificateFileDeliveryControllerTest extends BaseMockitoUnitTest {

    private static final String DOWNLOAD_ATTEMPT_KEY_HEADER = "X-DCC-Download-Attempt-Key";

    @Mock
    private DccRegistrationCertificateFilePreviewService previewService;

    @Mock
    private DccRegistrationCertificateFileDeliveryService deliveryService;

    @InjectMocks
    private DccRegistrationCertificateFilePreviewController controller;

    @Test
    void downloadEndpoint_exposesFrozenRoutePermissionAndStableAttemptKeyHeader() throws Exception {
        RequestMapping root = DccRegistrationCertificateFilePreviewController.class
                .getAnnotation(RequestMapping.class);
        assertNotNull(root);
        assertEquals("/dcc/registration-certificates/files", root.value()[0]);

        Method download = Arrays.stream(DccRegistrationCertificateFilePreviewController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("downloadFile"))
                .findFirst()
                .orElse(null);
        assertNotNull(download, "API-019 downloadFile method must exist as a real HTTP entry");
        GetMapping mapping = download.getAnnotation(GetMapping.class);
        assertNotNull(mapping);
        assertEquals("/{businessFileId}/download", mapping.value()[0]);
        PreAuthorize preAuthorize = download.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertTrue(preAuthorize.value().contains("dcc:registration-certificate:access-request:create"));
        assertEquals(ResponseEntity.class, download.getReturnType());

        Parameter[] parameters = download.getParameters();
        PathVariable pathVariable = parameters[0].getAnnotation(PathVariable.class);
        assertNotNull(pathVariable);
        assertEquals("businessFileId", pathVariable.value());
        RequestHeader attemptKey = parameters[1].getAnnotation(RequestHeader.class);
        assertNotNull(attemptKey);
        assertEquals(DOWNLOAD_ATTEMPT_KEY_HEADER, attemptKey.value());
        assertTrue(attemptKey.required());
        assertEquals(HttpServletRequest.class, parameters[2].getType());
    }

    @Test
    void downloadEndpoint_delegatesTenantUserBusinessFileAttemptAndAuditContextAndReturnsAttachment() {
        when(deliveryService.download(eq(11L), eq(22L), eq(33L), eq("ATT-1"), any(DccRequestAuditContext.class)))
                .thenReturn(new DccRegistrationCertificateFileDownloadResult(
                        "PD可编辑.pdf", "application/pdf", "pdf".getBytes(), 44L, 33L));
        TenantContextHolder.setTenantId(11L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(22L);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("10.8.0.31");
            request.addHeader("User-Agent", "JUnit");

            var response = controller.downloadFile(33L, "ATT-1", request);

            assertEquals("application/pdf", response.getHeaders().getContentType().toString());
            assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("attachment"));
            assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("filename*="));
            assertEquals("pdf", new String(response.getBody()));
            ArgumentCaptor<DccRequestAuditContext> context = ArgumentCaptor.forClass(DccRequestAuditContext.class);
            verify(deliveryService).download(eq(11L), eq(22L), eq(33L), eq("ATT-1"), context.capture());
            assertEquals("ATT-1", context.getValue().requestId());
            assertEquals("10.8.0.31", context.getValue().sourceIp());
        } finally {
            TenantContextHolder.clear();
        }
    }
}
