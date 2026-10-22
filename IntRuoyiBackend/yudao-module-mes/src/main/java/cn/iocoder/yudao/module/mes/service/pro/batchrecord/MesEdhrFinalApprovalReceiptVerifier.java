package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/** Verifies the final writer's receipt. Never advances a work task or creates downstream work. */
@Component
public class MesEdhrFinalApprovalReceiptVerifier {
    private final ElectronicSignatureQueryService signatures;
    private final MesProEdhrWorkTaskMapper tasks;

    public MesEdhrFinalApprovalReceiptVerifier(ElectronicSignatureQueryService signatures, MesProEdhrWorkTaskMapper tasks) {
        this.signatures = signatures;
        this.tasks = tasks;
    }

    public ElectronicSignatureEvidenceDTO requireSignature(MesProBatchRecordExecutionDO execution,
            MesProEdhrWorkTaskDO task, Long signatureId, Long actor) {
        return requireSignature(execution, task, signatureId, actor, "APPROVE", "APPROVE");
    }

    public ElectronicSignatureEvidenceDTO requireRejectSignature(MesProBatchRecordExecutionDO execution,
            MesProEdhrWorkTaskDO task, Long signatureId, Long actor) {
        return requireSignature(execution, task, signatureId, actor, "REJECT", "REVIEW");
    }

    private ElectronicSignatureEvidenceDTO requireSignature(MesProBatchRecordExecutionDO execution,
            MesProEdhrWorkTaskDO task, Long signatureId, Long actor, String action, String taskType) {
        require(signatureId != null && signatureId > 0 && actor != null && actor > 0, "signature/actor missing");
        var signature = signatures.getById(signatureId);
        require(signature != null && Objects.equals(signatureId, signature.id()) && Objects.equals(actor, signature.actorId())
                && "MES".equals(signature.moduleCode()) && "MES_BATCH_RECORD".equals(signature.subjectType())
                && action.equals(signature.actionCode()) && "VALID".equals(signature.verificationStatus())
                && "mes-batch-record-signature-v1".equals(signature.policyVersion())
                && "SESSION_PLUS_PASSWORD".equals(signature.authenticationMethod()) && signature.signedAt() != null
                && Objects.equals(execution.getProcessInstanceId(), signature.processInstanceId())
                && Objects.equals(task.getBpmTaskId(), signature.taskId()), "signature action/actor/process/task conflict");
        require(taskType.equals(task.getTaskType()), "signature task role conflict");
        String[] subject = requireSubject(signature);
        require(matches(task, subject) && action.equals(subject[1])
                && Objects.equals(execution.getId(), task.getExecutionId())
                && value(execution.getProcessInstanceId()).equals(subject[2]) && action.equals(subject[12])
                && value(signature.nodeCode()).equals(subject[4]) && !subject[4].isBlank()
                && value(execution.getFieldAuditRevision()).equals(subject[13])
                && value(execution.getFieldAuditHeadHash()).equals(subject[14])
                && value(execution.getCellValuesHash()).equals(subject[15]), "signature subject conflict");
        var verified = signatures.verifyEvidence(signatureId);
        require(verified != null && Objects.equals(signatureId, verified.signatureId()) && "VALID".equals(verified.verificationStatus())
                && signature.contentHash() != null && !signature.contentHash().isBlank()
                && signature.evidenceHash() != null && !signature.evidenceHash().isBlank()
                && Objects.equals(signature.contentHash(), verified.storedContentHash())
                && Objects.equals(signature.contentHash(), verified.calculatedContentHash())
                && Objects.equals(signature.evidenceHash(), verified.storedEvidenceHash())
                && Objects.equals(signature.evidenceHash(), verified.calculatedEvidenceHash()), "signature integrity conflict");
        var expected = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(new SignatureSubjectCommand(
                actor, "MES", action, "MES_BATCH_RECORD", signature.subjectId(), signature.subjectVersion(), signature.reason()));
        require(Objects.equals(expected.subjectVersion(), signature.subjectVersion())
                && JsonUtils.parseTree(expected.canonicalContentJson()).equals(JsonUtils.parseTree(signature.canonicalContentJson())),
                "signature canonical content conflict");
        return signature;
    }

