package cn.iocoder.yudao.module.mes.service.pro.productionrelease.report;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditCommand;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditRecorder;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlocker;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowFailureRespVO;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowIdempotency;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStage;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class MesProductionReleaseReportServiceImpl implements MesProductionReleaseReportService {

    public static final String BUSINESS_SCOPE_RELEASE_REPORT_NODE = "RELEASE_REPORT_NODE";
    private static final String TASK_TYPE_FILL = "FILL";
    private static final String NODE_STATUS_COMPLETED = "COMPLETED";
    private static final int BATCH_TASK_STATUS_APPROVED = 40;
    private static final Set<String> REQUIRED_NODE_TYPES = Set.of(
            "INCOMING_INSPECTION_REPORT",
            "STERILIZATION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_RECORD");

    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    private final MesProEdhrWorkTaskMapper workTaskMapper;
    private final MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    private final MesProductionReleaseReportNodePort reportNodePort;
    private final MesProductionReleaseManagerStageInitializer managerStageInitializer;
    private final MesReleaseFlowAuditRecorder auditRecorder;
    private final Clock clock;

    @Autowired
    public MesProductionReleaseReportServiceImpl(
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper,
            MesProEdhrWorkTaskMapper workTaskMapper,
            MesProEdhrBatchExecutionTaskMapper batchTaskMapper,
            MesProductionReleaseReportNodePort reportNodePort,
            ObjectProvider<MesProductionReleaseManagerStageInitializer> managerStageInitializerProvider,
            MesReleaseFlowAuditRecorder auditRecorder) {
        this(applicationMapper, workTaskMapper, batchTaskMapper, reportNodePort,
                managerStageInitializerProvider.getIfUnique(), auditRecorder, Clock.systemUTC());
    }

    MesProductionReleaseReportServiceImpl(
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper,
            MesProEdhrWorkTaskMapper workTaskMapper,
            MesProEdhrBatchExecutionTaskMapper batchTaskMapper,
            MesProductionReleaseReportNodePort reportNodePort,
            MesProductionReleaseManagerStageInitializer managerStageInitializer,
            MesReleaseFlowAuditRecorder auditRecorder,
            Clock clock) {
        this.applicationMapper = applicationMapper;
        this.workTaskMapper = workTaskMapper;
        this.batchTaskMapper = batchTaskMapper;
        this.reportNodePort = reportNodePort;
        this.managerStageInitializer = managerStageInitializer;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProductionReleaseReportAttachmentPrepareResult prepareAttachment(
            Long actorUserId, MesProductionReleaseReportAttachmentPrepareCommand command) {
        if (actorUserId == null || command == null || command.getBatchTaskId() == null
                || command.getExpectedVersion() == null || command.getExpectedVersion() < 0
                || StrUtil.isBlank(command.getFileName()) || StrUtil.isBlank(command.getContentType())
                || command.getContent() == null || command.getContent().length == 0) {
            throw blocker(null, MesReleaseFlowBlockerType.REPORT_ATTACHMENT_REQUIRED,
                    "task, expectedVersion and a non-empty upload file are required",
                    "select a file and submit the current report task version");
        }
        MesReleaseFlowIdempotency.requireKey(command.getIdempotencyKey());
        MesProEdhrWorkTaskDO workTask = requireReleaseReportWorkTask(command.getBatchTaskId());
        MesProcessPoolActiveOrderReleaseApplicationDO application =
                requireApplicationForUpdate(workTask.getBatchExecutionId());
        MesProEdhrBatchExecutionTaskDO batchTask = requireBatchTaskForUpdate(command.getBatchTaskId());
        requireProcessable(application, workTask, batchTask, actorUserId, command.getExpectedVersion());
        MesProductionReleaseReportAttachmentPrepareResult result = reportNodePort.prepareAttachment(
                new MesProductionReleaseReportAttachmentPreparePortCommand()
                        .setActorUserId(actorUserId)
                        .setBatchTaskId(batchTask.getId())
                        .setIdempotencyKey(command.getIdempotencyKey())
                        .setFileName(StrUtil.trim(command.getFileName()))
                        .setContentType(StrUtil.trim(command.getContentType()))
                        .setContent(command.getContent()));
        if (result == null || result.getFileId() == null || StrUtil.isBlank(result.getSha256())
                || !result.getSha256().matches("[0-9a-f]{64}")
                || StrUtil.isBlank(result.getStorageRetentionHash())) {
            throw blocker(application, MesReleaseFlowBlockerType.REPORT_ATTACHMENT_REQUIRED,
                    "prepared upload did not return complete storage and hash evidence",
                    "retry the upload after verifying the configured file storage service");
        }
        return result.setVersion(application.getVersion());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProductionReleaseReportNodeCompleteResult complete(
            Long actorUserId, MesProductionReleaseReportNodeCompleteCommand command) {
        requireCommand(actorUserId, command);
        String idempotencyKey = MesReleaseFlowIdempotency.requireKey(command.getIdempotencyKey());
        MesProEdhrWorkTaskDO workTask = requireReleaseReportWorkTask(command.getBatchTaskId());
        MesProcessPoolActiveOrderReleaseApplicationDO application =
                requireApplicationForUpdate(workTask.getBatchExecutionId());
        MesProEdhrBatchExecutionTaskDO batchTask = requireBatchTaskForUpdate(command.getBatchTaskId());
        String payloadHash = payloadHash(command, batchTask.getNodeType());

        MesProductionReleaseReportNodeCompleteResult replay = replayIfCompleted(
                batchTask, idempotencyKey, payloadHash);
        if (replay != null) {
            return replay;
        }
        requireProcessable(application, workTask, batchTask, actorUserId, command.getExpectedVersion());

        MesProductionReleaseReportNodeEvidence currentEvidence = reportNodePort.complete(
                new MesProductionReleaseReportNodePortCommand()
                        .setApplicationId(application.getId())
                        .setActorUserId(actorUserId)
                        .setWorkTaskId(workTask.getId())
                        .setBatchExecutionId(application.getBatchExecutionId())
                        .setBatchTaskId(batchTask.getId())
                        .setNodeType(batchTask.getNodeType())
                        .setSterilizationBatchNo(command.getSterilizationBatchNo())
                        .setAttachments(List.copyOf(command.getAttachments())));
        requireEvidence(application, batchTask, currentEvidence);
        if (workTaskMapper.completeReleaseReportTask(workTask.getId(), now()) != 1) {
            throw blocker(application, MesReleaseFlowBlockerType.REPORT_NODE_NOT_PROCESSABLE,
                    "report work task was completed concurrently", "refresh the report task and do not resubmit");
        }

        List<MesProductionReleaseReportNodeEvidence> completedEvidences =
                collectCompletedEvidences(application, currentEvidence);
        String reportSnapshotHash = snapshotHash(application, completedEvidences);
        MesProductionReleaseReportNodeCompleteResult result;
        if (completedEvidences.size() < REQUIRED_NODE_TYPES.size()) {
            if (applicationMapper.advanceReportVersion(application.getId(), application.getVersion()) != 1) {
                throw versionConflict(application);
            }
            result = buildResult(application, workTask, currentEvidence,
                    MesReleaseFlowStatus.REPORT_UPLOAD_PENDING, reportSnapshotHash,
                    null, null, application.getVersion() + 1);
        } else {
            MesProductionReleaseManagerStageInitializer initializer = requireManagerStageInitializer(application);
            MesProductionReleaseManagerStageInitializationResult managerStage =
                    initializer.initializeManagerReleaseStage(
                            new MesProductionReleaseManagerStageInitializationCommand()
                                    .setApplicationId(application.getId())
                                    .setBatchExecutionId(application.getBatchExecutionId())
                                    .setReportSnapshotHash(reportSnapshotHash)
                                    .setReportEvidences(completedEvidences)
                                    .setExpectedApplicationVersion(application.getVersion()));
            requireManagerStageResult(application, managerStage);
            if (applicationMapper.handoffReportsToManager(
                    application.getId(), application.getVersion(), reportSnapshotHash,
                    managerStage.getReleaseTransactionId(), managerStage.getManagerReleaseWorkTaskId(),
                    managerStage.getManagerCandidateSnapshotHash()) != 1) {
                throw versionConflict(application);
            }
            result = buildResult(application, workTask, currentEvidence,
                    MesReleaseFlowStatus.MANAGER_RELEASE_PENDING, reportSnapshotHash,
                    managerStage.getReleaseTransactionId(), managerStage.getManagerReleaseWorkTaskId(),
                    application.getVersion() + 1);
        }
        String completionPayload = completionPayload(currentEvidence, idempotencyKey, payloadHash, result);
        if (batchTaskMapper.updateReleaseReportCompletionPayload(batchTask.getId(), completionPayload) != 1) {
            throw blocker(application, MesReleaseFlowBlockerType.REPORT_NODE_NOT_PROCESSABLE,
                    "report completion evidence could not be locked", "retry after checking the report task state");
        }
        recordAudit(application, workTask, result, idempotencyKey, actorUserId);
        return result;
    }

    private void requireCommand(Long actorUserId, MesProductionReleaseReportNodeCompleteCommand command) {
        if (actorUserId == null || command == null || command.getBatchTaskId() == null
                || command.getExpectedVersion() == null || command.getExpectedVersion() < 0
                || command.getAttachments() == null || command.getAttachments().isEmpty()) {
            throw blocker(null, MesReleaseFlowBlockerType.REPORT_ATTACHMENT_REQUIRED,
                    "task, expectedVersion and at least one attachment are required",
                    "provide the current report task, application version and uploaded attachment evidence");
        }
    }

    private MesProEdhrWorkTaskDO requireReleaseReportWorkTask(Long batchTaskId) {
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectReleaseReportByBatchTaskId(batchTaskId);
        if (workTask == null || !Objects.equals(TASK_TYPE_FILL, workTask.getTaskType())
                || !Objects.equals(BUSINESS_SCOPE_RELEASE_REPORT_NODE, workTask.getBusinessScopeType())
                || !Objects.equals(batchTaskId, workTask.getBusinessScopeId())) {
            throw blocker(null, MesReleaseFlowBlockerType.REPORT_NODE_NOT_PROCESSABLE,
                    "target task is not a production release report node",
                    "use a frozen production release report task");
        }
        return workTask;
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO requireApplicationForUpdate(Long batchExecutionId) {
        MesProcessPoolActiveOrderReleaseApplicationDO application =
                applicationMapper.selectByBatchExecutionIdForUpdate(batchExecutionId);
        if (application == null) {
            throw blocker(null, MesReleaseFlowBlockerType.REPORT_NODE_NOT_PROCESSABLE,
                    "release application for the report batch is missing",
                    "complete PQC approval before uploading release reports");
        }
        return application;
    }

    private MesProEdhrBatchExecutionTaskDO requireBatchTaskForUpdate(Long batchTaskId) {
        MesProEdhrBatchExecutionTaskDO task = batchTaskMapper.selectByIdForUpdate(batchTaskId);
        if (task == null || !REQUIRED_NODE_TYPES.contains(task.getNodeType())) {
            throw blocker(null, MesReleaseFlowBlockerType.REPORT_NODE_NOT_PROCESSABLE,
                    "report node type is not one of the four frozen release reports",
                    "use the report task returned by the candidate workbench");
        }
        return task;
    }

    private void requireProcessable(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrWorkTaskDO workTask,
            MesProEdhrBatchExecutionTaskDO batchTask,
            Long actorUserId,
            Integer expectedVersion) {
        if (!Objects.equals(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING, application.getApplicationStatus())) {
            throw blocker(application, MesReleaseFlowBlockerType.REPORT_NODE_NOT_PROCESSABLE,
                    "release application is not waiting for report uploads",
                    "refresh the authoritative release application receipt");
        }
        if (!Objects.equals(expectedVersion, application.getVersion())) {
            throw versionConflict(application);
        }
        if (!Objects.equals(application.getBatchExecutionId(), workTask.getBatchExecutionId())
                || !Objects.equals(batchTask.getBatchExecutionId(), application.getBatchExecutionId())
                || !isProcessableWorkTask(workTask)
                || !isAssignedOrCandidate(workTask, actorUserId)) {
            throw blocker(application, MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE,
                    "current user is not an active frozen candidate for this report task",
                    "use the assigned report owner account and refresh the candidate task list");
        }
        if (Objects.equals(batchTask.getStatus(), BATCH_TASK_STATUS_APPROVED)) {
            throw blocker(application, MesReleaseFlowBlockerType.REPORT_ATTACHMENT_LOCKED,
                    "completed report evidence is locked", "do not overwrite a completed release report");
        }
    }

    private boolean isAssignedOrCandidate(MesProEdhrWorkTaskDO workTask, Long actorUserId) {
        if (Objects.equals(workTask.getAssigneeUserId(), actorUserId)) {
            return true;
        }
        if (StrUtil.isBlank(workTask.getCandidateUserSnapshot())) {
            return false;
        }
        for (String token : workTask.getCandidateUserSnapshot().split(",")) {
            if (Objects.equals(String.valueOf(actorUserId), StrUtil.trim(token))) {
                return true;
            }
        }
        return false;
    }

    private boolean isProcessableWorkTask(MesProEdhrWorkTaskDO workTask) {
        return Objects.equals(MesProEdhrWorkTaskStatus.TODO, workTask.getStatus())
                || Objects.equals(MesProEdhrWorkTaskStatus.DOING, workTask.getStatus())
                || Objects.equals(MesProEdhrWorkTaskStatus.OVERDUE, workTask.getStatus());
    }

    private void requireEvidence(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrBatchExecutionTaskDO batchTask,
            MesProductionReleaseReportNodeEvidence evidence) {
        if (evidence == null
                || !Objects.equals(application.getBatchExecutionId(), evidence.getBatchExecutionId())
                || !Objects.equals(batchTask.getId(), evidence.getBatchTaskId())
                || !Objects.equals(batchTask.getNodeType(), evidence.getNodeType())
                || (Objects.equals("STERILIZATION_REPORT", evidence.getNodeType())
                && StrUtil.isBlank(evidence.getSterilizationBatchNo()))
                || !Objects.equals(1, evidence.getActiveAttachmentVersion())
                || evidence.getAttachmentIds() == null || evidence.getAttachmentIds().isEmpty()
                || evidence.getAttachmentHashes() == null
                || evidence.getAttachmentHashes().size() != evidence.getAttachmentIds().size()
                || evidence.getAttachmentHashes().stream().anyMatch(hash -> hash == null || !hash.matches("[0-9a-f]{64}"))) {
            throw blocker(application, MesReleaseFlowBlockerType.REPORT_ATTACHMENT_REQUIRED,
                    "report attachment evidence is missing or inconsistent",
                    "upload a valid attachment and preserve its SHA-256 hash");
        }
    }

    private List<MesProductionReleaseReportNodeEvidence> collectCompletedEvidences(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProductionReleaseReportNodeEvidence currentEvidence) {
        List<MesProEdhrBatchExecutionTaskDO> reportTasks = batchTaskMapper
                .selectListByBatchExecutionId(application.getBatchExecutionId()).stream()
                .filter(task -> REQUIRED_NODE_TYPES.contains(task.getNodeType()))
                .toList();
        if (reportTasks.size() != REQUIRED_NODE_TYPES.size()
                || reportTasks.stream().map(MesProEdhrBatchExecutionTaskDO::getNodeType).distinct().count()
                != REQUIRED_NODE_TYPES.size()) {
            throw blocker(application, MesReleaseFlowBlockerType.REPORT_NODE_NOT_PROCESSABLE,
                    "release batch must contain exactly four distinct report nodes",
                    "rebuild the release batch from its frozen route before continuing");
        }
        Map<String, MesProductionReleaseReportNodeEvidence> evidences = new LinkedHashMap<>();
        for (MesProEdhrBatchExecutionTaskDO task : reportTasks) {
            if (Objects.equals(task.getId(), currentEvidence.getBatchTaskId())) {
                evidences.put(task.getNodeType(), currentEvidence);
                continue;
            }
            if (!Objects.equals(task.getStatus(), BATCH_TASK_STATUS_APPROVED)) {
                continue;
            }
            MesProductionReleaseReportNodeEvidence evidence =
                    MesProductionReleaseReportNodeEvidence.fromPayloadJson(task.getSpecialPayloadJson());
            requireEvidence(application, task, evidence);
            evidences.put(task.getNodeType(), evidence);
        }
        return evidences.values().stream()
                .sorted(Comparator.comparing(MesProductionReleaseReportNodeEvidence::getNodeType))
                .toList();
    }

    private String snapshotHash(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            List<MesProductionReleaseReportNodeEvidence> evidences) {
        return MesProductionReleaseReportSnapshots.hash(application, evidences);
    }

    private MesProductionReleaseManagerStageInitializer requireManagerStageInitializer(
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        if (managerStageInitializer == null) {
            throw blocker(application, MesReleaseFlowBlockerType.MANAGEMENT_REPRESENTATIVE_ROLE_REQUIRED,
                    "manager release stage initializer is not available",
                    "configure the formal management representative stage before completing the fourth report");
        }
        return managerStageInitializer;
    }

    private void requireManagerStageResult(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProductionReleaseManagerStageInitializationResult result) {
        if (result == null || result.getReleaseTransactionId() == null
                || result.getManagerReleaseWorkTaskId() == null
                || StrUtil.isBlank(result.getManagerCandidateSnapshotHash())) {
            throw blocker(application, MesReleaseFlowBlockerType.MANAGEMENT_REPRESENTATIVE_ROLE_REQUIRED,
                    "manager release stage did not return a transaction, task and frozen candidates",
                    "configure an enabled management representative candidate set");
        }
    }

    private MesProductionReleaseReportNodeCompleteResult buildResult(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrWorkTaskDO workTask,
            MesProductionReleaseReportNodeEvidence evidence,
            String status,
            String reportSnapshotHash,
            Long releaseTransactionId,
            Long managerWorkTaskId,
            Integer version) {
        return new MesProductionReleaseReportNodeCompleteResult()
                .setBatchExecutionId(application.getBatchExecutionId())
                .setBatchTaskId(evidence.getBatchTaskId())
                .setWorkTaskId(workTask.getId())
                .setNodeType(evidence.getNodeType())
                .setNodeStatus(NODE_STATUS_COMPLETED)
                .setActiveAttachmentVersion(evidence.getActiveAttachmentVersion())
                .setAttachmentIds(List.copyOf(evidence.getAttachmentIds()))
                .setAttachmentHashes(List.copyOf(evidence.getAttachmentHashes()))
                .setReportUploadStatus(status)
                .setReportSnapshotHash(reportSnapshotHash)
                .setReleaseTransactionId(releaseTransactionId)
                .setManagerReleaseWorkTaskId(managerWorkTaskId)
                .setVersion(version);
    }

    private String completionPayload(
            MesProductionReleaseReportNodeEvidence evidence,
            String idempotencyKey,
            String payloadHash,
            MesProductionReleaseReportNodeCompleteResult result) {
        JSONObject payload = JSON.parseObject(evidence.toPayloadJson());
        payload.put("releaseReportIdempotencyKey", idempotencyKey);
        payload.put("releaseReportPayloadHash", payloadHash);
        payload.put("releaseReportReceipt", JSON.toJSON(result));
        return payload.toJSONString();
    }

    private MesProductionReleaseReportNodeCompleteResult replayIfCompleted(
            MesProEdhrBatchExecutionTaskDO batchTask,
            String idempotencyKey,
            String payloadHash) {
        if (!Objects.equals(batchTask.getStatus(), BATCH_TASK_STATUS_APPROVED)
                || StrUtil.isBlank(batchTask.getSpecialPayloadJson())) {
            return null;
        }
        JSONObject payload = JSON.parseObject(batchTask.getSpecialPayloadJson());
        String storedKey = payload.getString("releaseReportIdempotencyKey");
        String storedHash = payload.getString("releaseReportPayloadHash");
        JSONObject storedReceipt = payload.getJSONObject("releaseReportReceipt");
        if (Objects.equals(storedKey, idempotencyKey) && Objects.equals(storedHash, payloadHash)
                && storedReceipt != null) {
            return storedReceipt.toJavaObject(MesProductionReleaseReportNodeCompleteResult.class);
        }
        if (Objects.equals(storedKey, idempotencyKey)) {
            throw blocker(null, MesReleaseFlowBlockerType.IDEMPOTENCY_PAYLOAD_CONFLICT,
                    "idempotency key was already used with different report evidence",
                    "reuse the original payload or submit with the authoritative task state");
        }
        throw blocker(null, MesReleaseFlowBlockerType.REPORT_ATTACHMENT_LOCKED,
                "completed report evidence cannot be overwritten",
                "use the original completion receipt and do not resubmit with another key");
    }

    private String payloadHash(MesProductionReleaseReportNodeCompleteCommand command, String nodeType) {
        String attachments = command.getAttachments().stream()
                .map(attachment -> String.valueOf(attachment.getFileId()) + ":" + StrUtil.trim(attachment.getSha256()))
                .sorted()
                .collect(java.util.stream.Collectors.joining("|"));
        return MesReleaseFlowIdempotency.payloadHash(
                String.valueOf(command.getBatchTaskId()), nodeType,
                StrUtil.trim(command.getSterilizationBatchNo()), attachments);
    }

    private void recordAudit(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrWorkTaskDO workTask,
            MesProductionReleaseReportNodeCompleteResult result,
            String idempotencyKey,
            Long actorUserId) {
        auditRecorder.record(new MesReleaseFlowAuditCommand()
                .setEventType(Objects.equals(result.getReportUploadStatus(), MesReleaseFlowStatus.MANAGER_RELEASE_PENDING)
                        ? "REPORT_UPLOAD_COMPLETED" : "REPORT_NODE_COMPLETED")
                .setStage(MesReleaseFlowStage.SP_3)
                .setRequestId(idempotencyKey)
                .setIdempotencyKey(idempotencyKey)
                .setTenantId(TenantContextHolder.getTenantId())
                .setApplicationId(application.getId())
                .setWorkTaskId(workTask.getId())
                .setBatchExecutionId(application.getBatchExecutionId())
                .setReleaseTransactionId(result.getReleaseTransactionId())
                .setFromStatus(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)
                .setToStatus(result.getReportUploadStatus())
                .setVersion(result.getVersion())
                .setActorUserId(actorUserId)
                .setOccurredAt(now())
                .setSourceSnapshotHash(result.getReportSnapshotHash())
                .setResultStatus("SUCCESS"));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private MesReleaseFlowBlockerException versionConflict(
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        return blocker(application, MesReleaseFlowBlockerType.STATE_VERSION_CONFLICT,
                "release application version changed",
                "refresh the authoritative application receipt before retrying");
    }

    private MesReleaseFlowBlockerException blocker(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesReleaseFlowBlockerType blockerType,
            String reason,
            String suggestion) {
        return new MesReleaseFlowBlockerException(reason, new MesReleaseFlowFailureRespVO()
                .setStage(MesReleaseFlowStage.SP_3)
                .setCurrentStatus(application == null ? null : application.getApplicationStatus())
                .setBlockers(List.of(new MesReleaseFlowBlocker()
                        .setBlockerType(blockerType)
                        .setObjectType("RELEASE_REPORT_NODE")
                        .setObjectId(application == null ? null : String.valueOf(application.getId()))
                        .setReason(reason)
                        .setSuggestion(suggestion))));
    }
}
