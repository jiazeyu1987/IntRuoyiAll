package cn.iocoder.yudao.module.mes.dal.mysql.pro.route;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * MES 工艺路线产品 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesProRouteProductMapper extends BaseMapperX<MesProRouteProductDO> {

    default List<MesProRouteProductDO> selectListByRouteId(Long routeId) {
        return selectList(MesProRouteProductDO::getRouteId, routeId);
    }

    default MesProRouteProductDO selectByItemId(Long itemId) {
        return selectOne(MesProRouteProductDO::getItemId, itemId);
    }

    default List<MesProRouteProductDO> selectListByItemId(Long itemId) {
        return selectList(MesProRouteProductDO::getItemId, itemId);
    }

    default List<MesProRouteProductDO> selectListByItemIds(Collection<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProRouteProductDO>()
                .in(MesProRouteProductDO::getItemId, itemIds)
                .orderByAsc(MesProRouteProductDO::getId));
    }

    default MesProRouteProductDO selectByRouteIdAndItemId(Long routeId, Long itemId) {
        return selectOne(MesProRouteProductDO::getRouteId, routeId,
                MesProRouteProductDO::getItemId, itemId);
    }

    default List<MesProRouteProductDO> selectListByRouteIds(Collection<Long> routeIds) {
        if (routeIds == null || routeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProRouteProductDO>()
                .in(MesProRouteProductDO::getRouteId, routeIds)
                .orderByAsc(MesProRouteProductDO::getRouteId)
                .orderByAsc(MesProRouteProductDO::getId));
    }

    default void deleteByRouteId(Long routeId) {
        delete(new LambdaQueryWrapperX<MesProRouteProductDO>()
                .eq(MesProRouteProductDO::getRouteId, routeId));
    }

}
