package cn.iocoder.yudao.module.showroom.workflow.service;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.foundation.contract.ShowroomRoleModelContract;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ShowroomApprovalActorResolver {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final AdminUserMapper adminUserMapper;

    public ShowroomApprovalActorResolver(RoleMapper roleMapper, UserRoleMapper userRoleMapper,
                                         AdminUserMapper adminUserMapper) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.adminUserMapper = adminUserMapper;
    }

    @DataPermission(enable = false)
    public Long resolvePublicityApproverUserId() {
        Long tenantId = TenantContextHolder.getTenantId();
        List<RoleDO> matchedRoles = roleMapper.selectList(new LambdaQueryWrapperX<RoleDO>()
                .eq(RoleDO::getCode, ShowroomRoleModelContract.gaoxinApproverRoleCode())
                .eqIfPresent(RoleDO::getTenantId, tenantId));
        List<RoleDO> enabledRoles = matchedRoles.stream()
                .filter(role -> CommonStatusEnum.isEnable(role.getStatus()))
                .toList();
        if (enabledRoles.isEmpty()) {
            throw new IllegalStateException(
                    "SHOWROOM_ROLE_BINDING_MISSING: publicity approver role showroom_publicity is required");
        }
        if (enabledRoles.size() > 1) {
            throw new IllegalStateException(
                    "SHOWROOM_ROLE_BINDING_AMBIGUOUS: publicity approver role must be unique in current tenant scope");
        }
        RoleDO publicityRole = enabledRoles.get(0);
        List<Long> candidateUserIds = userRoleMapper.selectListByRoleIds(List.of(publicityRole.getId())).stream()
                .map(UserRoleDO::getUserId)
                .distinct()
                .toList();
        List<Long> enabledUserIds = candidateUserIds.stream()
                .map(adminUserMapper::selectById)
                .filter(user -> user != null && CommonStatusEnum.isEnable(user.getStatus()))
                .map(AdminUserDO::getId)
                .toList();
        if (enabledUserIds.isEmpty()) {
            throw new IllegalStateException(
                    "SHOWROOM_ROLE_BINDING_MISSING: publicity approver role must bind one enabled user");
        }
        if (enabledUserIds.size() > 1) {
            throw new IllegalStateException(
                    "SHOWROOM_ROLE_BINDING_AMBIGUOUS: publicity approver role must bind exactly one enabled user");
        }
        return enabledUserIds.get(0);
    }

    public boolean hasPublicityRole(Long userId) {
        if (userId == null) {
            return false;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        List<RoleDO> enabledRoles = roleMapper.selectList(new LambdaQueryWrapperX<RoleDO>()
                        .eq(RoleDO::getCode, ShowroomRoleModelContract.gaoxinApproverRoleCode())
                        .eqIfPresent(RoleDO::getTenantId, tenantId))
                .stream()
                .filter(role -> CommonStatusEnum.isEnable(role.getStatus()))
                .toList();
        if (enabledRoles.isEmpty()) {
            return false;
        }
        List<Long> roleIds = enabledRoles.stream().map(RoleDO::getId).toList();
        return userRoleMapper.selectListByRoleIds(roleIds).stream()
                .anyMatch(binding -> Objects.equals(binding.getUserId(), userId));
    }
}
