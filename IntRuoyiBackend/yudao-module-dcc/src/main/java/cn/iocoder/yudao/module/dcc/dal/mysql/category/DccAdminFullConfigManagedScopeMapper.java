package cn.iocoder.yudao.module.dcc.dal.mysql.category;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccAdminFullConfigManagedScopeDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DccAdminFullConfigManagedScopeMapper extends BaseMapperX<DccAdminFullConfigManagedScopeDO> {

    default DccAdminFullConfigManagedScopeDO selectCurrentScope() {
        return selectOne(DccAdminFullConfigManagedScopeDO::getTenantId, TenantContextHolder.getRequiredTenantId());
    }
}
