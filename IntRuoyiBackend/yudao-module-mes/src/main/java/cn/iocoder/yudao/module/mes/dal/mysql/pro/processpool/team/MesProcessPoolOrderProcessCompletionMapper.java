package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import org.apache.ibatis.annotations.Mapper;

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
}
