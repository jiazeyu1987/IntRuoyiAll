package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureBindingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureBindingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_BINDING_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING;

@Service
public class DccControlledFileSignatureBindingService {

    static final String COPY_HASH_ALGORITHM = "SHA256";
    static final String BINDING_PAYLOAD_VERSION = "v2";
    static final String LEGACY_BINDING_PAYLOAD_VERSION = "v1";

    @Resource
    private DccControlledFileSignatureBindingMapper bindingMapper;
    @Resource
    private DccControlledFileSignatureMapper signatureMapper;
    @Resource
    private FileService fileService;

    public void bindPublishedCopy(DccControlledFileDO file, Long controlledCopyFileId,
                                  Long boundBy, String bindingEventKey) {
        if (file == null || file.getId() == null || controlledCopyFileId == null || StrUtil.isBlank(bindingEventKey)) {
            throw exception(CONTROLLED_FILE_SIGNATURE_BINDING_FAILED, "绑定参数不完整");
        }
        List<DccControlledFileSignatureDO> signatures = signatureMapper.selectListByControlledFileId(file.getId());
        if (signatures == null || signatures.isEmpty()) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
        FileDO controlledCopyFile = requireControlledCopyFile(controlledCopyFileId);
        byte[] controlledCopyContent = readControlledCopyContent(controlledCopyFile);
        for (DccControlledFileSignatureDO signature : signatures) {
            if (signature == null || signature.getId() == null || StrUtil.isBlank(signature.getEvidenceHash())) {
                throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            }
            DccControlledFileSignatureBindingDO candidate = createBindingEvent(signature, file, controlledCopyFileId,
                    controlledCopyFile.getPath(), controlledCopyContent, boundBy, bindingEventKey);
            DccControlledFileSignatureBindingDO existing = bindingMapper.selectBySignatureId(signature.getId());
            if (existing == null) {
                bindingMapper.insert(candidate);
                continue;
            }
            if (!sameImmutableBinding(existing, candidate) || !hasValidBindingHash(existing)) {
                throw exception(CONTROLLED_FILE_SIGNATURE_BINDING_FAILED,
                        "签名 " + signature.getId() + " 已存在不同的受控副本绑定");
            }
        }
    }

    public void bindPublishedCopyAfterEvidenceReissue(DccControlledFileDO file, Long controlledCopyFileId,
                                                       Long boundBy, String bindingEventKey,
                                                       Map<Long, String> previousEvidenceHashes) {
        if (file == null || file.getId() == null || controlledCopyFileId == null
                || StrUtil.isBlank(bindingEventKey) || previousEvidenceHashes == null
                || previousEvidenceHashes.isEmpty()) {
            throw exception(CONTROLLED_FILE_SIGNATURE_BINDING_FAILED, "重新封存绑定参数不完整");
        }
        List<DccControlledFileSignatureDO> signatures = signatureMapper.selectListByControlledFileId(file.getId());
        if (signatures == null || signatures.isEmpty()) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
        FileDO controlledCopyFile = requireControlledCopyFile(controlledCopyFileId);
        byte[] controlledCopyContent = readControlledCopyContent(controlledCopyFile);
        for (DccControlledFileSignatureDO signature : signatures) {
            if (signature == null || signature.getId() == null || StrUtil.isBlank(signature.getEvidenceHash())) {
                throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            }
            DccControlledFileSignatureBindingDO candidate = createBindingEvent(signature, file, controlledCopyFileId,
                    controlledCopyFile.getPath(), controlledCopyContent, boundBy, bindingEventKey);
            DccControlledFileSignatureBindingDO existing = bindingMapper.selectBySignatureId(signature.getId());
            if (existing == null) {
                bindingMapper.insert(candidate);
                continue;
            }
            String previousEvidenceHash = previousEvidenceHashes.get(signature.getId());
            if (previousEvidenceHash == null) {
                if (!sameImmutableBinding(existing, candidate) || !hasValidBindingHash(existing)) {
                    throw differentBinding(signature.getId());
                }
                continue;
            }
            if (!sameReissuableBinding(existing, candidate, previousEvidenceHash)
                    || !hasValidBindingHash(existing)) {
                throw differentBinding(signature.getId());
            }
            candidate.setId(existing.getId());
            if (bindingMapper.updateById(candidate) <= 0) {
                throw exception(CONTROLLED_FILE_SIGNATURE_BINDING_FAILED,
                        "签名 " + signature.getId() + " 重新封存绑定更新失败");
            }
        }
    }

