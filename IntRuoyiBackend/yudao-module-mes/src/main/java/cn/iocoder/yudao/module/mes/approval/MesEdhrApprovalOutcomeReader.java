package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseSignoffService;
import cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffContract;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import org.springframework.stereotype.Component;
import java.util.Objects;

/**
 * Enriches a task already authorized by the workbench query/timeline service.
 * This is not an authorization entry point and must not receive client-selected task rows.
 * No latest-row lookup or reconstruction of absent historical receipts is allowed.
 */
@Component
public class MesEdhrApprovalOutcomeReader {
    private final MesProEdhrWorkTaskMapper tasks;
    private final MesProEdhrReleaseTransactionMapper transactions;
    private final MesProEdhrReleaseDecisionMapper decisions;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applications;
    private final MesManagerReleaseCompletedActorResolver actors;
    private final ElectronicSignatureQueryService signatures;
    private final MesProductionReleaseSignoffService signoff;
    private final MesEdhrExecutionFinalOutcomeReader executionFinal;

    public MesEdhrApprovalOutcomeReader(MesProEdhrWorkTaskMapper tasks,
            MesProEdhrReleaseTransactionMapper transactions, MesProEdhrReleaseDecisionMapper decisions,
            MesProcessPoolActiveOrderReleaseApplicationMapper applications,
            MesManagerReleaseCompletedActorResolver actors, ElectronicSignatureQueryService signatures,
            MesProductionReleaseSignoffService signoff, MesEdhrExecutionFinalOutcomeReader executionFinal) {
        this.tasks = tasks;
        this.transactions = transactions;
        this.decisions = decisions;
        this.applications = applications;
        this.actors = actors;
        this.signatures = signatures;
        this.signoff = signoff;
        this.executionFinal = executionFinal;
    }

