package cn.iocoder.yudao.module.showroom.dal.mysql.content;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductRevisionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ShowroomProductRevisionMapper extends BaseMapperX<ShowroomProductRevisionDO> {

    default ShowroomProductRevisionDO selectLatestByProductId(Long productId) {
        return selectOne(new LambdaQueryWrapperX<ShowroomProductRevisionDO>()
                .eq(ShowroomProductRevisionDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomProductRevisionDO::getProductId, productId)
                .orderByDesc(ShowroomProductRevisionDO::getRevisionNo)
                .orderByDesc(ShowroomProductRevisionDO::getId)
                .last("LIMIT 1"));
    }

    default List<ShowroomProductRevisionDO> selectListByProductIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<ShowroomProductRevisionDO>()
                .eq(ShowroomProductRevisionDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .in(ShowroomProductRevisionDO::getProductId, productIds)
                .orderByAsc(ShowroomProductRevisionDO::getProductId)
                .orderByDesc(ShowroomProductRevisionDO::getRevisionNo)
                .orderByDesc(ShowroomProductRevisionDO::getId));
    }

    default List<ShowroomProductRevisionDO> selectListByIds(Collection<Long> revisionIds) {
        if (revisionIds == null || revisionIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<ShowroomProductRevisionDO>()
                .eq(ShowroomProductRevisionDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .in(ShowroomProductRevisionDO::getId, revisionIds));
    }

    default List<ShowroomProductRevisionDO> selectPublishedByProductId(Long productId) {
        return selectList(new LambdaQueryWrapperX<ShowroomProductRevisionDO>()
                .eq(ShowroomProductRevisionDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomProductRevisionDO::getProductId, productId)
                .eq(ShowroomProductRevisionDO::getStatus, "PUBLISHED")
                .orderByDesc(ShowroomProductRevisionDO::getRevisionNo)
                .orderByDesc(ShowroomProductRevisionDO::getId));
    }

}
