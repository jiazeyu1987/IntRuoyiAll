package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DccProjectApplicationAttributesMapper extends BaseMapperX<DccProjectApplicationAttributesDO> {
    default DccProjectApplicationAttributesDO find(String type, Long applicationId, Integer round) {
        return selectOne(new LambdaQueryWrapperX<DccProjectApplicationAttributesDO>()
                .eq(DccProjectApplicationAttributesDO::getTenantId, cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())
                .eq(DccProjectApplicationAttributesDO::getApplicationType, type)
                .eq(DccProjectApplicationAttributesDO::getApplicationId, applicationId)
                .eq(DccProjectApplicationAttributesDO::getApplicationRound, round));
    }
}
