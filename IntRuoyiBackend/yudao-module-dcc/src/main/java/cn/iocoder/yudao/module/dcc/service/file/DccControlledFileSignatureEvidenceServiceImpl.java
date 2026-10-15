package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING;

@Service
@Validated
public class DccControlledFileSignatureEvidenceServiceImpl implements DccControlledFileSignatureEvidenceService {

    public static final String PAYLOAD_VERSION_V2 = "v2";
    public static final String PAYLOAD_VERSION_V3_IMAGE = "v3-image";
    public static final String PAYLOAD_VERSION_V4_WORKFLOW = "v4-workflow";
    public static final String PAYLOAD_VERSION_V5_DELAYED = "v5-delayed-effective";
    public static final String EVIDENCE_HASH_ALGORITHM = "HMAC_SHA256";
    public static final String FILE_HASH_ALGORITHM = "SHA-256";
    public static final String STATUS_VALID = "VALID";
    public static final String HASH_STATUS_BOUND = "BOUND";
    public static final String COPY_HASH_STATUS_NOT_APPLICABLE = "NOT_APPLICABLE";

    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private FileService fileService;
    @Resource
    private DccSignatureEvidenceProperties signatureEvidenceProperties;

    @Override
    public DccControlledFileSignatureEvidence createEvidence(DccControlledFileSignatureEvidenceCreateReq req) {
        validateConfig();
        validateBaseReq(req);
        validateControlledCopyReq(req);
        validateSignatureImageReq(req);
        DccControlledFileDO revision = controlledFileMapper.selectById(req.getControlledFileId());
        if (revision == null
                || revision.getSourceFileId() == null
                || StrUtil.isBlank(revision.getFileNumber())
                || StrUtil.isBlank(revision.getVersionNo())
                || req.getProcessInstanceId() == null && StrUtil.isBlank(revision.getProcessInstanceId())) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
        String sourceFileHash = digestFile(revision.getSourceFileId());
        ControlledCopyEvidence controlledCopyEvidence = resolveControlledCopyEvidence(req);
        String signedAtText = toCanonicalSignedAt(req.getSignedAt());
        String canonicalPayload = buildCanonicalPayload(req, revision, sourceFileHash,
                controlledCopyEvidence.hashForPayload(), signedAtText);
        return DccControlledFileSignatureEvidence.builder()
                .selectedEffectiveTime(req.getSelectedEffectiveTime()).attributeRound(req.getAttributeRound())
                .revisionId(revision.getId())
                .processInstanceId(req.getProcessInstanceId())
                .fileNumberSnapshot(req.getProcessInstanceId() == null ? null : revision.getFileNumber())
                .versionNo(revision.getVersionNo())
                .sourceFileId(revision.getSourceFileId())
                .sourceFileHash(sourceFileHash)
                .sourceFileHashAlgorithm(FILE_HASH_ALGORITHM)
                .sourceFileHashStatus(HASH_STATUS_BOUND)
                .controlledCopyFileId(controlledCopyEvidence.fileId())
                .controlledCopyHash(controlledCopyEvidence.hash())
                .controlledCopyHashAlgorithm(controlledCopyEvidence.hashAlgorithm())
                .controlledCopyHashStatus(req.getControlledCopyHashStatus())
                .signatureImageId(req.getSignatureImageId())
                .signatureImageVersionNo(req.getSignatureImageVersionNo())
                .signatureImageFileId(req.getSignatureImageFileId())
                .signatureImageFileUrl(req.getSignatureImageFileUrl())
                .signatureImageSha256(req.getSignatureImageSha256())
                .signatureImageContentType(req.getSignatureImageContentType())
                .signatureImageFileSize(req.getSignatureImageFileSize())
                .signatureImageStatusSnapshot(req.getSignatureImageStatusSnapshot())
                .signatureImageVerifiedStatus(req.getSignatureImageVerifiedStatus())
                .evidencePayloadVersion(payloadVersion(req))
                .evidenceKeyVersion(signatureEvidenceProperties.getKeyVersion())
                .evidenceHash(hmacSha256Hex(canonicalPayload))
                .evidenceHashAlgorithm(EVIDENCE_HASH_ALGORITHM)
                .evidenceStatus(STATUS_VALID)
                .canonicalPayload(canonicalPayload)
                .recordVersionSnapshot(revision.getVersionNo())
                .recordHashSnapshot(sourceFileHash)
                .build();
    }

