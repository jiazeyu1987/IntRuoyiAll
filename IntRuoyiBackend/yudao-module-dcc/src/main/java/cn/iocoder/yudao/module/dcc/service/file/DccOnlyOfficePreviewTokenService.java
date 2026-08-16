package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenExpectedContext;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference;
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
    public static final String PURPOSE_CONTROLLED_PREVIEW = "CONTROLLED_PREVIEW";
    public static final String AUDIENCE_ONLINE_FILE_PREVIEW = "ONLINE_FILE_PREVIEW_CALLBACK";
    public static final String AUDIENCE_CONTROLLED_FILE_PREVIEW = "CONTROLLED_FILE_PREVIEW_CALLBACK";
    public static final String AUDIENCE_UPLOAD_PREVIEW = "UPLOAD_PREVIEW_CALLBACK";
    public static final String SERVICE_DCC_PDF_CONVERSION = "DCC_ONLYOFFICE_PDF_CONVERSION";

    @Resource
    private DccOnlyOfficePreviewProperties properties;

    public IssuedPreviewToken issueBusinessFile(String audience, BusinessFileAccessOperation operation,
                                                Long infraFileId, Long tenantId, Long userId,
                                                String serviceIdentity, BusinessFileAccessReference reference,
                                                Long ttlSeconds) {
        requireConfigured();
        requireBusinessContext(audience, operation, infraFileId, tenantId, userId, serviceIdentity,
                reference, ttlSeconds);
        Instant issuedAt = Instant.now();
        PreviewTokenPayload payload = new PreviewTokenPayload();
        payload.setResourceType("BUSINESS_FILE");
        payload.setAudience(StrUtil.trim(audience));
        payload.setTokenId(newTokenComponent("OT"));
        payload.setNonce(newTokenComponent("ON"));
        payload.setOperation(operation.name());
        payload.setInfraFileId(infraFileId);
        payload.setTenantId(tenantId);
        payload.setUserId(userId);
        payload.setServiceIdentity(StrUtil.trimToNull(serviceIdentity));
        applyReference(payload, reference);
        payload.setTtlSeconds(ttlSeconds);
        payload.setIssuedAtEpochSecond(issuedAt.getEpochSecond());
        payload.setExpiresAtEpochSecond(issuedAt.plusSeconds(ttlSeconds).getEpochSecond());
        String encodedPayload = encodePayload(payload);
        return new IssuedPreviewToken(encodedPayload + "." + sign(encodedPayload), payload);
    }

    public PreviewTokenPayload verifyBusinessFile(String token, String expectedAudience,
                                                  BusinessFileAccessOperation expectedOperation,
                                                  Long expectedInfraFileId) {
        PreviewTokenPayload payload = verifyBusinessFileToken(token, expectedAudience, expectedInfraFileId);
        if (expectedOperation == null || !expectedOperation.name().equals(payload.getOperation())) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        }
        return payload;
    }

    public PreviewTokenPayload verifyBusinessFileToken(String token, String expectedAudience,
                                                       Long expectedInfraFileId) {
        PreviewTokenPayload payload = verifySignedPayload(token);
        requireBusinessPayload(payload);
        if (!Instant.now().isBefore(Instant.ofEpochSecond(payload.getExpiresAtEpochSecond()))) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_EXPIRED);
        }
        if (!Objects.equals(StrUtil.trim(payload.getAudience()), StrUtil.trim(expectedAudience))
                || !Objects.equals(payload.getInfraFileId(), expectedInfraFileId)) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        }
        requireMatchingTenantContext(payload);
        return payload;
    }

    private void requireMatchingTenantContext(PreviewTokenPayload payload) {
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (!TenantContextHolder.isIgnore() && currentTenantId != null
                && !Objects.equals(payload.getTenantId(), currentTenantId)) {
            throw new IllegalStateException("OnlyOffice preview token tenant context is invalid");
        }
    }

    public IssuedPreviewToken issueControlledFile(Long tenantId, Long userId, Long fileId, String versionId,
                                                  Long accessEventId, String purpose, Long ttlSeconds,
                                                  Long infraFileId, BusinessFileAccessReference reference) {
        requireConfigured();
        requireControlledContext(tenantId, userId, fileId, versionId, accessEventId, purpose, ttlSeconds);
        requireBusinessContext(AUDIENCE_CONTROLLED_FILE_PREVIEW,
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, infraFileId, tenantId, userId,
                null, reference, ttlSeconds);
        if (reference == null || !Objects.equals(reference.businessId(), fileId)
                || !Objects.equals(StrUtil.trim(reference.versionKey()), StrUtil.trim(versionId))) {
            throw new IllegalArgumentException("controlled file token formal reference mismatch");
        }
        Instant issuedAt = Instant.now();
        PreviewTokenPayload payload = new PreviewTokenPayload();
        payload.setResourceType(RESOURCE_CONTROLLED_FILE);
        payload.setAudience(AUDIENCE_CONTROLLED_FILE_PREVIEW);
        payload.setTokenId(newTokenComponent("OT"));
        payload.setNonce(newTokenComponent("ON"));
        payload.setTenantId(tenantId);
        payload.setUserId(userId);
        payload.setFileId(fileId);
        payload.setInfraFileId(infraFileId);
        payload.setOperation(BusinessFileAccessOperation.ONLYOFFICE_PREVIEW.name());
        applyReference(payload, reference);
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
                || !PURPOSE_CONTROLLED_PREVIEW.equals(payload.getPurpose())
                || !AUDIENCE_CONTROLLED_FILE_PREVIEW.equals(payload.getAudience())
                || !BusinessFileAccessOperation.ONLYOFFICE_PREVIEW.name().equals(payload.getOperation())) {
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
        requireBusinessPayload(payload);
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

    private void requireBusinessContext(String audience, BusinessFileAccessOperation operation,
                                        Long infraFileId, Long tenantId, Long userId,
                                        String serviceIdentity, BusinessFileAccessReference reference,
                                        Long ttlSeconds) {
        requireNotBlank(audience, "audience");
        if (operation == null) {
            throw new IllegalArgumentException("operation is required");
        }
        requirePositive(infraFileId, "infraFileId");
        requirePositive(tenantId, "tenantId");
        requirePositive(ttlSeconds, "ttlSeconds");
        boolean hasUser = userId != null;
        boolean hasService = StrUtil.isNotBlank(serviceIdentity);
        if (hasUser == hasService) {
            throw new IllegalArgumentException("exactly one token subject is required");
        }
        if (hasUser) {
            requirePositive(userId, "userId");
        }
        requireReference(reference, tenantId);
    }

    private void requireBusinessPayload(PreviewTokenPayload payload) {
        if (payload == null || StrUtil.isBlank(payload.getAudience())
                || StrUtil.isBlank(payload.getTokenId()) || StrUtil.isBlank(payload.getNonce())
                || payload.getTenantId() == null || payload.getInfraFileId() == null
                || StrUtil.isBlank(payload.getOperation()) || payload.getTtlSeconds() == null
                || payload.getTtlSeconds() <= 0 || payload.getIssuedAtEpochSecond() == null
                || payload.getExpiresAtEpochSecond() == null
                || payload.getExpiresAtEpochSecond() <= payload.getIssuedAtEpochSecond()) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
        try {
            BusinessFileAccessOperation.valueOf(payload.getOperation());
        } catch (IllegalArgumentException ex) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
        boolean hasUser = payload.getUserId() != null;
        boolean hasService = StrUtil.isNotBlank(payload.getServiceIdentity());
        if (hasUser == hasService) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
        requireReference(payload.toBusinessFileReference(), payload.getTenantId());
    }

    private void requireReference(BusinessFileAccessReference reference, Long tenantId) {
        if (reference == null) {
            return;
        }
        if (StrUtil.isBlank(reference.providerId()) || StrUtil.isBlank(reference.businessType())
                || reference.businessId() == null || StrUtil.isBlank(reference.versionKey())
                || reference.tenantId() == null || !Objects.equals(reference.tenantId(), tenantId)) {
            throw new IllegalArgumentException("complete tenant-matched business file reference is required");
        }
    }

    private void applyReference(PreviewTokenPayload payload, BusinessFileAccessReference reference) {
        if (reference == null) {
            return;
        }
        payload.setProviderId(reference.providerId());
        payload.setBusinessType(reference.businessType());
        payload.setBusinessId(reference.businessId());
        payload.setVersionKey(reference.versionKey());
        payload.setCompanyId(reference.companyId());
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
        private String audience;
        private String tokenId;
        private String nonce;
        private Long tenantId;
        private Long userId;
        private String serviceIdentity;
        private String operation;
        private Long infraFileId;
        private String providerId;
        private String businessType;
        private Long businessId;
        private String versionKey;
        private Long companyId;
        private Long fileId;
        private String versionId;
        private Long accessEventId;
        private String purpose;
        private Long ttlSeconds;
        private Long issuedAtEpochSecond;
        private Long expiresAtEpochSecond;

        public BusinessFileAccessReference toBusinessFileReference() {
            if (StrUtil.isBlank(providerId)) {
                if (StrUtil.isBlank(businessType) && businessId == null && StrUtil.isBlank(versionKey)
                        && companyId == null) {
                    return null;
                }
                throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
            }
            return new BusinessFileAccessReference(providerId, businessType, businessId, versionKey,
                    tenantId, companyId);
        }
    }
}
