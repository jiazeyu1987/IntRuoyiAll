package cn.iocoder.yudao.module.mes.dal.mysql.pro.route;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProScheduleResourceAdjustmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MesProScheduleResourceAdjustmentMapper extends BaseMapperX<MesProScheduleResourceAdjustmentDO> {

    default List<MesProScheduleResourceAdjustmentDO> selectListByRouteAndDate(Long routeId, LocalDate calendarDate) {
        return selectList(new LambdaQueryWrapperX<MesProScheduleResourceAdjustmentDO>()
                .eq(MesProScheduleResourceAdjustmentDO::getRouteId, routeId)
                .eq(MesProScheduleResourceAdjustmentDO::getCalendarDate, calendarDate)
                .orderByAsc(MesProScheduleResourceAdjustmentDO::getRouteProcessId)
                .orderByAsc(MesProScheduleResourceAdjustmentDO::getId));
    }

    default MesProScheduleResourceAdjustmentDO selectAdjustment(Long routeProcessId, LocalDate calendarDate,
                                                               String resourceType, Long workstationMachineId,
                                                               Long machineryId) {
        return selectOne(new LambdaQueryWrapperX<MesProScheduleResourceAdjustmentDO>()
                .eq(MesProScheduleResourceAdjustmentDO::getRouteProcessId, routeProcessId)
                .eq(MesProScheduleResourceAdjustmentDO::getCalendarDate, calendarDate)
                .eq(MesProScheduleResourceAdjustmentDO::getResourceType, resourceType)
                .eqIfPresent(MesProScheduleResourceAdjustmentDO::getWorkstationMachineId, workstationMachineId)
                .eqIfPresent(MesProScheduleResourceAdjustmentDO::getMachineryId, machineryId));
    }

}
