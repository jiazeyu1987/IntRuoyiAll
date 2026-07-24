package cn.iocoder.yudao.module.system.api.permission;

import cn.iocoder.yudao.framework.common.biz.system.permission.PermissionCommonApi;
import cn.iocoder.yudao.module.system.api.permission.dto.SystemEntitlementRevokeReqDTO;
import cn.iocoder.yudao.module.system.api.permission.dto.SystemEntitlementSyncReqDTO;

import java.util.Collection;
import java.util.Set;

/**
 * 权限 API 接口
 *
 * @author 瑛泰源码
 */
public interface PermissionApi extends PermissionCommonApi {

    /**
     * 获得拥有多个角色的用户编号集合
     *
     * @param roleIds 角色编号集合
     * @return 用户编号集合
     */
    Set<Long> getUserRoleIdListByRoleIds(Collection<Long> roleIds);

    /**
     * 获得用户拥有的角色编号集合
     *
     * @param userId 用户编号
     * @return 角色编号集合
     */
    Set<Long> getUserRoleIdListByUserId(Long userId);

    /**
     * 判断指定角色集合是否显式拥有任一权限，不使用动态权益或超级管理员放宽。
     *
     * @param roleIds     角色编号集合
     * @param permissions 权限
     * @return 是否
     */
    boolean hasAnyPermissionsInRoles(Collection<Long> roleIds, String... permissions);

    /**
     * 判断是否拥有指定角色，并且默认把超级管理员视为通过。
     *
     * @param userId 用户编号
     * @param roles 角色数组
     * @return 是否拥有角色或超级管理员角色
     */
    boolean hasAnyRolesOrSuperAdmin(Long userId, String... roles);

    /**
     * 同步业务责任产生的动态权益声明。
     *
     * @param reqDTO 动态权益同步请求
     */
    void syncEntitlementClaims(SystemEntitlementSyncReqDTO reqDTO);

    /**
     * 显式撤销业务责任来源产生的动态权益声明。
     *
     * @param reqDTO 动态权益撤销请求
     */
    void revokeEntitlementSource(SystemEntitlementRevokeReqDTO reqDTO);

}
