package cn.iocoder.yudao.module.dcc.dal.mysql.route;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * DCC category approval route node mapper.
 */
@Mapper
public interface DccCategoryApprovalRouteNodeMapper extends BaseMapperX<DccCategoryApprovalRouteNodeDO> {

    default List<DccCategoryApprovalRouteNodeDO> selectListByRouteId(Long routeId) {
        return selectList(DccCategoryApprovalRouteNodeDO::getRouteId, routeId).stream()
                .sorted(Comparator.comparing(DccCategoryApprovalRouteNodeDO::getSort)
                        .thenComparing(DccCategoryApprovalRouteNodeDO::getStageNo))
                .toList();
    }

    default List<DccCategoryApprovalRouteNodeDO> selectListByRouteIds(Collection<Long> routeIds) {
        if (routeIds == null || routeIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<DccCategoryApprovalRouteNodeDO>()
                .in(DccCategoryApprovalRouteNodeDO::getRouteId, routeIds)
                .orderByAsc(DccCategoryApprovalRouteNodeDO::getRouteId)
                .orderByAsc(DccCategoryApprovalRouteNodeDO::getStageOrder)
                .orderByAsc(DccCategoryApprovalRouteNodeDO::getSort)
                .orderByAsc(DccCategoryApprovalRouteNodeDO::getStageNo));
    }
}
