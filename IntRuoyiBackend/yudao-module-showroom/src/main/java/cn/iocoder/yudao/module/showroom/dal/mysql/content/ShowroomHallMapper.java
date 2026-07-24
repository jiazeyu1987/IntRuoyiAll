package cn.iocoder.yudao.module.showroom.dal.mysql.content;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomHallDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShowroomHallMapper extends BaseMapperX<ShowroomHallDO> {

    default ShowroomHallDO selectByHallCode(String hallCode) {
        return selectOne(new LambdaQueryWrapperX<ShowroomHallDO>()
                .eq(ShowroomHallDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomHallDO::getHallCode, hallCode)
                .last("LIMIT 1"));
    }

    default List<ShowroomHallDO> selectListOrdered() {
        return selectList(new LambdaQueryWrapperX<ShowroomHallDO>()
                .eq(ShowroomHallDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .orderByAsc(ShowroomHallDO::getDisplayOrder)
                .orderByAsc(ShowroomHallDO::getId));
    }

}
