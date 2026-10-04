package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCodes;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import com.alibaba.fastjson.JSON;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
public class MesProductionReleaseSignoffService {
    private final ElectronicSignatureQueryService signatures;

    public MesProductionReleaseSignoffService(ElectronicSignatureQueryService signatures) {
        this.signatures = signatures;
    }

    public boolean isVerified(Long taskId, Long actorId, String subjectId, String evidenceHash, String opinion) {
        return findVerifiedSignatureId(taskId, actorId, subjectId, evidenceHash, opinion).isPresent();
    }

    public Optional<Long> findVerifiedSignatureId(
            Long taskId, Long actorId, String subjectId, String evidenceHash, String opinion) {
        if (taskId == null || taskId <= 0 || actorId == null || actorId <= 0 || subjectId == null || subjectId.isBlank()
                || evidenceHash == null || evidenceHash.isBlank()) {
            return Optional.empty();
        }
        Long verifiedSignatureId = null;
        for (var signature : signatures.listBySubject("BPM", "BPM_APPROVAL_TASK", subjectId)) {
            if (signature.id() == null || signature.id() <= 0 || !Objects.equals(signature.actorId(), actorId)
                    || !Objects.equals(signature.taskId(), String.valueOf(taskId))
                    || !Objects.equals(signature.subjectId(), subjectId)
                    || !"BPM".equals(signature.moduleCode())
                    || !"BPM_APPROVAL_TASK".equals(signature.subjectType())
                    || !"APPROVE".equals(signature.actionCode())
                    || !"SESSION_PLUS_PASSWORD".equals(signature.authenticationMethod())
                    || !"VALID".equals(signature.verificationStatus())
                    || !Objects.equals(signature.evidenceHash(), evidenceHash)) {
                continue;
            }
            var content = JSON.parseObject(signature.canonicalContentJson());
            if (content == null || !"BPM_APPROVAL".equals(content.getString("adapter"))
                    || !"EDHR".equals(content.getString("moduleCode"))
                    || !"EDHR_WORK_TASK".equals(content.getString("sourceTaskType"))
                    || !String.valueOf(taskId).equals(content.getString("sourceTaskId"))
                    || !String.valueOf(taskId).equals(content.getString("businessKey"))
                    || !"".equals(content.getString("processInstanceId"))
                    || !"APPROVE".equals(content.getString("reviewResult"))
                    || !Objects.equals(signature.reason(), content.getString("reason"))
                    || signature.signedAt() == null || signature.contentHash() == null
                    || signature.contentHash().isBlank()
                    || !Objects.equals(DigestUtil.sha256Hex(subjectId), signature.subjectVersion())) {
                continue;
            }
            String[] subject = new String(Base64.getUrlDecoder().decode(subjectId), StandardCharsets.UTF_8).split("\n", -1);
            if (subject.length != 6 || !"EDHR".equals(subject[0]) || !"EDHR_WORK_TASK".equals(subject[1])
                    || !String.valueOf(taskId).equals(subject[2]) || !String.valueOf(taskId).equals(subject[3])
                    || !subject[4].isEmpty() || !subject[5].matches("[0-9a-f]{64}")
                    || !Objects.equals(subject[5], content.getString("signatureImagePayloadHash"))) continue;
            String expectedOpinion = StrUtil.trim(opinion);
            if (StrUtil.isNotBlank(expectedOpinion)
                    && !Objects.equals(StrUtil.trim(signature.reason()), expectedOpinion)) {
                continue;
            }
            var verification = signatures.verifyEvidence(signature.id());
            if (verification != null && "VALID".equals(verification.verificationStatus())
                    && Objects.equals(signature.id(), verification.signatureId())
                    && Objects.equals(signature.contentHash(), verification.storedContentHash())
                    && Objects.equals(signature.contentHash(), verification.calculatedContentHash())
                    && Objects.equals(evidenceHash, verification.storedEvidenceHash())
                    && Objects.equals(evidenceHash, verification.calculatedEvidenceHash())) {
                if (verifiedSignatureId != null && !Objects.equals(verifiedSignatureId, signature.id())) {
                    throw new IllegalStateException("EDHR_MARKET_RELEASE_SIGNATURE_AMBIGUOUS");
                }
                verifiedSignatureId = signature.id();
            }
        }
        return Optional.ofNullable(verifiedSignatureId);
    }

    /** Bind BPM's exact task to the persisted MES owner graph; never use signature metadata as FKs. */
    public static void requireBinding(MesProEdhrWorkTaskDO task,
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrReleaseTransactionDO transaction, MesProEdhrBatchExecutionDO batch) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        if (task == null || application == null || transaction == null || batch == null
                || !positive(task.getId()) || !positive(application.getId())
                || !positive(transaction.getId()) || !positive(batch.getId())
                || !Objects.equals(application.getTenantId(), tenant) || !Objects.equals(batch.getTenantId(), tenant)
                || application.getActiveOrderId() == null || application.getActiveOrderId() <= 0
                || !Objects.equals(application.getReleaseApprovalWorkTaskId(), task.getId())
                || !Objects.equals(application.getReleaseTransactionId(), transaction.getId())
                || !Objects.equals(application.getBatchExecutionId(), batch.getId())
                || !Objects.equals(transaction.getBatchExecutionId(), batch.getId())
                || !Objects.equals(task.getBatchExecutionId(), batch.getId())
                || !"RELEASE_APPROVE".equals(task.getTaskType())
                || !"RELEASE_TRANSACTION".equals(task.getBusinessScopeType())
                || !Objects.equals(task.getBusinessScopeId(), transaction.getId())
                || !"ROLE_GROUP".equals(task.getCandidateSourceType())
                || !MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE.equals(task.getResponsibilitySourceKey())) {
            throw new IllegalStateException("EDHR_MARKET_RELEASE_SIGNATURE_BINDING_INVALID");
        }
    }

    private static boolean positive(Long id) { return id != null && id > 0; }
}
