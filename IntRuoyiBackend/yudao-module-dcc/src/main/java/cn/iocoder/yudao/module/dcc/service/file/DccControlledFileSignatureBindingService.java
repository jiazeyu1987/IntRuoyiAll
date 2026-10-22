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
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper controlledFileMapper;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper distributionMapper;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMessageJobMapper messageJobMapper;
    @Resource private cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService gxpAuditService;

    /** Internal acknowledgment transaction only. Binds one authoritative projection without changing approvals. */
    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY,rollbackFor=Exception.class)
    @cn.iocoder.yudao.module.system.service.gxpaudit.GxpWriteOperation(operationId="dcc.distribution.recipient.signature-bind")
    public void bindElectronicReceipt(DccControlledFileDO file, Long signatureId, Long actorId,
                                      Long distributionId, Long recipientId) {
        if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Electronic receipt binding requires its outer acknowledgment transaction");
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var login=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser();
        if(TenantContextHolder.isIgnore() || actorId==null || actorId<=0 || signatureId==null || signatureId<=0
                || distributionId==null || distributionId<=0 || recipientId==null || recipientId<=0
                || login==null || !Objects.equals(login.getId(),actorId) || !Objects.equals(login.getTenantId(),tenant)
                || !Integer.valueOf(2).equals(login.getUserType()) || file==null || file.getId()==null
                || !Objects.equals(file.getTenantId(),tenant))
            throw exception(CONTROLLED_FILE_SIGNATURE_BINDING_FAILED,"回执绑定身份不完整或不匹配");
        var current=controlledFileMapper.selectByIdAndTenantForUpdate(tenant,file.getId());
        if(current==null || !Objects.equals(current.getTenantId(),tenant) || Boolean.TRUE.equals(current.getDeleted())
                || !Objects.equals(current.getId(),file.getId()) || !Objects.equals(current.getVersionNo(),file.getVersionNo())
                || !Objects.equals(current.getPublishedFileId(),file.getPublishedFileId()) || current.getPublishedFileId()==null
                || !java.util.Set.of("ACTIVE","SUPERSEDED","OBSOLETE").contains(current.getStatus())
                || current.getControlledTime()==null || current.getActivatedTime()==null || current.getDistributedTime()==null)
            throw exception(CONTROLLED_FILE_SIGNATURE_BINDING_FAILED,"回执绑定必须使用本租户已受控生效的精确版本");
        var scope=distributionMapper.selectByIdAndTenantForUpdate(tenant,distributionId);
        var recipient=distributionRecipientMapper.selectByIdAndTenantForUpdate(tenant,recipientId);
        if(scope==null || !Objects.equals(scope.getId(),distributionId) || !Objects.equals(scope.getTenantId(),tenant)
                || Boolean.TRUE.equals(scope.getDeleted()) || !Objects.equals(scope.getControlledFileId(),current.getId())
                || !"PUBLIC_FOLDER".equals(scope.getDistributionMedium()) || scope.getRecoveredAt()!=null
                || !java.util.Set.of("PENDING","SENT","READ","ACKNOWLEDGED").contains(scope.getStatus())
                || recipient==null || !Objects.equals(recipient.getId(),recipientId) || !Objects.equals(recipient.getTenantId(),tenant)
                || Boolean.TRUE.equals(recipient.getDeleted()) || !Objects.equals(recipient.getDistributionId(),distributionId)
                || !Objects.equals(recipient.getUserId(),actorId) || recipient.getMessageJobId()==null)
            throw exception(CONTROLLED_FILE_SIGNATURE_BINDING_FAILED,"回执绑定缺少独立精确下发义务");
        var job=messageJobMapper.selectByIdAndTenantForUpdate(tenant,recipient.getMessageJobId());
        var signature=signatureMapper.selectById(signatureId);
        if(job==null || !Objects.equals(job.getId(),recipient.getMessageJobId()) || !Objects.equals(job.getTenantId(),tenant)
                || Boolean.TRUE.equals(job.getDeleted()) || !"SENT".equals(job.getStatus()) || job.getSentAt()==null
                || !"DISTRIBUTION".equals(job.getBusinessType()) || !Objects.equals(job.getBusinessId(),distributionId)
                || !Objects.equals(job.getRecipientUserId(),actorId)
                || signature==null || !Objects.equals(signature.getId(),signatureId) || Boolean.TRUE.equals(signature.getDeleted())
                || !Objects.equals(signature.getControlledFileId(),current.getId()) || !Objects.equals(signature.getRevisionId(),current.getId())
                || !Objects.equals(signature.getVersionNo(),current.getVersionNo()) || !Objects.equals(signature.getActorId(),actorId)
                || !Objects.equals(signature.getTaskId(),"DISTRIBUTION:"+distributionId+":"+recipientId)
                || !"DISTRIBUTION_ACK".equals(signature.getActionType()) || !"DISTRIBUTION_ACK".equals(signature.getMeaningCode())
                || !"DISTRIBUTION_ACK".equals(signature.getSignaturePurpose()) || !Boolean.TRUE.equals(signature.getPasswordVerified())
                || signature.getSignedAt()==null || !"VALID".equals(signature.getEvidenceStatus())
                || !"HMAC_SHA256".equals(signature.getEvidenceHashAlgorithm()) || signature.getEvidenceHash()==null
                || !signature.getEvidenceHash().matches("[a-fA-F0-9]{64}"))
            throw exception(CONTROLLED_FILE_SIGNATURE_BINDING_FAILED,"回执必须引用本次人员义务的正式DCC签名");
        var physical=requireControlledCopyFile(current.getPublishedFileId());
        if(!Objects.equals(physical.getId(),current.getPublishedFileId()) || Boolean.TRUE.equals(physical.getDeleted()))
            throw exception(CONTROLLED_FILE_SIGNATURE_BINDING_FAILED,"受控副本对象身份不匹配");
        byte[] bytes=readControlledCopyContent(physical);
        String event="DCC_RECEIPT_BIND:"+sha256Hex(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                List.of(tenant,current.getId(),current.getVersionNo(),distributionId,recipientId,signatureId,actorId)).getBytes(StandardCharsets.UTF_8));
        var candidate=createBindingEvent(signature,current,current.getPublishedFileId(),physical.getPath(),bytes,actorId,event);
        var existing=bindingMapper.selectBySignatureId(signatureId);
        if(existing!=null) {
            if(!sameImmutableBinding(existing,candidate) || !Objects.equals(existing.getBoundBy(),actorId)
                    || !Objects.equals(existing.getBindingEventKey(),event) || !hasValidBindingHash(existing))throw differentBinding(signatureId);
            return;
        }
        if(bindingMapper.insert(candidate)!=1 || candidate.getId()==null || candidate.getId()<=0)
            throw exception(CONTROLLED_FILE_SIGNATURE_BINDING_FAILED,"回执绑定未持久化一条记录");
        var persisted=bindingMapper.selectBySignatureId(signatureId);
        if(persisted==null || !Objects.equals(persisted.getId(),candidate.getId()) || !sameImmutableBinding(persisted,candidate)
                || !Objects.equals(persisted.getBoundBy(),actorId) || !Objects.equals(persisted.getBindingEventKey(),event)
                || !Objects.equals(persisted.getBoundAt(),candidate.getBoundAt()) || !hasValidBindingHash(persisted))
            throw exception(CONTROLLED_FILE_SIGNATURE_BINDING_FAILED,"回执绑定持久化证据不匹配");
        var facts=new java.util.TreeMap<String,Object>();facts.put("tenantId",tenant);facts.put("controlledFileId",current.getId());
        facts.put("versionNo",current.getVersionNo());facts.put("distributionId",distributionId);facts.put("recipientId",recipientId);
        facts.put("actorId",actorId);facts.put("signatureId",signatureId);facts.put("signatureEvidenceHash",signature.getEvidenceHash());
        facts.put("controlledCopyFileId",current.getPublishedFileId());facts.put("bindingHash",persisted.getBindingHash());
        facts.put("controlledCopySha256",persisted.getControlledCopySha256());facts.put("bindingEventKey",event);
        gxpAuditService.append(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand.builder()
                .operationId("dcc.distribution.recipient.signature-bind").subjectId("DCC_DISTRIBUTION_RECIPIENT:"+tenant+":"+recipientId)
                .subjectVersion(current.getVersionNo()).reason("将本次精确下发签收的正式签名绑定受控副本")
                .signatureRecordId("DCC_CONTROLLED_FILE_SIGNATURE:"+signatureId).signatureContentHash(signature.getEvidenceHash())
                .beforeState(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope.builder().state("ABSENT")
                        .objectVersion(current.getVersionNo()).canonicalJson("{}").build())
                .afterState(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope.builder().state("BOUND")
                        .objectVersion(current.getVersionNo()).canonicalJson(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(facts)).build())
                .idempotencyKey(event).source("DccControlledFileSignatureBindingService.bindElectronicReceipt").build());
    }

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
