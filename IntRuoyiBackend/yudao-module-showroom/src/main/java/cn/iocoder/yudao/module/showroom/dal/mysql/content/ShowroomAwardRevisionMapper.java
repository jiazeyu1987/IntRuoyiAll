package cn.iocoder.yudao.module.showroom.dal.mysql.content;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomAwardRevisionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ShowroomAwardRevisionMapper extends BaseMapperX<ShowroomAwardRevisionDO> {

    default ShowroomAwardRevisionDO selectLatestByAwardId(Long awardId) {
        return selectOne(new LambdaQueryWrapperX<ShowroomAwardRevisionDO>()
                .eq(ShowroomAwardRevisionDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomAwardRevisionDO::getAwardId, awardId)
                .orderByDesc(ShowroomAwardRevisionDO::getRevisionNo)
                .orderByDesc(ShowroomAwardRevisionDO::getId)
                .last("LIMIT 1"));
    }

    default List<ShowroomAwardRevisionDO> selectListByAwardIds(Collection<Long> awardIds) {
        if (awardIds == null || awardIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<ShowroomAwardRevisionDO>()
                .eq(ShowroomAwardRevisionDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .in(ShowroomAwardRevisionDO::getAwardId, awardIds)
                .orderByAsc(ShowroomAwardRevisionDO::getAwardId)
                .orderByDesc(ShowroomAwardRevisionDO::getRevisionNo)
                .orderByDesc(ShowroomAwardRevisionDO::getId));
    }

    default List<ShowroomAwardRevisionDO> selectListByIds(Collection<Long> revisionIds) {
        if (revisionIds == null || revisionIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<ShowroomAwardRevisionDO>()
                .eq(ShowroomAwardRevisionDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .in(ShowroomAwardRevisionDO::getId, revisionIds));
    }
}
