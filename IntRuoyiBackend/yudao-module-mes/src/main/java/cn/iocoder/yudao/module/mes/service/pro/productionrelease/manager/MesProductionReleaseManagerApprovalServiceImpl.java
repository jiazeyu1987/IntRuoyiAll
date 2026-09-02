package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.signature.BpmApprovalSignatureRecordDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.signature.BpmApprovalSignatureRecordMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseApproveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
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
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseReportNodeEvidence;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseReportSnapshots;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRequiredCandidateResolver;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCandidates;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCodes;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class MesProductionReleaseManagerApprovalServiceImpl
        implements MesProductionReleaseManagerApprovalService {

    private static final String EVENT_TYPE_APPROVE = "APPROVE";
    private static final String TASK_TYPE_RELEASE_APPROVE = "RELEASE_APPROVE";
    private static final String BUSINESS_SCOPE_RELEASE_TRANSACTION = "RELEASE_TRANSACTION";
    private static final String CANDIDATE_SOURCE_ROLE = "ROLE_GROUP";
    private static final int BATCH_TASK_STATUS_APPROVED = 40;
    private static final Set<String> REQUIRED_NODE_TYPES = Set.of(
            "INCOMING_INSPECTION_REPORT",
            "STERILIZATION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_RECORD");

    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    private final MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    private final MesProEdhrReleaseTransactionEventMapper releaseEventMapper;
    private final MesProEdhrWorkTaskMapper workTaskMapper;
    private final MesProEdhrBatchExecutionMapper batchExecutionMapper;
    private final MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    private final MesProductionReleaseRequiredCandidateResolver candidateResolver;
    private final BpmApprovalSignatureRecordMapper approvalSignatureRecordMapper;
    private final MesReleaseFlowAuditRecorder auditRecorder;
    private final Clock clock;

    @Autowired
    public MesProductionReleaseManagerApprovalServiceImpl(
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper,
            MesProEdhrReleaseTransactionMapper releaseTransactionMapper,
            MesProEdhrReleaseTransactionEventMapper releaseEventMapper,
            MesProEdhrWorkTaskMapper workTaskMapper,
            MesProEdhrBatchExecutionMapper batchExecutionMapper,
            MesProEdhrBatchExecutionTaskMapper batchTaskMapper,
            MesProductionReleaseRequiredCandidateResolver candidateResolver,
            BpmApprovalSignatureRecordMapper approvalSignatureRecordMapper,
            MesReleaseFlowAuditRecorder auditRecorder) {
        this(applicationMapper, releaseTransactionMapper, releaseEventMapper, workTaskMapper,
                batchExecutionMapper, batchTaskMapper, candidateResolver, approvalSignatureRecordMapper,
                auditRecorder, Clock.systemUTC());
    }

    MesProductionReleaseManagerApprovalServiceImpl(
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper,
            MesProEdhrReleaseTransactionMapper releaseTransactionMapper,
            MesProEdhrReleaseTransactionEventMapper releaseEventMapper,
            MesProEdhrWorkTaskMapper workTaskMapper,
            MesProEdhrBatchExecutionMapper batchExecutionMapper,
            MesProEdhrBatchExecutionTaskMapper batchTaskMapper,
            MesProductionReleaseRequiredCandidateResolver candidateResolver,
            BpmApprovalSignatureRecordMapper approvalSignatureRecordMapper,
            MesReleaseFlowAuditRecorder auditRecorder,
            Clock clock) {
        this.applicationMapper = applicationMapper;
        this.releaseTransactionMapper = releaseTransactionMapper;
        this.releaseEventMapper = releaseEventMapper;
        this.workTaskMapper = workTaskMapper;
        this.batchExecutionMapper = batchExecutionMapper;
        this.batchTaskMapper = batchTaskMapper;
        this.candidateResolver = candidateResolver;
        this.approvalSignatureRecordMapper = approvalSignatureRecordMapper;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    @Override
    public boolean isManagedReleaseTransaction(Long releaseTransactionId) {
        List<MesProcessPoolActiveOrderReleaseApplicationDO> applications =
                applicationMapper.selectListByReleaseTransactionId(releaseTransactionId);
        if (applications.size() > 1) {
            throw blocker(applications.get(0), MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                    "release transaction is linked to multiple release applications",
                    "repair the formal release application relationship before continuing");
        }
        return applications.size() == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProductionReleaseManagerApprovalResult prepareForFinalization(
            Long actorUserId, MesProEdhrReleaseApproveReqVO command) {
        requireCommand(actorUserId, command);
        String idempotencyKey = MesReleaseFlowIdempotency.requireKey(command.getIdempotencyKey());
        command.setIdempotencyKey(idempotencyKey)
                .setSignoffEvidenceHash(StrUtil.trim(command.getSignoffEvidenceHash()))
                .setApprovalOpinion(StrUtil.trim(command.getApprovalOpinion()));
        MesProcessPoolActiveOrderReleaseApplicationDO application =
                applicationMapper.selectByReleaseTransactionIdForUpdate(command.getReleaseTransactionId());
        if (application == null) {
            throw blocker(null, MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                    "release transaction is not linked to a production release application",
                    "use the authoritative manager release work task");
        }
        if (command.getReleaseApplicationId() != null
                && !Objects.equals(command.getReleaseApplicationId(), application.getId())) {
            throw blocker(application, MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                    "release application id does not match the transaction's authoritative application",
                    "retry with the release application id returned by the owner service");
        }
        MesProEdhrReleaseTransactionEventDO existingEvent =
                releaseEventMapper.selectByReleaseTransactionIdAndEventTypeAndIdempotencyKey(
                        command.getReleaseTransactionId(), EVENT_TYPE_APPROVE, idempotencyKey);
        if (existingEvent != null) {
            return replay(application, actorUserId, command, existingEvent);
        }

        MesProEdhrReleaseTransactionDO transaction =
                releaseTransactionMapper.selectByIdForUpdate(command.getReleaseTransactionId());
        requirePendingState(application, transaction, command);
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectByIdForUpdate(command.getWorkTaskId());
        requireManagerCandidate(application, workTask, actorUserId, command.getReleaseTransactionId());
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(application.getBatchExecutionId());
        if (batch == null) {
            throw blocker(application, MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                    "release batch execution is missing",
                    "restore the authoritative batch execution before continuing");
        }
        List<MesProductionReleaseReportNodeEvidence> evidences = collectReportEvidences(application);
        String recomputedReportSnapshotHash = MesProductionReleaseReportSnapshots.hash(application, evidences);
        if (!Objects.equals(application.getReportSnapshotHash(), recomputedReportSnapshotHash)) {
            throw blocker(application, MesReleaseFlowBlockerType.REPORT_SNAPSHOT_CHANGED,
                    "one or more frozen report evidences changed before final release",
                    "restore the four approved report evidences and retry with a fresh task receipt");
        }
        requireSignoffEvidence(workTask, actorUserId, command.getSignoffEvidenceHash(), application);

        return new MesProductionReleaseManagerApprovalResult()
                .setBatchExecution(batch)
                .setReleaseTransaction(transaction)
                .setApplication(application)
                .setWorkTask(workTask)
                .setReportSnapshotHash(recomputedReportSnapshotHash)
                .setApprovalPayloadHash(approvalPayloadHash(actorUserId, command, recomputedReportSnapshotHash))
                .setOccurredAt(now())
                .setApplicationStatus(application.getApplicationStatus())
                .setApplicationVersion(application.getVersion())
                .setReplayed(false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProductionReleaseManagerApprovalResult completeAfterFinalization(
            Long actorUserId,
            MesProEdhrReleaseApproveReqVO command,
            MesProductionReleaseManagerApprovalResult prepared,
            MesProEdhrReleaseTransactionDO released) {
        requireCommand(actorUserId, command);
        if (prepared == null || prepared.getApplication() == null || prepared.getWorkTask() == null
                || released == null
                || !Objects.equals(released.getReleaseStatus(), MesProEdhrReleaseServiceImpl.STATUS_RELEASED)) {
            throw blocker(prepared == null ? null : prepared.getApplication(),
                    MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                    "flow10 must release the transaction before manager side effects are completed",
                    "retry through the flow10 finalization command");
        }
        MesProcessPoolActiveOrderReleaseApplicationDO application = prepared.getApplication();
        MesProEdhrWorkTaskDO workTask = prepared.getWorkTask();
        String reportSnapshotHash = prepared.getReportSnapshotHash();
        LocalDateTime occurredAt = prepared.getOccurredAt() == null ? now() : prepared.getOccurredAt();
        String opinion = StrUtil.trim(command.getApprovalOpinion());
        if (applicationMapper.releaseFromManager(
                application.getId(), application.getVersion(), reportSnapshotHash,
                released.getId(), workTask.getId()) != 1) {
            throw versionConflict(application);
        }
        if (workTaskMapper.completeManagerReleaseTask(workTask.getId(), occurredAt, opinion) != 1) {
            throw blocker(application, MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE,
                    "manager release work task changed concurrently",
                    "refresh the manager release task before retrying");
        }
        recordEvent(released, application, workTask, actorUserId, command,
                prepared.getApprovalPayloadHash(), occurredAt);
        auditRecorder.record(new MesReleaseFlowAuditCommand()
                .setEventType(MesReleaseFlowAuditEventType.BATCH_RECORD_RELEASE_APPROVED)
                .setStage(MesReleaseFlowStage.SP_4)
                .setRequestId(command.getIdempotencyKey())
                .setIdempotencyKey(command.getIdempotencyKey())
                .setTenantId(TenantContextHolder.getTenantId())
                .setApplicationId(application.getId())
                .setWorkTaskId(workTask.getId())
                .setBatchExecutionId(application.getBatchExecutionId())
                .setReleaseTransactionId(released.getId())
                .setFromStatus(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING)
                .setToStatus(MesReleaseFlowStatus.RELEASED)
                .setVersion(application.getVersion() + 1)
                .setActorUserId(actorUserId)
                .setOccurredAt(occurredAt)
                .setSourceSnapshotHash(reportSnapshotHash)
                .setResultStatus("SUCCESS"));
        return prepared.setReleaseTransaction(released)
                .setApplicationStatus(MesReleaseFlowStatus.RELEASED)
                .setApplicationVersion(application.getVersion() + 1)
                .setReplayed(false);
    }

    @Override
    @Deprecated
    public MesProductionReleaseManagerApprovalResult approve(
            Long actorUserId, MesProEdhrReleaseApproveReqVO command) {
        throw blocker(null, MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                "manager approval must be finalized by flow10",
                "call the unified release finalization command");
    }

    @Override
    public void assertActionSupported(Long releaseTransactionId, String action) {
        if (releaseTransactionId == null || !(Objects.equals("REJECT", action) || Objects.equals("WITHDRAW", action))) {
            return;
        }
        List<MesProcessPoolActiveOrderReleaseApplicationDO> applications =
                applicationMapper.selectListByReleaseTransactionId(releaseTransactionId);
        if (!applications.isEmpty()) {
            throw blocker(applications.get(0), MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION,
                    "production release manager tasks do not support " + action.toLowerCase(),
                    "approve the final release or leave the task pending for an authorized manager representative");
        }
    }

    private void requireCommand(Long actorUserId, MesProEdhrReleaseApproveReqVO command) {
        if (actorUserId == null || command == null || command.getReleaseTransactionId() == null
                || command.getWorkTaskId() == null || command.getExpectedVersion() == null
                || command.getExpectedVersion() < 0 || StrUtil.isBlank(command.getSignoffEvidenceHash())) {
            throw blocker(null, MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                    "manager task, release transaction version and signoff evidence are required",
                    "submit the authoritative manager release task receipt and electronic signoff");
        }
    }

    private void requirePendingState(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrReleaseTransactionDO transaction,
            MesProEdhrReleaseApproveReqVO command) {
        if (!Objects.equals(application.getApplicationStatus(), MesReleaseFlowStatus.MANAGER_RELEASE_PENDING)
                || !Objects.equals(application.getReleaseTransactionId(), command.getReleaseTransactionId())
                || !Objects.equals(application.getReleaseApprovalWorkTaskId(), command.getWorkTaskId())
                || transaction == null
                || !Objects.equals(transaction.getBatchExecutionId(), application.getBatchExecutionId())
                || !Objects.equals(transaction.getReleaseStatus(), MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL)) {
            throw blocker(application, MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                    "release application is not in the manager release pending state",
                    "refresh the authoritative release application and manager task");
        }
        if (!Objects.equals(transaction.getVersion(), command.getExpectedVersion())) {
            throw versionConflict(application);
        }
    }

    private MesProductionReleaseRoleCandidates requireManagerCandidate(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrWorkTaskDO workTask,
            Long actorUserId,
            Long releaseTransactionId) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("tenantId is required for manager release approval");
        }
        MesProductionReleaseRoleCandidates candidates = candidateResolver.resolveRequiredCandidates(
                tenantId, MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE);
        boolean taskValid = workTask != null
                && Objects.equals(workTask.getTaskType(), TASK_TYPE_RELEASE_APPROVE)
                && Objects.equals(workTask.getBusinessScopeType(), BUSINESS_SCOPE_RELEASE_TRANSACTION)
                && Objects.equals(workTask.getBusinessScopeId(), releaseTransactionId)
                && Objects.equals(workTask.getBatchExecutionId(), application.getBatchExecutionId())
                && Objects.equals(workTask.getCandidateSourceType(), CANDIDATE_SOURCE_ROLE)
                && candidates != null
                && Objects.equals(candidates.roleCode(), MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE)
                && Objects.equals(workTask.getCandidateSourceId(), candidates.roleId())
                && Objects.equals(workTask.getResponsibilitySourceKey(),
                MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE)
                && candidates.candidateUserIds().contains(actorUserId)
                && containsCandidate(workTask.getCandidateUserSnapshot(), actorUserId)
                && isProcessable(workTask.getStatus());
        if (!taskValid) {
            throw blocker(application, MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE,
                    "current user is not both a management representative and a frozen task candidate",
                    "use an enabled tenant management representative account assigned to this task");
        }
        return candidates;
    }

    private List<MesProductionReleaseReportNodeEvidence> collectReportEvidences(
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        List<MesProEdhrBatchExecutionTaskDO> reportTasks = batchTaskMapper
                .selectListByBatchExecutionId(application.getBatchExecutionId()).stream()
                .filter(task -> REQUIRED_NODE_TYPES.contains(task.getNodeType()))
                .toList();
        if (reportTasks.size() != REQUIRED_NODE_TYPES.size()
                || reportTasks.stream().map(MesProEdhrBatchExecutionTaskDO::getNodeType).distinct().count()
                != REQUIRED_NODE_TYPES.size()) {
            throw reportChanged(application, "release batch does not contain four distinct report nodes");
        }
        return reportTasks.stream()
                .map(task -> requireReportEvidence(application, task))
                .sorted(Comparator.comparing(MesProductionReleaseReportNodeEvidence::getNodeType))
                .toList();
    }

    private MesProductionReleaseReportNodeEvidence requireReportEvidence(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrBatchExecutionTaskDO task) {
        MesProductionReleaseReportNodeEvidence evidence =
                MesProductionReleaseReportNodeEvidence.fromPayloadJson(task.getSpecialPayloadJson());
        if (!Objects.equals(task.getStatus(), BATCH_TASK_STATUS_APPROVED)
                || evidence == null
                || !Objects.equals(evidence.getBatchExecutionId(), application.getBatchExecutionId())
                || !Objects.equals(evidence.getBatchTaskId(), task.getId())
                || !Objects.equals(evidence.getNodeType(), task.getNodeType())
                || (Objects.equals("STERILIZATION_REPORT", evidence.getNodeType())
                && StrUtil.isBlank(evidence.getSterilizationBatchNo()))
                || !Objects.equals(evidence.getActiveAttachmentVersion(), 1)
                || evidence.getAttachmentIds() == null || evidence.getAttachmentIds().isEmpty()
                || evidence.getAttachmentHashes() == null
                || evidence.getAttachmentHashes().size() != evidence.getAttachmentIds().size()
                || evidence.getAttachmentHashes().stream().anyMatch(hash -> hash == null
                || !hash.matches("[0-9a-f]{64}"))) {
            throw reportChanged(application, "completed report evidence is missing or inconsistent");
        }
        return evidence;
    }

    private void requireSignoffEvidence(
            MesProEdhrWorkTaskDO workTask,
            Long actorUserId,
            String signoffEvidenceHash,
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        List<BpmApprovalSignatureRecordDO> records = approvalSignatureRecordMapper.selectList(
                new LambdaQueryWrapperX<BpmApprovalSignatureRecordDO>()
                        .eq(BpmApprovalSignatureRecordDO::getModuleCode, "EDHR")
                        .eq(BpmApprovalSignatureRecordDO::getSourceTaskType, "EDHR_WORK_TASK")
                        .eq(BpmApprovalSignatureRecordDO::getSourceTaskId, String.valueOf(workTask.getId()))
                        .eq(BpmApprovalSignatureRecordDO::getSignerUserId, actorUserId)
                        .eq(BpmApprovalSignatureRecordDO::getReviewResult, "APPROVE"));
        boolean evidenceMatched = records.stream().anyMatch(record ->
                Boolean.TRUE.equals(record.getPasswordVerified())
                        && StrUtil.isNotBlank(record.getSignatureImageFileUrl())
                        && Objects.equals(DigestUtil.sha256Hex(StrUtil.trim(record.getSignatureImageFileUrl())),
                        signoffEvidenceHash));
        if (!evidenceMatched) {
            throw blocker(application, MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                    "verified manager electronic signoff evidence is required",
                    "complete password verification and submit the resulting signature evidence hash");
        }
    }

    private MesProductionReleaseManagerApprovalResult replay(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            Long actorUserId,
            MesProEdhrReleaseApproveReqVO command,
            MesProEdhrReleaseTransactionEventDO existingEvent) {
        JSONObject snapshot = JSON.parseObject(existingEvent.getEventSnapshotJson());
        String storedPayloadHash = snapshot == null ? null : snapshot.getString("managerApprovalPayloadHash");
        String reportSnapshotHash = snapshot == null ? null : snapshot.getString("reportSnapshotHash");
        String incomingPayloadHash = approvalPayloadHash(actorUserId, command, reportSnapshotHash);
        if (!Objects.equals(storedPayloadHash, incomingPayloadHash)) {
            throw blocker(application, MesReleaseFlowBlockerType.IDEMPOTENCY_PAYLOAD_CONFLICT,
                    "manager approval idempotency key was used with a different payload",
                    "reuse the original task, version and signoff payload");
        }
        MesProEdhrReleaseTransactionDO transaction = releaseTransactionMapper.selectById(command.getReleaseTransactionId());
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(application.getBatchExecutionId());
        if (!Objects.equals(application.getApplicationStatus(), MesReleaseFlowStatus.RELEASED)
                || transaction == null
                || !Objects.equals(transaction.getReleaseStatus(), MesProEdhrReleaseServiceImpl.STATUS_RELEASED)
                || batch == null) {
            throw blocker(application, MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                    "stored manager approval receipt does not match released state",
                    "repair the atomic release transaction before replaying the receipt");
        }
        return new MesProductionReleaseManagerApprovalResult()
                .setBatchExecution(batch)
                .setReleaseTransaction(transaction)
                .setApplicationStatus(application.getApplicationStatus())
                .setApplicationVersion(application.getVersion())
                .setReplayed(true);
    }

    private void recordEvent(
            MesProEdhrReleaseTransactionDO transaction,
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrWorkTaskDO workTask,
            Long actorUserId,
            MesProEdhrReleaseApproveReqVO command,
            String payloadHash,
            LocalDateTime occurredAt) {
        JSONObject snapshot = new JSONObject(true);
        snapshot.put("releaseTransactionId", transaction.getId());
        snapshot.put("applicationId", application.getId());
        snapshot.put("workTaskId", workTask.getId());
        snapshot.put("expectedVersion", command.getExpectedVersion());
        snapshot.put("fromStatus", MesReleaseFlowStatus.MANAGER_RELEASE_PENDING);
        snapshot.put("toStatus", MesReleaseFlowStatus.RELEASED);
        snapshot.put("actorUserId", actorUserId);
        snapshot.put("opinion", StrUtil.trim(command.getApprovalOpinion()));
        snapshot.put("idempotencyKey", command.getIdempotencyKey());
        snapshot.put("signoffEvidenceHash", command.getSignoffEvidenceHash());
        snapshot.put("reportSnapshotHash", application.getReportSnapshotHash());
        snapshot.put("managerApprovalPayloadHash", payloadHash);
        snapshot.put("occurredAt", occurredAt);
        String snapshotJson = snapshot.toJSONString();
        releaseEventMapper.insert(MesProEdhrReleaseTransactionEventDO.builder()
                .releaseTransactionId(transaction.getId())
                .eventType(EVENT_TYPE_APPROVE)
                .fromStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL)
                .toStatus(MesProEdhrReleaseServiceImpl.STATUS_RELEASED)
                .actorUserId(actorUserId)
                .opinion(StrUtil.trim(command.getApprovalOpinion()))
                .idempotencyKey(command.getIdempotencyKey())
                .signoffEvidenceHash(command.getSignoffEvidenceHash())
                .eventSnapshotJson(snapshotJson)
                .evidenceHash(DigestUtil.sha256Hex(snapshotJson))
                .occurredAt(occurredAt)
                .build());
    }

    private String approvalPayloadHash(
            Long actorUserId,
            MesProEdhrReleaseApproveReqVO command,
            String reportSnapshotHash) {
        return MesReleaseFlowIdempotency.payloadHash(
                String.valueOf(command.getReleaseTransactionId()),
                String.valueOf(command.getWorkTaskId()),
                String.valueOf(command.getExpectedVersion()),
                String.valueOf(actorUserId),
                StrUtil.trim(command.getSignoffEvidenceHash()),
                StrUtil.trim(command.getApprovalOpinion()),
                StrUtil.trim(reportSnapshotHash));
    }

    private boolean containsCandidate(String candidateSnapshot, Long actorUserId) {
        if (StrUtil.isBlank(candidateSnapshot)) {
            return false;
        }
        for (String token : candidateSnapshot.split(",")) {
            if (Objects.equals(StrUtil.trim(token), String.valueOf(actorUserId))) {
                return true;
            }
        }
        return false;
    }

    private boolean isProcessable(String status) {
        return Objects.equals(status, MesProEdhrWorkTaskStatus.TODO)
                || Objects.equals(status, MesProEdhrWorkTaskStatus.DOING)
                || Objects.equals(status, MesProEdhrWorkTaskStatus.OVERDUE);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private MesReleaseFlowBlockerException reportChanged(
            MesProcessPoolActiveOrderReleaseApplicationDO application, String reason) {
        return blocker(application, MesReleaseFlowBlockerType.REPORT_SNAPSHOT_CHANGED,
                reason, "restore and verify all four frozen report evidences before final release");
    }

    private MesReleaseFlowBlockerException versionConflict(
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        return blocker(application, MesReleaseFlowBlockerType.STATE_VERSION_CONFLICT,
                "release transaction or application version changed",
                "refresh the authoritative manager release receipt before retrying");
    }

    private MesReleaseFlowBlockerException blocker(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesReleaseFlowBlockerType type,
            String reason,
            String suggestion) {
        return new MesReleaseFlowBlockerException(reason, new MesReleaseFlowFailureRespVO()
                .setStage(MesReleaseFlowStage.SP_4)
                .setCurrentStatus(application == null ? null : application.getApplicationStatus())
                .setBlockers(List.of(new MesReleaseFlowBlocker()
                        .setBlockerType(type)
                        .setObjectType("RELEASE_APPLICATION")
                        .setObjectId(application == null ? null : String.valueOf(application.getId()))
                        .setReason(reason)
                        .setSuggestion(suggestion))));
    }
}
