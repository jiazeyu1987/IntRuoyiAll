package cn.iocoder.yudao.module.dcc.service.filepreview;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.filepreview.DccOnlineFilePreviewController;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkOverlayRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkRespVO;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBinary;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestHeader;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccOnlineFilePreviewControllerTest extends BaseMockitoUnitTest {

    private static final String PREVIEW_WATERMARK_HEADER = "X-DCC-Preview-Watermark";
    private static final String ACCESS_EVENT_CODE_HEADER = "X-DCC-Access-Event-Code";
    private static final String VIEWER_TOKEN_HEADER = "X-DCC-Viewer-Token";
    private static final String VIEWER_TOKEN_ID_HEADER = "X-DCC-Viewer-Token-Id";
    private static final String VIEWER_TOKEN_NONCE_HEADER = "X-DCC-Viewer-Token-Nonce";
    private static final String WATERMARK_TRACE_CODE_HEADER = "X-DCC-Watermark-Trace-Code";

    @Mock
    private DccOnlineFilePreviewService previewService;

    @InjectMocks
    private DccOnlineFilePreviewController controller;

    @Test
    void previewFile_readsViewerContextFromHeadersNotQueryParams() throws NoSuchMethodException {
        Method method = DccOnlineFilePreviewController.class.getMethod("previewFile",
                Long.class, String.class, String.class, String.class, String.class, String.class,
                HttpServletRequest.class);
        Parameter[] parameters = method.getParameters();

        assertHeaderParameter(parameters[1], VIEWER_TOKEN_HEADER);
        assertHeaderParameter(parameters[2], ACCESS_EVENT_CODE_HEADER);
        assertHeaderParameter(parameters[3], WATERMARK_TRACE_CODE_HEADER);
        assertHeaderParameter(parameters[4], VIEWER_TOKEN_ID_HEADER);
        assertHeaderParameter(parameters[5], VIEWER_TOKEN_NONCE_HEADER);
    }

    @Test
    void previewEndpointsUseExistingDccOrEdhrReadPermissions() throws NoSuchMethodException {
        Method metadataMethod = DccOnlineFilePreviewController.class.getMethod("getPreviewMetadata",
                Long.class, HttpServletRequest.class);
        Method previewMethod = DccOnlineFilePreviewController.class.getMethod("previewFile",
                Long.class, String.class, String.class, String.class, String.class, String.class,
                HttpServletRequest.class);

        assertExistingReadPermission(metadataMethod);
        assertExistingReadPermission(previewMethod);
    }

    @Test
    void previewFileDelegatesToServiceAndExposesWatermarkHeaders() {
        when(previewService.readPreviewFile(99L, 7001L,
                "viewer-token", "AE-ONLINE-1", "WM-ONLINE-1", "VT-1", "VN-1",
                auditContext("AE-ONLINE-1")))
                .thenReturn(new DccControlledFileBinary(
                        "report.pdf",
                        "application/pdf",
                        "%PDF-1.7".getBytes(),
                        watermark()));

        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock =
                     mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);

            ResponseEntity<byte[]> response = controller.previewFile(7001L,
                    "viewer-token", "AE-ONLINE-1", "WM-ONLINE-1", "VT-1", "VN-1",
                    auditRequest());

            assertEquals("application/pdf", response.getHeaders().getContentType().toString());
            assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("inline"));
            assertTrue(response.getHeaders().containsKey(PREVIEW_WATERMARK_HEADER));
            assertEquals("AE-ONLINE-1", response.getHeaders().getFirst(ACCESS_EVENT_CODE_HEADER));
            assertEquals(
                    PREVIEW_WATERMARK_HEADER + "," + ACCESS_EVENT_CODE_HEADER,
                    response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
            verify(previewService).readPreviewFile(99L, 7001L,
                    "viewer-token", "AE-ONLINE-1", "WM-ONLINE-1", "VT-1", "VN-1",
                    auditContext("AE-ONLINE-1"));
        }
    }

    private void assertHeaderParameter(Parameter parameter, String expectedHeaderName) {
        RequestHeader header = parameter.getAnnotation(RequestHeader.class);
        assertNotNull(header, "preview context must come from request headers");
        assertEquals(expectedHeaderName, header.value());
    }

    private void assertExistingReadPermission(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "preview endpoint must be protected by existing permissions");
        String value = preAuthorize.value();
        assertTrue(value.contains("dcc:controlled-file:query"), value);
        assertTrue(value.contains("mes:pro-edhr-batch-execution:query"), value);
        assertTrue(value.contains("mes:pro-edhr-batch-execution:update"), value);
    }

    private MockHttpServletRequest auditRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.8.0.31");
        request.addHeader("User-Agent", "JUnit");
        return request;
    }

    private DccRequestAuditContext auditContext(String requestId) {
        return new DccRequestAuditContext("10.8.0.31", "JUnit", requestId);
    }

    private DccControlledPreviewWatermarkRespVO watermark() {
        return DccControlledPreviewWatermarkRespVO.builder()
                .label("在线预览")
                .text("在线预览 | report.pdf")
                .actorName("Quality User")
                .actorAccount("quality.user")
                .timestamp("2026-07-21 10:00:00")
                .purpose("preview")
                .overlay(DccControlledPreviewWatermarkOverlayRespVO.builder()
                        .textColor("#6b7280")
                        .opacity(0.18D)
                        .rotationDeg(-24)
                        .gapX(260)
                        .gapY(180)
                        .fontSize(18)
                        .build())
                .build();
    }
}
