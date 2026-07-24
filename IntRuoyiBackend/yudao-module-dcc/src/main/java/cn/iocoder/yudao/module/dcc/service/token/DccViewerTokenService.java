package cn.iocoder.yudao.module.dcc.service.token;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_EXPIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_INVALID;

@Service
public class DccViewerTokenService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    // Temporary user-approved fallback. Remove after runtime config is restored.
    private static final String DEFAULT_HMAC_SECRET = "dcc-viewer-token-default-secret-20260602";

    @Value("${yudao.dcc.viewer-token.hmac-secret:}")
    private String hmacSecret;

    private Clock clock = Clock.systemUTC();

    public DccIssuedViewerToken issue(DccViewerTokenIssueCommand command) {
        requireConfigured();
        requireIssueCommand(command);
        Instant issuedAt = Instant.now(clock);
        DccViewerTokenPayload payload = DccViewerTokenPayload.builder()
                .tokenId(newTokenComponent("VT"))
                .nonce(newTokenComponent("VN"))
                .tenantId(command.tenantId())
                .userId(command.userId())
                .fileId(command.fileId())
                .versionId(StrUtil.trim(command.versionId()))
                .accessEventId(command.accessEventId())
                .purpose(StrUtil.trim(command.purpose()))
                .ttlSeconds(command.ttlSeconds())
                .issuedAtEpochSecond(issuedAt.getEpochSecond())
                .expiresAtEpochSecond(issuedAt.plusSeconds(command.ttlSeconds()).getEpochSecond())
                .build();
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(JsonUtils.toJsonString(payload).getBytes(StandardCharsets.UTF_8));
        return new DccIssuedViewerToken(encodedPayload + "." + sign(encodedPayload), payload);
    }

    public DccViewerTokenPayload verify(String token, DccViewerTokenExpectedContext expectedContext) {
        requireConfigured();
        requireExpectedContext(expectedContext);
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
        DccViewerTokenPayload payload = parsePayload(parts[0]);
        requirePayload(payload);
        if (!Instant.now(clock).isBefore(Instant.ofEpochSecond(payload.getExpiresAtEpochSecond()))) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_EXPIRED);
        }
        if (!matches(payload, expectedContext)) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        }
        return payload;
    }

    private boolean matches(DccViewerTokenPayload payload, DccViewerTokenExpectedContext expectedContext) {
        return Objects.equals(payload.getTenantId(), expectedContext.tenantId())
                && Objects.equals(payload.getUserId(), expectedContext.userId())
                && Objects.equals(payload.getFileId(), expectedContext.fileId())
                && Objects.equals(payload.getVersionId(), StrUtil.trim(expectedContext.versionId()))
                && Objects.equals(payload.getAccessEventId(), expectedContext.accessEventId())
                && Objects.equals(payload.getPurpose(), StrUtil.trim(expectedContext.purpose()))
                && Objects.equals(payload.getTtlSeconds(), expectedContext.ttlSeconds())
                && Objects.equals(payload.getNonce(), StrUtil.trim(expectedContext.nonce()))
                && Objects.equals(payload.getTokenId(), StrUtil.trim(expectedContext.tokenId()));
    }

    private DccViewerTokenPayload parsePayload(String encodedPayload) {
        try {
            String json = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
            return JsonUtils.parseObject(json, DccViewerTokenPayload.class);
        } catch (RuntimeException ex) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
    }

    private void requireConfigured() {
        resolveHmacSecret();
    }

    private void requireIssueCommand(DccViewerTokenIssueCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("viewer token issue command is required");
        }
        requirePositive(command.tenantId(), "tenantId");
        requirePositive(command.userId(), "userId");
        requirePositive(command.fileId(), "fileId");
        requireNotBlank(command.versionId(), "versionId");
        requirePositive(command.accessEventId(), "accessEventId");
        requireNotBlank(command.purpose(), "purpose");
        requirePositive(command.ttlSeconds(), "ttlSeconds");
    }

    private void requireExpectedContext(DccViewerTokenExpectedContext expectedContext) {
        if (expectedContext == null) {
            throw new IllegalArgumentException("viewer token expected context is required");
        }
        requirePositive(expectedContext.tenantId(), "tenantId");
        requirePositive(expectedContext.userId(), "userId");
        requirePositive(expectedContext.fileId(), "fileId");
        requireNotBlank(expectedContext.versionId(), "versionId");
        requirePositive(expectedContext.accessEventId(), "accessEventId");
        requireNotBlank(expectedContext.purpose(), "purpose");
        requirePositive(expectedContext.ttlSeconds(), "ttlSeconds");
        requireNotBlank(expectedContext.nonce(), "nonce");
        requireNotBlank(expectedContext.tokenId(), "tokenId");
    }

    private void requirePayload(DccViewerTokenPayload payload) {
        if (payload == null) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
        if (StrUtil.isBlank(payload.getTokenId())
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

    private String sign(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(resolveHmacSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
    }

    private String resolveHmacSecret() {
        String configuredSecret = StrUtil.trim(hmacSecret);
        if (StrUtil.length(configuredSecret) >= 32) {
            return configuredSecret;
        }
        return DEFAULT_HMAC_SECRET;
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private String newTokenComponent(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

}
