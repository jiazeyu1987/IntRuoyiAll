package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordChangeEventDO;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** The exact event FK and signed subject establish the decision, never task status alone. */
@Component
public class MesEdhrRecordChangeApprovalEvidence {
    @Resource private ElectronicSignatureQueryService signatures;

    public ElectronicSignatureEvidenceDTO requireApproved(MesProEdhrRecordChangeEventDO event) {
        require(event != null && "EFFECTIVE".equals(event.getChangeStatus())
                && positive(event.getApprovalSignatureId()) && positive(event.getApprovedBy())
                && !Objects.equals(event.getRequestedBy(), event.getApprovedBy())
                && event.getApprovedAt() != null && event.getEffectiveAt() != null, "批准事实缺失");
        return requireSignature(event, "APPROVE");
    }

    public ElectronicSignatureEvidenceDTO requireRejected(MesProEdhrRecordChangeEventDO event) {
        require(event != null && java.util.Set.of("REJECTED", "RETURNED", "RESUBMITTED").contains(event.getChangeStatus())
                && positive(event.getApprovalSignatureId()) && positive(event.getApprovedBy())
                && !Objects.equals(event.getRequestedBy(), event.getApprovedBy()) && event.getApprovedAt() != null
                && event.getEffectiveAt() == null && event.getDecisionReason() != null && !event.getDecisionReason().isBlank(),
                "否决事实缺失");
        return requireSignature(event, "REJECT");
    }

    private ElectronicSignatureEvidenceDTO requireSignature(MesProEdhrRecordChangeEventDO event, String action) {
        var signature = signatures.getById(event.getApprovalSignatureId());
        require(signature != null && Objects.equals(signature.id(), event.getApprovalSignatureId())
                && Objects.equals(signature.actorId(), event.getApprovedBy())
                && "MES".equals(signature.moduleCode()) && "MES_BATCH_RECORD".equals(signature.subjectType())
                && action.equals(signature.actionCode()) && "VALID".equals(signature.verificationStatus())
                && "SESSION_PLUS_PASSWORD".equals(signature.authenticationMethod())
                && signature.signedAt() != null
                && !signature.signedAt().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).isAfter(event.getApprovedAt()), "批准签名身份冲突");
        var content = JsonUtils.parseTree(signature.canonicalContentJson());
        var business = JsonUtils.parseTree(MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(
                signature.subjectId(), action, signature.reason()));
        require(business != null && "RECORD_CHANGE".equals(business.path("reviewSourceType").asText())
                && event.getId().toString().equals(business.path("reviewSourceId").asText())
                && action.equals(business.path("approvalResult").asText())
                && Objects.toString(event.getExecutionId(), "0").equals(business.path("executionId").asText())
                && content != null, "签名未绑定本次记录变更");
        if ("REJECT".equals(action)) {
            require(event.getDecisionReason().equals(business.path("reason").asText()), "否决原因与签名正文冲突");
        }
        business.fields().forEachRemaining(field -> require(Objects.equals(field.getValue(), content.get(field.getKey())),
                "批准签名业务正文与签名主题冲突"));
        var verified = signatures.verifyEvidence(signature.id());
        require(verified != null && Objects.equals(signature.id(), verified.signatureId())
                && "VALID".equals(verified.verificationStatus())
                && signature.contentHash() != null && signature.evidenceHash() != null
                && Objects.equals(signature.contentHash(), verified.storedContentHash())
                && Objects.equals(signature.contentHash(), verified.calculatedContentHash())
                && Objects.equals(signature.evidenceHash(), verified.storedEvidenceHash())
                && Objects.equals(signature.evidenceHash(), verified.calculatedEvidenceHash()), "批准签名证据校验失败");
        return signature;
    }

    private boolean positive(Long value) { return value != null && value > 0; }
    private void require(boolean condition, String reason) {
        if (!condition) throw new IllegalStateException("记录变更批准证据无效：" + reason);
    }
}