    DccControlledFileSignatureBindingDO createBindingEvent(DccControlledFileSignatureDO signature,
                                                            DccControlledFileDO file,
                                                            Long controlledCopyFileId,
                                                            String controlledCopyObjectKey,
                                                            byte[] controlledCopyContent,
                                                            Long boundBy,
                                                            String bindingEventKey) {
        DccControlledFileSignatureBindingDO binding = DccControlledFileSignatureBindingDO.builder()
                .signatureId(signature.getId())
                .controlledFileId(file.getId())
                .originalEvidenceHash(signature.getEvidenceHash())
                .controlledCopyFileId(controlledCopyFileId)
                .controlledCopyObjectKey(controlledCopyObjectKey)
                .controlledCopySha256(sha256Hex(controlledCopyContent))
                .controlledCopyHashAlgorithm(COPY_HASH_ALGORITHM)
                .boundAt(LocalDateTime.now().withNano(0))
                .boundBy(boundBy)
                .bindingEventKey(bindingEventKey)
                .bindingPayloadVersion(BINDING_PAYLOAD_VERSION)
                .bindingHashAlgorithm(COPY_HASH_ALGORITHM)
                .build();
        binding.setBindingHash(sha256Hex(canonicalBindingPayload(binding).getBytes(StandardCharsets.UTF_8)));
        return binding;
    }

    public DccControlledFileSignatureBindingVerification verifyPublishedCopyBinding(
            DccControlledFileSignatureDO signature, DccControlledFileDO file) {
        if (file == null || file.getPublishedFileId() == null) {
            return DccControlledFileSignatureBindingVerification.notApplicable();
        }
        DccControlledFileSignatureBindingDO binding = bindingMapper.selectBySignatureId(signature.getId());
        if (binding == null) {
            return DccControlledFileSignatureBindingVerification.invalid("CONTROLLED_COPY_BINDING_MISSING");
        }
        if (!Objects.equals(binding.getSignatureId(), signature.getId())
                || !Objects.equals(binding.getControlledFileId(), file.getId())
                || !Objects.equals(binding.getControlledCopyFileId(), file.getPublishedFileId())) {
            return DccControlledFileSignatureBindingVerification.invalid(
                    "CONTROLLED_COPY_BINDING_CONTEXT_MISMATCH", binding);
        }
        if (!hasValidBindingHash(binding)) {
            return DccControlledFileSignatureBindingVerification.invalid("CONTROLLED_COPY_BINDING_TAMPERED", binding);
        }
        byte[] currentContent;
        try {
            FileDO currentFile = requireControlledCopyFile(binding.getControlledCopyFileId());
            if (!StrUtil.equals(binding.getControlledCopyObjectKey(), currentFile.getPath())) {
                return DccControlledFileSignatureBindingVerification.invalid(
                        "CONTROLLED_COPY_OBJECT_KEY_MISMATCH", binding);
            }
            currentContent = readControlledCopyContent(currentFile);
        } catch (RuntimeException ex) {
            return DccControlledFileSignatureBindingVerification.invalid("CONTROLLED_COPY_FILE_UNREADABLE", binding);
        }
        if (!StrUtil.equalsIgnoreCase(binding.getControlledCopySha256(), sha256Hex(currentContent))) {
            return DccControlledFileSignatureBindingVerification.invalid("CONTROLLED_COPY_HASH_MISMATCH", binding);
        }
        if (!StrUtil.equalsIgnoreCase(binding.getOriginalEvidenceHash(), signature.getEvidenceHash())) {
            return DccControlledFileSignatureBindingVerification.invalid(
                    "CONTROLLED_COPY_BINDING_EVIDENCE_MISMATCH", binding);
        }
        return DccControlledFileSignatureBindingVerification.bound(binding);
    }

    private FileDO requireControlledCopyFile(Long fileId) {
        FileDO file = fileService.getFile(fileId);
        if (file == null || file.getConfigId() == null || StrUtil.isBlank(file.getPath())) {
            throw exception(CONTROLLED_FILE_SIGNATURE_BINDING_FAILED, "受控副本文件不存在");
        }
        return file;
    }

    private byte[] readControlledCopyContent(FileDO file) {
        try {
            byte[] content = fileService.getFileContent(file.getConfigId(), file.getPath());
            if (content == null || content.length == 0) {
                throw exception(CONTROLLED_FILE_SIGNATURE_BINDING_FAILED, "受控副本内容为空");
            }
            return content;
        } catch (Exception ex) {
            throw exception(CONTROLLED_FILE_SIGNATURE_BINDING_FAILED, "受控副本不可读取");
        }
    }