    @Override
    public DccControlledFileSignatureEvidence createDelayedSigningIntent(DccControlledFileSignatureEvidenceCreateReq req){
        validateConfig();validateSignatureImageReq(req);
        if(req==null || !"DELAYED_EFFECTIVE_CONFIRM_APPROVE".equals(req.getMeaningCode()) || req.getTenantId()==null
                || req.getProcessInstanceId()==null || req.getProcessInstanceId().isBlank() || req.getTaskId()==null || req.getTaskId().isBlank()
                || req.getSignerUserId()==null || req.getSelectedEffectiveTime()==null || req.getSelectedEffectiveTime().getNano()!=0
                || req.getAttributeRound()==null || req.getAttributeRound()<=0 || req.getReasonText()==null || req.getReasonText().isBlank())
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        var file=controlledFileMapper.selectById(req.getControlledFileId());
        if(file==null || file.getSourceFileId()==null || !Objects.equals(file.getTenantId(),req.getTenantId())
                || !Objects.equals(req.getTenantId(),cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())
                || Boolean.TRUE.equals(file.getDeleted()) || !Objects.equals(file.getRequesterId(),req.getSignerUserId())
                || !Objects.equals(file.getProcessInstanceId(),req.getProcessInstanceId()))throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        String sourceHash=digestFile(file.getSourceFileId());req.setSignedAt(null);
        return DccControlledFileSignatureEvidence.builder().revisionId(file.getId()).versionNo(file.getVersionNo()).fileNumberSnapshot(file.getFileNumber())
                .processInstanceId(req.getProcessInstanceId()).sourceFileId(file.getSourceFileId()).sourceFileHash(sourceHash).recordHashSnapshot(sourceHash)
                .recordVersionSnapshot(file.getVersionNo()).controlledCopyHash("").evidenceHash("").evidencePayloadVersion(PAYLOAD_VERSION_V5_DELAYED)
                .selectedEffectiveTime(req.getSelectedEffectiveTime()).attributeRound(req.getAttributeRound())
                .canonicalPayload(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(req)).build();
    }

    private void validateConfig() {
        signatureEvidenceProperties.validateRuntimeConfig();
    }

    private static void validateBaseReq(DccControlledFileSignatureEvidenceCreateReq req) {
        if (req == null
                || req.getTenantId() == null
                || req.getControlledFileId() == null
                || StrUtil.isBlank(req.getTaskId())
                || StrUtil.isBlank(req.getTaskActionResult())
                || StrUtil.isBlank(req.getMeaningCode())
                || req.getSignerUserId() == null
                || StrUtil.isBlank(req.getSignerUsername())
                || StrUtil.isBlank(req.getSignerNickname())
                || StrUtil.isBlank(req.getSignerPostNames())
                || StrUtil.isBlank(req.getSignerRoleNames())
                || StrUtil.isBlank(req.getSignaturePurpose())
                || StrUtil.isBlank(req.getAuthorizationBasis())
                || StrUtil.isBlank(req.getAuthenticationMethod())
                || req.getSignedAt() == null) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
        if (req.getProcessInstanceId() != null && StrUtil.isBlank(req.getProcessInstanceId()))
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        if ("DELAYED_EFFECTIVE_CONFIRM_APPROVE".equals(req.getMeaningCode())) {
            if(req.getProcessInstanceId()==null || req.getAttributeRound()==null || req.getAttributeRound()<=0
                    || req.getSelectedEffectiveTime()==null || req.getSelectedEffectiveTime().getNano()!=0
                    || req.getSelectedEffectiveTime().isBefore(req.getSignedAt()) || StrUtil.isBlank(req.getReasonText()))
                throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        } else if(req.getSelectedEffectiveTime()!=null || req.getAttributeRound()!=null)
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
    }

    private static void validateControlledCopyReq(DccControlledFileSignatureEvidenceCreateReq req) {
        if (HASH_STATUS_BOUND.equals(req.getControlledCopyHashStatus())) {
            if (req.getControlledCopyFileId() == null) {
                throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            }
            return;
        }
        if (COPY_HASH_STATUS_NOT_APPLICABLE.equals(req.getControlledCopyHashStatus())) {
            if (req.getControlledCopyFileId() != null) {
                throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            }
            return;
        }
        throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
    }

    private static void validateSignatureImageReq(DccControlledFileSignatureEvidenceCreateReq req) {
        if (req.getSignatureImageId() == null
                || req.getSignatureImageVersionNo() == null
                || req.getSignatureImageFileId() == null
                || StrUtil.hasBlank(req.getSignatureImageSha256(), req.getSignatureImageContentType(),
                req.getSignatureImageStatusSnapshot(), req.getSignatureImageVerifiedStatus())) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
    }

    private ControlledCopyEvidence resolveControlledCopyEvidence(DccControlledFileSignatureEvidenceCreateReq req) {
        if (COPY_HASH_STATUS_NOT_APPLICABLE.equals(req.getControlledCopyHashStatus())) {
            return new ControlledCopyEvidence(null, null, null, "");
        }
        String hash = digestFile(req.getControlledCopyFileId());
        return new ControlledCopyEvidence(req.getControlledCopyFileId(), hash, FILE_HASH_ALGORITHM, hash);
    }

