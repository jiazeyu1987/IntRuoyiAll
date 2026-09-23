package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import com.alibaba.fastjson.JSON;
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
        if (taskId == null || actorId == null || subjectId == null || subjectId.isBlank()
                || evidenceHash == null || evidenceHash.isBlank()) {
            return Optional.empty();
        }
        Long verifiedSignatureId = null;
        for (var signature : signatures.listBySubject("BPM", "BPM_APPROVAL_TASK", subjectId)) {
            if (!Objects.equals(signature.actorId(), actorId)
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
            if (content == null || !"EDHR".equals(content.getString("moduleCode"))
                    || !"EDHR_WORK_TASK".equals(content.getString("sourceTaskType"))
                    || !String.valueOf(taskId).equals(content.getString("sourceTaskId"))) {
                continue;
            }
            String expectedOpinion = StrUtil.trim(opinion);
            if (StrUtil.isNotBlank(expectedOpinion)
                    && !Objects.equals(StrUtil.trim(signature.reason()), expectedOpinion)) {
                continue;
            }
            var verification = signatures.verifyEvidence(signature.id());
            if (verification != null && "VALID".equals(verification.verificationStatus())
                    && Objects.equals(signature.id(), verification.signatureId())
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
}
