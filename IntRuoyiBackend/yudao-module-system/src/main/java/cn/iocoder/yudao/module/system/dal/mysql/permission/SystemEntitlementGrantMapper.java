package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementGrantDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SystemEntitlementGrantMapper extends BaseMapperX<SystemEntitlementGrantDO> {

    default List<SystemEntitlementGrantDO> selectActiveListByUserId(Long tenantId, Long resolvedUserId) {
        return selectList(new LambdaQueryWrapperX<SystemEntitlementGrantDO>()
                .eq(SystemEntitlementGrantDO::getTenantId, tenantId)
                .eq(SystemEntitlementGrantDO::getResolvedUserId, resolvedUserId)
                .eq(SystemEntitlementGrantDO::getStatus, "ACTIVE"));
    }

    default List<SystemEntitlementGrantDO> selectActiveListByUserIdAndPermission(Long tenantId, Long resolvedUserId,
                                                                                 String permissionCode) {
        return selectList(new LambdaQueryWrapperX<SystemEntitlementGrantDO>()
                .eq(SystemEntitlementGrantDO::getTenantId, tenantId)
                .eq(SystemEntitlementGrantDO::getResolvedUserId, resolvedUserId)
                .eq(SystemEntitlementGrantDO::getPermissionCode, permissionCode)
                .eq(SystemEntitlementGrantDO::getStatus, "ACTIVE"));
    }

    default SystemEntitlementGrantDO selectByIdentity(Long tenantId, Long resolvedUserId, String permissionCode,
                                                      Long menuId, String policyCode) {
        return selectOne(new LambdaQueryWrapperX<SystemEntitlementGrantDO>()
                .eq(SystemEntitlementGrantDO::getTenantId, tenantId)
                .eq(SystemEntitlementGrantDO::getResolvedUserId, resolvedUserId)
                .eq(SystemEntitlementGrantDO::getPermissionCode, permissionCode)
                .eq(SystemEntitlementGrantDO::getMenuId, menuId)
                .eq(SystemEntitlementGrantDO::getPolicyCode, policyCode));
    }

    default void revokeByTenantAndUser(Long tenantId, Long resolvedUserId) {
        update(new LambdaUpdateWrapper<SystemEntitlementGrantDO>()
                .set(SystemEntitlementGrantDO::getStatus, "REVOKED")
                .set(SystemEntitlementGrantDO::getActiveClaimCount, 0)
                .eq(SystemEntitlementGrantDO::getTenantId, tenantId)
                .eq(SystemEntitlementGrantDO::getResolvedUserId, resolvedUserId)
                .eq(SystemEntitlementGrantDO::getStatus, "ACTIVE"));
    }

}
