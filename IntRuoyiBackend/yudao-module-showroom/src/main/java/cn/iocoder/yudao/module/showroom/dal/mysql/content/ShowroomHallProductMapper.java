package cn.iocoder.yudao.module.showroom.dal.mysql.content;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomHallProductDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ShowroomHallProductMapper extends BaseMapperX<ShowroomHallProductDO> {

    @Delete("DELETE FROM showroom_hall_product WHERE tenant_id = #{tenantId} AND hall_id = #{hallId}")
    int deleteByHallIdForce(@Param("tenantId") Long tenantId, @Param("hallId") Long hallId);

    default List<ShowroomHallProductDO> selectListByHallIds(Collection<Long> hallIds) {
        if (hallIds == null || hallIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<ShowroomHallProductDO>()
                .eq(ShowroomHallProductDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .in(ShowroomHallProductDO::getHallId, hallIds)
                .orderByAsc(ShowroomHallProductDO::getHallId)
                .orderByAsc(ShowroomHallProductDO::getDisplayOrder)
                .orderByAsc(ShowroomHallProductDO::getId));
    }

    default List<ShowroomHallProductDO> selectListByProductIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<ShowroomHallProductDO>()
                .eq(ShowroomHallProductDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .in(ShowroomHallProductDO::getProductId, productIds)
                .orderByAsc(ShowroomHallProductDO::getProductId)
                .orderByAsc(ShowroomHallProductDO::getHallId)
                .orderByAsc(ShowroomHallProductDO::getDisplayOrder)
                .orderByAsc(ShowroomHallProductDO::getId));
    }

}
