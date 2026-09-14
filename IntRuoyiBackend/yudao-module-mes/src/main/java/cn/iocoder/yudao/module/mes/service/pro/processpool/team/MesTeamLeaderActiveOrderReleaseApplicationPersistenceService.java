package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

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
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStage;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCandidates;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class MesTeamLeaderActiveOrderReleaseApplicationPersistenceService {

    private static final String TASK_TYPE_PQC_RELEASE = "PQC_PRODUCTION_RELEASE";
    private static final String BUSINESS_SCOPE_RELEASE_APPLICATION = "RELEASE_APPLICATION";

    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    private final MesProEdhrWorkTaskMapper workTaskMapper;
    private final MesReleaseFlowAuditRecorder auditRecorder;

    public MesTeamLeaderActiveOrderReleaseApplicationPersistenceService(
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper,
            MesProEdhrWorkTaskMapper workTaskMapper,
            MesReleaseFlowAuditRecorder auditRecorder) {
        this.applicationMapper = applicationMapper;
        this.workTaskMapper = workTaskMapper;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public MesTeamLeaderActiveOrderReleaseApplicationResult persistPending(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProductionReleaseRoleCandidates candidates) {
        validateNewApplication(application, candidates);
        InsertOutcome outcome = insertOrReadStrict(application);
        if (!outcome.created()) {
            return toResult(outcome.application());
        }
        MesProEdhrWorkTaskDO task = buildPqcTask(application, candidates);
        if (workTaskMapper.insert(task) != 1 || task.getId() == null) {
            throw new IllegalStateException("PQC production release work task insert failed");
        }
        MesProcessPoolActiveOrderReleaseApplicationDO binding =
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(application.getId())
                        .setPqcReleaseWorkTaskId(task.getId());
        if (applicationMapper.updateById(binding) != 1) {
            throw new IllegalStateException("PQC production release work task binding failed");
        }
        application.setPqcReleaseWorkTaskId(task.getId());
        auditRecorder.record(new MesReleaseFlowAuditCommand()
                .setEventType(MesReleaseFlowAuditEventType.PQC_PRODUCTION_RELEASE_APPLIED)
                .setStage(MesReleaseFlowStage.SP_1)
                .setRequestId(application.getRequestIdempotencyKey())
                .setIdempotencyKey(application.getRequestIdempotencyKey())
                .setTenantId(TenantContextHolder.getTenantId())
                .setApplicationId(application.getId())
                .setWorkTaskId(task.getId())
                .setToStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                .setVersion(application.getVersion())
                .setActorUserId(application.getAppliedBy())
                .setOccurredAt(application.getAppliedAt())
                .setSourceSnapshotHash(application.getSourceSnapshotHash())
                .setResultStatus("SUCCESS"));
        return toResult(application);
    }

    public MesTeamLeaderActiveOrderReleaseApplicationResult toResult(
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        if (application == null || application.getId() == null) {
            throw new IllegalArgumentException("release application is required");
        }
        return new MesTeamLeaderActiveOrderReleaseApplicationResult()
                .setApplicationId(application.getId())
                .setActiveOrderId(application.getActiveOrderId())
                .setWorkOrderId(application.getWorkOrderId())
                .setWorkOrderCode(application.getWorkOrderCode())
                .setBatchCode(application.getBatchCode())
                .setRouteId(application.getRouteId())
                .setRouteVersionId(application.getRouteVersionId())
                .setPqcReleaseWorkTaskId(application.getPqcReleaseWorkTaskId())
                .setStatus(application.getApplicationStatus())
                .setSourceSnapshotHash(application.getSourceSnapshotHash())
                .setVersion(application.getVersion())
                .setAppliedAt(application.getAppliedAt());
    }

    private void validateNewApplication(MesProcessPoolActiveOrderReleaseApplicationDO application,
                                        MesProductionReleaseRoleCandidates candidates) {
        if (application == null || application.getActiveOrderId() == null || application.getWorkOrderId() == null
                || application.getRouteId() == null || application.getRouteVersionId() == null
                || StrUtil.isBlank(application.getBatchCode())
                || StrUtil.isBlank(application.getRequestIdempotencyKey())
                || StrUtil.isBlank(application.getBusinessIdempotencyKey())
                || StrUtil.isBlank(application.getSourceSnapshotHash())
                || !MesReleaseFlowStatus.PQC_RELEASE_PENDING.equals(application.getApplicationStatus())
                || !Objects.equals(1, application.getVersion())
                || application.getAppliedBy() == null || application.getAppliedAt() == null
                || application.getBatchExecutionId() != null || application.getReleaseTransactionId() != null
                || application.getReleaseApprovalWorkTaskId() != null) {
            throw new IllegalArgumentException("complete SP-1 release application identity is required");
        }
        if (candidates == null || candidates.roleId() == null || StrUtil.isBlank(candidates.roleCode())
                || candidates.candidateUserIds().isEmpty() || StrUtil.isBlank(candidates.candidateSnapshotHash())) {
            throw new IllegalArgumentException("frozen PQC role candidates are required");
        }
    }

    private InsertOutcome insertOrReadStrict(MesProcessPoolActiveOrderReleaseApplicationDO application) {
        try {
            if (applicationMapper.insert(application) != 1 || application.getId() == null) {
                throw new IllegalStateException("production release application insert failed");
            }
            return new InsertOutcome(application, true);
        } catch (DuplicateKeyException duplicate) {
            MesProcessPoolActiveOrderReleaseApplicationDO requestExisting =
                    applicationMapper.selectByRequestIdempotencyKey(
                            application.getActiveOrderId(), application.getRequestIdempotencyKey());
            if (requestExisting != null) {
                if (sameRequestIdentity(requestExisting, application)) {
                    return new InsertOutcome(requestExisting, false);
                }
                throw payloadConflict(requestExisting);
            }
            MesProcessPoolActiveOrderReleaseApplicationDO businessExisting =
                    applicationMapper.selectByBusinessIdempotencyKey(
                            application.getActiveOrderId(), application.getBusinessIdempotencyKey());
            if (sameBusinessIdentity(businessExisting, application)) {
                return new InsertOutcome(businessExisting, false);
            }
            throw duplicate;
        }
    }

    private MesProEdhrWorkTaskDO buildPqcTask(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProductionReleaseRoleCandidates candidates) {
        String candidateSnapshot = candidates.candidateUserIds().stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
        return new MesProEdhrWorkTaskDO()
                .setTaskCode("PQC-RELEASE-" + application.getId())
                .setTaskType(TASK_TYPE_PQC_RELEASE)
                .setBusinessScopeType(BUSINESS_SCOPE_RELEASE_APPLICATION)
                .setBusinessScopeId(application.getId())
                .setWorkOrderId(application.getWorkOrderId())
                .setWorkOrderCode(application.getWorkOrderCode())
                .setBatchCode(application.getBatchCode())
                .setRouteId(application.getRouteId())
                .setProcessName("PQC生产放行")
                .setAssigneeUserId(candidates.candidateUserIds().get(0))
                .setCandidateSourceType("ROLE")
                .setCandidateSourceId(candidates.roleId())
                .setCandidateUserSnapshot(candidateSnapshot)
                .setSourceUserId(application.getAppliedBy())
                .setResponsibilitySourceType("ROLE")
                .setResponsibilitySourceKey(candidates.roleCode())
                .setResponsibilitySourceVersion(candidates.candidateSnapshotHash())
                .setResponsibilitySourceDigest(candidates.candidateSnapshotHash())
                .setOwnershipLocked(true)
                .setStatus(MesProEdhrWorkTaskStatus.TODO)
                .setActionUrl("/mes/production-release/pqc?applicationId=" + application.getId())
                .setRemark("PQC生产放行待办");
    }

    private boolean sameRequestIdentity(MesProcessPoolActiveOrderReleaseApplicationDO existing,
                                        MesProcessPoolActiveOrderReleaseApplicationDO incoming) {
        return sameBusinessIdentity(existing, incoming)
                && Objects.equals(existing.getRequestIdempotencyKey(), incoming.getRequestIdempotencyKey())
                && Objects.equals(existing.getSourceSnapshotHash(), incoming.getSourceSnapshotHash());
    }

    private boolean sameBusinessIdentity(MesProcessPoolActiveOrderReleaseApplicationDO existing,
                                         MesProcessPoolActiveOrderReleaseApplicationDO incoming) {
        return existing != null
                && Objects.equals(existing.getActiveOrderId(), incoming.getActiveOrderId())
                && Objects.equals(existing.getWorkOrderId(), incoming.getWorkOrderId())
                && Objects.equals(existing.getBatchCode(), incoming.getBatchCode())
                && Objects.equals(existing.getRouteId(), incoming.getRouteId())
                && Objects.equals(existing.getRouteVersionId(), incoming.getRouteVersionId())
                && Objects.equals(existing.getBusinessIdempotencyKey(), incoming.getBusinessIdempotencyKey());
    }

    private MesReleaseFlowBlockerException payloadConflict(
            MesProcessPoolActiveOrderReleaseApplicationDO existing) {
        MesReleaseFlowBlocker flowBlocker = new MesReleaseFlowBlocker()
                .setBlockerType(MesReleaseFlowBlockerType.IDEMPOTENCY_PAYLOAD_CONFLICT)
                .setObjectType("RELEASE_APPLICATION")
                .setObjectId(String.valueOf(existing.getId()))
                .setReason("the request key is already bound to a different authoritative snapshot")
                .setSuggestion("query the existing receipt and use a new key only for a new command");
        return new MesReleaseFlowBlockerException(flowBlocker.getReason(), new MesReleaseFlowFailureRespVO()
                .setStage(MesReleaseFlowStage.SP_1)
                .setCurrentStatus(existing.getApplicationStatus())
                .setBlockers(List.of(flowBlocker)));
    }

    private record InsertOutcome(MesProcessPoolActiveOrderReleaseApplicationDO application, boolean created) {
    }
}
