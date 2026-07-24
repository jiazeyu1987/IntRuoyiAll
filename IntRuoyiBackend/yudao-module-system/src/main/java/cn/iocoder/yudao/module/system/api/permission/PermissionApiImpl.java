package cn.iocoder.yudao.module.system.api.permission;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.module.system.api.permission.dto.SystemEntitlementRevokeReqDTO;
import cn.iocoder.yudao.module.system.api.permission.dto.SystemEntitlementSyncReqDTO;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.SystemEntitlementService;
import cn.iocoder.yudao.module.system.service.permission.bo.SystemEntitlementSyncCommand;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.Set;

/**
 * 权限 API 实现类
 *
 * @author 瑛泰源码
 */
@Service
public class PermissionApiImpl implements PermissionApi {

    @Resource
    private PermissionService permissionService;
    @Resource
    private SystemEntitlementService entitlementService;

    @Override
    public Set<Long> getUserRoleIdListByRoleIds(Collection<Long> roleIds) {
        return permissionService.getUserRoleIdListByRoleId(roleIds);
    }

    @Override
    public Set<Long> getUserRoleIdListByUserId(Long userId) {
        return permissionService.getUserRoleIdListByUserId(userId);
    }

    @Override
    public boolean hasAnyPermissions(Long userId, String... permissions) {
        return permissionService.hasAnyPermissions(userId, permissions);
    }

    @Override
    public boolean hasAnyPermissionsInRoles(Collection<Long> roleIds, String... permissions) {
        return permissionService.hasAnyPermissionsInRoles(roleIds, permissions);
    }

    @Override
    public boolean hasAnyRoles(Long userId, String... roles) {
        return permissionService.hasAnyRoles(userId, roles);
    }

    @Override
    public boolean hasAnyRolesOrSuperAdmin(Long userId, String... roles) {
        return permissionService.hasAnyRolesOrSuperAdmin(userId, roles);
    }

    @Override
    public void syncEntitlementClaims(SystemEntitlementSyncReqDTO reqDTO) {
        entitlementService.syncClaims(SystemEntitlementSyncCommand.builder()
                .tenantId(reqDTO.getTenantId())
                .sourceType(reqDTO.getSourceType())
                .sourceKey(reqDTO.getSourceKey())
                .sourceVersion(reqDTO.getSourceVersion())
                .sourceDigest(reqDTO.getSourceDigest())
                .policyCode(reqDTO.getPolicyCode())
                .resolvedUserIds(reqDTO.getResolvedUserIds())
                .operatorUserId(reqDTO.getOperatorUserId())
                .operatorUsername(reqDTO.getOperatorUsername())
                .build());
    }

    @Override
    public void revokeEntitlementSource(SystemEntitlementRevokeReqDTO reqDTO) {
        entitlementService.revokeEntitlementSource(reqDTO.getTenantId(), reqDTO.getSourceType(), reqDTO.getSourceKey(),
                reqDTO.getPolicyCode(), reqDTO.getOperatorUserId(), reqDTO.getOperatorUsername());
    }

    @Override
    public DeptDataPermissionRespDTO getDeptDataPermission(Long userId) {
        return permissionService.getDeptDataPermission(userId);
    }

}
