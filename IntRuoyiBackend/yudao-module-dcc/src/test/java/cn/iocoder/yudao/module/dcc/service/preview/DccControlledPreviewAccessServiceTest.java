package cn.iocoder.yudao.module.dcc.service.preview;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAccessLogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileWatermarkTraceDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileWatermarkTraceMapper;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.dcc.service.token.DccIssuedViewerToken;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenExpectedContext;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenIssueCommand;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenPayload;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_EXPIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_INVALID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({
        DccControlledPreviewAccessService.class,
        DccControlledFileAccessAuditService.class,
        DccViewerTokenService.class
})
class DccControlledPreviewAccessServiceTest extends BaseDbUnitTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-28T03:30:00Z"), ZoneOffset.UTC);
    private static final String TOKEN_SECRET = "t04-viewer-token-secret-with-at-least-32-bytes";

    @Resource
    private DccControlledPreviewAccessService previewAccessService;
    @Resource
    private DccControlledFileAccessAuditService accessAuditService;
    @Resource
    private DccViewerTokenService viewerTokenService;
    @Resource
    private DccControlledFileAccessEventMapper accessEventMapper;
    @Resource
    private DccControlledFileWatermarkTraceMapper watermarkTraceMapper;
    @Resource
    private DccControlledFileAccessLogMapper accessLogMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(previewAccessService, "clock", FIXED_CLOCK);
        ReflectionTestUtils.setField(viewerTokenService, "clock", FIXED_CLOCK);
        ReflectionTestUtils.setField(viewerTokenService, "hmacSecret", TOKEN_SECRET);
    }

    @Test
    void prepareAccess_createsEventWatermarkTraceAccessLogAndViewerToken() {
        DccPreviewAccessResult result = previewAccessService.prepareAccess(accessRequest());

        assertNotNull(result.accessEventId());
        assertTrue(result.accessEventCode().startsWith("AE-20260528-"));
        assertNotNull(result.watermarkTraceId());
        assertTrue(result.watermarkTraceCode().startsWith("WM-20260528-"));
        assertNotNull(result.viewerToken());
        assertNotNull(result.viewerTokenId());
        assertNotNull(result.viewerTokenNonce());

        DccControlledFileAccessEventDO accessEvent = accessEventMapper.selectById(result.accessEventId());
        assertNotNull(accessEvent);
        assertEquals(result.accessEventCode(), accessEvent.getAccessEventCode());
        assertEquals(1001L, accessEvent.getControlledFileId());
        assertEquals("V1.0", accessEvent.getFileVersionNo());
        assertEquals(2001L, accessEvent.getUserId());
        assertEquals("PREVIEW", accessEvent.getAccessType());
        assertEquals("CONTROLLED_PREVIEW", accessEvent.getPurpose());
        assertEquals("SUCCESS", accessEvent.getResult());
        assertEquals("10.0.0.8", accessEvent.getSourceIp());
        assertEquals("JUnit", accessEvent.getUserAgent());
        assertEquals("req-t04-001", accessEvent.getRequestId());
        assertEquals(LocalDateTime.of(2026, 5, 28, 3, 30), accessEvent.getOccurredAt());

        DccControlledFileWatermarkTraceDO trace = watermarkTraceMapper.selectById(result.watermarkTraceId());
        assertNotNull(trace);
        assertEquals(result.watermarkTraceCode(), trace.getTraceCode());
        assertEquals(result.accessEventId(), trace.getAccessEventId());
        assertEquals(result.accessEventCode(), trace.getAccessEventCode());
        assertEquals(1001L, trace.getControlledFileId());
        assertEquals("DCC-QP-001", trace.getFileNumber());
        assertEquals("V1.0", trace.getFileVersionNo());
        assertEquals(2001L, trace.getUserId());
        assertEquals("U2001", trace.getUserIdentifier());
        assertEquals("Alice", trace.getUserDisplayName());
        assertEquals(3001L, trace.getDeptId());
        assertEquals("Quality", trace.getDeptName());
        assertEquals("Test Tenant", trace.getTenantName());
        assertEquals("TRACE_CODE_ONLY", trace.getPrivacyMode());
        assertEquals(LocalDateTime.of(2026, 5, 28, 3, 30), trace.getIssuedAt());
        assertEquals(LocalDateTime.of(2026, 5, 28, 3, 45), trace.getExpiresAt());
        Map<String, Object> watermarkPayload = JsonUtils.parseObject(trace.getWatermarkPayloadJson(),
                new TypeReference<>() {
                });
        assertEquals(result.watermarkTraceCode(), watermarkPayload.get("traceCode"));
        assertEquals(result.accessEventCode(), watermarkPayload.get("accessEventCode"));
        assertEquals(1001L, ((Number) watermarkPayload.get("fileId")).longValue());
        assertEquals("V1.0", watermarkPayload.get("versionId"));
        assertEquals("CONTROLLED_PREVIEW", watermarkPayload.get("purpose"));

        DccControlledFileAccessLogDO accessLog = accessLogMapper.selectOne(
                new LambdaQueryWrapper<DccControlledFileAccessLogDO>()
                        .eq(DccControlledFileAccessLogDO::getAccessEventCode, result.accessEventCode()));
        assertNotNull(accessLog);
        assertEquals(result.accessEventId(), accessLog.getAccessEventId());
        assertEquals(result.watermarkTraceCode(), accessLog.getWatermarkTraceCode());
        assertEquals("PREVIEW", accessLog.getActionType());
        assertEquals("CONTROLLED_PREVIEW", accessLog.getPurpose());
        assertEquals("SUCCESS", accessLog.getResult());
        assertEquals("req-t04-001", accessLog.getRequestId());

        DccViewerTokenPayload tokenPayload = viewerTokenService.verify(result.viewerToken(),
                new DccViewerTokenExpectedContext(31L, 2001L, 1001L, "V1.0",
                        result.accessEventId(), "CONTROLLED_PREVIEW", 900L,
                        result.viewerTokenNonce(), result.viewerTokenId()));
        assertEquals(31L, tokenPayload.getTenantId());
        assertEquals(2001L, tokenPayload.getUserId());
        assertEquals(1001L, tokenPayload.getFileId());
        assertEquals("V1.0", tokenPayload.getVersionId());
        assertEquals(result.accessEventId(), tokenPayload.getAccessEventId());
        assertEquals("CONTROLLED_PREVIEW", tokenPayload.getPurpose());
    }

    @Test
    void verifyViewerToken_rejectsCrossContextExpiredNonceAndSignature() {
        DccIssuedViewerToken issued = viewerTokenService.issue(new DccViewerTokenIssueCommand(
                31L, 2001L, 1001L, "V1.0", 333L, "CONTROLLED_PREVIEW", 300L));
        DccViewerTokenExpectedContext expected = expected(issued.payload());
        assertDoesNotThrow(() -> viewerTokenService.verify(issued.token(), expected));

        assertServiceException(() -> viewerTokenService.verify(issued.token(),
                new DccViewerTokenExpectedContext(32L, 2001L, 1001L, "V1.0", 333L,
                        "CONTROLLED_PREVIEW", 300L, issued.payload().getNonce(), issued.payload().getTokenId())),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        assertServiceException(() -> viewerTokenService.verify(issued.token(),
                new DccViewerTokenExpectedContext(31L, 2002L, 1001L, "V1.0", 333L,
                        "CONTROLLED_PREVIEW", 300L, issued.payload().getNonce(), issued.payload().getTokenId())),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        assertServiceException(() -> viewerTokenService.verify(issued.token(),
                new DccViewerTokenExpectedContext(31L, 2001L, 1002L, "V1.0", 333L,
                        "CONTROLLED_PREVIEW", 300L, issued.payload().getNonce(), issued.payload().getTokenId())),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        assertServiceException(() -> viewerTokenService.verify(issued.token(),
                new DccViewerTokenExpectedContext(31L, 2001L, 1001L, "V2.0", 333L,
                        "CONTROLLED_PREVIEW", 300L, issued.payload().getNonce(), issued.payload().getTokenId())),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        assertServiceException(() -> viewerTokenService.verify(issued.token(),
                new DccViewerTokenExpectedContext(31L, 2001L, 1001L, "V1.0", 333L,
                        "ONLYOFFICE_READ", 300L, issued.payload().getNonce(), issued.payload().getTokenId())),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        assertServiceException(() -> viewerTokenService.verify(issued.token(),
                new DccViewerTokenExpectedContext(31L, 2001L, 1001L, "V1.0", 333L,
                        "CONTROLLED_PREVIEW", 300L, "wrong-nonce", issued.payload().getTokenId())),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        assertServiceException(() -> viewerTokenService.verify(issued.token(),
                new DccViewerTokenExpectedContext(31L, 2001L, 1001L, "V1.0", 333L,
                        "CONTROLLED_PREVIEW", 301L, issued.payload().getNonce(), issued.payload().getTokenId())),
                CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);

        DccIssuedViewerToken expiring = viewerTokenService.issue(new DccViewerTokenIssueCommand(
                31L, 2001L, 1001L, "V1.0", 334L, "CONTROLLED_PREVIEW", 1L));
        ReflectionTestUtils.setField(viewerTokenService, "clock",
                Clock.fixed(Instant.parse("2026-05-28T03:30:02Z"), ZoneOffset.UTC));
        assertServiceException(() -> viewerTokenService.verify(expiring.token(), expected(expiring.payload())),
                CONTROLLED_FILE_VIEWER_TOKEN_EXPIRED);

        ReflectionTestUtils.setField(viewerTokenService, "clock", FIXED_CLOCK);
        assertServiceException(() -> viewerTokenService.verify(issued.token() + "x", expected),
                CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
    }

    private DccPreviewAccessRequest accessRequest() {
        return new DccPreviewAccessRequest(
                31L,
                2001L,
                1001L,
                "V1.0",
                "DCC-QP-001",
                "PREVIEW",
                "CONTROLLED_PREVIEW",
                900L,
                "U2001",
                "Alice",
                3001L,
                "Quality",
                "Test Tenant",
                "TRACE_CODE_ONLY",
                "10.0.0.8",
                "JUnit",
                "req-t04-001");
    }

    private DccViewerTokenExpectedContext expected(DccViewerTokenPayload payload) {
        return new DccViewerTokenExpectedContext(payload.getTenantId(), payload.getUserId(), payload.getFileId(),
                payload.getVersionId(), payload.getAccessEventId(), payload.getPurpose(), payload.getTtlSeconds(),
                payload.getNonce(), payload.getTokenId());
    }

}
