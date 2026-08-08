package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import org.apache.ibatis.annotations.Mapper;

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
}
