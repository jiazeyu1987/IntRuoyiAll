package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePublishReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkOverlayRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkRespVO;
import cn.iocoder.yudao.module.dcc.service.download.DccDownloadFileBinary;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DOWNLOAD_REQUEST_ID_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFilePreviewDownloadApiTest extends BaseMockitoUnitTest {

    private static final String PREVIEW_WATERMARK_HEADER = "X-DCC-Preview-Watermark";
    private static final String ACCESS_EVENT_CODE_HEADER = "X-DCC-Access-Event-Code";
    private static final String VIEWER_TOKEN_HEADER = "X-DCC-Viewer-Token";
    private static final String VIEWER_TOKEN_ID_HEADER = "X-DCC-Viewer-Token-Id";
    private static final String VIEWER_TOKEN_NONCE_HEADER = "X-DCC-Viewer-Token-Nonce";
    private static final String WATERMARK_TRACE_CODE_HEADER = "X-DCC-Watermark-Trace-Code";

    @Mock
    private DccControlledFileWorkflowService workflowService;
    @Mock
    private DccControlledFileFinalizationService finalizationService;
    @Mock
    private DccControlledFileUploadService uploadService;
    @Mock
    private DccControlledFileQueryService queryService;
    @Mock
    private DccControlledFileObsoleteService obsoleteService;
    @Mock
    private DccControlledFilePublishService publishService;
    @Mock
    private DccTrainingAssignmentAckService trainingAssignmentAckService;

    @InjectMocks
    private DccControlledFileController controller;

    @Test
    void previewControlledFile_readsViewerContextFromHeadersNotQueryParams() throws NoSuchMethodException {
        Method method = DccControlledFileController.class.getMethod("previewControlledFile",
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
    void previewEndpoints_reuseQueryPermissionInsteadOfStandalonePreviewPermission() throws NoSuchMethodException {
        Method previewMethod = DccControlledFileController.class.getMethod("previewControlledFile",
                Long.class, String.class, String.class, String.class, String.class, String.class,
                HttpServletRequest.class);
        Method previewMetadataMethod = DccControlledFileController.class.getMethod("getPreviewMetadata",
                Long.class, HttpServletRequest.class);

        assertUsesMergedReadPermission(previewMethod);
        assertUsesMergedReadPermission(previewMetadataMethod);
    }

    @Test
    void previewControlledFile_delegatesToQueryService() {
        when(queryService.readPreviewFile(99L, 900L,
                "viewer-token", "AE-20260528-0001", "WM-20260528-0001", "VT-1", "VN-1",
                auditContext("AE-20260528-0001")))
                .thenReturn(new DccControlledFileBinary(
                        "sample.pdf",
                        "application/pdf",
                        "pdf".getBytes(),
                        DccControlledPreviewWatermarkRespVO.builder()
                                .label("受控预览")
                                .text("受控预览 sample.pdf")
                                .actorName("Quality User")
                                .actorAccount("quality.user")
                                .timestamp("2026-05-16 12:00:00")
                                .purpose("preview")
                                .overlay(DccControlledPreviewWatermarkOverlayRespVO.builder()
                                        .textColor("#6b7280")
                                        .opacity(0.18D)
                                        .rotationDeg(-24)
                                        .gapX(260)
                                        .gapY(180)
                                        .fontSize(18)
                                        .build())
                                .build()));

        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);

            ResponseEntity<byte[]> response = controller.previewControlledFile(900L,
                    "viewer-token", "AE-20260528-0001", "WM-20260528-0001", "VT-1", "VN-1",
                    auditRequest());

            assertEquals("application/pdf", response.getHeaders().getContentType().toString());
            assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("inline"));
            assertTrue(response.getHeaders().containsKey(PREVIEW_WATERMARK_HEADER));
            assertEquals("AE-20260528-0001", response.getHeaders().getFirst(ACCESS_EVENT_CODE_HEADER));
            assertEquals(
                    PREVIEW_WATERMARK_HEADER + "," + ACCESS_EVENT_CODE_HEADER,
                    response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
            verify(queryService).readPreviewFile(99L, 900L,
                    "viewer-token", "AE-20260528-0001", "WM-20260528-0001", "VT-1", "VN-1",
                    auditContext("AE-20260528-0001"));
        }
    }

    @Test
    void downloadControlledFile_requiresDownloadRequestIdBeforeDelegating() {
        assertServiceException(() -> controller.downloadControlledFile(901L, true, " ", auditRequest()),
                DCC_DOWNLOAD_REQUEST_ID_REQUIRED);

        verify(queryService, never()).readDownloadFile(any(), any(), any(), any(), any());
    }

    @Test
    void downloadControlledFile_delegatesDownloadRequestIdToQueryServiceAndExposesEvidenceHeaders() {
        when(queryService.readDownloadFile(eq(99L), eq(901L), eq(true), eq("DR-20260528-0001"),
                any(DccRequestAuditContext.class)))
                .thenReturn(new DccDownloadFileBinary(
                        "sample.pdf",
                        "application/pdf",
                        "%PDF-1.7".getBytes(),
                        "DR-20260528-0001",
                        "AE-20260528-0001",
                        null,
                        null,
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        null));

        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);

            ResponseEntity<byte[]> response = controller.downloadControlledFile(901L, true,
                    "DR-20260528-0001", auditRequest());

            assertEquals("application/pdf", response.getHeaders().getContentType().toString());
            assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("attachment"));
            assertEquals("AE-20260528-0001", response.getHeaders().getFirst("X-DCC-Access-Event-Code"));
            assertEquals("DR-20260528-0001", response.getHeaders().getFirst("X-DCC-Download-Request-Id"));
            assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    response.getHeaders().getFirst("X-DCC-Plain-SHA256"));
            assertFalse(response.getHeaders().containsKey("X-DCC-Encryption-Policy-Version"));
            assertFalse(response.getHeaders().containsKey("X-DCC-Artifact-Id"));
            assertFalse(response.getHeaders().containsKey("X-DCC-Cipher-SHA256"));
            assertTrue(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)
                    .contains("X-DCC-Download-Request-Id"));
            assertTrue(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)
                    .contains(HttpHeaders.CONTENT_DISPOSITION));
            assertTrue(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)
                    .contains("X-DCC-Plain-SHA256"));
            verify(queryService).readDownloadFile(eq(99L), eq(901L), eq(true), eq("DR-20260528-0001"),
                    any(DccRequestAuditContext.class));
        }
    }

    @Test
    void downloadControlledFile_encodesLocalizedFileNameForBrowserReadableDisposition() {
        when(queryService.readDownloadFile(eq(99L), eq(901L), eq(true), eq("DR-20260603-0001"),
                any(DccRequestAuditContext.class)))
                .thenReturn(new DccDownloadFileBinary(
                        "PD可编辑.pdf",
                        "application/pdf",
                        "%PDF-1.7".getBytes(),
                        "DR-20260603-0001",
                        "AE-20260603-0001",
                        null,
                        null,
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        null));

        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);

            ResponseEntity<byte[]> response = controller.downloadControlledFile(901L, true,
                    "DR-20260603-0001", auditRequest());

            String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            assertNotNull(contentDisposition);
            assertTrue(contentDisposition.contains("filename*="));
            assertTrue(contentDisposition.contains("UTF-8''PD%E5%8F%AF%E7%BC%96%E8%BE%91.pdf"));
            assertFalse(contentDisposition.contains(".dcc"));
            assertTrue(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)
                    .contains(HttpHeaders.CONTENT_DISPOSITION));
        }
    }

    @Test
    void onlyOfficePreviewFile_encodesLocalizedFileNameForInlineDisposition() throws Exception {
        when(queryService.readOnlyOfficePreviewFile(eq(2054545668044051918L), eq("office-token"),
                any(DccRequestAuditContext.class)))
                .thenReturn(new DccControlledFileBinary(
                        "INT∕RE∕6.4-65（E∕0）虫害（黏鼠板鼠笼）控制巡检表.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "docx".getBytes(),
                        null));

        ResponseEntity<byte[]> response = controller.getOnlyOfficePreviewFile(
                2054545668044051918L, "office-token", auditRequest());

        String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(contentDisposition);
        assertTrue(contentDisposition.contains("inline"));
        assertTrue(contentDisposition.contains("filename*="));
        assertTrue(contentDisposition.contains("UTF-8''INT%E2%88%95RE%E2%88%956.4-65"));
        assertTrue(contentDisposition.contains("%E8%99%AB%E5%AE%B3"));
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                response.getHeaders().getContentType().toString());
    }

    @Test
    void downloadControlledFile_returnsOpenablePdfAndDoesNotExposeEncryptionPackageHeaders() {
        when(queryService.readDownloadFile(eq(99L), eq(901L), eq(true), eq("DR-20260603-0002"),
                any(DccRequestAuditContext.class)))
                .thenReturn(new DccDownloadFileBinary(
                        "PD可编辑.pdf",
                        "application/pdf",
                        "%PDF-1.7".getBytes(),
                        "DR-20260603-0002",
                        "AE-20260603-0002",
                        null,
                        null,
                        "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
                        null));

        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);

            ResponseEntity<byte[]> response = controller.downloadControlledFile(901L, true,
                    "DR-20260603-0002", auditRequest());

            assertEquals("application/pdf", response.getHeaders().getContentType().toString());
            String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            assertNotNull(contentDisposition);
            assertTrue(contentDisposition.contains("filename*="));
            assertTrue(contentDisposition.contains("PD%E5%8F%AF%E7%BC%96%E8%BE%91.pdf"));
            assertFalse(contentDisposition.contains(".dcc"));
            assertEquals("AE-20260603-0002", response.getHeaders().getFirst("X-DCC-Access-Event-Code"));
            assertEquals("DR-20260603-0002", response.getHeaders().getFirst("X-DCC-Download-Request-Id"));
            assertEquals("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
                    response.getHeaders().getFirst("X-DCC-Plain-SHA256"));
            assertFalse(response.getHeaders().containsKey("X-DCC-Encryption-Policy-Version"));
            assertFalse(response.getHeaders().containsKey("X-DCC-Artifact-Id"));
            assertFalse(response.getHeaders().containsKey("X-DCC-Cipher-SHA256"));
            String exposedHeaders = response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS);
            assertNotNull(exposedHeaders);
            assertTrue(exposedHeaders.contains(HttpHeaders.CONTENT_DISPOSITION));
            assertTrue(exposedHeaders.contains("X-DCC-Plain-SHA256"));
            assertFalse(exposedHeaders.contains("X-DCC-Encryption-Policy-Version"));
            assertFalse(exposedHeaders.contains("X-DCC-Artifact-Id"));
            assertFalse(exposedHeaders.contains("X-DCC-Cipher-SHA256"));
        }
    }

    private MockHttpServletRequest auditRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.8.0.51");
        request.addHeader("User-Agent", "JUnit-DCC-Controller/1.0");
        return request;
    }

    private DccRequestAuditContext auditContext(String requestId) {
        return new DccRequestAuditContext("10.8.0.51", "JUnit-DCC-Controller/1.0", requestId);
    }

    private void assertHeaderParameter(Parameter parameter, String headerName) {
        RequestHeader requestHeader = parameter.getAnnotation(RequestHeader.class);
        assertNotNull(requestHeader);
        assertEquals(headerName, requestHeader.value());
        assertFalse(parameter.isAnnotationPresent(RequestParam.class));
    }

    private void assertUsesMergedReadPermission(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertTrue(preAuthorize.value().contains("dcc:controlled-file:query"));
        assertFalse(preAuthorize.value().contains("dcc:controlled-file:preview"));
    }

    @Test
    void obsoleteControlledFile_delegatesToObsoleteServiceWithoutPassword() {
        DccControlledFileObsoleteReqVO reqVO = new DccControlledFileObsoleteReqVO();
        reqVO.setReason("Replace with newer revision");
        FormInstanceRespVO submitted = new FormInstanceRespVO();
        submitted.setId(37L);
        submitted.setStatus("IN_APPROVAL");
        when(obsoleteService.obsoleteControlledFile(99L, 902L, reqVO)).thenReturn(submitted);

        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);

            CommonResult<FormInstanceRespVO> result = controller.obsoleteControlledFile(902L, reqVO);

            assertEquals("IN_APPROVAL", result.getData().getStatus());
            verify(obsoleteService).obsoleteControlledFile(99L, 902L, reqVO);
        }
    }

    @Test
    void publishControlledFile_delegatesToPublishServiceWithoutPassword() {
        DccControlledFilePublishReqVO reqVO = new DccControlledFilePublishReqVO();
        reqVO.setReason("Release approved revision");
        FormInstanceRespVO submitted = new FormInstanceRespVO();
        submitted.setId(57L);
        submitted.setStatus("IN_APPROVAL");
        when(publishService.publishControlledFile(99L, 920L, reqVO)).thenReturn(submitted);

        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);

            CommonResult<FormInstanceRespVO> result = controller.publishControlledFile(920L, reqVO);

            assertEquals("IN_APPROVAL", result.getData().getStatus());
            verify(publishService).publishControlledFile(99L, 920L, reqVO);
        }
    }

    @Test
    void acknowledgeTraining_delegatesToAckService() {
        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);

            CommonResult<Boolean> result = controller.acknowledgeTraining(903L);

            assertTrue(Boolean.TRUE.equals(result.getData()));
            verify(trainingAssignmentAckService).acknowledgeTraining(99L, 903L);
        }
    }

    @Test
    void releaseManualDistribution_delegatesToFinalizationService() {
        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);

            CommonResult<Boolean> result = controller.releaseManualDistribution(904L);

            assertTrue(Boolean.TRUE.equals(result.getData()));
            verify(finalizationService).releaseManualDistribution(99L, 904L);
        }
    }
}