    private boolean sameImmutableBinding(DccControlledFileSignatureBindingDO left,
                                         DccControlledFileSignatureBindingDO right) {
        return Objects.equals(left.getSignatureId(), right.getSignatureId())
                && Objects.equals(left.getControlledFileId(), right.getControlledFileId())
                && StrUtil.equalsIgnoreCase(left.getOriginalEvidenceHash(), right.getOriginalEvidenceHash())
                && Objects.equals(left.getControlledCopyFileId(), right.getControlledCopyFileId())
                && StrUtil.equals(left.getControlledCopyObjectKey(), right.getControlledCopyObjectKey())
                && StrUtil.equalsIgnoreCase(left.getControlledCopySha256(), right.getControlledCopySha256());
    }

    private boolean sameReissuableBinding(DccControlledFileSignatureBindingDO existing,
                                          DccControlledFileSignatureBindingDO candidate,
                                          String previousEvidenceHash) {
        return Objects.equals(existing.getSignatureId(), candidate.getSignatureId())
                && Objects.equals(existing.getControlledFileId(), candidate.getControlledFileId())
                && StrUtil.equalsIgnoreCase(existing.getOriginalEvidenceHash(), previousEvidenceHash)
                && Objects.equals(existing.getControlledCopyFileId(), candidate.getControlledCopyFileId())
                && StrUtil.equals(existing.getControlledCopyObjectKey(), candidate.getControlledCopyObjectKey())
                && StrUtil.equalsIgnoreCase(existing.getControlledCopySha256(), candidate.getControlledCopySha256());
    }

    private RuntimeException differentBinding(Long signatureId) {
        return exception(CONTROLLED_FILE_SIGNATURE_BINDING_FAILED,
                "签名 " + signatureId + " 已存在不同的受控副本绑定");
    }

    private boolean hasValidBindingHash(DccControlledFileSignatureBindingDO binding) {
        if (!COPY_HASH_ALGORITHM.equals(binding.getBindingHashAlgorithm())) {
            return false;
        }
        String canonicalPayload;
        if (BINDING_PAYLOAD_VERSION.equals(binding.getBindingPayloadVersion())) {
            canonicalPayload = canonicalBindingPayload(binding);
        } else if (LEGACY_BINDING_PAYLOAD_VERSION.equals(binding.getBindingPayloadVersion())) {
            canonicalPayload = canonicalLegacyBindingPayload(binding);
        } else {
            return false;
        }
        return StrUtil.equalsIgnoreCase(binding.getBindingHash(),
                sha256Hex(canonicalPayload.getBytes(StandardCharsets.UTF_8)));
    }

    private String canonicalBindingPayload(DccControlledFileSignatureBindingDO binding) {
        return String.join("\n",
                "payloadVersion=" + StrUtil.nullToEmpty(binding.getBindingPayloadVersion()),
                "tenantId=" + TenantContextHolder.getRequiredTenantId(),
                "signatureId=" + binding.getSignatureId(),
                "controlledFileId=" + binding.getControlledFileId(),
                "originalEvidenceHash=" + StrUtil.nullToEmpty(binding.getOriginalEvidenceHash()),
                "controlledCopyFileId=" + binding.getControlledCopyFileId(),
                "controlledCopyObjectKey=" + StrUtil.nullToEmpty(binding.getControlledCopyObjectKey()),
                "controlledCopySha256=" + StrUtil.nullToEmpty(binding.getControlledCopySha256()),
                "controlledCopyHashAlgorithm=" + StrUtil.nullToEmpty(binding.getControlledCopyHashAlgorithm()),
                "boundAt=" + binding.getBoundAt(),
                "boundBy=" + binding.getBoundBy(),
                "bindingEventKey=" + StrUtil.nullToEmpty(binding.getBindingEventKey()));
    }

    private String canonicalLegacyBindingPayload(DccControlledFileSignatureBindingDO binding) {
        return String.join("\n",
                "payloadVersion=" + StrUtil.nullToEmpty(binding.getBindingPayloadVersion()),
                "tenantId=" + TenantContextHolder.getRequiredTenantId(),
                "signatureId=" + binding.getSignatureId(),
                "controlledFileId=" + binding.getControlledFileId(),
                "originalEvidenceHash=" + StrUtil.nullToEmpty(binding.getOriginalEvidenceHash()),
                "controlledCopyFileId=" + binding.getControlledCopyFileId(),
                "controlledCopySha256=" + StrUtil.nullToEmpty(binding.getControlledCopySha256()),
                "controlledCopyHashAlgorithm=" + StrUtil.nullToEmpty(binding.getControlledCopyHashAlgorithm()),
                "boundAt=" + binding.getBoundAt(),
                "boundBy=" + binding.getBoundBy(),
                "bindingEventKey=" + StrUtil.nullToEmpty(binding.getBindingEventKey()));
    }

    private String sha256Hex(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception ex) {
            throw exception(CONTROLLED_FILE_SIGNATURE_BINDING_FAILED, "SHA-256 计算失败");
        }
    }

}
