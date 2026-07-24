package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAccessLogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileWatermarkTraceDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileWatermarkTraceMapper;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.preview.DccControlledPreviewAccessService;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessRequest;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessResult;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenExpectedContext;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenPayload;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.function.Function;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_EXPIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_INVALID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
class DccControlledFilePreviewProtectionTest extends BaseMockitoUnitTest {

    private static final Long TENANT_ID = 31L;
    private static final Long USER_ID = 99L;
    private static final Long FILE_ID = 991L;
    private static final Long PUBLISHED_FILE_ID = 7002L;
    private static final Long ACCESS_EVENT_ID = 88001L;
    private static final String VERSION_NO = "V1.0";
    private static final String VIEWER_TOKEN = "viewer-token";
    private static final String VIEWER_TOKEN_ID = "VT-20260528-0001";
    private static final String VIEWER_TOKEN_NONCE = "VN-20260528-0001";
    private static final String ACCESS_EVENT_CODE = "AE-20260528-0001";
    private static final String WATERMARK_TRACE_CODE = "WM-20260528-0001";
    private static final DccRequestAuditContext AUDIT_CONTEXT =
            new DccRequestAuditContext("10.8.0.31", "JUnit", "REQ-PREVIEW-20260528-0001");

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileAccessLogMapper accessLogMapper;
    @Mock
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Mock
    private DccControlledFileViewMatrixAccessService viewMatrixAccessService;
    @Mock
    private DccDirectoryAccessPermissionService directoryAccessPermissionService;
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

    @InjectMocks
    private DccControlledFileQueryServiceImpl queryService;

