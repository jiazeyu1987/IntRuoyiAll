package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationFollowupBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationImpactAuditDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationImpactTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationRelationSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationFollowupBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationImpactAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationImpactTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationRelationSnapshotMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileChangeTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_IMPACT_ASSIGNEE_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_IMPACT_ASSIGNEE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_IMPACT_DECISION_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_IMPACT_DOC_CONTROL_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_IMPACT_REASON_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_IMPACT_REVISION_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_IMPACT_TASK_STATE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_IMPACT_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_IMPACT_VERSION_CONFLICT;

@Service
public class DccRelatedFileImpactAssessmentServiceImpl implements DccRelatedFileImpactAssessmentService {

    private static final String DOC_CONTROL_ROLE = "doc_control";
    private static final String APPROVE_PERMISSION = "dcc:controlled-file:approve";
    private static final String TASK_PENDING = "PENDING";
    private static final String TASK_UNASSIGNED = "UNASSIGNED";
    private static final String TASK_IN_REVIEW = "IN_REVIEW";
    private static final String TASK_COMPLETED = "COMPLETED";
    private static final String DECISION_NONE = "NO_REVISION_REQUIRED";
    private static final String DECISION_REQUIRED = "REVISION_REQUIRED";
    private static final String TRACKING_NOT_APPLICABLE = "NOT_APPLICABLE";
    private static final String TRACKING_NOT_STARTED = "NOT_STARTED";
    private static final String TRACKING_LINKED = "REVISION_LINKED";
    private static final String TRACKING_RESOLVED = "RESOLVED";

