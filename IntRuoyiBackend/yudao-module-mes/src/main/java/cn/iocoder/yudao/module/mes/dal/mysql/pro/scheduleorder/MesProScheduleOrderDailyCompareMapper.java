package cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDailyCompareDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProScheduleOrderDailyCompareMapper extends BaseMapperX<MesProScheduleOrderDailyCompareDO> {

    default List<MesProScheduleOrderDailyCompareDO> selectListByScheduleOrderIdAndDateRange(
            Long scheduleOrderId, LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<MesProScheduleOrderDailyCompareDO>()
                .eq(MesProScheduleOrderDailyCompareDO::getScheduleOrderId, scheduleOrderId)
                .betweenIfPresent(MesProScheduleOrderDailyCompareDO::getPlanDate, new LocalDate[]{startDate, endDate})
                .orderByAsc(MesProScheduleOrderDailyCompareDO::getPlanDate)
                .orderByAsc(MesProScheduleOrderDailyCompareDO::getScheduleOrderProcessId));
    }

    default List<MesProScheduleOrderDailyCompareDO> selectListByProcessIdsAndDateRange(
            Collection<Long> scheduleOrderProcessIds, LocalDate startDate, LocalDate endDate) {
        if (scheduleOrderProcessIds == null || scheduleOrderProcessIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProScheduleOrderDailyCompareDO>()
                .in(MesProScheduleOrderDailyCompareDO::getScheduleOrderProcessId, scheduleOrderProcessIds)
                .betweenIfPresent(MesProScheduleOrderDailyCompareDO::getPlanDate, new LocalDate[]{startDate, endDate})
                .orderByAsc(MesProScheduleOrderDailyCompareDO::getPlanDate)
                .orderByAsc(MesProScheduleOrderDailyCompareDO::getScheduleOrderProcessId));
    }

}
