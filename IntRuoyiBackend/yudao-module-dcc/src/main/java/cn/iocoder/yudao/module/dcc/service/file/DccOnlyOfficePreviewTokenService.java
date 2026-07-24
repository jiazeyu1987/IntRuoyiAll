package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenExpectedContext;
import lombok.Data;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_EXPIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_INVALID;

@Service
public class DccOnlyOfficePreviewTokenService {

    public static final String RESOURCE_CONTROLLED_FILE = "CONTROLLED_FILE";
    public static final String RESOURCE_UPLOAD_PREVIEW = "UPLOAD_PREVIEW";
    public static final String PURPOSE_CONTROLLED_PREVIEW = "CONTROLLED_PREVIEW";

    @Resource
    private DccOnlyOfficePreviewProperties properties;

    public String issue(String resourceType, Long resourceId) {
        requireConfigured();
        long expiresAt = Instant.now().plusSeconds(Math.max(properties.getTokenExpireSeconds(), 60)).getEpochSecond();
        Map<String, Object> payloadMap = new LinkedHashMap<>();
        payloadMap.put("resourceType", resourceType);
        payloadMap.put("resourceId", resourceId);
        payloadMap.put("tenantId", TenantContextHolder.getTenantId());
        payloadMap.put("expiresAt", expiresAt);
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(JsonUtils.toJsonString(payloadMap).getBytes(StandardCharsets.UTF_8));
        return payload + "." + sign(payload);
    }

    public PreviewTokenPayload verify(String token, String expectedResourceType, Long expectedResourceId) {
        requireConfigured();
        if (StrUtil.isBlank(token) || !token.contains(".")) {
            throw new IllegalStateException("OnlyOffice preview token is invalid");
        }
        String[] parts = token.split("\\.", 2);
        if (!Objects.equals(sign(parts[0]), parts[1])) {
            throw new IllegalStateException("OnlyOffice preview token signature is invalid");
        }
        String json = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        PreviewTokenPayload payload = JsonUtils.parseObject(json, PreviewTokenPayload.class);
        if (payload == null
                || !Objects.equals(payload.getResourceType(), expectedResourceType)
                || !Objects.equals(payload.getResourceId(), expectedResourceId)) {
            throw new IllegalStateException("OnlyOffice preview token payload is invalid");
        }
        if (payload.getExpiresAt() == null || payload.getExpiresAt() < Instant.now().getEpochSecond()) {
            throw new IllegalStateException("OnlyOffice preview token expired");
        }
        requireMatchingTenantContext(payload);
        return payload;
    }

