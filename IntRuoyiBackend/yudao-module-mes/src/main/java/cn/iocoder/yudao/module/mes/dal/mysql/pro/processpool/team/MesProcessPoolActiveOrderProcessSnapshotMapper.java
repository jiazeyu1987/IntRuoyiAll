package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MesProcessPoolActiveOrderProcessSnapshotMapper
        extends BaseMapperX<MesProcessPoolActiveOrderProcessSnapshotDO> {

    default MesProcessPoolActiveOrderProcessSnapshotDO selectByActiveOrderAndProcess(
            Long activeOrderId, Long routeProcessId, Long processId) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderProcessSnapshotDO>()
                .eq(MesProcessPoolActiveOrderProcessSnapshotDO::getActiveOrderId, activeOrderId)
                .eq(MesProcessPoolActiveOrderProcessSnapshotDO::getRouteProcessId, routeProcessId)
                .eq(MesProcessPoolActiveOrderProcessSnapshotDO::getProcessId, processId));
    }

    default List<MesProcessPoolActiveOrderProcessSnapshotDO> selectListByActiveOrderId(Long activeOrderId) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderProcessSnapshotDO>()
                .eq(MesProcessPoolActiveOrderProcessSnapshotDO::getActiveOrderId, activeOrderId)
                .orderByAsc(MesProcessPoolActiveOrderProcessSnapshotDO::getId));
    }

    default List<MesProcessPoolActiveOrderProcessSnapshotDO> selectListByActiveOrderAndProcess(
            Long activeOrderId, Long processId) {
        if (activeOrderId == null || processId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderProcessSnapshotDO>()
                .eq(MesProcessPoolActiveOrderProcessSnapshotDO::getActiveOrderId, activeOrderId)
                .eq(MesProcessPoolActiveOrderProcessSnapshotDO::getProcessId, processId)
                .orderByAsc(MesProcessPoolActiveOrderProcessSnapshotDO::getId));
    }

    default List<MesProcessPoolActiveOrderProcessSnapshotDO> selectListByActiveOrderAndProcessForUpdate(
            Long activeOrderId, Long processId) {
        if (activeOrderId == null || processId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderProcessSnapshotDO>()
                .eq(MesProcessPoolActiveOrderProcessSnapshotDO::getActiveOrderId, activeOrderId)
                .eq(MesProcessPoolActiveOrderProcessSnapshotDO::getProcessId, processId)
                .orderByAsc(MesProcessPoolActiveOrderProcessSnapshotDO::getId)
                .last("FOR UPDATE"));
    }

    default List<MesProcessPoolActiveOrderProcessSnapshotDO> selectListByActiveOrderIdForUpdate(Long activeOrderId) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderProcessSnapshotDO>()
                .eq(MesProcessPoolActiveOrderProcessSnapshotDO::getActiveOrderId, activeOrderId)
                .orderByAsc(MesProcessPoolActiveOrderProcessSnapshotDO::getId)
                .last("FOR UPDATE"));
    }

    default List<MesProcessPoolActiveOrderProcessSnapshotDO> selectListByActiveOrderIds(
            Collection<Long> activeOrderIds) {
        if (activeOrderIds == null || activeOrderIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderProcessSnapshotDO>()
                .in(MesProcessPoolActiveOrderProcessSnapshotDO::getActiveOrderId, activeOrderIds)
                .orderByAsc(MesProcessPoolActiveOrderProcessSnapshotDO::getActiveOrderId)
                .orderByAsc(MesProcessPoolActiveOrderProcessSnapshotDO::getId));
    }

    default int deleteByActiveOrderId(Long activeOrderId) {
        return activeOrderId == null ? 0 : physicalDeleteByActiveOrderId(activeOrderId);
    }

    @Delete("DELETE FROM mes_pro_process_pool_active_order_process_snapshot WHERE active_order_id = #{activeOrderId}")
    int physicalDeleteByActiveOrderId(@Param("activeOrderId") Long activeOrderId);
}
