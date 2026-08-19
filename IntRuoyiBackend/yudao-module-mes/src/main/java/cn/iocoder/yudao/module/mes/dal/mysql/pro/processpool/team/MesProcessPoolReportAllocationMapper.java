package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
                .eq(MesProcessPoolReportAllocationDO::getLifecycleStatus,
                        MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT)
                .orderByAsc(MesProcessPoolReportAllocationDO::getId));
    }

    default List<MesProcessPoolReportAllocationDO> selectListByEventIdForUpdate(Long eventId) {
        if (eventId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolReportAllocationDO>()
                .eq(MesProcessPoolReportAllocationDO::getEventId, eventId)
                .eq(MesProcessPoolReportAllocationDO::getLifecycleStatus,
                        MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT)
                .orderByAsc(MesProcessPoolReportAllocationDO::getId)
                .last("FOR UPDATE"));
    }

    default List<MesProcessPoolReportAllocationDO> selectAllListByEventIdForUpdate(Long eventId) {
        if (eventId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolReportAllocationDO>()
                .eq(MesProcessPoolReportAllocationDO::getEventId, eventId)
                .orderByAsc(MesProcessPoolReportAllocationDO::getId)
                .last("FOR UPDATE"));
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
                .eq(MesProcessPoolReportAllocationDO::getLifecycleStatus,
                        MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT)
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
                .eq(MesProcessPoolReportAllocationDO::getLifecycleStatus,
                        MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT)
                .last("FOR UPDATE"));
    }

    default List<MesProcessPoolReportAllocationDO> selectListByActiveOrderIdsAndProcessForUpdate(
            Collection<Long> activeOrderIds, Long processId) {
        if (activeOrderIds == null || activeOrderIds.isEmpty() || processId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolReportAllocationDO>()
                .in(MesProcessPoolReportAllocationDO::getActiveOrderId, activeOrderIds)
                .eq(MesProcessPoolReportAllocationDO::getProcessId, processId)
                .eq(MesProcessPoolReportAllocationDO::getLifecycleStatus,
                        MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT)
                .orderByAsc(MesProcessPoolReportAllocationDO::getActiveOrderId)
                .orderByAsc(MesProcessPoolReportAllocationDO::getId)
                .last("FOR UPDATE"));
    }

    default List<MesProcessPoolReportAllocationDO> selectListByActiveOrderIdForUpdate(Long activeOrderId) {
        if (activeOrderId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolReportAllocationDO>()
                .eq(MesProcessPoolReportAllocationDO::getActiveOrderId, activeOrderId)
                .eq(MesProcessPoolReportAllocationDO::getLifecycleStatus,
                        MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT)
                .orderByAsc(MesProcessPoolReportAllocationDO::getId)
                .last("FOR UPDATE"));
    }

    default List<MesProcessPoolReportAllocationDO> selectAllListByActiveOrderIdForUpdate(Long activeOrderId) {
        if (activeOrderId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolReportAllocationDO>()
                .eq(MesProcessPoolReportAllocationDO::getActiveOrderId, activeOrderId)
                .orderByAsc(MesProcessPoolReportAllocationDO::getId)
                .last("FOR UPDATE"));
    }

    default List<MesProcessPoolReportAllocationDO> selectListByActiveOrderIds(Collection<Long> activeOrderIds) {
        if (activeOrderIds == null || activeOrderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolReportAllocationDO>()
                .in(MesProcessPoolReportAllocationDO::getActiveOrderId, activeOrderIds)
                .eq(MesProcessPoolReportAllocationDO::getLifecycleStatus,
                        MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT)
                .orderByAsc(MesProcessPoolReportAllocationDO::getActiveOrderId)
                .orderByAsc(MesProcessPoolReportAllocationDO::getRouteProcessId)
                .orderByAsc(MesProcessPoolReportAllocationDO::getProcessId)
                .orderByAsc(MesProcessPoolReportAllocationDO::getId));
    }

    default List<MesProcessPoolReportAllocationDO> selectListByActiveOrderIdsAndProcess(
            Collection<Long> activeOrderIds, Long processId) {
        if (activeOrderIds == null || activeOrderIds.isEmpty() || processId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolReportAllocationDO>()
                .in(MesProcessPoolReportAllocationDO::getActiveOrderId, activeOrderIds)
                .eq(MesProcessPoolReportAllocationDO::getProcessId, processId)
                .eq(MesProcessPoolReportAllocationDO::getLifecycleStatus,
                        MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT)
                .orderByAsc(MesProcessPoolReportAllocationDO::getActiveOrderId)
                .orderByAsc(MesProcessPoolReportAllocationDO::getId));
    }

    default int supersedeCurrentRows(Collection<Long> ids, Integer supersededVersion) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return update(null, new LambdaUpdateWrapper<MesProcessPoolReportAllocationDO>()
                .in(MesProcessPoolReportAllocationDO::getId, ids)
                .eq(MesProcessPoolReportAllocationDO::getLifecycleStatus,
                        MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT)
                .set(MesProcessPoolReportAllocationDO::getLifecycleStatus,
                        MesProcessPoolReportAllocationDO.LIFECYCLE_SUPERSEDED)
                .set(MesProcessPoolReportAllocationDO::getSupersededVersion, supersededVersion));
    }

    default int deleteAllByActiveOrderId(Long activeOrderId) {
        return activeOrderId == null ? 0 : physicalDeleteAllByActiveOrderId(activeOrderId);
    }

    @Delete("DELETE FROM mes_pro_process_pool_report_allocation WHERE active_order_id = #{activeOrderId}")
    int physicalDeleteAllByActiveOrderId(@Param("activeOrderId") Long activeOrderId);
}
