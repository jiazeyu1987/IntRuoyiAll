package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
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
        List<MesProcessPoolActiveOrderDO> activeOrders = selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getWorkOrderId, workOrderId)
                .eq(MesProcessPoolActiveOrderDO::getRouteId, routeId)
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE")
                .orderByDesc(MesProcessPoolActiveOrderDO::getJoinedAt)
                .orderByDesc(MesProcessPoolActiveOrderDO::getId)
                .last("LIMIT 1"));
        return activeOrders == null || activeOrders.isEmpty() ? null : activeOrders.get(0);
    }

    default MesProcessPoolActiveOrderDO selectActiveByLeaderAndWorkOrderForUpdate(Long leaderUserId,
                                                                                   Long workOrderId) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getLeaderUserId, leaderUserId)
                .eq(MesProcessPoolActiveOrderDO::getWorkOrderId, workOrderId)
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE")
                .last("FOR UPDATE"));
    }

    default MesProcessPoolActiveOrderDO selectActiveByWorkOrderRouteVersion(Long workOrderId, Long routeId,
                                                                           Long routeVersionId) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getWorkOrderId, workOrderId)
                .eq(MesProcessPoolActiveOrderDO::getRouteId, routeId)
                .eq(MesProcessPoolActiveOrderDO::getRouteVersionId, routeVersionId)
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE"));
    }

    default MesProcessPoolActiveOrderDO selectRemovedByWorkOrderRouteVersion(Long workOrderId, Long routeId,
                                                                            Long routeVersionId) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getWorkOrderId, workOrderId)
                .eq(MesProcessPoolActiveOrderDO::getRouteId, routeId)
                .eq(MesProcessPoolActiveOrderDO::getRouteVersionId, routeVersionId)
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "REMOVED")
                .orderByDesc(MesProcessPoolActiveOrderDO::getRemovedAt)
                .orderByDesc(MesProcessPoolActiveOrderDO::getId)
                .last("LIMIT 1"));
    }

    default int reactivateRemovedActiveOrder(Long activeOrderId, Long leaderUserId, Integer version,
                                              LocalDateTime joinedAt) {
        return update(null, new LambdaUpdateWrapper<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getId, activeOrderId)
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "REMOVED")
                .eq(version != null, MesProcessPoolActiveOrderDO::getVersion, version)
                .set(MesProcessPoolActiveOrderDO::getLeaderUserId, leaderUserId)
                .set(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE")
                .set(MesProcessPoolActiveOrderDO::getBusinessStatus, "ACTIVE")
                .set(MesProcessPoolActiveOrderDO::getJoinedAt, joinedAt)
                .set(MesProcessPoolActiveOrderDO::getRemovedAt, null)
                .setSql("version = version + 1"));
    }

    default int removeActiveOrder(Long activeOrderId, Integer version, LocalDateTime removedAt) {
        return update(null, new LambdaUpdateWrapper<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getId, activeOrderId)
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE")
                .eq(version != null, MesProcessPoolActiveOrderDO::getVersion, version)
                .set(MesProcessPoolActiveOrderDO::getActiveStatus, "REMOVED")
                .set(MesProcessPoolActiveOrderDO::getBusinessStatus, "REMOVED")
                .set(MesProcessPoolActiveOrderDO::getRemovedAt, removedAt)
                .setSql("version = version + 1"));
    }
}
