package cn.iocoder.yudao.module.showroom.dal.mysql.content;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomCompanyDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShowroomCompanyMapper extends BaseMapperX<ShowroomCompanyDO> {

    default ShowroomCompanyDO selectMainCompany() {
        return selectOne(new LambdaQueryWrapperX<ShowroomCompanyDO>()
                .eq(ShowroomCompanyDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomCompanyDO::getCompanyType, "MAIN")
                .orderByAsc(ShowroomCompanyDO::getId)
                .last("LIMIT 1"));
    }

    default List<ShowroomCompanyDO> selectListOrdered() {
        return selectList(new LambdaQueryWrapperX<ShowroomCompanyDO>()
                .eq(ShowroomCompanyDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .orderByAsc(ShowroomCompanyDO::getId));
    }

}
