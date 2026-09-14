package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MesProcessPoolOrderProcessCompletionMapper
        extends BaseMapperX<MesProcessPoolOrderProcessCompletionDO> {

    default MesProcessPoolOrderProcessCompletionDO selectByWorkOrderAndProcessForUpdate(
            Long workOrderId, Long routeProcessId, Long processId) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolOrderProcessCompletionDO>()
                .eq(MesProcessPoolOrderProcessCompletionDO::getWorkOrderId, workOrderId)
                .eq(MesProcessPoolOrderProcessCompletionDO::getRouteProcessId, routeProcessId)
                .eq(MesProcessPoolOrderProcessCompletionDO::getProcessId, processId)
                .last("FOR UPDATE"));
    }

    default MesProcessPoolOrderProcessCompletionDO selectByWorkOrderAndProcess(
            Long workOrderId, Long routeProcessId, Long processId) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolOrderProcessCompletionDO>()
                .eq(MesProcessPoolOrderProcessCompletionDO::getWorkOrderId, workOrderId)
                .eq(MesProcessPoolOrderProcessCompletionDO::getRouteProcessId, routeProcessId)
                .eq(MesProcessPoolOrderProcessCompletionDO::getProcessId, processId));
    }

    default List<MesProcessPoolOrderProcessCompletionDO> selectListByWorkOrderIds(Collection<Long> workOrderIds) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolOrderProcessCompletionDO>()
                .in(MesProcessPoolOrderProcessCompletionDO::getWorkOrderId, workOrderIds)
                .orderByAsc(MesProcessPoolOrderProcessCompletionDO::getWorkOrderId)
                .orderByAsc(MesProcessPoolOrderProcessCompletionDO::getRouteProcessId)
                .orderByAsc(MesProcessPoolOrderProcessCompletionDO::getProcessId));
    }

    default List<MesProcessPoolOrderProcessCompletionDO> selectListByWorkOrderIdsForUpdate(
            Collection<Long> workOrderIds) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolOrderProcessCompletionDO>()
                .in(MesProcessPoolOrderProcessCompletionDO::getWorkOrderId, workOrderIds)
                .orderByAsc(MesProcessPoolOrderProcessCompletionDO::getWorkOrderId)
                .orderByAsc(MesProcessPoolOrderProcessCompletionDO::getRouteProcessId)
                .orderByAsc(MesProcessPoolOrderProcessCompletionDO::getProcessId)
                .last("FOR UPDATE"));
    }

    default int deleteByWorkOrderId(Long workOrderId) {
        return workOrderId == null ? 0 : physicalDeleteByWorkOrderId(workOrderId);
    }

    @Delete("DELETE FROM mes_pro_process_pool_order_process_completion WHERE work_order_id = #{workOrderId}")
    int physicalDeleteByWorkOrderId(@Param("workOrderId") Long workOrderId);
}
