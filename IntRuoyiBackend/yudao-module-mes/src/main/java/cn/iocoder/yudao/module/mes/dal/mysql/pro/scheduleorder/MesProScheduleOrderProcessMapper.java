package cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * MES 排产工单工序快照 Mapper
 */
@Mapper
public interface MesProScheduleOrderProcessMapper extends BaseMapperX<MesProScheduleOrderProcessDO> {

    default List<MesProScheduleOrderProcessDO> selectListByScheduleOrderId(Long scheduleOrderId) {
        return selectList(new LambdaQueryWrapperX<MesProScheduleOrderProcessDO>()
                .eq(MesProScheduleOrderProcessDO::getScheduleOrderId, scheduleOrderId)
                .orderByAsc(MesProScheduleOrderProcessDO::getSort));
    }

    default List<MesProScheduleOrderProcessDO> selectListByScheduleOrderIds(Collection<Long> scheduleOrderIds) {
        if (scheduleOrderIds == null || scheduleOrderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProScheduleOrderProcessDO>()
                .in(MesProScheduleOrderProcessDO::getScheduleOrderId, scheduleOrderIds)
                .orderByAsc(MesProScheduleOrderProcessDO::getScheduleOrderId)
                .orderByAsc(MesProScheduleOrderProcessDO::getSort));
    }

    default List<MesProScheduleOrderProcessDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProScheduleOrderProcessDO>()
                .in(MesProScheduleOrderProcessDO::getId, ids));
    }

    default List<MesProScheduleOrderProcessDO> selectListByProcessIds(Collection<Long> processIds) {
        if (processIds == null || processIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProScheduleOrderProcessDO>()
                .in(MesProScheduleOrderProcessDO::getProcessId, processIds)
                .eq(MesProScheduleOrderProcessDO::getEnabled, Boolean.TRUE)
                .gt(MesProScheduleOrderProcessDO::getRemainingQuantity, BigDecimal.ZERO)
                .orderByAsc(MesProScheduleOrderProcessDO::getSort)
                .orderByDesc(MesProScheduleOrderProcessDO::getId));
    }

    default List<MesProScheduleOrderProcessDO> selectListByProcessIdsOrZeroSnapshots(Collection<Long> processIds) {
        if (processIds == null || processIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProScheduleOrderProcessDO>()
                .and(wrapper -> wrapper.in(MesProScheduleOrderProcessDO::getProcessId, processIds)
                        .or()
                        .eq(MesProScheduleOrderProcessDO::getProcessId, 0L)
                        .isNotNull(MesProScheduleOrderProcessDO::getRouteProcessId))
                .eq(MesProScheduleOrderProcessDO::getEnabled, Boolean.TRUE)
                .gt(MesProScheduleOrderProcessDO::getRemainingQuantity, BigDecimal.ZERO)
                .orderByAsc(MesProScheduleOrderProcessDO::getSort)
                .orderByDesc(MesProScheduleOrderProcessDO::getId));
    }

    default List<MesProScheduleOrderProcessDO> selectListByRouteVersionId(Long routeVersionId) {
        return selectList(new LambdaQueryWrapperX<MesProScheduleOrderProcessDO>()
                .eq(MesProScheduleOrderProcessDO::getRouteVersionId, routeVersionId)
                .orderByAsc(MesProScheduleOrderProcessDO::getScheduleOrderId)
                .orderByAsc(MesProScheduleOrderProcessDO::getSort));
    }

    default int updateProgress(Long id, BigDecimal reportedQuantity, BigDecimal remainingQuantity,
            BigDecimal progressPercent) {
        MesProScheduleOrderProcessDO updateObj = new MesProScheduleOrderProcessDO();
        updateObj.setId(id);
        updateObj.setReportedQuantity(reportedQuantity);
        updateObj.setRemainingQuantity(remainingQuantity);
        updateObj.setProgressPercent(progressPercent);
        return updateById(updateObj);
    }

}