    private void requireMatchingTenantContext(PreviewTokenPayload payload) {
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId != null && !Objects.equals(payload.getTenantId(), currentTenantId)) {
            throw new IllegalStateException("OnlyOffice preview token tenant context is invalid");
        }
    }

    public IssuedPreviewToken issueControlledFile(Long tenantId, Long userId, Long fileId, String versionId,
                                                  Long accessEventId, String purpose, Long ttlSeconds) {
        requireConfigured();
        requireControlledContext(tenantId, userId, fileId, versionId, accessEventId, purpose, ttlSeconds);
        Instant issuedAt = Instant.now();
        PreviewTokenPayload payload = new PreviewTokenPayload();
        payload.setResourceType(RESOURCE_CONTROLLED_FILE);
        payload.setTokenId(newTokenComponent("OT"));
        payload.setNonce(newTokenComponent("ON"));
        payload.setTenantId(tenantId);
        payload.setUserId(userId);
        payload.setFileId(fileId);
        payload.setVersionId(StrUtil.trim(versionId));
        payload.setAccessEventId(accessEventId);
        payload.setPurpose(StrUtil.trim(purpose));
        payload.setTtlSeconds(ttlSeconds);
        payload.setIssuedAtEpochSecond(issuedAt.getEpochSecond());
        payload.setExpiresAtEpochSecond(issuedAt.plusSeconds(ttlSeconds).getEpochSecond());
        String encodedPayload = encodePayload(payload);
        return new IssuedPreviewToken(encodedPayload + "." + sign(encodedPayload), payload);
    }

    public PreviewTokenPayload verifyControlledFile(String token, Long expectedFileId) {
        PreviewTokenPayload payload = verifySignedPayload(token);
        requireControlledPayload(payload);
        if (!Instant.now().isBefore(Instant.ofEpochSecond(payload.getExpiresAtEpochSecond()))) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_EXPIRED);
        }
        if (!RESOURCE_CONTROLLED_FILE.equals(payload.getResourceType())
                || !Objects.equals(payload.getFileId(), expectedFileId)
                || !PURPOSE_CONTROLLED_PREVIEW.equals(payload.getPurpose())) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        }
        return payload;
    }

    public PreviewTokenPayload verifyControlledFile(String token, DccViewerTokenExpectedContext expectedContext) {
        requireExpectedContext(expectedContext);
        PreviewTokenPayload payload = verifyControlledFile(token, expectedContext.fileId());
        if (!Objects.equals(payload.getTenantId(), expectedContext.tenantId())
                || !Objects.equals(payload.getUserId(), expectedContext.userId())
                || !Objects.equals(payload.getVersionId(), StrUtil.trim(expectedContext.versionId()))
                || !Objects.equals(payload.getAccessEventId(), expectedContext.accessEventId())
                || !Objects.equals(payload.getPurpose(), StrUtil.trim(expectedContext.purpose()))
                || !Objects.equals(payload.getTtlSeconds(), expectedContext.ttlSeconds())
                || !Objects.equals(payload.getNonce(), StrUtil.trim(expectedContext.nonce()))
                || !Objects.equals(payload.getTokenId(), StrUtil.trim(expectedContext.tokenId()))) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        }
        return payload;
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new IllegalStateException(properties.missingReason());
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("OnlyOffice preview token signing failed", ex);
        }
    }

    private PreviewTokenPayload verifySignedPayload(String token) {
        requireConfigured();
        if (StrUtil.isBlank(token)) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 2 || StrUtil.isBlank(parts[0]) || StrUtil.isBlank(parts[1])) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
        if (!constantTimeEquals(sign(parts[0]), parts[1])) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
        try {
            String json = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            return JsonUtils.parseObject(json, PreviewTokenPayload.class);
        } catch (RuntimeException ex) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
    }

    private String encodePayload(PreviewTokenPayload payload) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(JsonUtils.toJsonString(payload).getBytes(StandardCharsets.UTF_8));
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private void requireControlledContext(Long tenantId, Long userId, Long fileId, String versionId,
                                          Long accessEventId, String purpose, Long ttlSeconds) {
        requirePositive(tenantId, "tenantId");
        requirePositive(userId, "userId");
        requirePositive(fileId, "fileId");
        requireNotBlank(versionId, "versionId");
        requirePositive(accessEventId, "accessEventId");
        requireNotBlank(purpose, "purpose");
        requirePositive(ttlSeconds, "ttlSeconds");
    }

    private void requireExpectedContext(DccViewerTokenExpectedContext expectedContext) {
        if (expectedContext == null) {
            throw new IllegalArgumentException("OnlyOffice token expected context is required");
        }
        requireControlledContext(expectedContext.tenantId(), expectedContext.userId(), expectedContext.fileId(),
                expectedContext.versionId(), expectedContext.accessEventId(), expectedContext.purpose(),
                expectedContext.ttlSeconds());
        requireNotBlank(expectedContext.nonce(), "nonce");
        requireNotBlank(expectedContext.tokenId(), "tokenId");
    }

    private void requireControlledPayload(PreviewTokenPayload payload) {
        if (payload == null
                || StrUtil.isBlank(payload.getResourceType())
                || StrUtil.isBlank(payload.getTokenId())
                || StrUtil.isBlank(payload.getNonce())
                || payload.getTenantId() == null
                || payload.getUserId() == null
                || payload.getFileId() == null
                || StrUtil.isBlank(payload.getVersionId())
                || payload.getAccessEventId() == null
                || StrUtil.isBlank(payload.getPurpose())
                || payload.getTtlSeconds() == null
                || payload.getTtlSeconds() <= 0
                || payload.getIssuedAtEpochSecond() == null
                || payload.getExpiresAtEpochSecond() == null
                || payload.getExpiresAtEpochSecond() <= payload.getIssuedAtEpochSecond()) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
    }

    private void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private void requireNotBlank(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private String newTokenComponent(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    public record IssuedPreviewToken(String token, PreviewTokenPayload payload) {
    }

    @Data
    public static class PreviewTokenPayload {
        private String resourceType;
        private Long resourceId;
        private Long expiresAt;
        private String tokenId;
        private String nonce;
        private Long tenantId;
        private Long userId;
        private Long fileId;
        private String versionId;
        private Long accessEventId;
        private String purpose;
        private Long ttlSeconds;
        private Long issuedAtEpochSecond;
        private Long expiresAtEpochSecond;
    }
}
