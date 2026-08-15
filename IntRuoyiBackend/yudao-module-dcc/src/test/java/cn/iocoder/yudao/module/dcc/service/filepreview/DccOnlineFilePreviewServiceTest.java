package cn.iocoder.yudao.module.dcc.service.filepreview;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkOverlayRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileWatermarkTraceDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileWatermarkTraceMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBinary;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileScope;
import cn.iocoder.yudao.module.dcc.service.file.DccOnlyOfficePreviewProperties;
import cn.iocoder.yudao.module.dcc.service.file.DccOnlyOfficePreviewTokenService;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledPreviewWatermarkService;
import cn.iocoder.yudao.module.dcc.service.preview.DccControlledPreviewAccessService;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessRequest;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessResult;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenExpectedContext;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessDeniedException;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessRequest;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.dcc.service.filepreview.DccOnlineFilePreviewServiceImpl.ONLINE_FILE_PREVIEW_PURPOSE;
import static cn.iocoder.yudao.module.dcc.service.filepreview.DccOnlineFilePreviewServiceImpl.ONLINE_FILE_PREVIEW_TTL_SECONDS;
import static cn.iocoder.yudao.module.dcc.service.filepreview.DccOnlineFilePreviewServiceImpl.RESOURCE_ONLINE_FILE_PREVIEW;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccOnlineFilePreviewServiceTest extends BaseMockitoUnitTest {

    private static final Long TENANT_ID = 31L;
    private static final Long USER_ID = 99L;
    private static final Long FILE_ID = 7001L;
    private static final String VIEWER_TOKEN = "viewer-token";
    private static final String VIEWER_TOKEN_ID = "VT-ONLINE-1";
    private static final String VIEWER_TOKEN_NONCE = "VN-ONLINE-1";
    private static final String ACCESS_EVENT_CODE = "AE-ONLINE-1";
    private static final String WATERMARK_TRACE_CODE = "WM-ONLINE-1";
    private static final Long ACCESS_EVENT_ID = 88001L;
    private static final String VERSION_ID = "INFRA_FILE:7001";
    private static final DccRequestAuditContext AUDIT_CONTEXT =
            new DccRequestAuditContext("10.8.0.31", "JUnit", "REQ-ONLINE-PREVIEW-1");
    private static final BusinessFileAccessReference DCC_REFERENCE = new BusinessFileAccessReference(
            "dcc", "DCC_CONTROLLED_FILE", 990L, "1.0", TENANT_ID, null);

    @Mock
    private FileMapper fileMapper;
    @Mock
    private FileService fileService;
    @Mock
    private DccControlledPreviewWatermarkService watermarkService;
    @Mock
    private DccControlledPreviewAccessService previewAccessService;
    @Mock
    private DccViewerTokenService viewerTokenService;
    @Mock
    private DccControlledFileAccessEventMapper accessEventMapper;
    @Mock
    private DccControlledFileWatermarkTraceMapper watermarkTraceMapper;
    @Mock
    private DccOnlyOfficePreviewProperties onlyOfficePreviewProperties;
    @Mock
    private DccOnlyOfficePreviewTokenService onlyOfficePreviewTokenService;
    @Mock
    private BusinessFileAccessService businessFileAccessService;

    @InjectMocks
    private DccOnlineFilePreviewServiceImpl service;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void getPreviewMetadata_officeFileReturnsUnifiedOnlyOfficeDocumentUrl() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(fileMapper.selectById(FILE_ID)).thenReturn(officeFile());
        when(previewAccessService.prepareAccess(any(DccPreviewAccessRequest.class))).thenReturn(previewAccessResult());
        when(watermarkService.build(USER_ID, "preview", "Spec.docx")).thenReturn(watermark());
        when(onlyOfficePreviewProperties.isConfigured()).thenReturn(true);
        when(onlyOfficePreviewProperties.getBaseUrl()).thenReturn("http://onlyoffice.local/");
        when(onlyOfficePreviewProperties.getPublicFileBaseUrl()).thenReturn("http://127.0.0.1:48081/");
        when(onlyOfficePreviewProperties.getTokenExpireSeconds()).thenReturn(300);
        when(businessFileAccessService.assertAllowed(any(BusinessFileAccessRequest.class)))
                .thenReturn(java.util.Optional.of(DCC_REFERENCE));
        when(onlyOfficePreviewTokenService.issueBusinessFile(
                DccOnlyOfficePreviewTokenService.AUDIENCE_ONLINE_FILE_PREVIEW,
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, FILE_ID, TENANT_ID, USER_ID, null,
                DCC_REFERENCE, 300L))
                .thenReturn(new DccOnlyOfficePreviewTokenService.IssuedPreviewToken(
                        "office-token", new DccOnlyOfficePreviewTokenService.PreviewTokenPayload()));

        DccControlledFilePreviewMetadataRespVO result =
                service.getPreviewMetadata(USER_ID, FILE_ID, AUDIT_CONTEXT);

        assertEquals("OFFICE", result.getPreviewKind());
        assertEquals("Spec.docx", result.getFileName());
        assertEquals(VIEWER_TOKEN, result.getViewerToken());
        assertEquals("http://onlyoffice.local", result.getOnlyofficeBaseUrl());
        assertEquals("http://127.0.0.1:48081/admin-api/dcc/file-preview/files/7001/onlyoffice-file?token=office-token",
                result.getOnlyofficeDocumentUrl());
        assertEquals("preview", result.getWatermark().getPurpose());

        ArgumentCaptor<DccPreviewAccessRequest> requestCaptor =
                ArgumentCaptor.forClass(DccPreviewAccessRequest.class);
        verify(previewAccessService).prepareAccess(requestCaptor.capture());
        DccPreviewAccessRequest request = requestCaptor.getValue();
        assertEquals(TENANT_ID, request.tenantId());
        assertEquals(USER_ID, request.userId());
        assertEquals(FILE_ID, request.fileId());
        assertEquals(VERSION_ID, request.versionId());
        assertEquals("PREVIEW", request.accessType());
        assertEquals(ONLINE_FILE_PREVIEW_PURPOSE, request.purpose());
        assertEquals(AUDIT_CONTEXT.sourceIp(), request.sourceIp());
        assertEquals(AUDIT_CONTEXT.userAgent(), request.userAgent());
        assertEquals(AUDIT_CONTEXT.requestId(), request.requestId());
        verify(businessFileAccessService).assertAllowed(argThat(this::isPreviewBusinessAccessRequest));
        verify(onlyOfficePreviewTokenService).issueBusinessFile(
                DccOnlyOfficePreviewTokenService.AUDIENCE_ONLINE_FILE_PREVIEW,
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, FILE_ID, TENANT_ID, USER_ID, null,
                DCC_REFERENCE, 300L);
    }

    @Test
    void getPreviewMetadata_businessGateDenialStopsBeforeFileLookup() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(businessFileAccessService.assertAllowed(any(BusinessFileAccessRequest.class)))
                .thenThrow(new BusinessFileAccessDeniedException("denied",
                        BusinessFileAccessOperation.PREVIEW, FILE_ID, "dcc"));

        assertThrows(ServiceException.class,
                () -> service.getPreviewMetadata(USER_ID, FILE_ID, AUDIT_CONTEXT));

        verify(fileMapper, never()).selectById(any());
        verify(previewAccessService, never()).prepareAccess(any());
    }

    @Test
    void getPreviewMetadata_officeFileWithoutOnlyOfficeConfigReturnsUnavailableReason() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(fileMapper.selectById(FILE_ID)).thenReturn(officeFile());
        when(previewAccessService.prepareAccess(any(DccPreviewAccessRequest.class))).thenReturn(previewAccessResult());
        when(watermarkService.build(USER_ID, "preview", "Spec.docx")).thenReturn(watermark());
        when(onlyOfficePreviewProperties.isConfigured()).thenReturn(false);
        when(onlyOfficePreviewProperties.missingReason()).thenReturn("base-url missing");

        DccControlledFilePreviewMetadataRespVO result =
                service.getPreviewMetadata(USER_ID, FILE_ID, AUDIT_CONTEXT);

        assertEquals("OFFICE", result.getPreviewKind());
        assertNull(result.getOnlyofficeBaseUrl());
        assertNull(result.getOnlyofficeDocumentUrl());
        assertEquals("OnlyOffice preview config is missing: base-url missing",
                result.getPreviewUnavailableReason());
    }

    @Test
    void readPreviewFile_validatesViewerContextBeforeStorageRead() throws Exception {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(fileMapper.selectById(FILE_ID)).thenReturn(pdfFile());
        when(accessEventMapper.selectOne(any())).thenReturn(accessEvent());
        when(watermarkTraceMapper.selectOne(any())).thenReturn(watermarkTrace());
        when(fileService.getFileContent(1L, "edhr/special-nodes/report.pdf"))
                .thenReturn("%PDF-1.7".getBytes());
        when(watermarkService.build(USER_ID, "preview", "report.pdf")).thenReturn(watermark());

        DccControlledFileBinary result = service.readPreviewFile(USER_ID, FILE_ID, VIEWER_TOKEN,
                ACCESS_EVENT_CODE, WATERMARK_TRACE_CODE, VIEWER_TOKEN_ID, VIEWER_TOKEN_NONCE, AUDIT_CONTEXT);

        assertEquals("report.pdf", result.fileName());
        assertEquals("application/pdf", result.contentType());
        assertArrayEquals("%PDF-1.7".getBytes(), result.bytes());

        InOrder inOrder = inOrder(businessFileAccessService, viewerTokenService, fileService);
        inOrder.verify(businessFileAccessService).assertAllowed(argThat(this::isPreviewBusinessAccessRequest));
        inOrder.verify(viewerTokenService).verify(eq(VIEWER_TOKEN), eq(new DccViewerTokenExpectedContext(
                TENANT_ID, USER_ID, FILE_ID, VERSION_ID, ACCESS_EVENT_ID, ONLINE_FILE_PREVIEW_PURPOSE,
                ONLINE_FILE_PREVIEW_TTL_SECONDS, VIEWER_TOKEN_NONCE, VIEWER_TOKEN_ID)));
        inOrder.verify(fileService).getFileContent(1L, "edhr/special-nodes/report.pdf");
    }

    @Test
    void readOnlyOfficePreviewFile_rechecksTokenClaimAndStopsBeforeFileLookupWhenRevoked() throws Exception {
        DccOnlyOfficePreviewTokenService.PreviewTokenPayload payload = businessTokenPayload();
        when(onlyOfficePreviewProperties.isConfigured()).thenReturn(true);
        when(onlyOfficePreviewTokenService.verifyBusinessFile(
                "office-token", DccOnlyOfficePreviewTokenService.AUDIENCE_ONLINE_FILE_PREVIEW,
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, FILE_ID))
                .thenReturn(payload);
        when(businessFileAccessService.assertAllowed(any(BusinessFileAccessRequest.class)))
                .thenThrow(new BusinessFileAccessDeniedException("revoked",
                        BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, FILE_ID, "dcc"));

        assertThrows(ServiceException.class,
                () -> service.readOnlyOfficePreviewFile(FILE_ID, "office-token", AUDIT_CONTEXT));

        verify(fileMapper, never()).selectById(any());
        verify(fileService, never()).getFileContent(any(), any());
        verify(businessFileAccessService).assertAllowed(argThat(request ->
                request.operation() == BusinessFileAccessOperation.ONLYOFFICE_PREVIEW
                        && FILE_ID.equals(request.fileId())
                        && TENANT_ID.equals(request.tenantId())
                        && USER_ID.equals(request.userId())
                        && DCC_REFERENCE.equals(request.claim())
                        && request.tokenClaimRequired()));
    }

    @Test
    void readOnlyOfficePreviewFile_runsInTokenTenantAndRestoresIgnoredCallerContext() throws Exception {
        TenantContextHolder.setTenantId(88L);
        TenantContextHolder.setIgnore(true);
        DccOnlyOfficePreviewTokenService.PreviewTokenPayload payload = businessTokenPayload();
        when(onlyOfficePreviewProperties.isConfigured()).thenReturn(true);
        when(onlyOfficePreviewTokenService.verifyBusinessFile(
                "office-token", DccOnlyOfficePreviewTokenService.AUDIENCE_ONLINE_FILE_PREVIEW,
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, FILE_ID)).thenReturn(payload);
        when(businessFileAccessService.assertAllowed(any(BusinessFileAccessRequest.class)))
                .thenAnswer(invocation -> {
                    assertEquals(TENANT_ID, TenantContextHolder.getRequiredTenantId());
                    assertTrue(!TenantContextHolder.isIgnore());
                    return java.util.Optional.of(DCC_REFERENCE);
                });
        when(fileMapper.selectById(FILE_ID)).thenReturn(officeFile());
        when(fileService.getFileContent(1L, "edhr/special-nodes/Spec.docx"))
                .thenReturn("office".getBytes());

        service.readOnlyOfficePreviewFile(FILE_ID, "office-token", AUDIT_CONTEXT);

        assertEquals(88L, TenantContextHolder.getRequiredTenantId());
        assertTrue(TenantContextHolder.isIgnore());
    }

    private DccOnlyOfficePreviewTokenService.PreviewTokenPayload businessTokenPayload() {
        DccOnlyOfficePreviewTokenService.PreviewTokenPayload payload =
                new DccOnlyOfficePreviewTokenService.PreviewTokenPayload();
        payload.setTokenId("OT-ONLINE-1");
        payload.setTenantId(TENANT_ID);
        payload.setUserId(USER_ID);
        payload.setOperation(BusinessFileAccessOperation.ONLYOFFICE_PREVIEW.name());
        payload.setInfraFileId(FILE_ID);
        payload.setProviderId(DCC_REFERENCE.providerId());
        payload.setBusinessType(DCC_REFERENCE.businessType());
        payload.setBusinessId(DCC_REFERENCE.businessId());
        payload.setVersionKey(DCC_REFERENCE.versionKey());
        return payload;
    }

    private boolean isPreviewBusinessAccessRequest(BusinessFileAccessRequest request) {
        return request.operation() == BusinessFileAccessOperation.PREVIEW
                && FILE_ID.equals(request.fileId())
                && TENANT_ID.equals(request.tenantId())
                && USER_ID.equals(request.userId())
                && AUDIT_CONTEXT.requestId().equals(request.requestId());
    }

    private FileDO officeFile() {
        return FileDO.builder()
                .id(FILE_ID)
                .configId(1L)
                .path("edhr/special-nodes/Spec.docx")
                .name("Spec.docx")
                .type("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .build();
    }

    private FileDO pdfFile() {
        return FileDO.builder()
                .id(FILE_ID)
                .configId(1L)
                .path("edhr/special-nodes/report.pdf")
                .name("report.pdf")
                .type("application/pdf")
                .build();
    }

    private DccPreviewAccessResult previewAccessResult() {
        return new DccPreviewAccessResult(ACCESS_EVENT_ID, ACCESS_EVENT_CODE, 99001L, WATERMARK_TRACE_CODE,
                VIEWER_TOKEN, VIEWER_TOKEN_ID, VIEWER_TOKEN_NONCE,
                LocalDateTime.parse("2026-07-21T10:00:00"),
                LocalDateTime.parse("2026-07-21T10:15:00"), "{}");
    }

    private DccControlledFileAccessEventDO accessEvent() {
        return DccControlledFileAccessEventDO.builder()
                .id(ACCESS_EVENT_ID)
                .accessEventCode(ACCESS_EVENT_CODE)
                .controlledFileId(FILE_ID)
                .fileVersionNo(VERSION_ID)
                .userId(USER_ID)
                .accessType("PREVIEW")
                .purpose(ONLINE_FILE_PREVIEW_PURPOSE)
                .result("SUCCESS")
                .build();
    }

    private DccControlledFileWatermarkTraceDO watermarkTrace() {
        return DccControlledFileWatermarkTraceDO.builder()
                .id(99001L)
                .traceCode(WATERMARK_TRACE_CODE)
                .accessEventId(ACCESS_EVENT_ID)
                .accessEventCode(ACCESS_EVENT_CODE)
                .controlledFileId(FILE_ID)
                .fileVersionNo(VERSION_ID)
                .userId(USER_ID)
                .build();
    }

    private DccControlledPreviewWatermarkRespVO watermark() {
        return DccControlledPreviewWatermarkRespVO.builder()
                .label("在线预览")
                .text("在线预览 | Spec.docx")
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
