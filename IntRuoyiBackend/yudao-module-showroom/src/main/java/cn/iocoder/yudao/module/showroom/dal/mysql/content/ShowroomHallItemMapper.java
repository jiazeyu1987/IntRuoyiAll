package cn.iocoder.yudao.module.showroom.dal.mysql.content;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomHallItemDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ShowroomHallItemMapper extends BaseMapperX<ShowroomHallItemDO> {

    @Delete("DELETE FROM showroom_hall_item WHERE tenant_id = #{tenantId} AND hall_id = #{hallId}")
    int deleteByHallIdForce(@Param("tenantId") Long tenantId, @Param("hallId") Long hallId);

    @Delete("""
            DELETE FROM showroom_hall_item
            WHERE tenant_id = #{tenantId}
              AND item_type = #{itemType}
              AND hall_id <> #{hallId}
            """)
    int deleteByItemTypeOutsideHallForce(@Param("tenantId") Long tenantId,
                                         @Param("itemType") String itemType,
                                         @Param("hallId") Long hallId);

    @Delete("""
            <script>
            DELETE FROM showroom_hall_item
            WHERE tenant_id = #{tenantId}
              AND item_type = #{itemType}
              AND hall_id NOT IN
              <foreach collection="hallIds" item="hallId" open="(" separator="," close=")">
                #{hallId}
              </foreach>
            </script>
            """)
    int deleteByItemTypeOutsideHallsForce(@Param("tenantId") Long tenantId,
                                          @Param("itemType") String itemType,
                                          @Param("hallIds") Collection<Long> hallIds);

    default List<ShowroomHallItemDO> selectListByHallIds(Collection<Long> hallIds) {
        if (hallIds == null || hallIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<ShowroomHallItemDO>()
                .eq(ShowroomHallItemDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .in(ShowroomHallItemDO::getHallId, hallIds)
                .orderByAsc(ShowroomHallItemDO::getHallId)
                .orderByAsc(ShowroomHallItemDO::getDisplayOrder)
                .orderByAsc(ShowroomHallItemDO::getId));
    }

    default List<ShowroomHallItemDO> selectListByItems(String itemType, Collection<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<ShowroomHallItemDO>()
                .eq(ShowroomHallItemDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomHallItemDO::getItemType, itemType)
                .in(ShowroomHallItemDO::getItemId, itemIds)
                .orderByAsc(ShowroomHallItemDO::getItemId)
                .orderByAsc(ShowroomHallItemDO::getHallId)
                .orderByAsc(ShowroomHallItemDO::getDisplayOrder)
                .orderByAsc(ShowroomHallItemDO::getId));
    }
}
