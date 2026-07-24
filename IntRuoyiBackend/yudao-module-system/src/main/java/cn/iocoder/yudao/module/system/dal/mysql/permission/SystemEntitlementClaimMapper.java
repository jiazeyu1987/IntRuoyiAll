package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementClaimDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SystemEntitlementClaimMapper extends BaseMapperX<SystemEntitlementClaimDO> {

    default List<SystemEntitlementClaimDO> selectActiveListBySource(Long tenantId, String sourceType, String sourceKey,
                                                                    String policyCode) {
        return selectList(new LambdaQueryWrapperX<SystemEntitlementClaimDO>()
                .eq(SystemEntitlementClaimDO::getTenantId, tenantId)
                .eq(SystemEntitlementClaimDO::getSourceType, sourceType)
                .eq(SystemEntitlementClaimDO::getSourceKey, sourceKey)
                .eq(SystemEntitlementClaimDO::getPolicyCode, policyCode)
                .eq(SystemEntitlementClaimDO::getStatus, "ACTIVE"));
    }

    default List<SystemEntitlementClaimDO> selectListBySource(Long tenantId, String sourceType, String sourceKey,
                                                              String policyCode) {
        return selectList(new LambdaQueryWrapperX<SystemEntitlementClaimDO>()
                .eq(SystemEntitlementClaimDO::getTenantId, tenantId)
                .eq(SystemEntitlementClaimDO::getSourceType, sourceType)
                .eq(SystemEntitlementClaimDO::getSourceKey, sourceKey)
                .eq(SystemEntitlementClaimDO::getPolicyCode, policyCode));
    }

    default List<SystemEntitlementClaimDO> selectListBySource(Long tenantId, String sourceType, String sourceKey,
                                                              String policyCode, Long resolvedUserId) {
        return selectList(new LambdaQueryWrapperX<SystemEntitlementClaimDO>()
                .eq(SystemEntitlementClaimDO::getTenantId, tenantId)
                .eq(SystemEntitlementClaimDO::getSourceType, sourceType)
                .eq(SystemEntitlementClaimDO::getSourceKey, sourceKey)
                .eq(SystemEntitlementClaimDO::getPolicyCode, policyCode)
                .eq(SystemEntitlementClaimDO::getResolvedUserId, resolvedUserId));
    }

    default List<SystemEntitlementClaimDO> selectActiveListByUserId(Long tenantId, Long resolvedUserId) {
        return selectList(new LambdaQueryWrapperX<SystemEntitlementClaimDO>()
                .eq(SystemEntitlementClaimDO::getTenantId, tenantId)
                .eq(SystemEntitlementClaimDO::getResolvedUserId, resolvedUserId)
                .eq(SystemEntitlementClaimDO::getStatus, "ACTIVE"));
    }

    default List<SystemEntitlementClaimDO> selectActiveListByUserIds(Long tenantId, Collection<Long> resolvedUserIds) {
        return selectList(new LambdaQueryWrapperX<SystemEntitlementClaimDO>()
                .eq(SystemEntitlementClaimDO::getTenantId, tenantId)
                .in(SystemEntitlementClaimDO::getResolvedUserId, resolvedUserIds)
                .eq(SystemEntitlementClaimDO::getStatus, "ACTIVE"));
    }

}
