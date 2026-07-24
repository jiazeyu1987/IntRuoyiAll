package cn.iocoder.yudao.module.showroom.dal.mysql.content;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomAwardDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ShowroomAwardMapper extends BaseMapperX<ShowroomAwardDO> {

    default ShowroomAwardDO selectByAwardCode(String awardCode) {
        return selectOne(new LambdaQueryWrapperX<ShowroomAwardDO>()
                .eq(ShowroomAwardDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomAwardDO::getAwardCode, awardCode)
                .last("LIMIT 1"));
    }

    default List<ShowroomAwardDO> selectListOrdered() {
        return selectList(new LambdaQueryWrapperX<ShowroomAwardDO>()
                .eq(ShowroomAwardDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .orderByAsc(ShowroomAwardDO::getId));
    }

    default List<ShowroomAwardDO> selectCurrentListOrdered() {
        return selectList(new LambdaQueryWrapperX<ShowroomAwardDO>()
                .eq(ShowroomAwardDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .isNotNull(ShowroomAwardDO::getCurrentRevisionId)
                .orderByAsc(ShowroomAwardDO::getAwardCode)
                .orderByAsc(ShowroomAwardDO::getId));
    }

    default List<ShowroomAwardDO> selectListByIds(Collection<Long> awardIds) {
        if (awardIds == null || awardIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<ShowroomAwardDO>()
                .eq(ShowroomAwardDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .in(ShowroomAwardDO::getId, awardIds)
                .orderByAsc(ShowroomAwardDO::getId));
    }
}