    public void requireCompleted(MesProBatchRecordExecutionDO execution, MesProBatchRecordApprovalSnapshotDO snapshot, Long actor) {
        require(Objects.equals(execution.getStatus(), MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_APPROVED)
                && "APPROVED".equals(snapshot.getApprovalStatus()) && Objects.equals(execution.getId(), snapshot.getExecutionId())
                && Objects.equals(execution.getProcessInstanceId(), snapshot.getProcessInstanceId())
                && Objects.equals(execution.getProcessDefinitionKey(), snapshot.getProcessDefinitionKey())
                && Objects.equals(actor, execution.getApprovedBy()) && Objects.equals(actor, snapshot.getApprovedBy())
                && execution.getApprovedAt() != null && Objects.equals(execution.getApprovedAt(), snapshot.getApprovedAt())
                && Objects.equals(execution.getApprovedAt(), execution.getClosedAt())
                && Objects.equals(execution.getApprovedAt(), snapshot.getClosedAt())
                && snapshot.getCurrentBpmTaskId() == null && snapshot.getCurrentTaskDefinitionKey() == null,
                "final execution/snapshot/actor/time conflict");
        require(snapshot.getApproveSignatureId() != null && snapshot.getApproveSignatureId() > 0, "final signature FK missing");
        var signature = signatures.getById(snapshot.getApproveSignatureId());
        require(signature != null, "final signature missing");
        String[] subject = requireSubject(signature);
        var rows = tasks.selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getExecutionId, execution.getId())
                .eq(MesProEdhrWorkTaskDO::getDeleted, false).apply("tenant_id = {0}", TenantContextHolder.getRequiredTenantId()));
        require(rows != null && rows.stream().noneMatch(task -> "REVIEW".equals(task.getTaskType())
                && !"DONE".equals(task.getStatus()) && !"CANCELED".equals(task.getStatus())), "unfinished review remains");
        var matching = rows.stream().filter(task -> "APPROVE".equals(task.getTaskType()) && matches(task, subject)).toList();
        require(matching.size() == 1 && "DONE".equals(matching.get(0).getStatus())
                && matching.get(0).getCompletedAt() != null
                && Objects.equals(execution.getWorkOrderId(), matching.get(0).getWorkOrderId())
                && Objects.equals(execution.getBatchExecutionId(), matching.get(0).getBatchExecutionId()), "final approve task missing/ambiguous");
        signature = requireSignature(execution, matching.get(0), snapshot.getApproveSignatureId(), actor);
        require(Objects.equals(signature.signedAt(), execution.getApprovedAt()), "final signature time conflict");
    }

    private static String[] requireSubject(ElectronicSignatureEvidenceDTO signature) {
        String[] subject = new String(Base64.getUrlDecoder().decode(signature.subjectId()), StandardCharsets.UTF_8).split("\n", -1);
        require(subject.length == 17 || subject.length == 18, "final signature subject invalid");
        // Validate the explicit source contract against its reason; preserve the full subject for canonical verification.
        MesBatchRecordSignatureSubjectAdapter.verifiedUserOpinion(signature.subjectId(), signature.actionCode(), signature.reason());
        return subject;
    }

    private static boolean matches(MesProEdhrWorkTaskDO task, String[] subject) {
        return task.getBpmTaskId() != null && !task.getBpmTaskId().isBlank()
                && value(task.getExecutionId()).equals(subject[0]) && value(task.getBpmTaskId()).equals(subject[3])
                && value(task.getSignatureCellKey()).equals(subject[6]) && value(task.getSignatureRowIndex()).equals(subject[7])
                && value(task.getSignatureColumnIndex()).equals(subject[8]) && value(task.getReviewSourceType()).equals(subject[9])
                && value(task.getReviewSourceId()).equals(subject[10]) && value(task.getReviewSourceName()).equals(subject[11]);
    }
    private static String value(Object value) { return value == null ? "" : value.toString().trim(); }
    private static void require(boolean valid, String reason) {
        if (!valid) throw new IllegalStateException("EDHR_FINAL_APPROVAL_RECEIPT_INVALID: " + reason);
    }
}
