package cn.iocoder.yudao.module.mes.productionrelease.core;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class MesReleaseFlowLifecycleServiceImpl {

    private static final String LEGACY_BLOCKED = "BLOCKED";
    private static final String LEGACY_PENDING_RELEASE_APPROVAL = "PENDING_RELEASE_APPROVAL";

    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    private final MesReleaseFlowAuditRecorder auditRecorder;

    public MesReleaseFlowLifecycleServiceImpl(
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper,
            MesReleaseFlowAuditRecorder auditRecorder) {
        this.applicationMapper = Objects.requireNonNull(applicationMapper, "applicationMapper must not be null");
        this.auditRecorder = Objects.requireNonNull(auditRecorder, "auditRecorder must not be null");
    }

    public MesProcessPoolActiveOrderReleaseApplicationDO transition(MesReleaseFlowTransitionCommand command) {
        validateCommand(command);
        MesProcessPoolActiveOrderReleaseApplicationDO application = lockAndValidate(
                command.getApplicationId(), command.getExpectedVersion(), command.getExpectedStatus(),
                command.getStage());
        if (!MesReleaseFlowStatus.isAllowedTransition(command.getExpectedStatus(), command.getTargetStatus())) {
            throw blocker(command.getStage(), application, MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION,
                    "release status transition is not defined",
                    "refresh the application and use an action defined for the current stage");
        }

        int updated = applicationMapper.compareAndSetStatus(
                command.getApplicationId(), command.getExpectedVersion(), command.getExpectedStatus(),
                command.getTargetStatus());
        if (updated != 1) {
            throw blocker(command.getStage(), application, MesReleaseFlowBlockerType.STATE_VERSION_CONFLICT,
                    "release application status or version changed",
                    "refresh the application before retrying");
        }

        int nextVersion = command.getExpectedVersion() + 1;
        application.setApplicationStatus(command.getTargetStatus()).setVersion(nextVersion);
        auditRecorder.record(new MesReleaseFlowAuditCommand()
                .setEventType(command.getAuditEventType())
                .setStage(command.getStage())
                .setRequestId(command.getRequestId())
                .setIdempotencyKey(command.getIdempotencyKey())
                .setTenantId(application.getTenantId())
                .setApplicationId(application.getId())
                .setWorkTaskId(command.getWorkTaskId())
                .setBatchExecutionId(application.getBatchExecutionId())
                .setReleaseTransactionId(command.getReleaseTransactionId())
                .setFromStatus(command.getExpectedStatus())
                .setToStatus(command.getTargetStatus())
                .setVersion(nextVersion)
                .setActorUserId(command.getActorUserId())
                .setOccurredAt(LocalDateTime.now())
                .setSourceSnapshotHash(application.getSourceSnapshotHash())
                .setResultStatus("SUCCESS"));
        return application;
    }

    public MesProcessPoolActiveOrderReleaseApplicationDO lockAndValidate(
            Long applicationId, Integer expectedVersion, String expectedStatus, String stage) {
        Objects.requireNonNull(applicationId, "applicationId must not be null");
        Objects.requireNonNull(expectedVersion, "expectedVersion must not be null");
        requireText(expectedStatus, "expectedStatus");
        requireText(stage, "stage");

        MesProcessPoolActiveOrderReleaseApplicationDO application =
                applicationMapper.selectByIdForUpdate(applicationId);
        if (application == null) {
            throw blocker(stage, applicationId, null, MesReleaseFlowBlockerType.STATE_VERSION_CONFLICT,
                    "release application does not exist",
                    "refresh the release application list");
        }
        if (LEGACY_BLOCKED.equals(application.getApplicationStatus())
                || LEGACY_PENDING_RELEASE_APPROVAL.equals(application.getApplicationStatus())
                || !MesReleaseFlowStatus.isPersistentStatus(application.getApplicationStatus())) {
            throw blocker(stage, application,
                    MesReleaseFlowBlockerType.LEGACY_RELEASE_APPLICATION_MIGRATION_REQUIRED,
                    "release application uses a legacy or unknown status",
                    "complete the approved release application migration before processing");
        }
        if (!Objects.equals(expectedVersion, application.getVersion())
                || !Objects.equals(expectedStatus, application.getApplicationStatus())) {
            throw blocker(stage, application, MesReleaseFlowBlockerType.STATE_VERSION_CONFLICT,
                    "release application status or version changed",
                    "refresh the application before retrying");
        }
        return application;
    }

    private void validateCommand(MesReleaseFlowTransitionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.getApplicationId(), "applicationId must not be null");
        Objects.requireNonNull(command.getExpectedVersion(), "expectedVersion must not be null");
        Objects.requireNonNull(command.getActorUserId(), "actorUserId must not be null");
        requireText(command.getExpectedStatus(), "expectedStatus");
        requireText(command.getTargetStatus(), "targetStatus");
        requireText(command.getStage(), "stage");
        requireText(command.getAuditEventType(), "auditEventType");
        requireText(command.getRequestId(), "requestId");
        MesReleaseFlowIdempotency.requireKey(command.getIdempotencyKey());
        if (!MesReleaseFlowStatus.isPersistentStatus(command.getExpectedStatus())
                || !MesReleaseFlowStatus.isPersistentStatus(command.getTargetStatus())) {
            throw new IllegalArgumentException("expectedStatus and targetStatus must use the release status contract");
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private MesReleaseFlowBlockerException blocker(
            String stage, MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesReleaseFlowBlockerType blockerType, String reason, String suggestion) {
        return blocker(stage, application.getId(), application.getApplicationStatus(), blockerType, reason, suggestion);
    }

    private MesReleaseFlowBlockerException blocker(
            String stage, Long applicationId, String currentStatus,
            MesReleaseFlowBlockerType blockerType, String reason, String suggestion) {
        MesReleaseFlowFailureRespVO failure = new MesReleaseFlowFailureRespVO()
                .setStage(stage)
                .setCurrentStatus(currentStatus)
                .setBlockers(List.of(new MesReleaseFlowBlocker()
                        .setBlockerType(blockerType)
                        .setObjectType("RELEASE_APPLICATION")
                        .setObjectId(String.valueOf(applicationId))
                        .setReason(reason)
                        .setSuggestion(suggestion)));
        return new MesReleaseFlowBlockerException("production release precondition failed", failure);
    }
}
