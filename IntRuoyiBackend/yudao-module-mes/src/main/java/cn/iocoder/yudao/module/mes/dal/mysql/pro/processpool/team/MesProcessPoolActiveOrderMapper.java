package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProcessPoolActiveOrderMapper extends BaseMapperX<MesProcessPoolActiveOrderDO> {

    default List<MesProcessPoolActiveOrderDO> selectActiveListByLeader(Long leaderUserId) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getLeaderUserId, leaderUserId)
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE")
                .orderByAsc(MesProcessPoolActiveOrderDO::getJoinedAt)
                .orderByAsc(MesProcessPoolActiveOrderDO::getId));
    }

    default List<MesProcessPoolActiveOrderDO> selectActiveList() {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE")
                .orderByDesc(MesProcessPoolActiveOrderDO::getJoinedAt)
                .orderByAsc(MesProcessPoolActiveOrderDO::getId));
    }

    default MesProcessPoolActiveOrderDO selectActiveByWorkOrderAndRoute(Long workOrderId, Long routeId) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getWorkOrderId, workOrderId)
                .eq(MesProcessPoolActiveOrderDO::getRouteId, routeId)
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE"));
    }

    default MesProcessPoolActiveOrderDO selectActiveByWorkOrderRouteVersion(Long workOrderId, Long routeId,
                                                                           Long routeVersionId) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getWorkOrderId, workOrderId)
                .eq(MesProcessPoolActiveOrderDO::getRouteId, routeId)
                .eq(MesProcessPoolActiveOrderDO::getRouteVersionId, routeVersionId)
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE"));
    }
}
