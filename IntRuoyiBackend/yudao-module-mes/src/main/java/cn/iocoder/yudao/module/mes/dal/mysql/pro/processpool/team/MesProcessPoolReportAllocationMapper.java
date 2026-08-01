package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProcessPoolReportAllocationMapper extends BaseMapperX<MesProcessPoolReportAllocationDO> {

    default List<MesProcessPoolReportAllocationDO> selectListByEventId(Long eventId) {
        if (eventId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolReportAllocationDO>()
                .eq(MesProcessPoolReportAllocationDO::getEventId, eventId)
                .orderByAsc(MesProcessPoolReportAllocationDO::getId));
    }

    default List<MesProcessPoolReportAllocationDO> selectListByTrace(Long eventId, Long workOrderId,
                                                                     Long routeProcessId, Long processId) {
        if (eventId == null || workOrderId == null || routeProcessId == null || processId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolReportAllocationDO>()
                .eq(MesProcessPoolReportAllocationDO::getEventId, eventId)
                .eq(MesProcessPoolReportAllocationDO::getWorkOrderId, workOrderId)
                .eq(MesProcessPoolReportAllocationDO::getRouteProcessId, routeProcessId)
                .eq(MesProcessPoolReportAllocationDO::getProcessId, processId)
                .orderByAsc(MesProcessPoolReportAllocationDO::getId));
    }

    default List<MesProcessPoolReportAllocationDO> selectListByWorkOrderIdsAndProcessForUpdate(
            Collection<Long> workOrderIds, Long routeProcessId, Long processId) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolReportAllocationDO>()
                .in(MesProcessPoolReportAllocationDO::getWorkOrderId, workOrderIds)
                .eq(MesProcessPoolReportAllocationDO::getRouteProcessId, routeProcessId)
                .eq(MesProcessPoolReportAllocationDO::getProcessId, processId)
                .last("FOR UPDATE"));
    }
}
