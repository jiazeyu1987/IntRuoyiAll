package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.time.LocalDateTime;

/** Reads a completed release decision's formal actor without changing frozen task ownership. */
@Component
public class MesManagerReleaseCompletedActorResolver {

    private static final String RELEASE_TRANSACTION = "RELEASE_TRANSACTION";
    private final MesProEdhrWorkTaskMapper taskMapper;
    private final MesProEdhrReleaseTransactionMapper transactionMapper;
    @Resource private MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    @Resource private AdminUserMapper users;
    @Resource private ElectronicSignatureQueryService signatures;
    @Resource private cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper nonconformanceReviews;

    public MesManagerReleaseCompletedActorResolver(MesProEdhrWorkTaskMapper taskMapper,
                                                  MesProEdhrReleaseTransactionMapper transactionMapper) {
        this.taskMapper = taskMapper;
        this.transactionMapper = transactionMapper;
    }

    public Long resolve(Long taskId, String scopeType, Long transactionId,
                        Long batchExecutionId, Long workOrderId) {
        require(positive(taskId) && RELEASE_TRANSACTION.equals(scopeType) && positive(transactionId)
                && positive(batchExecutionId) && positive(workOrderId), taskId, "task identity is incomplete");
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        // These DOs inherit BaseDO, so tenant identity must be constrained in the query itself.
        MesProEdhrWorkTaskDO task = taskMapper.selectOne(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getId, taskId)
                .eq(MesProEdhrWorkTaskDO::getDeleted, false)
                .apply("tenant_id = {0}", tenantId));
        require(task != null
                && MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE.equals(task.getTaskType())
                && MesProEdhrWorkTaskStatus.DONE.equals(task.getStatus())
                && RELEASE_TRANSACTION.equals(task.getBusinessScopeType())
                && Objects.equals(transactionId, task.getBusinessScopeId())
                && Objects.equals(batchExecutionId, task.getBatchExecutionId())
                && Objects.equals(workOrderId, task.getWorkOrderId())
                && (task.getReason() == null || !task.getReason().startsWith("REJECT:")),
                taskId, "persisted completed task does not match the approved release identity");
        MesProEdhrReleaseTransactionDO transaction = transactionMapper.selectOne(
                new LambdaQueryWrapperX<MesProEdhrReleaseTransactionDO>()
                        .eq(MesProEdhrReleaseTransactionDO::getId, transactionId)
                        .eq(MesProEdhrReleaseTransactionDO::getDeleted, false)
                        .apply("tenant_id = {0}", tenantId));
        require(transaction != null && "RELEASED".equals(transaction.getReleaseStatus())
                && Objects.equals(batchExecutionId, transaction.getBatchExecutionId())
                && Objects.equals(workOrderId, transaction.getWorkOrderId())
                && positive(transaction.getApprovedBy()),
                taskId, "formal released transaction or its approvedBy is missing or inconsistent");
        return transaction.getApprovedBy();
    }

    /** The current application FK identifies the completed PQC decision; candidate ownership is immutable. */
    public Long resolvePqc(Long taskId, String scopeType, Long applicationId,
                           Long batchExecutionId, Long workOrderId, String decision, LocalDateTime completedAt) {
        require(positive(taskId) && "RELEASE_APPLICATION".equals(scopeType) && positive(applicationId)
                && positive(workOrderId) && decision != null && Set.of("APPROVE", "REJECT", "NONCONFORMANCE_REWORK", "NONCONFORMANCE_VOID").contains(decision)
                && completedAt != null, taskId, "PQC completed identity is incomplete");
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        var task = taskMapper.selectOne(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getId, taskId).eq(MesProEdhrWorkTaskDO::getDeleted, false)
                .apply("tenant_id = {0}", tenantId));
        require(task != null && "PQC_PRODUCTION_RELEASE".equals(task.getTaskType())
                && MesProEdhrWorkTaskStatus.DONE.equals(task.getStatus())
                && Objects.equals(scopeType, task.getBusinessScopeType())
                && Objects.equals(applicationId, task.getBusinessScopeId())
                && Objects.equals(workOrderId, task.getWorkOrderId())
                && Objects.equals(batchExecutionId, task.getBatchExecutionId())
                && Objects.equals(decision, task.getReason()) && Objects.equals(completedAt, task.getCompletedAt()),
                taskId, "persisted PQC completion does not match the projected task");
        var application = applicationMapper.selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getId, applicationId)
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getTenantId, tenantId)
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getDeleted, false));
        require(application != null && Objects.equals(tenantId, application.getTenantId())
                && Objects.equals(taskId, application.getPqcReleaseWorkTaskId())
                && Objects.equals(workOrderId, application.getWorkOrderId())
                && positive(application.getActiveOrderId()) && positive(application.getBatchExecutionId())
                && (batchExecutionId == null || Objects.equals(batchExecutionId, application.getBatchExecutionId()))
                , taskId, "formal current PQC application identity is inconsistent");
        boolean qaClosed = application.getApplicationStatus() != null
                && Set.of("NONCONFORMANCE_REWORK", "NONCONFORMANCE_VOID").contains(application.getApplicationStatus());
        if (decision.startsWith("NONCONFORMANCE_")) {
            require(qaClosed && Objects.equals(decision, application.getApplicationStatus())
                    && "EDHR_NONCONFORMANCE_REVIEW".equals(task.getReviewSourceType()) && positive(task.getReviewSourceId())
                    && application.getPqcDecision() == null && application.getPqcDecidedBy() == null
                    && application.getPqcDecidedAt() == null, taskId, "QA closure cannot replace a persisted PQC decision");
            var review = requireQaClosure(taskId, application, task.getReviewSourceId());
            require(Objects.equals(completedAt, review.getClosedAt()), taskId, "QA closure time differs from task completion");
            return review.getQaUserId();
        }
        if (qaClosed) requireQaClosure(taskId, application, null);
        require(Objects.equals(decision, application.getPqcDecision())
                && positive(application.getPqcDecidedBy()) && Objects.equals(completedAt, application.getPqcDecidedAt())
                && ("APPROVE".equals(decision)
                    ? qaClosed || (application.getApplicationStatus() != null && Set.of("REPORT_UPLOAD_PENDING", "MANAGER_RELEASE_PENDING", "RELEASED").contains(application.getApplicationStatus()))
                    : (qaClosed || "PQC_RELEASE_REJECTED".equals(application.getApplicationStatus()))
                        && text(application.getPqcRejectReason())), taskId, "formal current PQC application decision is inconsistent");
        var actor = users.selectOne(new LambdaQueryWrapperX<AdminUserDO>()
                .select(AdminUserDO::getId, AdminUserDO::getTenantId, AdminUserDO::getStatus)
                .eq(AdminUserDO::getId, application.getPqcDecidedBy()).eq(AdminUserDO::getTenantId, tenantId)
                .eq(AdminUserDO::getDeleted, false));
        require(actor != null && Objects.equals(tenantId, actor.getTenantId())
                && Objects.equals(application.getPqcDecidedBy(), actor.getId()) && CommonStatusEnum.isEnable(actor.getStatus()),
                taskId, "formal PQC deciding system user is missing, disabled or outside the tenant");
        // Both official decisions persist their receipt; only APPROVE creates a PQC_RELEASE signature.
        var receipt = JsonUtils.parseTree(application.getDossierSummaryJson());
        require(receipt != null && receipt.isObject()
                && exactJsonId(receipt.path("applicationId"), applicationId)
                && exactJsonId(receipt.path("pqcReleaseWorkTaskId"), taskId)
                && ("APPROVE".equals(decision)
                    ? exactJsonId(receipt.path("batchExecutionId"), application.getBatchExecutionId())
                    : (receipt.path("batchExecutionId").isMissingNode() || receipt.path("batchExecutionId").isNull()))
                && exactJsonId(receipt.path("decidedBy"), actor.getId())
                && Objects.equals(decision, receipt.path("decision").asText())
                && (!"REJECT".equals(decision) || (Objects.equals(application.getPqcRejectReason(), receipt.path("rejectReason").asText())
                    && (receipt.path("signatureId").isMissingNode() || receipt.path("signatureId").isNull()))),
                taskId, "formal PQC decision receipt is inconsistent");
        if ("APPROVE".equals(decision)) {
            var signatureId = receipt.path("signatureId");
            require(signatureId.asText().matches("[1-9][0-9]*"), taskId, "formal PQC approval signature is missing");
            requirePqcSignature(taskId, application, Long.valueOf(signatureId.asText()));
        }
        return actor.getId();
    }

    private cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO requireQaClosure(
            Long taskId, MesProcessPoolActiveOrderReleaseApplicationDO application, Long reviewId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String disposition = "NONCONFORMANCE_REWORK".equals(application.getApplicationStatus()) ? "rework" : "void";
        require(positive(application.getQaClosureReviewId()) && (reviewId == null
                || Objects.equals(reviewId, application.getQaClosureReviewId())), taskId, "formal QA closure reference is missing or inconsistent");
        var review = nonconformanceReviews.selectOne(new LambdaQueryWrapperX<cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO>()
                .eq(cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO::getId, application.getQaClosureReviewId())
                .eq(cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO::getTenantId, tenantId)
                .eq(cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO::getReviewStatus, "closed")
                .eq(cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO::getDisposition, disposition)
                );
        require(review != null && Objects.equals(tenantId, review.getTenantId())
                && Objects.equals(application.getActiveOrderId(), review.getActiveOrderId())
                && (review.getBatchExecutionId() == null || Objects.equals(application.getBatchExecutionId(), review.getBatchExecutionId()))
                && Objects.equals(application.getWorkOrderId(), review.getWorkOrderId())
                && review.getClosedAt() != null && positive(review.getQaUserId()), taskId, "formal QA closure is missing or mismatched");
        var actor = users.selectById(review.getQaUserId());
        require(actor != null && Objects.equals(tenantId, actor.getTenantId()) && CommonStatusEnum.isEnable(actor.getStatus()),
                taskId, "QA deciding system account is missing, disabled or outside the tenant");
        var snapshot = JsonUtils.parseTree(review.getTraceSnapshotJson()).path("qaSignatureSnapshotJson");
        require(exactJsonId(snapshot.path("reviewId"), review.getId()) && exactJsonId(snapshot.path("qaUserId"), actor.getId())
                && disposition.equals(snapshot.path("disposition").asText()) && "QA_DISPOSITION".equals(snapshot.path("actionType").asText())
                && snapshot.path("signatureId").asText().matches("[1-9][0-9]*") && text(snapshot.path("aggregateHash").asText()),
                taskId, "QA closure signature snapshot is inconsistent");
        Long signatureId = Long.valueOf(snapshot.path("signatureId").asText());
        var evidence = signatures.getById(signatureId);
        require(evidence != null && Objects.equals(signatureId, evidence.id()) && Objects.equals(actor.getId(), evidence.actorId())
                && "MES".equals(evidence.moduleCode()) && "QA_DISPOSITION".equals(evidence.actionCode())
                && "MES_BATCH_RECORD".equals(evidence.subjectType()) && "VALID".equals(evidence.verificationStatus())
                && "SESSION_PLUS_PASSWORD".equals(evidence.authenticationMethod()) && evidence.signedAt() != null
                && Objects.equals(review.getReviewOpinion(), evidence.reason()) && text(evidence.contentHash()) && text(evidence.evidenceHash()),
                taskId, "QA closure signature identity is inconsistent");
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, "QA_DISPOSITION",
                null, null, null, null, null, null, null, "EDHR_NONCONFORMANCE_REVIEW", review.getId(),
                "eDHR不合格评审处置", "QA_DISPOSITION", null, null, snapshot.path("aggregateHash").asText(), null);
        require(Objects.equals(subject, evidence.subjectId()) && Objects.equals(MesProBatchRecordExecutionFieldAuditHasher.sha256(subject), evidence.subjectVersion()),
                taskId, "QA signature belongs to another disposition");
        var verified = signatures.verifyEvidence(signatureId);
        require(verified != null && Objects.equals(signatureId, verified.signatureId()) && "VALID".equals(verified.verificationStatus())
                && Objects.equals(evidence.contentHash(), verified.storedContentHash()) && Objects.equals(evidence.contentHash(), verified.calculatedContentHash())
                && Objects.equals(evidence.evidenceHash(), verified.storedEvidenceHash()) && Objects.equals(evidence.evidenceHash(), verified.calculatedEvidenceHash()),
                taskId, "QA closure signature integrity failed");
        var expected = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(new SignatureSubjectCommand(actor.getId(),
                "MES", "QA_DISPOSITION", "MES_BATCH_RECORD", subject, evidence.subjectVersion(), evidence.reason()));
        require(JsonUtils.parseTree(expected.canonicalContentJson()).equals(JsonUtils.parseTree(evidence.canonicalContentJson())),
                taskId, "QA disposition canonical signature content is inconsistent");
        return review;
    }

    private void requirePqcSignature(Long taskId, MesProcessPoolActiveOrderReleaseApplicationDO application, Long signatureId) {
        var evidence = signatures.getById(signatureId);
        require(evidence != null && Objects.equals(signatureId, evidence.id())
                && Objects.equals(application.getPqcDecidedBy(), evidence.actorId()) && evidence.signedAt() != null
                && "MES".equals(evidence.moduleCode()) && "PQC_RELEASE".equals(evidence.actionCode())
                && "MES_BATCH_RECORD".equals(evidence.subjectType()) && "VALID".equals(evidence.verificationStatus())
                && "SESSION_PLUS_PASSWORD".equals(evidence.authenticationMethod())
                && text(evidence.contentHash()) && text(evidence.evidenceHash()),
                taskId, "formal PQC approval signature identity is inconsistent");
        var verified = signatures.verifyEvidence(signatureId);
        require(verified != null && Objects.equals(signatureId, verified.signatureId())
                && "VALID".equals(verified.verificationStatus())
                && Objects.equals(evidence.contentHash(), verified.storedContentHash())
                && Objects.equals(evidence.contentHash(), verified.calculatedContentHash())
                && Objects.equals(evidence.evidenceHash(), verified.storedEvidenceHash())
                && Objects.equals(evidence.evidenceHash(), verified.calculatedEvidenceHash()), taskId, "PQC approval signature integrity failed");
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(application.getBatchExecutionId(), "PQC_RELEASE",
                null, null, null, null, null, null, null, "PQC_RELEASE_APPLICATION", application.getId(),
                "PQC生产放行", "PQC_RELEASE", null, null, null, null);
        require(Objects.equals(subject, evidence.subjectId())
                && Objects.equals(MesProBatchRecordExecutionFieldAuditHasher.sha256(subject), evidence.subjectVersion()),
                taskId, "PQC approval signature belongs to another application or batch");
        var expected = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(new SignatureSubjectCommand(
                evidence.actorId(), "MES", "PQC_RELEASE", "MES_BATCH_RECORD", subject, evidence.subjectVersion(), evidence.reason()));
        require(JsonUtils.parseTree(expected.canonicalContentJson()).equals(JsonUtils.parseTree(evidence.canonicalContentJson())),
                taskId, "PQC approval canonical signature content is inconsistent");
    }

    private static boolean exactJsonId(com.fasterxml.jackson.databind.JsonNode node, Long expected) {
        return (node.isIntegralNumber() || node.isTextual()) && Objects.equals(String.valueOf(expected), node.asText());
    }

    private static boolean text(String value) { return value != null && !value.isBlank(); }

    private static boolean positive(Long value) {
        return value != null && value > 0;
    }

    private static void require(boolean condition, Long taskId, String reason) {
        if (!condition) {
            throw new IllegalStateException("APPROVAL_COMPLETED_ACTOR_INVALID: eDHR task " + taskId + ": " + reason);
        }
    }
}
