package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationHandlingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Reads the exact signed responsibility receipt; task completion alone proves no approval. */
@Component
public class MesEdhrDeviationApprovalOutcomeReader {
    @Resource private MesProEdhrDeviationMapper deviations;
    @Resource private MesProEdhrDeviationHandlingMapper handlings;
    @Resource private ElectronicSignatureQueryService signatures;

    public MesEdhrApprovalOutcome read(MesProEdhrWorkTaskDO task, Long tenant) {
        if (!"DEVIATION_APPROVE".equals(task.getTaskType()) || !"DONE".equals(task.getStatus())
                || !"EDHR_DEVIATION".equals(task.getBusinessScopeType())
                || task.getBusinessScopeId() == null || task.getResponsibilityScopeJson() == null
                || !"EDHR_DEVIATION_HANDLING".equals(task.getReviewSourceType())
                || !JSON.isValidObject(task.getResponsibilityScopeJson())
                || task.getCompletedAt() == null) return unknown(task, null);
        String action = switch (Objects.toString(task.getSignatureCellKey(), "")) {
            case "QA" -> MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_QA;
            case "QUALITY_OWNER" -> MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_QUALITY;
            case "MANAGEMENT_REP" -> MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_MANAGEMENT;
            default -> null;
        };
        if (action == null) return unknown(task, null);
        var receipt = JSON.parseObject(task.getResponsibilityScopeJson());
        if (receipt == null || !positiveNumber(receipt.get("signatureId"))
                || !positiveNumber(receipt.get("actorUserId")) || !positiveNumber(receipt.get("handlingId"))
                || !Objects.equals(receipt.getLong("actorUserId"), task.getAssigneeUserId())
                || !Objects.equals(receipt.getLong("handlingId"), task.getReviewSourceId())
                || !Objects.equals(receipt.getString("node"), task.getSignatureCellKey())
                || !Objects.equals(receipt.getString("contentVersion"), task.getResponsibilitySourceVersion())
                || !Objects.equals(receipt.getString("contentHash"), task.getResponsibilitySourceDigest())) {
            return unknown(task, null);
        }
        var deviation = deviations.selectByTenantAndId(tenant, task.getBusinessScopeId());
        var handling = handlings.selectByTenantAndDeviationId(tenant, task.getBusinessScopeId());
        if (deviation == null || handling == null
                || !Objects.equals(deviation.getTenantId(), tenant) || !Objects.equals(handling.getTenantId(), tenant)
                || !Objects.equals(deviation.getId(), task.getBusinessScopeId())
                || !Objects.equals(deviation.getWorkOrderId(), task.getWorkOrderId())
                || !Objects.equals(deviation.getBatchExecutionId(), task.getBatchExecutionId())
                || !Objects.equals(handling.getId(), task.getReviewSourceId())
                || !Objects.equals(handling.getDeviationId(), deviation.getId())) return unknown(task, null);
        if (!Objects.equals(Objects.toString(handling.getContentVersion(), null), task.getResponsibilitySourceVersion())
                || !Objects.equals(handling.getContentHash(), task.getResponsibilitySourceDigest())) {
            return unknown(task, "SUPERSEDED");
        }
        var assignees = handling.getReviewerAssignmentsJson() == null || !JSON.isValidObject(handling.getReviewerAssignmentsJson())
                ? null : JSON.parseObject(handling.getReviewerAssignmentsJson());
        if (assignees == null || !positiveNumber(assignees.get(task.getSignatureCellKey()))
                || !Objects.equals(assignees.getLong(task.getSignatureCellKey()), task.getAssigneeUserId())) {
            return unknown(task, deviation.getStatus());
        }
        String intent = "偏差 " + deviation.getDeviationCode() + " 处理节点 " + task.getSignatureCellKey();
        String opinion = receipt.getString("userOpinion");
        if (opinion == null) return unknown(task, deviation.getStatus());
        String expectedReason = opinion.isBlank() ? intent : opinion.trim();
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, action,
                null, null, null, null, null, null, null, "EDHR_DEVIATION_HANDLING", handling.getId(),
                intent, task.getSignatureCellKey(), handling.getContentVersion().longValue(),
                handling.getContentHash(), handling.getContentHash(), null);
        var signature = signatures.getById(receipt.getLong("signatureId"));
        if (signature == null || !Objects.equals(signature.id(), receipt.getLong("signatureId"))
                || !"MES".equals(signature.moduleCode()) || !Objects.equals(action, signature.actionCode())
                || !"MES_BATCH_RECORD".equals(signature.subjectType()) || !Objects.equals(subject, signature.subjectId())
                || !Objects.equals(MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject), signature.subjectVersion())
                || !Objects.equals(task.getAssigneeUserId(), signature.actorId())
                || !Objects.equals(expectedReason, signature.reason())
                || !"VALID".equals(signature.verificationStatus()) || signature.signedAt() == null
                || signature.signedAt().isAfter(task.getCompletedAt())) return unknown(task, deviation.getStatus());
        if (!JSON.isValidObject(signature.canonicalContentJson())
                || !JSON.parseObject(MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(subject, action,
                        signature.reason())).equals(JSON.parseObject(signature.canonicalContentJson()))) {
            return unknown(task, deviation.getStatus());
        }
        var verified = signatures.verifyEvidence(signature.id());
        if (verified == null || !Objects.equals(signature.id(), verified.signatureId())
                || !"VALID".equals(verified.verificationStatus())
                || signature.contentHash() == null || signature.contentHash().isBlank()
                || signature.evidenceHash() == null || signature.evidenceHash().isBlank()
                || !Objects.equals(signature.contentHash(), verified.storedContentHash())
                || !Objects.equals(signature.contentHash(), verified.calculatedContentHash())
                || !Objects.equals(signature.evidenceHash(), verified.storedEvidenceHash())
                || !Objects.equals(signature.evidenceHash(), verified.calculatedEvidenceHash())) {
            return unknown(task, deviation.getStatus());
        }
        return new MesEdhrApprovalOutcome("COMPLETED", ApprovalTaskReviewResult.APPROVE,
                deviation.getStatus(), task.getStatus(), "APPROVE", task.getSignatureCellKey(),
                opinion.isBlank() ? null : opinion, task.getReason(), null, signature.actorId(), signature.signedAt(),
                "EXISTING_DEVIATION_NODE_SIGNATURE", signature.id().toString(), signature.actorId());
    }

    private MesEdhrApprovalOutcome unknown(MesProEdhrWorkTaskDO task, String businessStatus) {
        return new MesEdhrApprovalOutcome("DONE".equals(task.getStatus()) ? "COMPLETED" : "UNKNOWN",
                null, businessStatus, task.getStatus(), "UNKNOWN", null, null, task.getReason(),
                null, null, null, null, null, null);
    }

    private boolean positiveNumber(Object value) {
        return value instanceof Number number && number.longValue() > 0;
    }
}