    @Resource private DccPublicationFollowupBatchMapper batchMapper;
    @Resource private DccPublicationRelationSnapshotMapper relationMapper;
    @Resource private DccPublicationImpactTaskMapper taskMapper;
    @Resource private DccPublicationImpactAuditMapper auditMapper;
    @Resource private AdminUserApi adminUserApi;
    @Resource private PermissionApi permissionApi;
    @Resource private DccControlledFileMapper controlledFileMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void materializeForPublicationBatch(Long batchId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        DccPublicationFollowupBatchDO batch = batchMapper.selectById(batchId);
        if (batch == null || !Objects.equals(batch.getTenantId(), tenantId)) {
            throw exception(PUBLICATION_IMPACT_TASK_NOT_EXISTS);
        }
        List<DccPublicationRelationSnapshotDO> relations = relationMapper.selectListByBatchId(batchId);
        if (relations == null) {
            throw new IllegalStateException("publication relation snapshots must not be null");
        }
        for (DccPublicationRelationSnapshotDO relation : relations) {
            DccPublicationImpactTaskDO existing = taskMapper.selectByBatchIdAndRelatedMasterId(
                    tenantId, batchId, relation.getRelatedMasterId());
            if (existing != null) {
                requireMatchingMaterializedIdentity(existing, batch, relation);
                continue;
            }
            Long responsibleUserId = relation.getResponsibleUserIdSnapshot();
            boolean activeAssignee = responsibleUserId != null
                    && Integer.valueOf(0).equals(relation.getResponsibleUserStatusSnapshot());
            Long assignee = activeAssignee ? responsibleUserId : null;
            String assigneeName = activeAssignee ? relation.getResponsibleUserNameSnapshot() : null;
            String taskStatus = activeAssignee ? TASK_PENDING : TASK_UNASSIGNED;
            DccPublicationImpactTaskDO task = DccPublicationImpactTaskDO.builder()
                    .batchId(batchId).publicationRelationSnapshotId(relation.getId())
                    .publishedControlledFileId(batch.getPublishedControlledFileId())
                    .relatedMasterId(relation.getRelatedMasterId())
                    .relatedActiveControlledFileId(relation.getRelatedActiveControlledFileId())
                    .relatedFileNumberSnapshot(relation.getRelatedFileNumberSnapshot())
                    .relatedFileNameSnapshot(relation.getRelatedFileNameSnapshot())
                    .relatedVersionNoSnapshot(relation.getRelatedVersionNoSnapshot())
                    .assigneeUserId(assignee).assigneeUserNameSnapshot(assigneeName)
                    .taskStatus(taskStatus).revisionTrackingStatus(TRACKING_NOT_APPLICABLE)
                    .creationToken(UUID.randomUUID().toString()).build();
            task.setTenantId(tenantId);
            taskMapper.insertOrKeepExisting(task);
            DccPublicationImpactTaskDO materialized = taskMapper.selectByBatchIdAndRelatedMasterId(
                    tenantId, batchId, relation.getRelatedMasterId());
            if (materialized == null || materialized.getId() == null) {
                throw new IllegalStateException("impact task must exist after idempotent insert");
            }
            requireMatchingMaterializedIdentity(materialized, batch, relation);
            if (!Objects.equals(task.getCreationToken(), materialized.getCreationToken())) {
                continue;
            }
            insertAudit(materialized, "MATERIALIZE", null, null, taskStatus, assignee,
                    null, null, 0, 0);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startTask(Long actorId, Long taskId, Integer expectedVersion) {
        DccPublicationImpactTaskDO task = requireTask(taskId);
        requireAssignee(task, actorId);
        if (!TASK_PENDING.equals(task.getTaskStatus())) {
            throw exception(PUBLICATION_IMPACT_TASK_STATE_INVALID);
        }
        int updated = taskMapper.startTask(TenantContextHolder.getRequiredTenantId(), taskId,
                expectedVersion, actorId);
        if (updated != 1) {
            throw exception(PUBLICATION_IMPACT_VERSION_CONFLICT);
        }
        insertAudit(task, "START", actorId, null, TASK_IN_REVIEW, task.getAssigneeUserId(),
                null, null, expectedVersion, expectedVersion + 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitDecision(Long actorId, Long taskId, Integer expectedVersion,
                               String decision, String reason) {
        DccPublicationImpactTaskDO task = requireTask(taskId);
        requireAssignee(task, actorId);
        if (!TASK_IN_REVIEW.equals(task.getTaskStatus())) {
            throw exception(PUBLICATION_IMPACT_TASK_STATE_INVALID);
        }
        if (!DECISION_NONE.equals(decision) && !DECISION_REQUIRED.equals(decision)) {
            throw exception(PUBLICATION_IMPACT_DECISION_INVALID);
        }
        String normalizedReason = StrUtil.trimToNull(reason);
        if (normalizedReason == null) {
            throw exception(PUBLICATION_IMPACT_REASON_REQUIRED);
        }
        String tracking = DECISION_NONE.equals(decision) ? TRACKING_NOT_APPLICABLE : TRACKING_NOT_STARTED;
        int updated = taskMapper.completeDecision(TenantContextHolder.getRequiredTenantId(), taskId,
                expectedVersion, actorId, decision, normalizedReason, tracking);
        if (updated != 1) {
            throw exception(PUBLICATION_IMPACT_VERSION_CONFLICT);
        }
        insertAudit(task, "DECIDE", actorId, normalizedReason, TASK_COMPLETED, task.getAssigneeUserId(),
                decision, null, expectedVersion, expectedVersion + 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reassignTask(Long actorId, Long taskId, Integer expectedVersion,
                             Long newAssigneeId, String reason) {
        requireDocControl(actorId);
        String normalizedReason = requireReason(reason);
        DccPublicationImpactTaskDO task = requireTask(taskId);
        AdminUserRespDTO newAssignee = adminUserApi.getUser(newAssigneeId);
        if (newAssignee == null || newAssignee.getId() == null || !Integer.valueOf(0).equals(newAssignee.getStatus())) {
            throw exception(PUBLICATION_IMPACT_ASSIGNEE_INVALID);
        }
        int updated = taskMapper.reassignTask(TenantContextHolder.getRequiredTenantId(), taskId,
                expectedVersion, newAssigneeId, newAssignee.getNickname(), normalizedReason);
        if (updated != 1) {
            throw exception(PUBLICATION_IMPACT_VERSION_CONFLICT);
        }
        insertAudit(task, "REASSIGN", actorId, normalizedReason, TASK_PENDING, newAssigneeId,
                null, null, expectedVersion, expectedVersion + 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reopenTask(Long actorId, Long taskId, Integer expectedVersion, String reason) {
        requireDocControl(actorId);
        String normalizedReason = requireReason(reason);
        DccPublicationImpactTaskDO task = requireTask(taskId);
        if (!TASK_COMPLETED.equals(task.getTaskStatus())) {
            throw exception(PUBLICATION_IMPACT_TASK_STATE_INVALID);
        }
        int updated = taskMapper.reopenTask(TenantContextHolder.getRequiredTenantId(), taskId,
                expectedVersion, normalizedReason);
        if (updated != 1) {
            throw exception(PUBLICATION_IMPACT_VERSION_CONFLICT);
        }
        insertAudit(task, "REOPEN", actorId, normalizedReason,
                task.getAssigneeUserId() == null ? TASK_UNASSIGNED : TASK_PENDING,
                task.getAssigneeUserId(), task.getDecision(), task.getLinkedRevisionControlledFileId(),
                expectedVersion, expectedVersion + 1);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void linkExistingMajorRevision(Long actorId, Long taskId, Integer expectedVersion,
                                          Long revisionId, String reason) {
        String normalizedReason = requireReason(reason);
        DccPublicationImpactTaskDO task = requireTask(taskId);
        requireAssignee(task, actorId);
        if (!TASK_COMPLETED.equals(task.getTaskStatus()) || !DECISION_REQUIRED.equals(task.getDecision())
                || !TRACKING_NOT_STARTED.equals(task.getRevisionTrackingStatus())) {
            throw exception(PUBLICATION_IMPACT_TASK_STATE_INVALID);
        }
        DccControlledFileDO revision = requireRevision(task, revisionId, actorId);
        int updated = taskMapper.linkRevision(TenantContextHolder.getRequiredTenantId(), taskId,
                expectedVersion, revisionId, revision.getVersionNo(), normalizedReason);
        if (updated != 1) {
            throw exception(PUBLICATION_IMPACT_VERSION_CONFLICT);
        }
        insertAudit(task, "LINK_REVISION", actorId, normalizedReason, TASK_COMPLETED,
                task.getAssigneeUserId(), DECISION_REQUIRED, revisionId,
                expectedVersion, expectedVersion + 1);
    }

    @Override
    public void assertRevisionCreationAllowed(Long actorId, Long taskId, Integer expectedVersion,
                                              Long sourceControlledFileId, String reason) {
        requireReason(reason);
        DccPublicationImpactTaskDO task = requireTask(taskId);
        requireAssignee(task, actorId);
        if (!Objects.equals(task.getRowVersion(), expectedVersion)) {
            throw exception(PUBLICATION_IMPACT_VERSION_CONFLICT);
        }
        if (!TASK_COMPLETED.equals(task.getTaskStatus()) || !DECISION_REQUIRED.equals(task.getDecision())
                || !TRACKING_NOT_STARTED.equals(task.getRevisionTrackingStatus())) {
            throw exception(PUBLICATION_IMPACT_TASK_STATE_INVALID);
        }
        DccControlledFileDO source = controlledFileMapper.selectById(sourceControlledFileId);
        if (source == null || !Objects.equals(source.getMasterId(), task.getRelatedMasterId())
                || !Objects.equals(source.getRequesterId(), actorId)) {
            throw exception(PUBLICATION_IMPACT_REVISION_INVALID);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveLinkedRevisionAfterPublication(DccControlledFileDO publishedRevision) {
        if (publishedRevision == null || !DccControlledFileStatusEnum.ACTIVE.getStatus()
                .equals(publishedRevision.getStatus())) {
            return;
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<DccPublicationImpactTaskDO> tasks = Objects.requireNonNull(
                taskMapper.selectListByLinkedRevisionId(tenantId, publishedRevision.getId()),
                "linked publication impact tasks must not be null");
        for (DccPublicationImpactTaskDO task : tasks) {
            if (!Objects.equals(task.getRelatedMasterId(), publishedRevision.getMasterId())) {
                throw new IllegalStateException("Linked impact task master does not match published revision");
            }
            int updated = taskMapper.resolveRevision(tenantId, task.getId(), task.getRowVersion(),
                    publishedRevision.getId());
            if (updated != 1) {
                if (isLegitimateResolveRace(tenantId, task, publishedRevision)) {
                    continue;
                }
                throw exception(PUBLICATION_IMPACT_VERSION_CONFLICT);
            }
            insertAudit(task, "RESOLVE_REVISION", null, "关联大版本已正式发布", task.getTaskStatus(),
                    task.getAssigneeUserId(), task.getDecision(), publishedRevision.getId(),
                    task.getRowVersion(), task.getRowVersion() + 1);
        }
    }

    private boolean isLegitimateResolveRace(Long tenantId, DccPublicationImpactTaskDO selected,
                                            DccControlledFileDO publishedRevision) {
        DccPublicationImpactTaskDO current = taskMapper.selectByIdAndTenantForUpdate(tenantId, selected.getId());
        if (current == null || !Objects.equals(current.getRelatedMasterId(), publishedRevision.getMasterId())) {
            return false;
        }
        if (current.getRowVersion() == null || selected.getRowVersion() == null
                || current.getRowVersion() <= selected.getRowVersion()) {
            return false;
        }
        if (!Objects.equals(current.getLinkedRevisionControlledFileId(), publishedRevision.getId())) {
            return current.getLinkedRevisionControlledFileId() != null
                    || !TRACKING_LINKED.equals(current.getRevisionTrackingStatus());
        }
        return TRACKING_RESOLVED.equals(current.getRevisionTrackingStatus());
    }

    private DccPublicationImpactTaskDO requireTask(Long taskId) {
        DccPublicationImpactTaskDO task = taskMapper.selectByIdAndTenant(
                TenantContextHolder.getRequiredTenantId(), taskId);
        if (task == null) {
            throw exception(PUBLICATION_IMPACT_TASK_NOT_EXISTS);
        }
        return task;
    }

    private void requireAssignee(DccPublicationImpactTaskDO task, Long actorId) {
        if (!Objects.equals(task.getAssigneeUserId(), actorId)) {
            throw exception(PUBLICATION_IMPACT_ASSIGNEE_DENIED);
        }
    }

    private void requireDocControl(Long actorId) {
        if (!permissionApi.hasAnyRoles(actorId, DOC_CONTROL_ROLE)
                || !permissionApi.hasAnyPermissions(actorId, APPROVE_PERMISSION)) {
            throw exception(PUBLICATION_IMPACT_DOC_CONTROL_DENIED);
        }
    }

    private String requireReason(String reason) {
        String normalized = StrUtil.trimToNull(reason);
        if (normalized == null) {
            throw exception(PUBLICATION_IMPACT_REASON_REQUIRED);
        }
        return normalized;
    }

    private DccControlledFileDO requireRevision(DccPublicationImpactTaskDO task, Long revisionId, Long actorId) {
        DccControlledFileDO revision = controlledFileMapper.selectById(revisionId);
        if (revision == null || !Objects.equals(revision.getMasterId(), task.getRelatedMasterId())
                || !Objects.equals(revision.getRequesterId(), actorId)
                || !DccControlledFileChangeTypeEnum.REVISION.getCode().equals(revision.getChangeType())
                || !isOpenRevision(revision.getStatus())) {
            throw exception(PUBLICATION_IMPACT_REVISION_INVALID);
        }
        List<DccControlledFileDO> chain = Objects.requireNonNull(
                controlledFileMapper.selectListByMasterId(task.getRelatedMasterId()),
                "controlled file revision chain must not be null");
        List<DccControlledFileDO> openRevisions = chain.stream().filter(item -> item.getId() != null
                && isOpenRevision(item.getStatus())
                && DccControlledFileChangeTypeEnum.REVISION.getCode().equals(item.getChangeType())).toList();
        if (openRevisions.size() != 1 || !Objects.equals(openRevisions.get(0).getId(), revisionId)) {
            throw exception(PUBLICATION_IMPACT_REVISION_INVALID);
        }
        return revision;
    }

    private boolean isOpenRevision(String status) {
        return List.of(
                DccControlledFileStatusEnum.DRAFT.getStatus(),
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus(),
                DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus(),
                DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus(),
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus(),
                DccControlledFileStatusEnum.PENDING_APPLICANT_REWORK.getStatus(),
                DccControlledFileStatusEnum.PENDING_APPLICANT_TRAINING_RECORD.getStatus(),
                DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus(),
                DccControlledFileStatusEnum.FINALIZING.getStatus(),
                DccControlledFileStatusEnum.APPROVING.getStatus()
        ).contains(status);
    }

    private void requireMatchingMaterializedIdentity(DccPublicationImpactTaskDO task,
                                                     DccPublicationFollowupBatchDO batch,
                                                     DccPublicationRelationSnapshotDO relation) {
        if (!Objects.equals(task.getBatchId(), batch.getId())
                || !Objects.equals(task.getPublishedControlledFileId(), batch.getPublishedControlledFileId())
                || !Objects.equals(task.getPublicationRelationSnapshotId(), relation.getId())
                || !Objects.equals(task.getRelatedMasterId(), relation.getRelatedMasterId())
                || !Objects.equals(task.getRelatedActiveControlledFileId(),
                relation.getRelatedActiveControlledFileId())) {
            throw new IllegalStateException("Publication impact task identity conflict for related master "
                    + relation.getRelatedMasterId());
        }
    }

    private void insertAudit(DccPublicationImpactTaskDO task, String actionType, Long actorId,
                             String reason, String statusAfter, Long assigneeAfter, String decision,
                             Long linkedRevisionId, Integer versionBefore, Integer versionAfter) {
        DccPublicationImpactAuditDO audit = DccPublicationImpactAuditDO.builder()
                .taskId(task.getId()).batchId(task.getBatchId()).actionType(actionType).actorId(actorId)
                .reason(reason).statusBefore(task.getTaskStatus()).statusAfter(statusAfter)
                .assigneeBefore(task.getAssigneeUserId()).assigneeAfter(assigneeAfter)
                .decisionSnapshot(decision == null ? task.getDecision() : decision)
                .linkedRevisionControlledFileId(linkedRevisionId)
                .rowVersionBefore(versionBefore == null ? task.getRowVersion() : versionBefore)
                .rowVersionAfter(versionAfter == null ? task.getRowVersion() : versionAfter)
                .occurredAt(LocalDateTime.now()).build();
        audit.setTenantId(TenantContextHolder.getRequiredTenantId());
        auditMapper.insert(audit);
    }
}
