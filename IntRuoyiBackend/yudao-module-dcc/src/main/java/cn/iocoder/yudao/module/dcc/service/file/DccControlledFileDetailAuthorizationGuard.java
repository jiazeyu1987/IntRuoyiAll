package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import jakarta.annotation.Resource;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Shared file-level detail authorization without a query-service dependency. */
@Component
class DccControlledFileDetailAuthorizationGuard {
    private final ThreadLocal<DccVerifiedOfficeReadActor> verifiedOfficeReadActor = new ThreadLocal<>();

    @Resource
    private DccControlledFileAssignmentScopeService assignmentScopeService;
    @Resource
    private DccDirectoryAccessPermissionService directoryAccessPermissionService;
    @Resource
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Resource
    private DccControlledFileViewMatrixAccessService viewMatrixAccessService;
    @Resource
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Resource
    private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Resource
    private BpmTaskService bpmTaskService;
    @Resource
    private DccOfflineTrainingRecordService offlineTraining;
    @Resource
    private DccBoundWorkflowContractResolver boundWorkflowContractResolver;
    @Resource
    private cn.iocoder.yudao.module.system.api.user.AdminUserApi adminUserApi;
    @Resource
    private cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService projectAccessService;
    @Resource
    private cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService bpmProcessInstanceService;
    @Resource
    private DccApplicationRoundService applicationRoundService;
    @Resource
    private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper masterMapper;

