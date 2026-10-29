package cn.iocoder.yudao.module.dcc.service.position;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccApprovalPositionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccPositionAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserDirectSupervisorService;
import jakarta.annotation.Resource;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.LinkedHashSet;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_POSITION_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_POSITION_UPLOADER_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_POSITION_UPLOADER_MAPPING_INVALID;

@Component
@RequiredArgsConstructor
public class DccApprovalPositionRuntimeResolver {

    private final DccApprovalPositionMapper positionMapper;
    private final AdminUserService adminUserService;
    private final DeptService deptService;
    @Resource private DccPositionAssignmentMapper assignmentMapper;
    @Resource private AdminUserDirectSupervisorService directSupervisorService;

    public boolean isUploaderDerivedPosition(Long positionId) {
        DccApprovalPositionDO position = positionMapper.selectById(positionId);
        return position != null && (DccUploaderDerivedPositionSupport.isUploaderDerivedPosition(position)
                || assignmentMapper.selectActiveListByPositionId(positionId).stream()
                    .anyMatch(row -> DccPositionAssignmentTypes.isRelationship(row.getAssignmentType())));
    }

    public List<Long> resolveUserIds(Long positionId, Long submitterUserId, boolean allowDeferredContext) {
        DccApprovalPositionDO position = positionMapper.selectById(positionId);
        if (position == null) {
            throw exception(APPROVAL_POSITION_NOT_EXISTS);
        }
        boolean nativePosition = DccUploaderDerivedPositionSupport.isUploaderDerivedPosition(position);
        List<DccPositionAssignmentDO> assignments = nativePosition ? List.of() : assignmentMapper.selectActiveListByPositionId(positionId);
        if (!nativePosition && assignments.stream().noneMatch(row -> DccPositionAssignmentTypes.isRelationship(row.getAssignmentType()))) {
            return List.of();
        }
        if (submitterUserId == null) {
            if (allowDeferredContext) {
                return List.of();
            }
            throw exception(APPROVAL_POSITION_UPLOADER_CONTEXT_REQUIRED, position.getName());
        }
        if (DccUploaderDerivedPositionSupport.isDirectManagerPositionName(position.getName())) {
            return resolveDirectSupervisor(position.getName(), submitterUserId);
        }
        if (DccUploaderDerivedPositionSupport.isDepartmentScopedPositionName(position.getName())) {
            return resolveSubmitterDepartmentLeaderUserIds(position.getName(), submitterUserId);
        }
        LinkedHashSet<Long> users = new LinkedHashSet<>();
        for (DccPositionAssignmentDO row : assignments) {
            DccPositionAssignmentTypes.validate(row.getAssignmentType(), row.getUserId(), row.getSystemPostId());
            switch (row.getAssignmentType()) {
                case DccPositionAssignmentTypes.DIRECT_SUPERVISOR -> users.addAll(resolveDirectSupervisor(position.getName(), submitterUserId));
                case DccPositionAssignmentTypes.DEPT_LEADER -> users.addAll(resolveSubmitterDepartmentLeaderUserIds(position.getName(), submitterUserId));
                case "USER" -> users.add(row.getUserId());
                case "POST" -> adminUserService.getUserListByPostIds(List.of(row.getSystemPostId())).forEach(user -> users.add(user.getId()));
                default -> throw mappingInvalid(position.getName(), "unsupported_relationship");
            }
        }
        for (Long userId : users) requireLocalEnabledUserById(userId, position.getName(), "local_resolved_account_missing");
        return List.copyOf(users);
    }

    private List<Long> resolveDirectSupervisor(String name, Long submitterId) {
        requireLocalEnabledUserById(submitterId, name, "local_submitter_missing");
        var relation = directSupervisorService.getDirectSupervisorRelation(submitterId);
        if (relation == null || !Objects.equals(relation.getUserId(), submitterId)
                || relation.getSupervisorUserId() == null || Objects.equals(relation.getSupervisorUserId(), submitterId)) {
            throw mappingInvalid(name, "申请人的直属主管未配置或关系无效，请在用户管理维护直属主管");
        }
        return List.of(requireLocalEnabledUserById(relation.getSupervisorUserId(), name, "直属主管账号不存在或已停用").getId());
    }

    private List<Long> resolveSubmitterDepartmentLeaderUserIds(String positionName, Long submitterUserId) {
        AdminUserDO submitter = requireLocalEnabledUserById(submitterUserId, positionName, "local_submitter_missing");
        Long departmentId = submitter.getDeptId();
        if (departmentId == null) {
            throw mappingInvalid(positionName, "local_department_missing");
        }
        DeptDO department = deptService.getDept(departmentId);
        if (department == null || !Objects.equals(department.getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
            throw mappingInvalid(positionName, "local_department_missing");
        }
        Long leaderUserId = department.getLeaderUserId();
        if (leaderUserId == null) {
            throw mappingInvalid(positionName, "local_department_leader_missing");
        }
        AdminUserDO leader = requireLocalEnabledUserById(leaderUserId, positionName,
                "local_department_leader_user_missing");
        return List.of(leader.getId());
    }

    private AdminUserDO requireLocalEnabledUserById(Long userId, String positionName, String reason) {
        AdminUserDO user = userId == null ? null : adminUserService.getUser(userId);
        if (user == null || StrUtil.isBlank(user.getUsername())
                || !Objects.equals(user.getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
            throw mappingInvalid(positionName, reason);
        }
        return user;
    }

    private RuntimeException mappingInvalid(String positionName, String reason) {
        return exception(APPROVAL_POSITION_UPLOADER_MAPPING_INVALID, describeMappingInvalid(positionName, reason));
    }

    private String describeMappingInvalid(String positionName, String reason) {
        return switch (reason) {
            case "local_submitter_missing" ->
                    StrUtil.format("{} cannot resolve because the submitter is missing or disabled in the local user directory",
                            positionName);
            case "local_department_missing" ->
                    StrUtil.format("{} requires the submitter to belong to an enabled local department", positionName);
            case "local_department_leader_missing" ->
                    StrUtil.format("{} requires a local department leader for the submitter", positionName);
            case "local_department_leader_user_missing" ->
                    StrUtil.format("{} cannot resolve because the submitter's local department leader is missing or disabled",
                            positionName);
            default -> StrUtil.format("{} cannot resolve because {}", positionName, reason);
        };
    }

}
