package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditCommand;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditEventType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditRecorder;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlocker;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowFailureRespVO;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowIdempotency;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStage;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRequiredCandidateResolver;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCandidates;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCodes;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class MesPqcProductionReleaseServiceImpl implements MesPqcProductionReleaseService {

    private static final String TASK_TYPE_PQC_RELEASE = "PQC_PRODUCTION_RELEASE";
    private static final String BUSINESS_SCOPE_RELEASE_APPLICATION = "RELEASE_APPLICATION";
    private static final Set<String> ACTIVE_TASK_STATUSES = Set.of(
            MesProEdhrWorkTaskStatus.TODO,
            MesProEdhrWorkTaskStatus.DOING,
            MesProEdhrWorkTaskStatus.OVERDUE);
    private static final Set<String> REQUIRED_REPORT_NODE_TYPES = Set.of(
            "INCOMING_INSPECTION_REPORT",
            "STERILIZATION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_RECORD");

    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    private final MesProEdhrWorkTaskMapper workTaskMapper;
    private final MesProductionReleaseRequiredCandidateResolver candidateResolver;
    private final MesPqcReleaseDossierPort dossierPort;
    private final MesProductionReleaseBatchExecutionPort batchExecutionPort;
    private final MesProductionReleaseReportStageInitializer reportStageInitializer;
    private final MesReleaseFlowAuditRecorder auditRecorder;
    private final Clock clock;

    @Autowired
    public MesPqcProductionReleaseServiceImpl(
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper,
            MesProEdhrWorkTaskMapper workTaskMapper,
            MesProductionReleaseRequiredCandidateResolver candidateResolver,
            MesPqcReleaseDossierPort dossierPort,
            MesProductionReleaseBatchExecutionPort batchExecutionPort,
            MesProductionReleaseReportStageInitializer reportStageInitializer,
            MesReleaseFlowAuditRecorder auditRecorder) {
        this(applicationMapper, workTaskMapper, candidateResolver, dossierPort, batchExecutionPort,
                reportStageInitializer, auditRecorder, Clock.systemUTC());
    }

    public MesPqcProductionReleaseServiceImpl(
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper,
            MesProEdhrWorkTaskMapper workTaskMapper,
            MesProductionReleaseRequiredCandidateResolver candidateResolver,
            MesPqcReleaseDossierPort dossierPort,
            MesProductionReleaseBatchExecutionPort batchExecutionPort,
            MesProductionReleaseReportStageInitializer reportStageInitializer,
            MesReleaseFlowAuditRecorder auditRecorder,
            Clock clock) {
        this.applicationMapper = applicationMapper;
        this.workTaskMapper = workTaskMapper;
        this.candidateResolver = candidateResolver;
        this.dossierPort = dossierPort;
        this.batchExecutionPort = batchExecutionPort;
        this.reportStageInitializer = reportStageInitializer;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesPqcProductionReleaseDecisionResult approve(
            Long actorUserId, MesPqcProductionReleaseApproveCommand command) {
        requireApproveCommand(actorUserId, command);
        String idempotencyKey = MesReleaseFlowIdempotency.requireKey(command.getIdempotencyKey());
        String opinion = trimAndValidateOptionalText(command.getApprovalOpinion(), "approvalOpinion");
        String payloadHash = decisionPayloadHash("APPROVE", command.getApplicationId(),
                command.getPqcReleaseWorkTaskId(), command.getExpectedVersion(), opinion);
        MesProcessPoolActiveOrderReleaseApplicationDO application = requireApplicationForUpdate(command.getApplicationId());
        MesPqcProductionReleaseDecisionResult replay = replayOrRejectProcessedApplication(
                application, "APPROVE", idempotencyKey, payloadHash);
        if (replay != null) {
            return replay;
        }
        MesProEdhrWorkTaskDO workTask = requireProcessableTask(
                application, command.getPqcReleaseWorkTaskId(), command.getExpectedVersion(), actorUserId);

        MesPqcReleaseDossierPlan dossierPlan = dossierPort.plan(application, actorUserId);
        if (dossierPlan == null || !Objects.equals(application.getSourceSnapshotHash(),
                dossierPlan.getSourceSnapshotHash())) {
            throw blocker(MesReleaseFlowBlockerType.FROZEN_ROUTE_SOURCE_REQUIRED, application,
                    "RELEASE_APPLICATION", String.valueOf(application.getId()),
                    "authoritative source snapshot changed after the release application",
                    "create a new release application from the current frozen route sources");
        }
        Long batchExecutionId = batchExecutionPort.openOrCreate(new MesProductionReleaseBatchExecutionCommand()
                .setApplicationId(application.getId())
                .setWorkOrderId(application.getWorkOrderId())
                .setWorkOrderCode(application.getWorkOrderCode())
                .setBatchCode(application.getBatchCode())
                .setRouteId(application.getRouteId())
                .setRouteVersionId(application.getRouteVersionId())
                .setEntryType(command.getEntryType())
                .setEntryBusinessId(command.getEntryBusinessId())
                .setSourceCredentialType(command.getSourceCredentialType())
                .setSourceCredentialId(command.getSourceCredentialId())
                .setSourceRelationId(command.getSourceRelationId())
                .setSourceContextHash(command.getSourceContextHash())
                .setTenantId(command.getTenantId())
                .setActiveOrderId(application.getActiveOrderId())
                .setPickListBindingId(command.getPickListBindingId())
                .setPickListId(command.getPickListId())
                .setBindingVersion(command.getBindingVersion())
                .setBatchPickListRelationId(command.getBatchPickListRelationId())
                .setSourceSnapshotHash(command.getSourceSnapshotHash())
                .setIdempotencyKey(command.getIdempotencyKey())
                .setExpectedSourceVersion(command.getExpectedSourceVersion())
                .setPayloadHash(command.getPayloadHash())
                .setCompletionTransactionId(command.getCompletionTransactionId())
                .setExpectedActiveOrderVersion(command.getExpectedActiveOrderVersion())
                .setCompletionVersion(command.getCompletionVersion())
                .setSourceVersion(command.getSourceVersion())
                .setSourceBundleHash(command.getSourceBundleHash())
                .setCompletionBackfillReceiptId(command.getCompletionBackfillReceiptId())
                .setCompletionBackfillReceiptHash(command.getCompletionBackfillReceiptHash())
                .setPickListHeaderSnapshotHash(command.getPickListHeaderSnapshotHash())
                .setPickListLineSnapshotHash(command.getPickListLineSnapshotHash())
                .setSourceEvidence(command.getSourceEvidence())
                .setCompletionBackfillReceipt(command.getCompletionBackfillReceipt())
                .setIndependentReceipt(command.getIndependentReceipt()));
        if (batchExecutionId == null || batchExecutionId <= 0) {
            throw blocker(MesReleaseFlowBlockerType.LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED, application,
                    "BATCH_EXECUTION", null, "production release batch execution was not created",
                    "repair the release-bound batch execution contract before retrying");
        }

        MesPqcReleaseDossierWriteResult dossierWrite = requireDossierWrite(
                application, dossierPort.write(dossierPlan, batchExecutionId));
        MesProductionReleaseReportStageInitializationResult reportStage = requireReportStage(
                application, reportStageInitializer.initializeRequiredReportStage(
                        new MesProductionReleaseReportStageInitializationCommand()
                                .setApplicationId(application.getId())
                                .setBatchExecutionId(batchExecutionId)
                                .setRouteId(application.getRouteId())
                                .setRouteVersionId(application.getRouteVersionId())
                                .setSourceSnapshotHash(application.getSourceSnapshotHash())
                                .setExpectedApplicationVersion(command.getExpectedVersion())));
        LocalDateTime decidedAt = LocalDateTime.now(clock);
        MesPqcProductionReleaseDecisionResult result = baseResult(application, workTask)
                .setDecision("APPROVE")
                .setStatus(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)
                .setBatchExecutionId(batchExecutionId)
                .setBatchRecordEvidenceIds(copy(dossierWrite.getBatchRecordEvidenceIds()))
                .setProcessInspectionEvidenceIds(copy(dossierWrite.getProcessInspectionEvidenceIds()))
                .setLossReportEvidenceIds(copy(dossierWrite.getLossReportEvidenceIds()))
                .setLossReportStatus(dossierWrite.getLossReportStatus())
                .setHasActualLoss(dossierWrite.getHasActualLoss())
                .setLossQuantity(dossierWrite.getLossQuantity())
                .setLossSourceSnapshotHash(dossierWrite.getSourceSnapshotHash())
                .setReportUploadTasks(copy(reportStage.getReportUploadTasks()))
                .setReportSnapshotHash(reportStage.getReportSnapshotHash())
                .setVersion(command.getExpectedVersion() + 1)
                .setDecidedBy(actorUserId)
                .setDecidedAt(decidedAt)
                .setDecisionIdempotencyKey(idempotencyKey)
                .setDecisionPayloadHash(payloadHash);
        int updated = applicationMapper.approveFromPending(application.getId(), command.getExpectedVersion(),
                batchExecutionId, actorUserId, decidedAt, reportStage.getReportSnapshotHash(),
                JSON.toJSONString(result));
        requireCasSuccess(updated, application);
        requireTaskCompletion(workTaskMapper.completePqcDecisionTask(workTask.getId(), decidedAt, "APPROVE"), application);
        recordDecisionAudit(application, workTask, result, idempotencyKey,
                MesReleaseFlowAuditEventType.PQC_PRODUCTION_RELEASE_APPROVED);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesPqcProductionReleaseDecisionResult reject(
            Long actorUserId, MesPqcProductionReleaseRejectCommand command) {
        requireRejectCommand(actorUserId, command);
        String idempotencyKey = MesReleaseFlowIdempotency.requireKey(command.getIdempotencyKey());
        String reason = StrUtil.trim(command.getRejectReason());
        if (StrUtil.isBlank(reason) || reason.length() > 500) {
            throw blocker(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION, null,
                    "REJECT_REASON", null, "rejectReason must contain 1 to 500 characters",
                    "provide the formal PQC rejection reason");
        }
        String payloadHash = decisionPayloadHash("REJECT", command.getApplicationId(),
                command.getPqcReleaseWorkTaskId(), command.getExpectedVersion(), reason);
        MesProcessPoolActiveOrderReleaseApplicationDO application = requireApplicationForUpdate(command.getApplicationId());
        MesPqcProductionReleaseDecisionResult replay = replayOrRejectProcessedApplication(
                application, "REJECT", idempotencyKey, payloadHash);
        if (replay != null) {
            return replay;
        }
        MesProEdhrWorkTaskDO workTask = requireProcessableTask(
                application, command.getPqcReleaseWorkTaskId(), command.getExpectedVersion(), actorUserId);
        LocalDateTime decidedAt = LocalDateTime.now(clock);
        MesPqcProductionReleaseDecisionResult result = baseResult(application, workTask)
                .setDecision("REJECT")
                .setStatus(MesReleaseFlowStatus.PQC_RELEASE_REJECTED)
                .setRejectReason(reason)
                .setVersion(command.getExpectedVersion() + 1)
                .setDecidedBy(actorUserId)
                .setDecidedAt(decidedAt)
                .setDecisionIdempotencyKey(idempotencyKey)
                .setDecisionPayloadHash(payloadHash);
        int updated = applicationMapper.rejectFromPending(application.getId(), command.getExpectedVersion(),
                actorUserId, decidedAt, reason, JSON.toJSONString(result));
        requireCasSuccess(updated, application);
        requireTaskCompletion(workTaskMapper.completePqcDecisionTask(workTask.getId(), decidedAt, "REJECT"), application);
        recordDecisionAudit(application, workTask, result, idempotencyKey,
                MesReleaseFlowAuditEventType.PQC_PRODUCTION_RELEASE_REJECTED);
        return result;
    }

    @Override
    public MesPqcProductionReleaseDecisionResult get(Long actorUserId, Long applicationId) {
        if (actorUserId == null || applicationId == null || applicationId <= 0) {
            throw blocker(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE, null,
                    "RELEASE_APPLICATION", applicationId == null ? null : String.valueOf(applicationId),
                    "release application query identity is invalid", "provide a valid applicationId");
        }
        MesProcessPoolActiveOrderReleaseApplicationDO application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw blocker(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE, null,
                    "RELEASE_APPLICATION", String.valueOf(applicationId),
                    "production release application does not exist", "query an existing release application");
        }
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(application.getPqcReleaseWorkTaskId());
        requireAuthorized(application, workTask, actorUserId);
        MesPqcProductionReleaseDecisionResult stored = parseStoredDecision(application);
        return stored == null ? baseResult(application, workTask)
                .setDecision(application.getPqcDecision())
                .setStatus(application.getApplicationStatus())
                .setRejectReason(application.getPqcRejectReason())
                .setBatchExecutionId(application.getBatchExecutionId())
                .setReportSnapshotHash(application.getReportSnapshotHash())
                .setVersion(application.getVersion())
                .setDecidedBy(application.getPqcDecidedBy())
                .setDecidedAt(application.getPqcDecidedAt()) : stored;
    }

    private void requireApproveCommand(Long actorUserId, MesPqcProductionReleaseApproveCommand command) {
        if (actorUserId == null || command == null || command.getApplicationId() == null
                || command.getApplicationId() <= 0 || command.getPqcReleaseWorkTaskId() == null
                || command.getPqcReleaseWorkTaskId() <= 0 || command.getExpectedVersion() == null
                || command.getExpectedVersion() <= 0) {
            throw blocker(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION, null,
                    "PQC_RELEASE_DECISION", null, "PQC approval command is incomplete",
                    "provide applicationId, pqcReleaseWorkTaskId and expectedVersion");
        }
    }

    private void requireRejectCommand(Long actorUserId, MesPqcProductionReleaseRejectCommand command) {
        if (actorUserId == null || command == null || command.getApplicationId() == null
                || command.getApplicationId() <= 0 || command.getPqcReleaseWorkTaskId() == null
                || command.getPqcReleaseWorkTaskId() <= 0 || command.getExpectedVersion() == null
                || command.getExpectedVersion() <= 0) {
            throw blocker(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION, null,
                    "PQC_RELEASE_DECISION", null, "PQC rejection command is incomplete",
                    "provide applicationId, pqcReleaseWorkTaskId and expectedVersion");
        }
    }

    private String trimAndValidateOptionalText(String value, String fieldName) {
        String trimmed = StrUtil.trim(value);
        if (trimmed != null && trimmed.length() > 500) {
            throw blocker(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION, null,
                    "PQC_RELEASE_DECISION", null, fieldName + " exceeds 500 characters",
                    "shorten " + fieldName + " to at most 500 characters");
        }
        return trimmed;
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO requireApplicationForUpdate(Long applicationId) {
        MesProcessPoolActiveOrderReleaseApplicationDO application = applicationMapper.selectByIdForUpdate(applicationId);
        if (application == null || !MesReleaseFlowStatus.isPersistentStatus(application.getApplicationStatus())
                || application.getVersion() == null || application.getVersion() <= 0) {
            throw blocker(MesReleaseFlowBlockerType.LEGACY_RELEASE_APPLICATION_MIGRATION_REQUIRED, application,
                    "RELEASE_APPLICATION", String.valueOf(applicationId),
                    "release application is missing or uses a legacy lifecycle",
                    "migrate the application with approved evidence before retrying");
        }
        return application;
    }

    private MesProEdhrWorkTaskDO requireProcessableTask(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            Long workTaskId,
            Integer expectedVersion,
            Long actorUserId) {
        if (!MesReleaseFlowStatus.PQC_RELEASE_PENDING.equals(application.getApplicationStatus())
                || !Objects.equals(expectedVersion, application.getVersion())) {
            throw blocker(MesReleaseFlowBlockerType.STATE_VERSION_CONFLICT, application,
                    "RELEASE_APPLICATION", String.valueOf(application.getId()),
                    "application status or version no longer matches the PQC decision command",
                    "reload the authoritative receipt before deciding");
        }
        if (!Objects.equals(workTaskId, application.getPqcReleaseWorkTaskId())) {
            throw blocker(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE, application,
                    "WORK_TASK", String.valueOf(workTaskId), "PQC work task does not belong to the application",
                    "use the frozen PQC work task from the application receipt");
        }
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(workTaskId);
        if (workTask == null || !TASK_TYPE_PQC_RELEASE.equals(workTask.getTaskType())
                || !BUSINESS_SCOPE_RELEASE_APPLICATION.equals(workTask.getBusinessScopeType())
                || !Objects.equals(application.getId(), workTask.getBusinessScopeId())
                || !ACTIVE_TASK_STATUSES.contains(workTask.getStatus())) {
            throw blocker(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE, application,
                    "WORK_TASK", String.valueOf(workTaskId), "PQC work task is not processable",
                    "reload the current PQC work task receipt");
        }
        requireAuthorized(application, workTask, actorUserId);
        return workTask;
    }

    private void requireAuthorized(MesProcessPoolActiveOrderReleaseApplicationDO application,
                                   MesProEdhrWorkTaskDO workTask,
                                   Long actorUserId) {
        if (workTask == null || !containsCandidate(workTask.getCandidateUserSnapshot(), actorUserId)) {
            throw blocker(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE, application,
                    "WORK_TASK", workTask == null ? null : String.valueOf(workTask.getId()),
                    "current user is not in the frozen PQC candidate snapshot",
                    "use an authorized frozen PQC candidate");
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw blocker(MesReleaseFlowBlockerType.PQC_RELEASE_ROLE_REQUIRED, application,
                    "TENANT", null, "tenant context is required for PQC role verification",
                    "retry in an authenticated tenant context");
        }
        MesProductionReleaseRoleCandidates currentCandidates = candidateResolver.resolveRequiredCandidates(
                tenantId, MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER);
        if (currentCandidates == null || !currentCandidates.candidateUserIds().contains(actorUserId)) {
            throw blocker(MesReleaseFlowBlockerType.PQC_RELEASE_ROLE_REQUIRED, application,
                    "ROLE", MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER,
                    "current user no longer holds the enabled PQC release role",
                    "assign an enabled MES_PQC_RELEASE_OWNER user and retry");
        }
    }

    private boolean containsCandidate(String snapshot, Long userId) {
        if (StrUtil.isBlank(snapshot) || userId == null) {
            return false;
        }
        String expected = String.valueOf(userId);
        return List.of(snapshot.split(",")).stream()
                .map(String::trim)
                .anyMatch(expected::equals);
    }

    private MesPqcReleaseDossierWriteResult requireDossierWrite(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesPqcReleaseDossierWriteResult result) {
        boolean validLossReceipt = result != null
                && (("SUCCESS".equals(result.getLossReportStatus())
                && Boolean.TRUE.equals(result.getHasActualLoss())
                && result.getLossQuantity() != null && result.getLossQuantity().signum() > 0
                && !empty(result.getLossReportEvidenceIds()))
                || ("NOT_REQUIRED".equals(result.getLossReportStatus())
                && Boolean.FALSE.equals(result.getHasActualLoss())
                && result.getLossQuantity() != null && result.getLossQuantity().signum() == 0
                && empty(result.getLossReportEvidenceIds())));
        if (result == null || empty(result.getBatchRecordEvidenceIds())
                || empty(result.getProcessInspectionEvidenceIds()) || !validLossReceipt) {
            throw blocker(MesReleaseFlowBlockerType.BATCH_RECORD_SOURCE_REQUIRED, application,
                    "RELEASE_DOSSIER", String.valueOf(application.getId()),
                    "all three formal document mappings must return persistent evidence identifiers",
                    "repair the batch-record, process-inspection and loss-report mappings before retrying");
        }
        return result;
    }

    private MesProductionReleaseReportStageInitializationResult requireReportStage(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProductionReleaseReportStageInitializationResult result) {
        if (result == null || StrUtil.isBlank(result.getReportSnapshotHash())
                || result.getReportUploadTasks() == null || result.getReportUploadTasks().size() != 4) {
            throw reportOwnerBlocker(application, "four frozen report upload tasks are required");
        }
        Set<String> nodeTypes = new HashSet<>();
        for (MesProductionReleaseReportUploadTaskReceipt task : result.getReportUploadTasks()) {
            if (task == null || task.getBatchTaskId() == null || task.getWorkTaskId() == null
                    || task.getCandidateUserIds() == null || task.getCandidateUserIds().isEmpty()
                    || !MesProEdhrWorkTaskStatus.TODO.equals(task.getStatus())
                    || !REQUIRED_REPORT_NODE_TYPES.contains(task.getNodeType())
                    || !nodeTypes.add(task.getNodeType())) {
                throw reportOwnerBlocker(application, "report upload task receipt is incomplete or duplicated");
            }
        }
        if (!nodeTypes.equals(REQUIRED_REPORT_NODE_TYPES)) {
            throw reportOwnerBlocker(application, "report upload task node set is incomplete");
        }
        return result;
    }

    private MesReleaseFlowBlockerException reportOwnerBlocker(
            MesProcessPoolActiveOrderReleaseApplicationDO application, String reason) {
        return blocker(MesReleaseFlowBlockerType.REPORT_OWNER_REQUIRED, application,
                "REPORT_UPLOAD_STAGE", String.valueOf(application.getId()), reason,
                "configure all four frozen report owners before PQC approval");
    }

    private MesPqcProductionReleaseDecisionResult baseResult(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrWorkTaskDO workTask) {
        return new MesPqcProductionReleaseDecisionResult()
                .setApplicationId(application.getId())
                .setPqcReleaseWorkTaskId(workTask == null ? application.getPqcReleaseWorkTaskId() : workTask.getId())
                .setBatchRecordEvidenceIds(List.of())
                .setProcessInspectionEvidenceIds(List.of())
                .setLossReportEvidenceIds(List.of())
                .setReportUploadTasks(List.of())
                .setSourceSnapshotHash(application.getSourceSnapshotHash());
    }

    private MesPqcProductionReleaseDecisionResult replayOrRejectProcessedApplication(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            String decision,
            String idempotencyKey,
            String payloadHash) {
        if (MesReleaseFlowStatus.PQC_RELEASE_PENDING.equals(application.getApplicationStatus())) {
            return null;
        }
        MesPqcProductionReleaseDecisionResult stored = parseStoredDecision(application);
        if (stored != null && Objects.equals(idempotencyKey, stored.getDecisionIdempotencyKey())) {
            if (Objects.equals(decision, stored.getDecision())
                    && Objects.equals(payloadHash, stored.getDecisionPayloadHash())) {
                return stored;
            }
            throw blocker(MesReleaseFlowBlockerType.IDEMPOTENCY_PAYLOAD_CONFLICT, application,
                    "IDEMPOTENCY_KEY", idempotencyKey,
                    "idempotency key is already bound to a different PQC decision payload",
                    "query the existing receipt or submit a new key for a new decision");
        }
        throw blocker(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION, application,
                "RELEASE_APPLICATION", String.valueOf(application.getId()),
                "PQC decision is already terminal for this application",
                "query the authoritative decision receipt");
    }

    private MesPqcProductionReleaseDecisionResult parseStoredDecision(
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        if (StrUtil.isBlank(application.getDossierSummaryJson())) {
            return null;
        }
        try {
            return JSON.parseObject(application.getDossierSummaryJson(), MesPqcProductionReleaseDecisionResult.class);
        } catch (RuntimeException exception) {
            throw blocker(MesReleaseFlowBlockerType.LEGACY_RELEASE_APPLICATION_MIGRATION_REQUIRED, application,
                    "RELEASE_APPLICATION", String.valueOf(application.getId()),
                    "stored PQC decision receipt is invalid",
                    "repair the historical receipt with approved evidence before retrying");
        }
    }

    private void requireCasSuccess(int updated, MesProcessPoolActiveOrderReleaseApplicationDO application) {
        if (updated != 1) {
            throw blocker(MesReleaseFlowBlockerType.STATE_VERSION_CONFLICT, application,
                    "RELEASE_APPLICATION", String.valueOf(application.getId()),
                    "PQC decision lost the application version race",
                    "reload the authoritative receipt before deciding");
        }
    }

    private void requireTaskCompletion(int updated, MesProcessPoolActiveOrderReleaseApplicationDO application) {
        if (updated != 1) {
            throw blocker(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE, application,
                    "WORK_TASK", String.valueOf(application.getPqcReleaseWorkTaskId()),
                    "PQC work task completion lost its status race",
                    "reload the authoritative work task before deciding");
        }
    }

    private void recordDecisionAudit(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrWorkTaskDO workTask,
            MesPqcProductionReleaseDecisionResult result,
            String idempotencyKey,
            String eventType) {
        auditRecorder.record(new MesReleaseFlowAuditCommand()
                .setEventType(eventType)
                .setStage(MesReleaseFlowStage.SP_2)
                .setRequestId(idempotencyKey)
                .setIdempotencyKey(idempotencyKey)
                .setTenantId(TenantContextHolder.getTenantId())
                .setApplicationId(application.getId())
                .setWorkTaskId(workTask.getId())
                .setBatchExecutionId(result.getBatchExecutionId())
                .setFromStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                .setToStatus(result.getStatus())
                .setVersion(result.getVersion())
                .setActorUserId(result.getDecidedBy())
                .setOccurredAt(result.getDecidedAt())
                .setSourceSnapshotHash(result.getSourceSnapshotHash())
                .setResultStatus("SUCCESS"));
    }

    private String decisionPayloadHash(
            String decision, Long applicationId, Long workTaskId, Integer expectedVersion, String detail) {
        return MesReleaseFlowIdempotency.payloadHash(decision, String.valueOf(applicationId),
                String.valueOf(workTaskId), String.valueOf(expectedVersion), detail);
    }

    private boolean empty(List<?> values) {
        return values == null || values.isEmpty();
    }

    private <T> List<T> copy(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private MesReleaseFlowBlockerException blocker(
            MesReleaseFlowBlockerType type,
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            String objectType,
            String objectId,
            String reason,
            String suggestion) {
        return new MesReleaseFlowBlockerException(reason, new MesReleaseFlowFailureRespVO()
                .setStage(MesReleaseFlowStage.SP_2)
                .setCurrentStatus(application == null ? null : application.getApplicationStatus())
                .setBlockers(List.of(new MesReleaseFlowBlocker()
                        .setBlockerType(type)
                        .setObjectType(objectType)
                        .setObjectId(objectId)
                        .setReason(reason)
                        .setSuggestion(suggestion))));
    }
}
