package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentFileMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class DccControlledFileAssignmentScopeService {

    private static final String FULL_FILE_SCOPE_PERMISSION = "dcc:controlled-file:scope:all";
    private static final String ASSIGNMENT_EXECUTE_PERMISSION = "dcc:project-code-assignment:execute";

    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DccProjectCodeAssignmentFileMapper assignmentFileMapper;
    @Resource
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;

    /**
     * A null result means that project-code assignment does not constrain this user.
     */
    public Set<Long> resolveActiveAssignedControlledFileIds(Long userId) {
        if (userId == null) {
            return null;
        }
        if (permissionApi.hasAnyPermissions(userId, FULL_FILE_SCOPE_PERMISSION)) {
            return null;
        }
        if (!permissionApi.hasAnyPermissions(userId, ASSIGNMENT_EXECUTE_PERMISSION)) {
            return null;
        }
        List<Long> assignedFileIds = Objects.requireNonNull(
                assignmentFileMapper.selectActiveControlledFileIdsByAssigneeUserId(userId, LocalDateTime.now()),
                "active assignment file ids must not be null");
        List<Long> distributedFileIds = Objects.requireNonNull(
                distributionRecipientMapper.selectActiveElectronicControlledFileIdsByUserId(
                        TenantContextHolder.getRequiredTenantId(), userId),
                "active electronic distribution file ids must not be null");
        Set<Long> scopedFileIds = new HashSet<>(assignedFileIds);
        scopedFileIds.addAll(distributedFileIds);
        return scopedFileIds;
    }

    public boolean isWithinAssignedFileScope(Long userId, Long controlledFileId) {
        Set<Long> assignedFileIds = resolveActiveAssignedControlledFileIds(userId);
        return assignedFileIds == null || controlledFileId != null && assignedFileIds.contains(controlledFileId);
    }

    public Set<Long> filterBusinessVisibleUserIds(Collection<Long> candidateUserIds, Long controlledFileId) {
        if (candidateUserIds == null || candidateUserIds.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        candidateUserIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .filter(userId -> isWithinAssignedFileScope(userId, controlledFileId))
                .forEach(result::add);
        return Set.copyOf(result);
    }
}