    private String digestFile(Long fileId) {
        try {
            FileDO file = fileService.getFile(fileId);
            if (file == null || file.getConfigId() == null || StrUtil.isBlank(file.getPath())) {
                throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            }
            byte[] content = fileService.getFileContent(file.getConfigId(), file.getPath());
            if (content == null) {
                throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance(FILE_HASH_ALGORITHM).digest(content));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
    }

    private String hmacSha256Hex(String canonicalPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signatureEvidenceProperties.getHmacSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonicalPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
    }

    private String buildCanonicalPayload(DccControlledFileSignatureEvidenceCreateReq req,
                                         DccControlledFileDO revision,
                                         String sourceFileHash,
                                         String controlledCopyHash,
                                         String signedAtText) {
        StringBuilder payload = new StringBuilder("{");
        append(payload, "payloadVersion", payloadVersion(req));
        append(payload, "hashAlgorithm", EVIDENCE_HASH_ALGORITHM);
        append(payload, "keyVersion", signatureEvidenceProperties.getKeyVersion());
        append(payload, "tenantId", req.getTenantId());
        append(payload, "controlledFileId", req.getControlledFileId());
        append(payload, "fileNumber", revision.getFileNumber());
        append(payload, "revisionId", revision.getId());
        append(payload, "versionNo", revision.getVersionNo());
        append(payload, "sourceFileHash", sourceFileHash);
        append(payload, "controlledCopyHashStatus", req.getControlledCopyHashStatus());
        append(payload, "controlledCopyHash", controlledCopyHash);
        append(payload, "signatureImageId", req.getSignatureImageId());
        append(payload, "signatureImageVersionNo", req.getSignatureImageVersionNo());
        append(payload, "signatureImageFileId", req.getSignatureImageFileId());
        append(payload, "signatureImageSha256", req.getSignatureImageSha256());
        append(payload, "signatureImageContentType", req.getSignatureImageContentType());
        append(payload, "signatureImageFileSize", req.getSignatureImageFileSize());
        append(payload, "signatureImageStatusSnapshot", req.getSignatureImageStatusSnapshot());
        append(payload, "signatureImageVerifiedStatus", req.getSignatureImageVerifiedStatus());
        append(payload, "processInstanceId", req.getProcessInstanceId() == null
                ? revision.getProcessInstanceId() : req.getProcessInstanceId());
        append(payload, "taskId", req.getTaskId());
        append(payload, "taskActionResult", req.getTaskActionResult());
        append(payload, "meaningCode", req.getMeaningCode());
        append(payload, "signerUserId", req.getSignerUserId());
        append(payload, "signerUsername", req.getSignerUsername());
        append(payload, "signerNickname", req.getSignerNickname());
        append(payload, "signerDeptId", req.getSignerDeptId());
        append(payload, "signerDeptName", req.getSignerDeptName());
        append(payload, "signerPostNames", req.getSignerPostNames());
        append(payload, "signerRoleNames", req.getSignerRoleNames());
        append(payload, "signaturePurpose", req.getSignaturePurpose());
        append(payload, "authorizationBasis", req.getAuthorizationBasis());
        append(payload, "authenticationMethod", req.getAuthenticationMethod());
        append(payload, "signedAt", signedAtText);
        append(payload, "reasonText", StrUtil.trimToEmpty(req.getReasonText()));
        if(PAYLOAD_VERSION_V5_DELAYED.equals(payloadVersion(req))) {
            append(payload, "selectedEffectiveTime", toCanonicalSignedAt(req.getSelectedEffectiveTime()));
            append(payload, "attributeRound", req.getAttributeRound());
        }
        payload.append('}');
        return payload.toString();
    }

    private static String payloadVersion(DccControlledFileSignatureEvidenceCreateReq req) {
        if("DELAYED_EFFECTIVE_CONFIRM_APPROVE".equals(req.getMeaningCode())) return PAYLOAD_VERSION_V5_DELAYED;
        return req.getProcessInstanceId() == null ? PAYLOAD_VERSION_V3_IMAGE : PAYLOAD_VERSION_V4_WORKFLOW;
    }

    private static void append(StringBuilder payload, String field, String value) {
        appendName(payload, field);
        payload.append('"').append(escapeJson(value)).append('"');
    }

    private static void append(StringBuilder payload, String field, Long value) {
        appendName(payload, field);
        payload.append(value);
    }

    private static void append(StringBuilder payload, String field, Integer value) {
        appendName(payload, field);
        payload.append(value);
    }

    private static void appendName(StringBuilder payload, String field) {
        if (payload.length() > 1) {
            payload.append(',');
        }
        payload.append('"').append(field).append("\":");
    }

    private static String escapeJson(String value) {
        return StrUtil.nullToEmpty(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static String toCanonicalSignedAt(LocalDateTime signedAt) {
        return OffsetDateTime.of(signedAt, ZoneOffset.ofHours(8)).toString();
    }

    private record ControlledCopyEvidence(Long fileId, String hash, String hashAlgorithm, String hashForPayload) {
    }
}
