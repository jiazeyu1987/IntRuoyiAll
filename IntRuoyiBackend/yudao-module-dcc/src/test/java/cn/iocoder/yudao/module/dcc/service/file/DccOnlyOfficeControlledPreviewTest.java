package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAccessLogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileWatermarkTraceDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileWatermarkTraceMapper;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenExpectedContext;
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
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_EXPIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_INVALID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccOnlyOfficeControlledPreviewTest extends BaseMockitoUnitTest {

    private static final Long TENANT_ID = 31L;
    private static final Long OTHER_TENANT_ID = 32L;
    private static final Long USER_ID = 99L;
    private static final Long OTHER_USER_ID = 100L;
    private static final Long FILE_ID = 991L;
    private static final Long OTHER_FILE_ID = 992L;
    private static final Long PUBLISHED_FILE_ID = 7002L;
    private static final Long ACCESS_EVENT_ID = 88001L;
    private static final Long OTHER_ACCESS_EVENT_ID = 88002L;
    private static final String ACCESS_EVENT_CODE = "AE-20260528-0001";
    private static final String WATERMARK_TRACE_CODE = "WM-20260528-0001";
    private static final String VERSION_NO = "V1.0";
    private static final String OTHER_VERSION_NO = "V2.0";
    private static final String PURPOSE = "CONTROLLED_PREVIEW";
    private static final String OFFICE_READ_ACTION_TYPE = "OFFICE_READ";
    private static final String HMAC_SECRET = "0123456789abcdef0123456789abcdef";
    private static final String SOURCE_IP = "10.8.0.41";
    private static final String USER_AGENT = "OnlyOffice-Test-Agent/1.0";
    private static final String OFFICE_REQUEST_ID = "REQ-ONLYOFFICE-20260528-0001";

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileAccessLogMapper accessLogMapper;
    @Mock
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
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
    private DccControlledFileAccessEventMapper accessEventMapper;
    @Mock
    private DccControlledFileWatermarkTraceMapper watermarkTraceMapper;

    @InjectMocks
    private DccControlledFileQueryServiceImpl queryService;

    private final DccOnlyOfficePreviewTokenService tokenService = new DccOnlyOfficePreviewTokenService();

    @BeforeEach
    void setUpOnlyOfficeTokenService() {
        DccOnlyOfficePreviewProperties properties = new DccOnlyOfficePreviewProperties();
        properties.setBaseUrl("http://onlyoffice.local");
        properties.setPublicFileBaseUrl("http://127.0.0.1:48081");
        properties.setJwtSecret(HMAC_SECRET);
        properties.setTokenExpireSeconds(300);
        ReflectionTestUtils.setField(tokenService, "properties", properties);
        ReflectionTestUtils.setField(queryService, "onlyOfficePreviewProperties", properties);
        ReflectionTestUtils.setField(queryService, "onlyOfficePreviewTokenService", tokenService);
        lenient().when(viewMatrixAccessService.canAccessCurrentViewMatrix(any(), any(DccControlledFileDO.class)))
                .thenReturn(true);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void readOnlyOfficePreviewFile_rejectsLegacyResourceIdOnlyTokenBeforeStorageRead() throws Exception {
        String legacyToken = tokenService.issue(DccOnlyOfficePreviewTokenService.RESOURCE_CONTROLLED_FILE, FILE_ID);

        assertServiceException(() -> queryService.readOnlyOfficePreviewFile(FILE_ID, legacyToken, auditContext()),
                CONTROLLED_FILE_VIEWER_TOKEN_INVALID);

        verify(fileService, never()).getFileContent(any(), any());
    }

    @Test
    void readOnlyOfficePreviewFile_contextTokenVerifiesEvidenceBeforeStorageReadAndWritesEventAudit() throws Exception {
        stubReadableOfficeFile();
        String token = contextToken(TENANT_ID, USER_ID, FILE_ID, VERSION_NO, ACCESS_EVENT_ID, PURPOSE,
                900L, "VT-20260528-0001", "VN-20260528-0001", 60L);

        DccControlledFileBinary result = queryService.readOnlyOfficePreviewFile(FILE_ID, token, auditContext());

        assertEquals("spec.docx", result.fileName());
        assertArrayEquals("docx".getBytes(StandardCharsets.UTF_8), result.bytes());
        InOrder inOrder = inOrder(accessEventMapper, watermarkTraceMapper, fileService);
        inOrder.verify(accessEventMapper).selectById(ACCESS_EVENT_ID);
        inOrder.verify(watermarkTraceMapper).selectOne(any());
        inOrder.verify(fileService).getFileContent(1L, "dcc/published/spec.docx");

        verify(accessLogMapper).insert(org.mockito.ArgumentMatchers.<DccControlledFileAccessLogDO>argThat(log ->
                FILE_ID.equals(log.getControlledFileId())
                        && ACCESS_EVENT_ID.equals(log.getAccessEventId())
                        && ACCESS_EVENT_CODE.equals(log.getAccessEventCode())
                        && WATERMARK_TRACE_CODE.equals(log.getWatermarkTraceCode())
                        && VERSION_NO.equals(log.getFileVersionNo())
                        && USER_ID.equals(log.getUserId())
                        && OFFICE_READ_ACTION_TYPE.equals(log.getActionType())
                        && PURPOSE.equals(log.getPurpose())
                        && "ALLOWED".equals(log.getResult())
                        && "OK".equals(log.getReason())
                        && SOURCE_IP.equals(log.getSourceIp())
                        && USER_AGENT.equals(log.getUserAgent())
                        && OFFICE_REQUEST_ID.equals(log.getRequestId())));
    }

    @Test
    void readOnlyOfficePreviewFile_accessDeniedWritesOfficeReadDeniedAuditBeforeStorageRead() throws Exception {
        stubReadableOfficeFile();
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(any(), any(DccControlledFileDO.class)))
                .thenReturn(false);
        String token = contextToken(TENANT_ID, USER_ID, FILE_ID, VERSION_NO, ACCESS_EVENT_ID, PURPOSE,
                900L, "VT-20260528-0001", "VN-20260528-0001", 60L);

        assertServiceException(() -> queryService.readOnlyOfficePreviewFile(FILE_ID, token, auditContext()),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED);

        verify(fileService, never()).getFileContent(any(), any());
        verify(accessLogMapper).insert(org.mockito.ArgumentMatchers.<DccControlledFileAccessLogDO>argThat(log ->
                FILE_ID.equals(log.getControlledFileId())
                        && ACCESS_EVENT_ID.equals(log.getAccessEventId())
                        && ACCESS_EVENT_CODE.equals(log.getAccessEventCode())
                        && WATERMARK_TRACE_CODE.equals(log.getWatermarkTraceCode())
                        && VERSION_NO.equals(log.getFileVersionNo())
                        && USER_ID.equals(log.getUserId())
                        && OFFICE_READ_ACTION_TYPE.equals(log.getActionType())
                        && PURPOSE.equals(log.getPurpose())
                        && "DENIED".equals(log.getResult())
                        && "ACCESS_DENIED".equals(log.getReason())
                        && SOURCE_IP.equals(log.getSourceIp())
                        && USER_AGENT.equals(log.getUserAgent())
                        && OFFICE_REQUEST_ID.equals(log.getRequestId())));
    }

    @Test
    void onlyOfficeContextTokenVerifierRejectsEveryBoundContextMismatch() {
        DccOnlyOfficePreviewTokenService.IssuedPreviewToken issued = tokenService.issueControlledFile(
                TENANT_ID, USER_ID, FILE_ID, VERSION_NO, ACCESS_EVENT_ID, PURPOSE, 900L);
        DccOnlyOfficePreviewTokenService.PreviewTokenPayload payload = issued.payload();

        tokenService.verifyControlledFile(issued.token(), new DccViewerTokenExpectedContext(
                TENANT_ID, USER_ID, FILE_ID, VERSION_NO, ACCESS_EVENT_ID, PURPOSE, 900L,
                payload.getNonce(), payload.getTokenId()));

        assertContextMismatch(issued.token(), new DccViewerTokenExpectedContext(
                OTHER_TENANT_ID, USER_ID, FILE_ID, VERSION_NO, ACCESS_EVENT_ID, PURPOSE, 900L,
                payload.getNonce(), payload.getTokenId()));
        assertContextMismatch(issued.token(), new DccViewerTokenExpectedContext(
                TENANT_ID, OTHER_USER_ID, FILE_ID, VERSION_NO, ACCESS_EVENT_ID, PURPOSE, 900L,
                payload.getNonce(), payload.getTokenId()));
        assertContextMismatch(issued.token(), new DccViewerTokenExpectedContext(
                TENANT_ID, USER_ID, OTHER_FILE_ID, VERSION_NO, ACCESS_EVENT_ID, PURPOSE, 900L,
                payload.getNonce(), payload.getTokenId()));
        assertContextMismatch(issued.token(), new DccViewerTokenExpectedContext(
                TENANT_ID, USER_ID, FILE_ID, OTHER_VERSION_NO, ACCESS_EVENT_ID, PURPOSE, 900L,
                payload.getNonce(), payload.getTokenId()));
        assertContextMismatch(issued.token(), new DccViewerTokenExpectedContext(
                TENANT_ID, USER_ID, FILE_ID, VERSION_NO, OTHER_ACCESS_EVENT_ID, PURPOSE, 900L,
                payload.getNonce(), payload.getTokenId()));
        assertContextMismatch(issued.token(), new DccViewerTokenExpectedContext(
                TENANT_ID, USER_ID, FILE_ID, VERSION_NO, ACCESS_EVENT_ID, "DOWNLOAD", 900L,
                payload.getNonce(), payload.getTokenId()));
        assertContextMismatch(issued.token(), new DccViewerTokenExpectedContext(
                TENANT_ID, USER_ID, FILE_ID, VERSION_NO, ACCESS_EVENT_ID, PURPOSE, 901L,
                payload.getNonce(), payload.getTokenId()));
        assertContextMismatch(issued.token(), new DccViewerTokenExpectedContext(
                TENANT_ID, USER_ID, FILE_ID, VERSION_NO, ACCESS_EVENT_ID, PURPOSE, 900L,
                "VN-OTHER", payload.getTokenId()));
        assertContextMismatch(issued.token(), new DccViewerTokenExpectedContext(
                TENANT_ID, USER_ID, FILE_ID, VERSION_NO, ACCESS_EVENT_ID, PURPOSE, 900L,
                payload.getNonce(), "VT-OTHER"));
    }

    @Test
    void readOnlyOfficePreviewFile_rejectsMissingExpiredAndContextMismatchBeforeStorageRead() throws Exception {
        assertOnlyOfficeReadRejected(null, CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        assertOnlyOfficeReadRejected(contextToken(TENANT_ID, USER_ID, FILE_ID, VERSION_NO, ACCESS_EVENT_ID, PURPOSE,
                900L, "VT-20260528-0001", "VN-20260528-0001", -60L), CONTROLLED_FILE_VIEWER_TOKEN_EXPIRED);
        assertOnlyOfficeReadRejected(contextToken(OTHER_TENANT_ID, USER_ID, FILE_ID, VERSION_NO, ACCESS_EVENT_ID,
                PURPOSE, 900L, "VT-20260528-0001", "VN-20260528-0001", 60L),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        assertOnlyOfficeReadRejected(contextToken(TENANT_ID, USER_ID, OTHER_FILE_ID, VERSION_NO, ACCESS_EVENT_ID,
                PURPOSE, 900L, "VT-20260528-0001", "VN-20260528-0001", 60L),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        assertOnlyOfficeReadRejected(contextToken(TENANT_ID, USER_ID, FILE_ID, OTHER_VERSION_NO, ACCESS_EVENT_ID,
                PURPOSE, 900L, "VT-20260528-0001", "VN-20260528-0001", 60L),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        assertOnlyOfficeReadRejected(contextToken(TENANT_ID, USER_ID, FILE_ID, VERSION_NO, ACCESS_EVENT_ID,
                "DOWNLOAD", 900L, "VT-20260528-0001", "VN-20260528-0001", 60L),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);

        verify(fileService, never()).getFileContent(any(), any());
    }

    @Test
    void readOnlyOfficePreviewFile_rejectsMissingOrMismatchedEvidenceBeforeStorageRead() throws Exception {
        assertEvidenceRejected(null, watermarkTrace(), CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        assertEvidenceRejected(accessEventBuilder().userId(OTHER_USER_ID).build(), watermarkTrace(),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        assertEvidenceRejected(accessEventBuilder().controlledFileId(OTHER_FILE_ID).build(), watermarkTrace(),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        assertEvidenceRejected(accessEventBuilder().fileVersionNo(OTHER_VERSION_NO).build(), watermarkTrace(),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        assertEvidenceRejected(accessEventBuilder().purpose("DOWNLOAD").build(), watermarkTrace(),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        assertEvidenceRejected(accessEventBuilder().result("DENIED").build(), watermarkTrace(),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        assertEvidenceRejected(accessEvent(), null, CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        assertEvidenceRejected(accessEvent(), watermarkTraceBuilder().accessEventId(OTHER_ACCESS_EVENT_ID).build(),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        assertEvidenceRejected(accessEvent(), watermarkTraceBuilder().accessEventCode("AE-OTHER").build(),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        assertEvidenceRejected(accessEvent(), watermarkTraceBuilder().controlledFileId(OTHER_FILE_ID).build(),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        assertEvidenceRejected(accessEvent(), watermarkTraceBuilder().userId(OTHER_USER_ID).build(),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        assertEvidenceRejected(accessEvent(), watermarkTraceBuilder().fileVersionNo(OTHER_VERSION_NO).build(),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);

        verify(fileService, never()).getFileContent(any(), any());
    }

    private void assertContextMismatch(String token, DccViewerTokenExpectedContext expectedContext) {
        assertServiceException(() -> tokenService.verifyControlledFile(token, expectedContext),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
    }

    private DccRequestAuditContext auditContext() {
        return new DccRequestAuditContext(SOURCE_IP, USER_AGENT, OFFICE_REQUEST_ID);
    }

    private void assertOnlyOfficeReadRejected(String token, ErrorCode expectedError) throws Exception {
        reset(controlledFileMapper, accessLogMapper, permissionSupport, directoryAccessPermissionService,
                accessEventMapper, watermarkTraceMapper, fileMapper, fileService);
        stubReadableOfficeFile();
        TenantContextHolder.setTenantId(TENANT_ID);
        assertServiceException(() -> queryService.readOnlyOfficePreviewFile(FILE_ID, token, auditContext()),
                expectedError);
        verify(fileService, never()).getFileContent(any(), any());
    }

    private void assertEvidenceRejected(DccControlledFileAccessEventDO accessEvent,
                                        DccControlledFileWatermarkTraceDO watermarkTrace,
                                        ErrorCode expectedError) throws Exception {
        reset(controlledFileMapper, accessLogMapper, permissionSupport, directoryAccessPermissionService,
                accessEventMapper, watermarkTraceMapper, fileMapper, fileService);
        stubReadableOfficeFile();
        when(accessEventMapper.selectById(ACCESS_EVENT_ID)).thenReturn(accessEvent);
        when(watermarkTraceMapper.selectOne(any())).thenReturn(watermarkTrace);
        String token = contextToken(TENANT_ID, USER_ID, FILE_ID, VERSION_NO, ACCESS_EVENT_ID, PURPOSE,
                900L, "VT-20260528-0001", "VN-20260528-0001", 60L);

        assertServiceException(() -> queryService.readOnlyOfficePreviewFile(FILE_ID, token, auditContext()),
                expectedError);
        verify(fileService, never()).getFileContent(any(), any());
    }

    private void stubReadableOfficeFile() {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(FILE_ID)
                .categoryId(10L)
                .directoryId(20L)
                .publishedFileId(PUBLISHED_FILE_ID)
                .fileNumber("DCC-QP-991")
                .versionNo(VERSION_NO)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
        lenient().when(controlledFileMapper.selectById(FILE_ID)).thenReturn(file);
        lenient().when(viewMatrixAccessService.canAccessCurrentViewMatrix(USER_ID, file)).thenReturn(true);
        lenient().when(accessEventMapper.selectById(ACCESS_EVENT_ID)).thenReturn(accessEvent());
        lenient().when(watermarkTraceMapper.selectOne(any())).thenReturn(watermarkTrace());
        lenient().when(fileMapper.selectById(PUBLISHED_FILE_ID)).thenReturn(FileDO.builder()
                .id(PUBLISHED_FILE_ID)
                .configId(1L)
                .path("dcc/published/spec.docx")
                .name("spec.docx")
                .type("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .build());
        try {
            lenient().when(fileService.getFileContent(1L, "dcc/published/spec.docx"))
                    .thenReturn("docx".getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
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
                .purpose(PURPOSE)
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

    private String contextToken(Long tenantId, Long userId, Long fileId, String versionNo, Long accessEventId,
                                String purpose, Long ttlSeconds, String tokenId, String nonce,
                                Long expiresInSeconds) {
        long now = Instant.now().getEpochSecond();
        long expiresAt = now + expiresInSeconds;
        long issuedAt = expiresInSeconds < 0 ? expiresAt - ttlSeconds : now;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("resourceType", DccOnlyOfficePreviewTokenService.RESOURCE_CONTROLLED_FILE);
        payload.put("tokenId", tokenId);
        payload.put("nonce", nonce);
        payload.put("tenantId", tenantId);
        payload.put("userId", userId);
        payload.put("fileId", fileId);
        payload.put("versionId", versionNo);
        payload.put("accessEventId", accessEventId);
        payload.put("purpose", purpose);
        payload.put("ttlSeconds", ttlSeconds);
        payload.put("issuedAtEpochSecond", issuedAt);
        payload.put("expiresAtEpochSecond", expiresAt);
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(JsonUtils.toJsonString(payload).getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + sign(encodedPayload);
    }

    private String sign(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(HMAC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