    @BeforeEach
    void setUpViewMatrixAccessDefault() {
        lenient().when(viewMatrixAccessService.canAccessCurrentViewMatrix(any(), any(DccControlledFileDO.class)))
                .thenReturn(true);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void getPreviewMetadata_returnsPublicControlledPreviewContextWithoutInternalIds() {
        TenantContextHolder.setTenantId(TENANT_ID);
        stubReadableFile();
        stubBinaryFile();
        when(previewAccessService.prepareAccess(any(DccPreviewAccessRequest.class)))
                .thenReturn(previewAccessResult());
        when(watermarkService.build(USER_ID, "preview", "spec.pdf"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build());

        DccControlledFilePreviewMetadataRespVO result = queryService.getPreviewMetadata(USER_ID, FILE_ID,
                AUDIT_CONTEXT);

        assertEquals(VIEWER_TOKEN, result.getViewerToken());
        assertEquals(VIEWER_TOKEN_ID, result.getViewerTokenId());
        assertEquals(VIEWER_TOKEN_NONCE, result.getViewerTokenNonce());
        assertEquals(ACCESS_EVENT_CODE, result.getAccessEventCode());
        assertEquals(WATERMARK_TRACE_CODE, result.getWatermarkTraceCode());
        assertNotNull(result.getWatermark());
        assertNull(findProperty(result, "accessEventId"));
        assertNull(findProperty(result, "watermarkTraceId"));
    }

    @Test
    void readPreviewFile_missingViewerTokenOrContextFields_failsClosedBeforeStorageRead() throws Exception {
        assertInvalidContext(null, ACCESS_EVENT_CODE, WATERMARK_TRACE_CODE, VIEWER_TOKEN_ID, VIEWER_TOKEN_NONCE);
        assertInvalidContext(VIEWER_TOKEN, null, WATERMARK_TRACE_CODE, VIEWER_TOKEN_ID, VIEWER_TOKEN_NONCE);
        assertInvalidContext(VIEWER_TOKEN, ACCESS_EVENT_CODE, null, VIEWER_TOKEN_ID, VIEWER_TOKEN_NONCE);
        assertInvalidContext(VIEWER_TOKEN, ACCESS_EVENT_CODE, WATERMARK_TRACE_CODE, null, VIEWER_TOKEN_NONCE);
        assertInvalidContext(VIEWER_TOKEN, ACCESS_EVENT_CODE, WATERMARK_TRACE_CODE, VIEWER_TOKEN_ID, null);

        verifyNoInteractions(controlledFileMapper, accessEventMapper, watermarkTraceMapper, viewerTokenService,
                fileMapper, fileService);
    }

    @Test
    void readPreviewFile_accessEventMissing_failsClosedBeforeStorageRead() throws Exception {
        stubReadableFile();
        when(accessEventMapper.selectOne(any())).thenReturn(null);

        assertServiceException(() -> readPreview(), CONTROLLED_FILE_VIEWER_TOKEN_INVALID);

        verify(fileService, never()).getFileContent(any(), any());
        verifyNoInteractions(viewerTokenService);
    }

    @Test
    void readPreviewFile_watermarkTraceMissing_failsClosedBeforeStorageRead() throws Exception {
        stubReadableFile();
        when(accessEventMapper.selectOne(any())).thenReturn(accessEvent());
        when(watermarkTraceMapper.selectOne(any())).thenReturn(null);

        assertServiceException(() -> readPreview(), CONTROLLED_FILE_VIEWER_TOKEN_INVALID);

        verify(fileService, never()).getFileContent(any(), any());
        verifyNoInteractions(viewerTokenService);
    }

    @Test
    void readPreviewFile_accessEventMismatch_failsClosedBeforeStorageRead() throws Exception {
        assertEventMismatch(event -> event.controlledFileId(992L));
        assertEventMismatch(event -> event.userId(100L));
        assertEventMismatch(event -> event.fileVersionNo("V2.0"));
        assertEventMismatch(event -> event.purpose("DOWNLOAD"));
        assertEventMismatch(event -> event.result("FAILED"));

        verify(fileService, never()).getFileContent(any(), any());
        verifyNoInteractions(viewerTokenService);
    }

    @Test
    void readPreviewFile_watermarkTraceMismatch_failsClosedBeforeStorageRead() throws Exception {
        assertWatermarkMismatch(trace -> trace.accessEventId(88002L));
        assertWatermarkMismatch(trace -> trace.accessEventCode("AE-OTHER"));
        assertWatermarkMismatch(trace -> trace.controlledFileId(992L));
        assertWatermarkMismatch(trace -> trace.userId(100L));
        assertWatermarkMismatch(trace -> trace.fileVersionNo("V2.0"));

        verify(fileService, never()).getFileContent(any(), any());
        verifyNoInteractions(viewerTokenService);
    }

    @Test
    void readPreviewFile_tokenErrorsPropagateBeforeStorageRead() throws Exception {
        assertTokenError(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        assertTokenError(CONTROLLED_FILE_VIEWER_TOKEN_EXPIRED);
        assertTokenError(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);

        verify(fileService, never()).getFileContent(any(), any());
    }

    @Test
    void readPreviewFile_successVerifiesExpectedTokenContextBeforeReadingBytes() throws Exception {
        TenantContextHolder.setTenantId(TENANT_ID);
        stubReadableFile();
        stubBinaryFile();
        when(accessEventMapper.selectOne(any())).thenReturn(accessEvent());
        when(watermarkTraceMapper.selectOne(any())).thenReturn(watermarkTrace());
        when(viewerTokenService.verify(eq(VIEWER_TOKEN), any(DccViewerTokenExpectedContext.class)))
                .thenReturn(DccViewerTokenPayload.builder().tokenId(VIEWER_TOKEN_ID).nonce(VIEWER_TOKEN_NONCE).build());
        when(fileService.getFileContent(1L, "dcc/published/spec.pdf")).thenReturn("pdf".getBytes());
        when(watermarkService.build(USER_ID, "preview", "spec.pdf"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build());

        DccControlledFileBinary result = readPreview();

        assertEquals("spec.pdf", result.fileName());
        assertArrayEquals("pdf".getBytes(), result.bytes());
        ArgumentCaptor<DccViewerTokenExpectedContext> contextCaptor =
                ArgumentCaptor.forClass(DccViewerTokenExpectedContext.class);
        verify(viewerTokenService).verify(eq(VIEWER_TOKEN), contextCaptor.capture());
        DccViewerTokenExpectedContext expectedContext = contextCaptor.getValue();
        assertEquals(TENANT_ID, expectedContext.tenantId());
        assertEquals(USER_ID, expectedContext.userId());
        assertEquals(FILE_ID, expectedContext.fileId());
        assertEquals(VERSION_NO, expectedContext.versionId());
        assertEquals(ACCESS_EVENT_ID, expectedContext.accessEventId());
        assertEquals("CONTROLLED_PREVIEW", expectedContext.purpose());
        assertEquals(900L, expectedContext.ttlSeconds());
        assertEquals(VIEWER_TOKEN_ID, expectedContext.tokenId());
        assertEquals(VIEWER_TOKEN_NONCE, expectedContext.nonce());

        InOrder inOrder = inOrder(accessEventMapper, watermarkTraceMapper, viewerTokenService, fileService);
        inOrder.verify(accessEventMapper).selectOne(any());
        inOrder.verify(watermarkTraceMapper).selectOne(any());
        inOrder.verify(viewerTokenService).verify(eq(VIEWER_TOKEN), any(DccViewerTokenExpectedContext.class));
        inOrder.verify(fileService).getFileContent(1L, "dcc/published/spec.pdf");
    }

    private void assertInvalidContext(String viewerToken, String accessEventCode, String watermarkTraceCode,
                                      String viewerTokenId, String viewerTokenNonce) {
        assertServiceException(() -> queryService.readPreviewFile(USER_ID, FILE_ID, viewerToken, accessEventCode,
                watermarkTraceCode, viewerTokenId, viewerTokenNonce, AUDIT_CONTEXT),
                CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
    }

    private void assertEventMismatch(Function<DccControlledFileAccessEventDO.DccControlledFileAccessEventDOBuilder,
            DccControlledFileAccessEventDO.DccControlledFileAccessEventDOBuilder> mutator) {
        TenantContextHolder.setTenantId(TENANT_ID);
        stubReadableFile();
        when(accessEventMapper.selectOne(any())).thenReturn(mutator.apply(accessEventBuilder()).build());
        when(watermarkTraceMapper.selectOne(any())).thenReturn(watermarkTrace());

        assertServiceException(() -> readPreview(), CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
    }

    private void assertWatermarkMismatch(Function<DccControlledFileWatermarkTraceDO.DccControlledFileWatermarkTraceDOBuilder,
            DccControlledFileWatermarkTraceDO.DccControlledFileWatermarkTraceDOBuilder> mutator) {
        TenantContextHolder.setTenantId(TENANT_ID);
        stubReadableFile();
        when(accessEventMapper.selectOne(any())).thenReturn(accessEvent());
        when(watermarkTraceMapper.selectOne(any())).thenReturn(mutator.apply(watermarkTraceBuilder()).build());

        assertServiceException(() -> readPreview(), CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
    }

    private void assertTokenError(ErrorCode errorCode) {
        TenantContextHolder.setTenantId(TENANT_ID);
        stubReadableFile();
        when(accessEventMapper.selectOne(any())).thenReturn(accessEvent());
        when(watermarkTraceMapper.selectOne(any())).thenReturn(watermarkTrace());
        when(viewerTokenService.verify(eq(VIEWER_TOKEN), any(DccViewerTokenExpectedContext.class)))
                .thenThrow(exception(errorCode));

        assertServiceException(() -> readPreview(), errorCode);
    }

    private void stubReadableFile() {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(FILE_ID)
                .categoryId(10L)
                .directoryId(20L)
                .publishedFileId(PUBLISHED_FILE_ID)
                .fileNumber("DCC-QP-991")
                .versionNo(VERSION_NO)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
        when(controlledFileMapper.selectById(FILE_ID)).thenReturn(file);
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(USER_ID, file)).thenReturn(true);
    }

    private void stubBinaryFile() {
        when(fileMapper.selectById(PUBLISHED_FILE_ID)).thenReturn(FileDO.builder()
                .id(PUBLISHED_FILE_ID)
                .configId(1L)
                .path("dcc/published/spec.pdf")
                .name("spec.pdf")
                .type("application/pdf")
                .build());
    }

    private DccControlledFileBinary readPreview() {
        return queryService.readPreviewFile(USER_ID, FILE_ID, VIEWER_TOKEN, ACCESS_EVENT_CODE,
                WATERMARK_TRACE_CODE, VIEWER_TOKEN_ID, VIEWER_TOKEN_NONCE, AUDIT_CONTEXT);
    }

    private DccPreviewAccessResult previewAccessResult() {
        return new DccPreviewAccessResult(
                ACCESS_EVENT_ID,
                ACCESS_EVENT_CODE,
                99001L,
                WATERMARK_TRACE_CODE,
                VIEWER_TOKEN,
                VIEWER_TOKEN_ID,
                VIEWER_TOKEN_NONCE,
                LocalDateTime.of(2026, 5, 28, 3, 30),
                LocalDateTime.of(2026, 5, 28, 3, 45),
                "{\"traceCode\":\"" + WATERMARK_TRACE_CODE + "\"}");
    }

    private DccControlledFileAccessEventDO accessEvent() {
        return accessEventBuilder().build();
    }

    private DccControlledFileAccessEventDO.DccControlledFileAccessEventDOBuilder accessEventBuilder() {
        return DccControlledFileAccessEventDO.builder()
                .id(ACCESS_EVENT_ID)
                .accessEventCode(ACCESS_EVENT_CODE)
                .controlledFileId(FILE_ID)
                .fileVersionNo(VERSION_NO)
                .userId(USER_ID)
                .accessType("PREVIEW")
                .purpose("CONTROLLED_PREVIEW")
                .result("SUCCESS");
    }

    private DccControlledFileWatermarkTraceDO watermarkTrace() {
        return watermarkTraceBuilder().build();
    }

    private DccControlledFileWatermarkTraceDO.DccControlledFileWatermarkTraceDOBuilder watermarkTraceBuilder() {
        return DccControlledFileWatermarkTraceDO.builder()
                .id(99001L)
                .traceCode(WATERMARK_TRACE_CODE)
                .accessEventId(ACCESS_EVENT_ID)
                .accessEventCode(ACCESS_EVENT_CODE)
                .controlledFileId(FILE_ID)
                .fileVersionNo(VERSION_NO)
                .userId(USER_ID);
    }

    private Object findProperty(Object target, String propertyName) {
        try {
            return target.getClass().getDeclaredField(propertyName);
        } catch (NoSuchFieldException ex) {
            return null;
        }
    }
}
