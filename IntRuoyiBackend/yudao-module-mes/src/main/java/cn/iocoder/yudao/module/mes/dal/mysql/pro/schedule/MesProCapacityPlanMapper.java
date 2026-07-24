package cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProCapacityPlanDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProCapacityPlanMapper extends BaseMapperX<MesProCapacityPlanDO> {

    default List<MesProCapacityPlanDO> selectListByLineIdsAndDate(Collection<Long> lineIds, LocalDateTime startDate) {
        if (lineIds == null || lineIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProCapacityPlanDO>()
                .in(MesProCapacityPlanDO::getLineId, lineIds)
                .ge(MesProCapacityPlanDO::getCalendarDate, startDate)
                .eq(MesProCapacityPlanDO::getEnabled, Boolean.TRUE)
                .orderByAsc(MesProCapacityPlanDO::getCalendarDate)
                .orderByAsc(MesProCapacityPlanDO::getShiftId));
    }

    default List<MesProCapacityPlanDO> selectListByLineIdsAndDateRange(Collection<Long> lineIds,
                                                                       LocalDateTime startDate,
                                                                       LocalDateTime endExclusive) {
        if (lineIds == null || lineIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProCapacityPlanDO>()
                .in(MesProCapacityPlanDO::getLineId, lineIds)
                .ge(MesProCapacityPlanDO::getCalendarDate, startDate)
                .lt(MesProCapacityPlanDO::getCalendarDate, endExclusive)
                .orderByAsc(MesProCapacityPlanDO::getCalendarDate)
                .orderByAsc(MesProCapacityPlanDO::getLineId)
                .orderByAsc(MesProCapacityPlanDO::getShiftId));
    }

    default int updateCapacityMinutesByLineAndShiftFromDate(Long lineId,
                                                            Long shiftId,
                                                            LocalDateTime startDate,
                                                            Integer capacityMinutes) {
        return update(null, new LambdaUpdateWrapper<MesProCapacityPlanDO>()
                .eq(MesProCapacityPlanDO::getLineId, lineId)
                .eq(MesProCapacityPlanDO::getShiftId, shiftId)
                .ge(MesProCapacityPlanDO::getCalendarDate, startDate)
                .eq(MesProCapacityPlanDO::getEnabled, Boolean.TRUE)
                .set(MesProCapacityPlanDO::getCapacityMinutes, capacityMinutes));
    }

}