    /** Body access is separate from metadata access. The requested intent never grants permission. */
    void requireBodyRead(Long userId, DccControlledFileDO file, DccControlledFileBodyReadContext context) {
        if (file == null || file.getId() == null || Boolean.TRUE.equals(file.getDeleted())
                || !Objects.equals(file.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || !hasAuthenticatedActor(userId, file)) bodyDenied();
        context.requireFile(file.getId(), file.getVersionNo());
        if (!assignmentScopeService.isWithinAssignedFileScope(userId, file.getId())) bodyDenied();
        switch (context.intent()) {
            case DAILY -> {
                if (!"ACTIVE".equals(file.getStatus()) || file.getActivatedTime() == null) bodyDenied();
                requireReadableProject(userId, file);
            }
            case VERSION_HISTORY -> {
                if (!canReadRetainedVersion(userId, file, null)) bodyDenied();
            }
            case AUTHORING -> {
                if (Set.of("ACTIVE", "SUPERSEDED", "OBSOLETE", "CONTROLLED_PENDING_EFFECTIVE").contains(file.getStatus())) bodyDenied();
                if (!hasCompilerScope(userId, file)) bodyDenied();
                requireMasterIdentity(file);
                if (context.bpmRound() == null) {
                    if (!"WORKING".equals(file.getStatus()) || StrUtil.isNotBlank(file.getProcessInstanceId())
                            || file.getControlledTime() != null) bodyDenied();
                } else {
                    var round = requireExactBodyRound(file, context);
                    if (!Objects.equals(context.bpmRound(), file.getProcessInstanceId())
                            || !Objects.equals(round.startUserId(), userId.toString())) bodyDenied();
                }
            }
            case APPROVAL_TASK -> {
                var round = requireExactBodyRound(file, context);
                var task = bpmTaskService.getTask(context.taskId());
                if (!round.active() || task == null || task.isSuspended()
                        || !Objects.equals(task.getId(), context.taskId())
                        || !Objects.equals(task.getTenantId(), file.getTenantId().toString())
                        || !Objects.equals(task.getProcessInstanceId(), context.bpmRound())
                        || !Objects.equals(task.getProcessDefinitionId(), round.definitionId())
                        || !Objects.equals(task.getAssignee(), userId.toString())) bodyDenied();
            }
            case APPROVAL_HISTORY -> {
                requireReadableProject(userId, file);
                var round = requireExactBodyRound(file, context);
                if (context.taskId() == null) {
                    if (!Objects.equals(round.startUserId(), userId.toString())) bodyDenied();
                } else {
                    var task = bpmTaskService.getHistoricTask(context.taskId());
                    if (task == null || task.getEndTime() == null || !Objects.equals(task.getId(), context.taskId())
                            || !Objects.equals(task.getTenantId(), file.getTenantId().toString())
                            || !Objects.equals(task.getProcessInstanceId(), context.bpmRound())
                            || !Objects.equals(task.getProcessDefinitionId(), round.definitionId())
                            || !Objects.equals(task.getAssignee(), userId.toString())) bodyDenied();
                }
            }
            case TRAINING_APPLICATION -> {
                requireReadableProject(userId, file);
                requireExactBodyRound(file, context);
                if (!Objects.equals(file.getProcessInstanceId(), context.bpmRound())
                        || !DccOfflineTrainingRecordService.isNativeWaitingFile(file)
                        || !offlineTraining.canReadTrainingBody(userId, file)) bodyDenied();
            }
        }
    }

    private void requireReadableProject(Long actor, DccControlledFileDO file) {
        if (file.getDccProjectCodeId() == null
                || !projectAccessService.listReadableProjectIds(actor).contains(file.getDccProjectCodeId())) bodyDenied();
    }

    boolean canReadRetainedVersion(Long userId, DccControlledFileDO file, Map<Long, Boolean> viewMatrixCache) {
        if (file == null || file.getId() == null || file.getStatus() == null || Boolean.TRUE.equals(file.getDeleted())
                || !Set.of("ACTIVE", "SUPERSEDED", "OBSOLETE").contains(file.getStatus())
                || file.getPublishedFileId() == null || file.getPublishedFileId() <= 0
                || !Objects.equals(file.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || !hasAuthenticatedActor(userId, file)
                || !assignmentScopeService.isWithinAssignedFileScope(userId, file.getId())
                || file.getDccProjectCodeId() == null
                || !projectAccessService.listReadableProjectIds(userId).contains(file.getDccProjectCodeId())
                || !matchesMasterIdentity(file)) return false;
        return hasAuthorizedDirectoryAccess(userId, file) || canAccessCurrentViewMatrix(userId, file, viewMatrixCache);
    }

    private void requireMasterIdentity(DccControlledFileDO file) {
        if (!matchesMasterIdentity(file)) bodyDenied();
    }

    private boolean matchesMasterIdentity(DccControlledFileDO file) {
        var master = file.getMasterId() == null ? null : masterMapper.selectById(file.getMasterId());
        return master != null && !Boolean.TRUE.equals(master.getDeleted()) && Objects.equals(master.getId(), file.getMasterId())
                && Objects.equals(master.getTenantId(), file.getTenantId())
                && Objects.equals(master.getDccProjectCodeId(), file.getDccProjectCodeId())
                && Objects.equals(master.getCategoryId(), file.getCategoryId())
                && Objects.equals(master.getFileTypeTaxonomyLeafId(), file.getFileTypeTaxonomyId())
                && Objects.equals(master.getNormalizedFileNumber(), file.getFileNumber());
    }

    private record BodyRound(String definitionId, String startUserId, boolean active) {}

    private BodyRound requireExactBodyRound(DccControlledFileDO file, DccControlledFileBodyReadContext context) {
        String bpm = context.bpmRound();
        var runtime = bpmProcessInstanceService.getProcessInstance(bpm);
        var history = bpmProcessInstanceService.getHistoricProcessInstance(bpm);
        if (history == null || !Objects.equals(history.getId(), bpm)
                || !Objects.equals(history.getTenantId(), file.getTenantId().toString())) bodyDenied();
        // Explicit legacy lineage, not a fallback for a missing modern round mapping.
        if (DccControlledFileProcessDefinitionKeys.LEGACY_APPROVAL.equals(history.getProcessDefinitionKey())) {
            if (!Objects.equals(bpm, file.getProcessInstanceId())
                    || !Objects.equals(file.getId().toString(), history.getBusinessKey())
                    || !Objects.equals(file.getRequesterId().toString(), history.getStartUserId())
                    || !Objects.equals(context.applicationType(), "NEW".equals(file.getChangeType()) ? "UPLOAD"
                        : "REVISION".equals(file.getChangeType()) ? "REVISION" : null)) bodyDenied();
        } else if (DccControlledFileProcessDefinitionKeys.LEGACY_OBSOLETE_FORM_CENTER.equals(history.getProcessDefinitionKey())) {
            var facts = history.getProcessVariables();
            if (!"OBSOLETE".equals(context.applicationType()) || facts == null
                    || !"DCC".equals(facts.get("systemCode")) || !"CONTROLLED_FILE".equals(facts.get("objectType"))
                    || !"OBSOLETE".equals(facts.get("actionCode")) || !file.getId().toString().equals(String.valueOf(facts.get("objectId")))
                    || !Objects.equals(file.getVersionNo(), facts.get("objectVersion"))
                    || history.getBusinessKey() == null || !history.getBusinessKey().matches("FORM_ACTION:\\S+")) bodyDenied();
        } else {
            int attributeRound = applicationRoundService.require(context.applicationType(), file.getId(), bpm);
            var row = new DccApplicationRoundSummary();
            row.setControlledFileId(file.getId()); row.setApplicationType(context.applicationType());
            row.setBpmRound(bpm); row.setAttributeRound(attributeRound);
            var flow = Objects.requireNonNull(boundWorkflowContractResolver.resolveRoundFlowContract(file, row), "formal round flow");
            if (!Objects.equals(flow.tenantId(), file.getTenantId()) || !Objects.equals(flow.controlledFileId(), file.getId())
                    || !Objects.equals(flow.applicationType(), context.applicationType()) || !Objects.equals(flow.bpmRound(), bpm)
                    || !Objects.equals(flow.processDefinitionId(), history.getProcessDefinitionId())) bodyDenied();
            if (runtime != null && flow.status() != DccBoundWorkflowContractResolver.RoundContractStatus.READY) bodyDenied();
        }
        if (StrUtil.isBlank(history.getProcessDefinitionId()) || StrUtil.isBlank(history.getStartUserId())) bodyDenied();
        if (runtime != null && (!Objects.equals(runtime.getId(), bpm)
                || !Objects.equals(runtime.getTenantId(), file.getTenantId().toString())
                || !Objects.equals(runtime.getProcessDefinitionId(), history.getProcessDefinitionId())
                || !Objects.equals(runtime.getBusinessKey(), history.getBusinessKey()))) bodyDenied();
        return new BodyRound(history.getProcessDefinitionId(), history.getStartUserId(), runtime != null);
    }

    private static void bodyDenied() {
        throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED);
    }

    boolean isAllowed(Long userId, DccControlledFileDO file) {
        return isAllowed(userId, file,
                directoryAccessPermissionService.hasDirectoryManagementPermission(userId), null);
    }

    boolean isAllowed(Long userId, DccControlledFileDO file, boolean directoryManager,
                      Map<Long, Boolean> viewMatrixCache) {
        if (file != null && DccControlledFileStatusEnum.PENDING_SUPERVISOR_REVIEW.getStatus().equals(file.getStatus()))
            return hasCurrentSupervisorTask(userId, file) || hasCurrentCompilerAccess(userId, file);
        if (hasCurrentRunningApprovalTask(userId, file)) {
            return true;
        }
        if (!assignmentScopeService.isWithinAssignedFileScope(userId, file == null ? null : file.getId())) {
            return false;
        }
        if (file != null && offlineTraining.canReadProgress(userId, file)) {
            return true;
        }
        if (userId != null && userId.equals(file.getRequesterId())) {
            return true;
        }
        if (file != null && (isObsolete(file) || "SUPERSEDED".equals(file.getStatus()))
                && canReadRetainedVersion(userId, file, viewMatrixCache)) {
            return true;
        }
        if (directoryManager) {
            return !isObsolete(file) || canSeeObsolete(userId, file);
        }
        if (canManageNonActiveLifecycle(userId, file)) {
            return true;
        }
        if (!canAccessBrowseScope(userId, file, viewMatrixCache)
                && !hasAuthorizedDirectoryAccess(userId, file)) {
            return false;
        }
        return !isObsolete(file) || canSeeObsolete(userId, file);
    }

    private boolean canAccessBrowseScope(Long userId, DccControlledFileDO file,
                                         Map<Long, Boolean> viewMatrixCache) {
        if (userId == null || file == null) {
            return false;
        }
        if (userId.equals(file.getRequesterId())) {
            return true;
        }
        if (isPendingPreviewStatus(file.getStatus())) {
            return isCurrentRouteSnapshotParticipant(userId, file)
                    || hasCurrentRunningApprovalTask(userId, file);
        }
        if (!DccControlledFileVersionPolicy.isCurrentControlledStatus(file.getStatus())) {
            return false;
        }
        return canAccessCurrentViewMatrix(userId, file, viewMatrixCache)
                || hasActiveElectronicDistributionAccess(userId, file);
    }

    private boolean canManageNonActiveLifecycle(Long userId, DccControlledFileDO file) {
        if (userId == null || file == null || file.getCategoryId() == null) {
            return false;
        }
        String status = file.getStatus();
        if (DccControlledFileStatusEnum.PENDING_MANUAL_DISTRIBUTION.getStatus().equals(status)
                || DccControlledFileStatusEnum.TRAINING_IN_PROGRESS.getStatus().equals(status)) {
            return permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                    DccFileCategoryPermissionActionEnum.DISTRIBUTE);
        }
        if (DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus().equals(status)
                || DccControlledFileStatusEnum.FINALIZING.getStatus().equals(status)
                || DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus().equals(status)) {
            return permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                    DccFileCategoryPermissionActionEnum.APPROVE);
        }
        if (DccControlledFileStatusEnum.WORKING.getStatus().equals(status)
                || DccControlledFileStatusEnum.REJECTED.getStatus().equals(status)) {
            return permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                    DccFileCategoryPermissionActionEnum.UPLOAD);
        }
        if (DccControlledFileStatusEnum.SUPERSEDED.getStatus().equals(status)
                || DccControlledFileStatusEnum.OBSOLETE.getStatus().equals(status)) {
            return permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                    DccFileCategoryPermissionActionEnum.OBSOLETE);
        }
        return false;
    }

    private boolean canAccessCurrentViewMatrix(Long userId, DccControlledFileDO file,
                                               Map<Long, Boolean> viewMatrixCache) {
        if (userId == null || file == null || file.getCategoryId() == null) {
            return false;
        }
        if (viewMatrixCache == null) {
            return viewMatrixAccessService.canAccessCurrentViewMatrix(userId, file);
        }
        return viewMatrixCache.computeIfAbsent(file.getCategoryId(),
                ignored -> viewMatrixAccessService.canAccessCurrentViewMatrix(userId, file));
    }

    private boolean hasAuthorizedDirectoryAccess(Long userId, DccControlledFileDO file) {
        if (file.getDirectoryId() == null) {
            return false;
        }
        Set<Long> authorizedDirectoryIds = Objects.requireNonNull(
                directoryAccessPermissionService.getAuthorizedDirectoryIds(userId, DccAccessTypeEnum.PREVIEW),
                "authorizedDirectoryIds");
        return authorizedDirectoryIds.contains(file.getDirectoryId());
    }

    private boolean hasActiveElectronicDistributionAccess(Long userId, DccControlledFileDO file) {
        return userId != null
                && file != null
                && file.getId() != null
                && DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())
                && distributionRecipientMapper.countActiveElectronicRecipientAccess(
                TenantContextHolder.getRequiredTenantId(), file.getId(), userId) > 0;
    }

    private boolean isCurrentRouteSnapshotParticipant(Long userId, DccControlledFileDO file) {
        if (userId == null || file == null || file.getId() == null) {
            return false;
        }
        String stageCode = resolvePendingStageCode(file.getStatus());
        if (StrUtil.isBlank(stageCode)) {
            return false;
        }
        return routeSnapshotMapper.selectListByControlledFileId(file.getId()).stream()
                .filter(snapshot -> StrUtil.equals(snapshot.getStageCode(), stageCode))
                .anyMatch(snapshot -> parseResolvedUserIds(snapshot).contains(userId));
    }

    private boolean hasCurrentRunningApprovalTask(Long userId, DccControlledFileDO file) {
        if (file != null && DccControlledFileStatusEnum.PENDING_SUPERVISOR_REVIEW.getStatus().equals(file.getStatus()))
            return hasCurrentSupervisorTask(userId, file);
        if (userId == null || file == null || StrUtil.isBlank(file.getProcessInstanceId())
                || (!isPendingPreviewStatus(file.getStatus())
                && !DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus().equals(file.getStatus()))) {
            return false;
        }
        String stageCode = resolvePendingStageCode(file.getStatus());
        if (StrUtil.isBlank(stageCode)) {
            return false;
        }
        List<Task> runningTasks = Objects.requireNonNull(
                bpmTaskService.getRunningTaskListByProcessInstanceId(file.getProcessInstanceId(), null, null),
                "runningTasks");
        return runningTasks.stream()
                .filter(Objects::nonNull)
                .anyMatch(task -> StrUtil.equals(task.getTaskDefinitionKey(), stageCode)
                        && StrUtil.equals(task.getAssignee(), String.valueOf(userId)));
    }

    private String resolvePendingStageCode(String status) {
        if (DccControlledFileStatusEnum.PENDING_SUPERVISOR_REVIEW.getStatus().equals(status))
            return DccControlledFileStageCodeEnum.SUPERVISOR_REVIEW.getCode();
        if (DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus().equals(status)) {
            return DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode();
        }
        if (DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus().equals(status)) {
            return DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode();
        }
        if (DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus().equals(status)) {
            return DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode();
        }
        if (DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus().equals(status)) {
            return DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode();
        }
        if (DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus().equals(status)) {
            return DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode();
        }
        if (DccControlledFileStatusEnum.PENDING_APPLICANT_REWORK.getStatus().equals(status)) {
            return DccControlledFileStageCodeEnum.APPLICANT_REWORK.getCode();
        }
        return null;
    }

    private Set<Long> parseResolvedUserIds(DccControlledFileRouteSnapshotDO snapshot) {
        if (snapshot == null || StrUtil.isBlank(snapshot.getResolvedUserIds())) {
            return Set.of();
        }
        return Arrays.stream(snapshot.getResolvedUserIds().split(","))
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }

    private boolean isPendingPreviewStatus(String status) {
        return DccControlledFileStatusEnum.PENDING_SUPERVISOR_REVIEW.getStatus().equals(status)
                || DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus().equals(status)
                || DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus().equals(status)
                || DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus().equals(status)
                || DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus().equals(status)
                || DccControlledFileStatusEnum.PENDING_APPLICANT_REWORK.getStatus().equals(status);
    }

    private boolean canSeeObsolete(Long userId, DccControlledFileDO file) {
        return userId != null && userId.equals(file.getRequesterId())
                || permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.OBSOLETE);
    }

    boolean hasCurrentSupervisorTask(Long userId, DccControlledFileDO file) {
        if (file == null || !hasAuthenticatedActor(userId, file)) return false;
        var bound = Objects.requireNonNull(boundWorkflowContractResolver.resolve(file), "bound workflow contract");
        return isActivePublicBound(bound) && matchesSupervisorTask(userId, bound);
    }

    private boolean isActivePublicBound(DccBoundWorkflowContractResolver.BoundExecution bound) {
        return bound.active() && bound.trainingAfterDocumentControl()
                && Set.of("PUBLIC_MATRIX_NATIVE_V1", "PUBLIC_MATRIX_NATIVE_V2").contains(bound.executionContract());
    }

    private boolean matchesSupervisorTask(Long userId, DccBoundWorkflowContractResolver.BoundExecution bound) {
        List<Task> runningTasks = Objects.requireNonNull(
                bpmTaskService.getRunningTaskListByProcessInstanceId(bound.processInstanceId(), null, null), "runningTasks");
        if (runningTasks.size() != 1) return false;
        Task task = runningTasks.get(0);
        return task != null && !task.isSuspended()
                && Objects.equals(task.getTenantId(), bound.tenantId().toString())
                && Objects.equals(task.getProcessInstanceId(), bound.processInstanceId())
                && Objects.equals(task.getProcessDefinitionId(), bound.processDefinitionId())
                && Objects.equals(task.getTaskDefinitionKey(), DccControlledFileStageCodeEnum.SUPERVISOR_REVIEW.getCode())
                && Objects.equals(task.getAssignee(), userId.toString());
    }

    boolean hasCurrentCompilerAccess(Long userId, DccControlledFileDO file) {
        if (file == null || !hasAuthenticatedActor(userId, file) || !hasCompilerScope(userId, file)) return false;
        var bound = Objects.requireNonNull(boundWorkflowContractResolver.resolve(file), "bound workflow contract");
        return isActivePublicBound(bound) && matchesCompilerProcess(userId, file, bound);
    }

    private boolean hasCompilerScope(Long userId, DccControlledFileDO file) {
        if (!Objects.equals(userId, file.getRequesterId())
                || !assignmentScopeService.isWithinAssignedFileScope(userId, file.getId())
                || file.getDccProjectCodeId() == null || file.getSourceFileId() == null || StrUtil.isBlank(file.getSourceSha256())
                || !projectAccessService.hasProjectEditorOrOwner(userId, file.getDccProjectCodeId())
                || !projectAccessService.listReadableProjectIds(userId).contains(file.getDccProjectCodeId())
                || !permissionSupport.hasCategoryPermission(file.getCategoryId(), userId, DccFileCategoryPermissionActionEnum.UPLOAD))
            return false;
        return true;
    }

    private boolean matchesCompilerProcess(Long userId, DccControlledFileDO file,
            DccBoundWorkflowContractResolver.BoundExecution bound) {
        var process = bpmProcessInstanceService.getProcessInstance(bound.processInstanceId());
        return process != null && Objects.equals(process.getId(), bound.processInstanceId())
                && Objects.equals(process.getTenantId(), bound.tenantId().toString())
                && Objects.equals(process.getBusinessKey(), file.getId().toString())
                && Objects.equals(process.getStartUserId(), userId.toString());
    }

    DccBoundWorkflowContractResolver.BoundExecution requireAuthenticatedCurrentReadBound(Long userId, DccControlledFileDO file) {
        if (file == null || !DccControlledFileStatusEnum.PENDING_SUPERVISOR_REVIEW.getStatus().equals(file.getStatus())
                || !hasAuthenticatedReadActor(userId, file))
            throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                    cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED);
        var bound = Objects.requireNonNull(boundWorkflowContractResolver.resolve(file), "bound workflow contract");
        if (!isActivePublicBound(bound) || !(matchesSupervisorTask(userId, bound)
                || hasCompilerScope(userId, file) && matchesCompilerProcess(userId, file, bound)))
            throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                    cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED);
        return bound;
    }

    boolean hasAuthenticatedCompilerAccess(Long userId, DccControlledFileDO file) {
        return cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser() != null
                && hasCurrentCompilerAccess(userId, file);
    }
    boolean hasAuthenticatedReadActor(Long userId, DccControlledFileDO file) {
        return cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser() != null
                && hasAuthenticatedActor(userId, file);
    }

    <T> T withVerifiedOfficeReadActor(DccVerifiedOfficeReadActor actor, java.util.concurrent.Callable<T> read) throws Exception {
        if (verifiedOfficeReadActor.get() != null) throw new IllegalStateException("VERIFIED_OFFICE_READ_SCOPE_ALREADY_BOUND");
        verifiedOfficeReadActor.set(Objects.requireNonNull(actor, "verified Office actor"));
        try { return read.call(); }
        finally { verifiedOfficeReadActor.remove(); }
    }

    private boolean hasAuthenticatedActor(Long userId, DccControlledFileDO file) {
        if (userId == null || TenantContextHolder.isIgnore()) return false;
        Long tenant = TenantContextHolder.getRequiredTenantId();
        var login = cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser();
        boolean subject;
        if (login != null) {
            subject = Objects.equals(userId, login.getId()) && Objects.equals(tenant, login.getTenantId())
                    && Integer.valueOf(2).equals(login.getUserType());
        } else {
            var officeActor = verifiedOfficeReadActor.get();
            subject = officeActor != null && Objects.equals(tenant, officeActor.tenantId()) && officeActor.matches(userId, file);
        }
        return subject && DccAccessUserIdentity.isEnabledTenantUser(userId, adminUserApi.getUser(userId));
    }

    private boolean isObsolete(DccControlledFileDO file) {
        return DccControlledFileStatusEnum.OBSOLETE.getStatus().equals(file.getStatus());
    }
}
