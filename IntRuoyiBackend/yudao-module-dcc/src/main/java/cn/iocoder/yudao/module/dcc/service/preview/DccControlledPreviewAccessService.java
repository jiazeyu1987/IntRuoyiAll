package cn.iocoder.yudao.module.dcc.service.preview;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileWatermarkTraceDO;
import cn.iocoder.yudao.module.dcc.service.audit.DccAccessEventCreateCommand;
import cn.iocoder.yudao.module.dcc.service.audit.DccAccessLogCreateCommand;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.dcc.service.audit.DccWatermarkTraceCreateCommand;
import cn.iocoder.yudao.module.dcc.service.token.DccIssuedViewerToken;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenIssueCommand;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenService;
import cn.iocoder.yudao.module.dcc.service.token.DccPreviewSourceIdentity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;

@Service
public class DccControlledPreviewAccessService {

    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneOffset.UTC);

    private Clock clock = Clock.systemUTC();

    @Resource
    private DccControlledFileAccessAuditService accessAuditService;
    @Resource
    private DccViewerTokenService viewerTokenService;

    @Transactional(rollbackFor = Exception.class)
    public DccPreviewAccessResult prepareAccess(DccPreviewAccessRequest request) {
        return prepareAccessInternal(request, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public DccPreviewAccessResult prepareSourceBoundAccess(DccPreviewAccessRequest request, DccPreviewSourceIdentity source) {
        Objects.requireNonNull(source, "preview source identity");
        if (request == null || !"PREVIEW".equals(request.accessType()))
            throw new IllegalArgumentException("Source-bound access is only valid for preview");
        return prepareAccessInternal(request, source);
    }

    private DccPreviewAccessResult prepareAccessInternal(DccPreviewAccessRequest request, DccPreviewSourceIdentity source) {
        requireRequest(request);
        LocalDateTime issuedAt = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        // The existing PREVIEW event column is DATETIME(0); persist and expose this same UTC second.
        if ("PREVIEW".equals(StrUtil.trim(request.accessType()))) issuedAt = issuedAt.truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime expiresAt = issuedAt.plusSeconds(request.ttlSeconds());
        String accessEventCode = newCode("AE");
        DccControlledFileAccessEventDO accessEvent = accessAuditService.createAccessEvent(
                new DccAccessEventCreateCommand(accessEventCode, request.fileId(), request.versionId(),
                        request.userId(), request.accessType(), request.purpose(), RESULT_SUCCESS,
                        null, null, request.sourceIp(), request.userAgent(), request.requestId(), issuedAt,
                        request.processInstanceId()));
        if (request.processInstanceId() != null && (accessEvent == null
                || !Objects.equals(request.processInstanceId(), accessEvent.getProcessInstanceId())
                || !Objects.equals(request.tenantId(), accessEvent.getTenantId()))) {
            throw new IllegalStateException("The issuing approval round was not persisted");
        }
        if ("PREVIEW".equals(StrUtil.trim(request.accessType())) && (accessEvent == null
                || accessEvent.getId() == null || accessEvent.getId() <= 0
                || !Objects.equals(accessEvent.getTenantId(), request.tenantId())
                || !Objects.equals(accessEvent.getAccessEventCode(), accessEventCode)
                || !Objects.equals(accessEvent.getControlledFileId(), request.fileId())
                || !Objects.equals(accessEvent.getFileVersionNo(), StrUtil.trim(request.versionId()))
                || !Objects.equals(accessEvent.getUserId(), request.userId())
                || !Objects.equals(accessEvent.getProcessInstanceId(), request.processInstanceId())
                || !Objects.equals(accessEvent.getAccessType(), "PREVIEW")
                || !Objects.equals(accessEvent.getPurpose(), StrUtil.trim(request.purpose()))
                || !Objects.equals(accessEvent.getResult(), RESULT_SUCCESS)
                || !Objects.equals(accessEvent.getOccurredAt(), issuedAt))) {
            throw new IllegalStateException("The PREVIEW event identity and registration time were not persisted");
        }

        String watermarkTraceCode = newCode("WM");
        String watermarkPayloadJson = JsonUtils.toJsonString(watermarkPayload(request, accessEventCode,
                watermarkTraceCode, issuedAt, expiresAt));
        DccControlledFileWatermarkTraceDO watermarkTrace = accessAuditService.recordWatermarkTrace(
                new DccWatermarkTraceCreateCommand(watermarkTraceCode, accessEvent.getId(), accessEventCode,
                        request.fileId(), request.fileNumber(), request.versionId(), request.userId(),
                        request.userIdentifier(), request.userDisplayName(), request.deptId(), request.deptName(),
                        request.tenantName(), request.privacyMode(), watermarkPayloadJson, issuedAt, expiresAt));

        accessAuditService.recordAccessLog(new DccAccessLogCreateCommand(request.fileId(), accessEvent.getId(),
                accessEventCode, watermarkTraceCode, request.versionId(), request.userId(), request.accessType(),
                request.purpose(), RESULT_SUCCESS, null, null, request.sourceIp(), request.requestId(),
                request.userAgent()));

        var command = new DccViewerTokenIssueCommand(
                request.tenantId(), request.userId(), request.fileId(), request.versionId(), accessEvent.getId(),
                request.purpose(), request.ttlSeconds());
        DccIssuedViewerToken issuedToken = source == null ? viewerTokenService.issue(command)
                : viewerTokenService.issueSourceBound(command, source);
        return new DccPreviewAccessResult(accessEvent.getId(), accessEventCode, watermarkTrace.getId(),
                watermarkTraceCode, issuedToken.token(), issuedToken.payload().getTokenId(),
                issuedToken.payload().getNonce(), issuedAt, expiresAt, watermarkPayloadJson);
    }

    private Map<String, Object> watermarkPayload(DccPreviewAccessRequest request, String accessEventCode,
                                                 String watermarkTraceCode, LocalDateTime issuedAt,
                                                 LocalDateTime expiresAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("traceCode", watermarkTraceCode);
        payload.put("accessEventCode", accessEventCode);
        payload.put("tenantId", request.tenantId());
        payload.put("fileId", request.fileId());
        payload.put("versionId", StrUtil.trim(request.versionId()));
        payload.put("fileNumber", StrUtil.trimToEmpty(request.fileNumber()));
        payload.put("userId", request.userId());
        payload.put("userIdentifier", StrUtil.trim(request.userIdentifier()));
        payload.put("userDisplayName", StrUtil.trim(request.userDisplayName()));
        payload.put("deptId", request.deptId());
        payload.put("deptName", StrUtil.trim(request.deptName()));
        payload.put("tenantName", StrUtil.trim(request.tenantName()));
        payload.put("privacyMode", StrUtil.trim(request.privacyMode()));
        payload.put("purpose", StrUtil.trim(request.purpose()));
        payload.put("issuedAt", issuedAt.toString());
        payload.put("expiresAt", expiresAt.toString());
        return payload;
    }

    private void requireRequest(DccPreviewAccessRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("preview access request is required");
        }
        requirePositive(request.tenantId(), "tenantId");
        requirePositive(request.userId(), "userId");
        requirePositive(request.fileId(), "fileId");
        requireNotBlank(request.versionId(), "versionId");
        requireNotBlank(request.accessType(), "accessType");
        requireNotBlank(request.purpose(), "purpose");
        requirePositive(request.ttlSeconds(), "ttlSeconds");
        requireNotBlank(request.privacyMode(), "privacyMode");
        if ("PREVIEW".equals(StrUtil.trim(request.accessType()))
                && !Objects.equals(request.tenantId(), TenantContextHolder.getRequiredTenantId())) {
            throw new IllegalArgumentException("The PREVIEW request tenant does not match the current tenant");
        }
        if (request.processInstanceId() != null) {
            String round = request.processInstanceId();
            if (round.isBlank() || !round.equals(round.strip()) || round.length() > 128
                    || round.chars().anyMatch(value -> value < 33 || value > 126)
                    || !Objects.equals(request.tenantId(), TenantContextHolder.getRequiredTenantId())) {
                throw new IllegalArgumentException("Exact preview round and tenant identity are invalid");
            }
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

    private String newCode(String prefix) {
        return prefix + "-" + CODE_DATE_FORMATTER.format(clock.instant()) + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

}