    public MesEdhrApprovalOutcome readAuthorizedTask(MesProEdhrWorkTaskDO projected) {
        require(projected != null && positive(projected.getId()), "task identity missing");
        Long tenant = TenantContextHolder.getRequiredTenantId();
        var task = tasks.selectOne(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getId, projected.getId())
                .eq(MesProEdhrWorkTaskDO::getDeleted, false).apply("tenant_id = {0}", tenant));
        require(task != null && Objects.equals(task.getId(), projected.getId())
                && Objects.equals(task.getTaskType(), projected.getTaskType())
                && Objects.equals(task.getBusinessScopeType(), projected.getBusinessScopeType())
                && Objects.equals(task.getBusinessScopeId(), projected.getBusinessScopeId())
                && Objects.equals(task.getBatchExecutionId(), projected.getBatchExecutionId())
                && Objects.equals(task.getWorkOrderId(), projected.getWorkOrderId())
                && Objects.equals(task.getExecutionId(), projected.getExecutionId())
                && Objects.equals(task.getBpmTaskId(), projected.getBpmTaskId())
                && Objects.equals(task.getSignatureCellKey(), projected.getSignatureCellKey())
                && Objects.equals(task.getSignatureRowIndex(), projected.getSignatureRowIndex())
                && Objects.equals(task.getSignatureColumnIndex(), projected.getSignatureColumnIndex())
                && Objects.equals(task.getReviewSourceType(), projected.getReviewSourceType())
                && Objects.equals(task.getReviewSourceId(), projected.getReviewSourceId())
                && Objects.equals(task.getReviewSourceName(), projected.getReviewSourceName())
                && Objects.equals(task.getReason(), projected.getReason())
                && Objects.equals(task.getRemark(), projected.getRemark())
                && Objects.equals(task.getAssigneeUserId(), projected.getAssigneeUserId())
                && Objects.equals(task.getCandidateUserSnapshot(), projected.getCandidateUserSnapshot())
                && Objects.equals(task.getCandidateSourceType(), projected.getCandidateSourceType())
                && Objects.equals(task.getCandidateSourceId(), projected.getCandidateSourceId())
                && Objects.equals(task.getResponsibilityScopeJson(), projected.getResponsibilityScopeJson())
                && Objects.equals(task.getUpdateTime(), projected.getUpdateTime())
                && Objects.equals(task.getStatus(), projected.getStatus())
                && Objects.equals(task.getCompletedAt(), projected.getCompletedAt()), "projected task identity changed");
        if (!"DONE".equals(task.getStatus())) return unproven(task, null);
        if ("PQC_PRODUCTION_RELEASE".equals(task.getTaskType())) return pqc(task, tenant);
        if ("RELEASE_APPROVE".equals(task.getTaskType())) return market(task, tenant);
        if (("REVIEW".equals(task.getTaskType()) || "APPROVE".equals(task.getTaskType())) && positive(task.getExecutionId())) {
            return executionFinal.read(task, tenant);
        }
        // Missing execution identity cannot select an execution's final snapshot.
        return unproven(task, null);
    }

    private MesEdhrApprovalOutcome market(MesProEdhrWorkTaskDO task, Long tenant) {
        require("RELEASE_TRANSACTION".equals(task.getBusinessScopeType())
                && positive(task.getBusinessScopeId()), "market task scope invalid");
        var tx = transactions.selectOne(new LambdaQueryWrapperX<MesProEdhrReleaseTransactionDO>()
                .eq(MesProEdhrReleaseTransactionDO::getId, task.getBusinessScopeId())
                .eq(MesProEdhrReleaseTransactionDO::getDeleted, false).apply("tenant_id = {0}", tenant));
        if (tx == null) return unproven(task, null);
        require(Objects.equals(tx.getId(), task.getBusinessScopeId())
                && Objects.equals(tx.getBatchExecutionId(), task.getBatchExecutionId())
                && Objects.equals(tx.getWorkOrderId(), task.getWorkOrderId()), "market transaction identity conflict");
        if (!positive(tx.getReleaseDecisionId())) return unproven(task, tx.getReleaseStatus());
        var decision = decisions.selectOne(new LambdaQueryWrapperX<MesProEdhrReleaseDecisionDO>()
                .eq(MesProEdhrReleaseDecisionDO::getId, tx.getReleaseDecisionId())
                .eq(MesProEdhrReleaseDecisionDO::getDeleted, false).apply("tenant_id = {0}", tenant));
        if (decision == null) return unproven(task, tx.getReleaseStatus());
        require(Objects.equals(decision.getId(), tx.getReleaseDecisionId())
                && Objects.equals(decision.getReleaseTransactionId(), tx.getId())
                && Objects.equals(decision.getBatchExecutionId(), task.getBatchExecutionId())
                && Objects.equals(decision.getWorkOrderId(), task.getWorkOrderId()), "market decision identity conflict");
        // The manager application is the persisted exact task FK. Legacy standalone
        // decisions and REJECT: text do not establish this relation.
        if (!positive(decision.getReleaseApplicationId())) return unproven(task, tx.getReleaseStatus());
        var app = application(decision.getReleaseApplicationId(), tenant);
        if (app == null) return unproven(task, tx.getReleaseStatus());
        require(Objects.equals(app.getReleaseApprovalWorkTaskId(), task.getId())
                && positive(app.getActiveOrderId())
                && Objects.equals(app.getReleaseTransactionId(), tx.getId())
                && Objects.equals(app.getBatchExecutionId(), task.getBatchExecutionId())
                && Objects.equals(app.getWorkOrderId(), task.getWorkOrderId()), "manager application task conflict");
        require(Objects.equals(tx.getReleaseStatus(), decision.getDecisionStatus()), "market decision state conflict");
        if (!"RELEASED".equals(decision.getDecisionStatus())) {
            if (decision.getDecisionStatus() != null && !decision.getDecisionStatus().isBlank()
                    && !"REJECTED".equals(decision.getDecisionStatus()) && !"WITHDRAWN".equals(decision.getDecisionStatus())) {
                return unsupported(task, tx.getReleaseStatus(), decision.getDecisionStatus());
            }
            return unproven(task, tx.getReleaseStatus());
        }
        if (!positive(tx.getApprovalSignatureId())) return unproven(task, tx.getReleaseStatus());
        Long actor = actors.resolve(task.getId(), task.getBusinessScopeType(), tx.getId(),
                task.getBatchExecutionId(), task.getWorkOrderId());
        require(Objects.equals(actor, decision.getActorUserId())
                && positive(actor) && task.getCompletedAt() != null
                && tx.getApprovedAt() != null && Objects.equals(tx.getApprovedAt(), decision.getDecidedAt())
                && Objects.equals(tx.getApprovalOpinion(), decision.getApprovalOpinion()), "market action conflict");
        var signature = signatures.getById(tx.getApprovalSignatureId());
        if (signature == null) return unproven(task, tx.getReleaseStatus());
        require(Objects.equals(signature.id(), tx.getApprovalSignatureId())
                && Objects.equals(signature.reason(), decision.getApprovalOpinion()), "market signature opinion conflict");
        require(signoff.findVerifiedSignatureId(task.getId(), actor, signature.subjectId(),
                tx.getApprovalSignoffEvidenceHash(), decision.getApprovalOpinion())
                .filter(tx.getApprovalSignatureId()::equals).isPresent(), "market signature not bound to decision");
        return new MesEdhrApprovalOutcome("COMPLETED", ApprovalTaskReviewResult.APPROVE,
                tx.getReleaseStatus(), task.getStatus(), "APPROVE", decision.getDecisionStatus(),
                decision.getApprovalOpinion(), task.getReason(), null,
                actor, decision.getDecidedAt(), "EXISTING_MARKET_RELEASE_DECISION", decision.getId().toString(), actor);
    }

    private MesEdhrApprovalOutcome pqc(MesProEdhrWorkTaskDO task, Long tenant) {
        require("RELEASE_APPLICATION".equals(task.getBusinessScopeType())
                && positive(task.getBusinessScopeId()), "PQC task scope invalid");
        var app = application(task.getBusinessScopeId(), tenant);
        if (app == null) return unproven(task, null);
        require(Objects.equals(app.getPqcReleaseWorkTaskId(), task.getId())
                && positive(app.getActiveOrderId())
                && Objects.equals(app.getWorkOrderId(), task.getWorkOrderId())
                && (task.getBatchExecutionId() == null
                    || Objects.equals(app.getBatchExecutionId(), task.getBatchExecutionId())), "PQC application task conflict");
        // QA closure belongs to a different action; an earlier PQC receipt cannot be
        // presented as the decision/opinion of the task's later closure.
        if ("EDHR_NONCONFORMANCE_REVIEW".equals(task.getReviewSourceType())
                || "NONCONFORMANCE_REWORK".equals(task.getReason()) || "NONCONFORMANCE_VOID".equals(task.getReason())) {
            require(task.getReviewSourceType() == null || "EDHR_NONCONFORMANCE_REVIEW".equals(task.getReviewSourceType()),
                    "QA task review source conflict");
            if (!"EDHR_NONCONFORMANCE_REVIEW".equals(task.getReviewSourceType())
                    || !positive(task.getReviewSourceId()) || !positive(app.getQaClosureReviewId())) {
                return unproven(task, app.getApplicationStatus());
            }
            Long actor = actors.resolvePqc(task.getId(), task.getBusinessScopeType(), app.getId(),
                    task.getBatchExecutionId(), task.getWorkOrderId(), task.getReason(), task.getCompletedAt());
            var review = actors.readVerifiedQaClosure(task.getId(), app, task.getReviewSourceId());
            require(positive(actor) && Objects.equals(actor, review.getQaUserId())
                    && Objects.equals(task.getCompletedAt(), review.getClosedAt()), "QA completion actor/time conflict");
            return new MesEdhrApprovalOutcome("COMPLETED", null, app.getApplicationStatus(), task.getStatus(),
                    "UNKNOWN", null, null, review.getReviewOpinion(), null, null, null,
                    "EXISTING_QA_NONCONFORMANCE_REVIEW", review.getId().toString(), actor);
        }
        if (app.getPqcDecision() == null || app.getPqcDecision().isBlank()) return unproven(task, app.getApplicationStatus());
        if (!"APPROVE".equals(app.getPqcDecision()) && !"REJECT".equals(app.getPqcDecision())) {
            return unsupported(task, app.getApplicationStatus(), app.getPqcDecision());
        }
        Long actor = actors.resolvePqc(task.getId(), task.getBusinessScopeType(), app.getId(),
                task.getBatchExecutionId(), task.getWorkOrderId(), task.getReason(), task.getCompletedAt());
        require(positive(actor) && Objects.equals(actor, app.getPqcDecidedBy())
                && task.getCompletedAt() != null && Objects.equals(task.getCompletedAt(), app.getPqcDecidedAt()),
                "PQC action actor/time conflict");
        if ("REJECT".equals(app.getPqcDecision())) {
            return new MesEdhrApprovalOutcome("COMPLETED", ApprovalTaskReviewResult.REJECT,
                    app.getApplicationStatus(), task.getStatus(), "REJECT", app.getPqcDecision(),
                    app.getPqcRejectReason(), task.getReason(), null,
                    actor, app.getPqcDecidedAt(), "EXISTING_PQC_APPLICATION", app.getId().toString(), actor);
        }
        // resolvePqc validates the exact receipt, actor, subject and evidence integrity.
        var receipt = JsonUtils.parseTree(app.getDossierSummaryJson());
        Long signatureId = Long.valueOf(receipt.path("signatureId").asText());
        var signature = signatures.getById(signatureId);
        require(signature != null && Objects.equals(signature.id(), signatureId)
                && Objects.equals(signature.actorId(), actor), "PQC signature identity/actor conflict");
        String opinion = cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter
                .verifiedUserOpinion(signature.subjectId(), "PQC_RELEASE", signature.reason());
        return new MesEdhrApprovalOutcome("COMPLETED", ApprovalTaskReviewResult.APPROVE,
                app.getApplicationStatus(), task.getStatus(), "APPROVE", app.getPqcDecision(),
                opinion, opinion == null ? signature.reason() : task.getReason(), null,
                actor, app.getPqcDecidedAt(), "EXISTING_PQC_APPLICATION", app.getId().toString(), actor);
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO application(Long id, Long tenant) {
        var app = applications.selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getId, id)
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getTenantId, tenant)
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getDeleted, false));
        require(app == null || (Objects.equals(app.getId(), id) && Objects.equals(app.getTenantId(), tenant)),
                "application outside exact tenant/identity");
        return app;
    }

    /** Handoff completion is a persisted business handoff; it never creates APPROVE. */
    public MesEdhrApprovalOutcome readAuthorizedHandoff(MesActiveOrderHandoffTaskDO task) {
        require(task != null && positive(task.getId())
                && Objects.equals(TenantContextHolder.getRequiredTenantId(), task.getTenantId()), "handoff tenant/identity conflict");
        require(positive(task.getActiveOrderId()) && positive(task.getWorkOrderId())
                && positive(task.getSourceId()) && positive(task.getRoundId()), "handoff source identity missing");
        MesActiveOrderHandoffContract.validate(task);
        if ("DONE".equals(task.getStatus())) {
            require(positive(task.getCompletedBy()) && positive(task.getCompletionSourceId())
                    && task.getCompletedAt() != null, "handoff completion fact missing");
        }
        return new MesEdhrApprovalOutcome(taskState(task.getStatus()), null, null, task.getStatus(),
                "UNKNOWN", null, null, task.getReason(), null, null, null,
                "EXISTING_ACTIVE_ORDER_HANDOFF", task.getId().toString(),
                "DONE".equals(task.getStatus()) ? task.getCompletedBy() : null);
    }

    private static MesEdhrApprovalOutcome unproven(MesProEdhrWorkTaskDO task, String businessStatus) {
        return new MesEdhrApprovalOutcome(taskState(task.getStatus()), null, businessStatus, task.getStatus(),
                "UNKNOWN", null, null, task.getReason(), null, null, null, null, null, null);
    }

    private static MesEdhrApprovalOutcome unsupported(MesProEdhrWorkTaskDO task, String businessStatus, String decisionCode) {
        return new MesEdhrApprovalOutcome(taskState(task.getStatus()), null, businessStatus, task.getStatus(),
                "UNSUPPORTED", decisionCode, null, task.getReason(), null, null, null, null, null, null);
    }

    private static String taskState(String status) {
        if (status == null || status.isBlank()) return "UNKNOWN";
        return switch (status) {
            case "TODO" -> "WAIT";
            case "DOING" -> "RUNNING";
            case "DONE" -> "COMPLETED";
            case "CANCELED" -> "CANCELLED";
            case "OVERDUE" -> "OVERDUE";
            default -> "UNSUPPORTED";
        };
    }

    private static boolean positive(Long value) { return value != null && value > 0; }
    private static void require(boolean condition, String reason) {
        if (!condition) throw new IllegalStateException("EDHR_APPROVAL_OUTCOME_INVALID: " + reason);
    }
}
