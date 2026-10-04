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

    boolean isAllowed(Long userId, DccControlledFileDO file) {
        return isAllowed(userId, file,
                directoryAccessPermissionService.hasDirectoryManagementPermission(userId), null);
    }

    boolean isAllowed(Long userId, DccControlledFileDO file, boolean directoryManager,
                      Map<Long, Boolean> viewMatrixCache) {
        if (hasCurrentRunningApprovalTask(userId, file)) {
            return true;
        }
        if (!assignmentScopeService.isWithinAssignedFileScope(userId, file == null ? null : file.getId())) {
            return false;
        }
        if (userId != null && userId.equals(file.getRequesterId())) {
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
        return DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus().equals(status)
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

    private boolean isObsolete(DccControlledFileDO file) {
        return DccControlledFileStatusEnum.OBSOLETE.getStatus().equals(file.getStatus());
    }
}
