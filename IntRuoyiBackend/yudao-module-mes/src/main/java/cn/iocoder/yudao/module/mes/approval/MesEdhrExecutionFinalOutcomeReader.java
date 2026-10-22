package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrApprovalStatusMapping;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/** Only called with a persisted, already-authorized task by MesEdhrApprovalOutcomeReader. */
@Component
public class MesEdhrExecutionFinalOutcomeReader {
    private final MesProBatchRecordExecutionMapper executions;
    private final MesProBatchRecordApprovalSnapshotMapper snapshots;
    private final MesProEdhrWorkTaskMapper tasks;
    private final ElectronicSignatureQueryService signatures;
    private final AdminUserMapper users;

    public MesEdhrExecutionFinalOutcomeReader(MesProBatchRecordExecutionMapper executions,
            MesProBatchRecordApprovalSnapshotMapper snapshots, MesProEdhrWorkTaskMapper tasks,
            ElectronicSignatureQueryService signatures, AdminUserMapper users) {
        this.executions = executions;
        this.snapshots = snapshots;
        this.tasks = tasks;
        this.signatures = signatures;
        this.users = users;
    }

    MesEdhrApprovalOutcome read(MesProEdhrWorkTaskDO task, Long tenant) {
        require(Objects.equals(tenant, TenantContextHolder.getRequiredTenantId()) && "DONE".equals(task.getStatus())
                && task.getCompletedAt() != null && task.getWorkOrderId() != null && task.getWorkOrderId() > 0,
                "completed execution task identity missing");
        var execution = executions.selectOne(new LambdaQueryWrapperX<MesProBatchRecordExecutionDO>()
                .eq(MesProBatchRecordExecutionDO::getId, task.getExecutionId())
                .eq(MesProBatchRecordExecutionDO::getDeleted, false).apply("tenant_id = {0}", tenant));
        if (execution == null) return unknown(task, null, null);
        require(Objects.equals(execution.getId(), task.getExecutionId())
                && Objects.equals(execution.getWorkOrderId(), task.getWorkOrderId())
                && Objects.equals(execution.getBatchExecutionId(), task.getBatchExecutionId()), "execution identity conflict");
        String businessStatus = execution.getStatus() == null ? null : execution.getStatus().toString();
        // This is the writer's selectByExecutionId relation, with explicit tenant/delete constraints.
        // selectOne rejects duplicates; never use selectLatestByExecutionId.
        var snapshot = snapshots.selectOne(new LambdaQueryWrapperX<MesProBatchRecordApprovalSnapshotDO>()
                .eq(MesProBatchRecordApprovalSnapshotDO::getExecutionId, execution.getId())
                .eq(MesProBatchRecordApprovalSnapshotDO::getDeleted, false).apply("tenant_id = {0}", tenant));
        if (snapshot == null) return unknown(task, businessStatus, null);
        require(snapshot.getId() != null && snapshot.getId() > 0 && execution.getProcessInstanceId() != null
                && !execution.getProcessInstanceId().isBlank() && Objects.equals(snapshot.getExecutionId(), execution.getId())
                && Objects.equals(snapshot.getProcessInstanceId(), execution.getProcessInstanceId()), "snapshot identity conflict");
        boolean approve = "APPROVED".equals(snapshot.getApprovalStatus());
        boolean reject = "REJECTED".equals(snapshot.getApprovalStatus());
        if (!approve && !reject) {
            var result = unknown(task, businessStatus, snapshot);
            if (snapshot.getApprovalStatus() == null || snapshot.getApprovalStatus().isBlank()
                    || "SUBMITTED".equals(snapshot.getApprovalStatus())) return result;
            return new MesEdhrApprovalOutcome(result.taskOutcome(), null, businessStatus, task.getStatus(), "UNSUPPORTED",
                    snapshot.getApprovalStatus(), null, task.getReason(), null, null, null, result.evidenceType(), result.evidenceId(), null);
        }
        require(Objects.equals(execution.getStatus(), approve ? MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_APPROVED
                : MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_REJECTED), "execution final status conflicts with snapshot");
        // A final APPROVE is never the earlier REVIEW_APPROVE action.
        if (!(approve ? "APPROVE" : "REVIEW").equals(task.getTaskType())) return unknown(task, businessStatus, snapshot);
        Long signatureId = approve ? snapshot.getApproveSignatureId() : snapshot.getRejectSignatureId();
        if (signatureId == null || signatureId <= 0) return unknown(task, businessStatus, snapshot);
        var signature = signatures.getById(signatureId); // existing service enforces current tenant
        if (signature == null) return unknown(task, businessStatus, snapshot);
        if (signature.signedAt() == null) return unknown(task, businessStatus, snapshot);
        Long actor = approve ? snapshot.getApprovedBy() : snapshot.getRejectedBy();
        var actedAt = approve ? snapshot.getApprovedAt() : snapshot.getRejectedAt();
        String action = approve ? "APPROVE" : "REJECT";
        require(Objects.equals(signature.id(), signatureId) && actor != null && actor > 0
                && Objects.equals(signature.actorId(), actor) && actedAt != null
                && Objects.equals(actor, approve ? execution.getApprovedBy() : execution.getRejectedBy())
                && Objects.equals(actedAt, approve ? execution.getApprovedAt() : execution.getRejectedAt())
                && "MES".equals(signature.moduleCode()) && "MES_BATCH_RECORD".equals(signature.subjectType())
                && action.equals(signature.actionCode()) && "VALID".equals(signature.verificationStatus())
                && "mes-batch-record-signature-v1".equals(signature.policyVersion())
                && "SESSION_PLUS_PASSWORD".equals(signature.authenticationMethod())
                && Objects.equals(signature.signedAt(), actedAt),
                "final signature/action actor conflict");
        String[] subject = new String(Base64.getUrlDecoder().decode(signature.subjectId()), StandardCharsets.UTF_8).split("\n", -1);
        require((subject.length == 17 || subject.length == 18) && value(execution.getId()).equals(subject[0]) && action.equals(subject[1])
                && value(execution.getProcessInstanceId()).equals(subject[2]) && action.equals(subject[12]),
                "final signature execution/process/action conflict");
        if (approve) MesBatchRecordSignatureSubjectAdapter.verifiedUserOpinion(signature.subjectId(), action, signature.reason());
        if (!matches(task, subject) || !Objects.equals(signature.taskId(), task.getBpmTaskId())
                || !Objects.equals(signature.processInstanceId(), execution.getProcessInstanceId())) {
            return unknown(task, businessStatus, snapshot);
        }
        if (execution.getFieldAuditRevision() == null || execution.getFieldAuditHeadHash() == null
                || execution.getFieldAuditHeadHash().isBlank() || execution.getCellValuesHash() == null
                || execution.getCellValuesHash().isBlank() || signature.nodeCode() == null
                || signature.nodeCode().isBlank() || subject[4].isBlank()) return unknown(task, businessStatus, snapshot);
        require(value(execution.getFieldAuditRevision()).equals(subject[13])
                && value(execution.getFieldAuditHeadHash()).equals(subject[14])
                && value(execution.getCellValuesHash()).equals(subject[15])
                && value(signature.nodeCode()).equals(subject[4]), "final signature domain audit/node conflict");
        // Shared BPM task IDs can cover multiple review cells/candidates. Prove the complete
        // persisted identity tuple is unique; no first row, assignee or completion-time heuristic.
        var siblings = tasks.selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getExecutionId, execution.getId())
                .eq(MesProEdhrWorkTaskDO::getTaskType, task.getTaskType())
                .eq(MesProEdhrWorkTaskDO::getDeleted, false).apply("tenant_id = {0}", tenant));
        var matching = siblings.stream().filter(row -> matches(row, subject)).toList();
        if (matching.size() != 1 || !Objects.equals(matching.get(0).getId(), task.getId())) {
            return unknown(task, businessStatus, snapshot);
        }
        var user = users.selectOne(new LambdaQueryWrapperX<AdminUserDO>()
                .eq(AdminUserDO::getId, actor).eq(AdminUserDO::getTenantId, tenant).eq(AdminUserDO::getDeleted, false));
        require(user != null && Objects.equals(actor, user.getId()) && Objects.equals(tenant, user.getTenantId())
                && !Boolean.TRUE.equals(user.getDeleted()), "final deciding actor outside tenant");
        var verified = signatures.verifyEvidence(signatureId);
        require(signature.contentHash() != null && !signature.contentHash().isBlank()
                && signature.evidenceHash() != null && !signature.evidenceHash().isBlank()
                && verified != null && Objects.equals(signatureId, verified.signatureId())
                && "VALID".equals(verified.verificationStatus())
                && Objects.equals(signature.contentHash(), verified.storedContentHash())
                && Objects.equals(signature.contentHash(), verified.calculatedContentHash())
                && Objects.equals(signature.evidenceHash(), verified.storedEvidenceHash())
                && Objects.equals(signature.evidenceHash(), verified.calculatedEvidenceHash()), "final signature integrity conflict");
        var expected = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(new SignatureSubjectCommand(
                actor, "MES", action, "MES_BATCH_RECORD", signature.subjectId(), signature.subjectVersion(), signature.reason()));
        var expectedContent = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.parseTree(expected.canonicalContentJson());
        expectedContent.put("reason", signature.reason() == null ? "" : signature.reason().trim());
        require(Objects.equals(expected.subjectVersion(), signature.subjectVersion())
                && expectedContent.equals(JsonUtils.parseTree(signature.canonicalContentJson())),
                "final signature canonical content conflict");
        if (reject) require(Objects.equals(snapshot.getRejectReason(), execution.getRejectReason())
                && Objects.equals(snapshot.getRejectReason(), signature.reason()), "final rejection opinion conflict");
        String opinion = approve ? MesBatchRecordSignatureSubjectAdapter.verifiedUserOpinion(
                signature.subjectId(), action, signature.reason()) : signature.reason();
        return new MesEdhrApprovalOutcome("COMPLETED", approve ? ApprovalTaskReviewResult.APPROVE : ApprovalTaskReviewResult.REJECT,
                businessStatus, task.getStatus(), action, snapshot.getApprovalStatus(), opinion,
                opinion == null && signature.reason() != null ? signature.reason() : task.getReason(), null,
                actor, actedAt, "EXISTING_EXECUTION_FINAL_SNAPSHOT", snapshot.getId().toString(), actor);
    }

    private static boolean matches(MesProEdhrWorkTaskDO task, String[] subject) {
        return task.getBpmTaskId() != null && !task.getBpmTaskId().isBlank()
                && value(task.getExecutionId()).equals(subject[0]) && value(task.getBpmTaskId()).equals(subject[3])
                && value(task.getSignatureCellKey()).equals(subject[6]) && value(task.getSignatureRowIndex()).equals(subject[7])
                && value(task.getSignatureColumnIndex()).equals(subject[8]) && value(task.getReviewSourceType()).equals(subject[9])
                && value(task.getReviewSourceId()).equals(subject[10]) && value(task.getReviewSourceName()).equals(subject[11]);
    }

    private static MesEdhrApprovalOutcome unknown(MesProEdhrWorkTaskDO task, String businessStatus,
            MesProBatchRecordApprovalSnapshotDO snapshot) {
        return new MesEdhrApprovalOutcome("COMPLETED", null, businessStatus, task.getStatus(), "UNKNOWN",
                snapshot == null ? null : snapshot.getApprovalStatus(), null, task.getReason(), null, null, null,
                snapshot == null ? null : "EXISTING_EXECUTION_FINAL_SNAPSHOT", snapshot == null ? null : snapshot.getId().toString(), null);
    }
    private static String value(Object value) { return value == null ? "" : value.toString().trim(); }
    private static void require(boolean valid, String reason) {
        if (!valid) throw new IllegalStateException("EDHR_EXECUTION_FINAL_OUTCOME_INVALID: " + reason);
    }
}
