package cn.iocoder.yudao.module.mes.dal.mysql.pro.route;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessLossReasonDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProRouteProcessLossReasonMapper extends BaseMapperX<MesProRouteProcessLossReasonDO> {

    default List<MesProRouteProcessLossReasonDO> selectListByRouteVersionId(Long routeVersionId) {
        return selectList(new LambdaQueryWrapperX<MesProRouteProcessLossReasonDO>()
                .eq(MesProRouteProcessLossReasonDO::getRouteVersionId, routeVersionId)
                .orderByAsc(MesProRouteProcessLossReasonDO::getRouteProcessId)
                .orderByAsc(MesProRouteProcessLossReasonDO::getSort)
                .orderByAsc(MesProRouteProcessLossReasonDO::getId));
    }

    default List<MesProRouteProcessLossReasonDO> selectListByRouteVersionIdAndRouteProcessId(
            Long routeVersionId, Long routeProcessId) {
        return selectList(new LambdaQueryWrapperX<MesProRouteProcessLossReasonDO>()
                .eq(MesProRouteProcessLossReasonDO::getRouteVersionId, routeVersionId)
                .eq(MesProRouteProcessLossReasonDO::getRouteProcessId, routeProcessId)
                .orderByAsc(MesProRouteProcessLossReasonDO::getSort)
                .orderByAsc(MesProRouteProcessLossReasonDO::getId));
    }

    default int deleteByRouteVersionId(Long routeVersionId) {
        return delete(new LambdaQueryWrapperX<MesProRouteProcessLossReasonDO>()
                .eq(MesProRouteProcessLossReasonDO::getRouteVersionId, routeVersionId));
    }

}
