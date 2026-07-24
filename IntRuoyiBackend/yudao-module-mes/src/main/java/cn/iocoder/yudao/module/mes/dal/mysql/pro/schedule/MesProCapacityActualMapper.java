package cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProCapacityActualDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProCapacityActualMapper extends BaseMapperX<MesProCapacityActualDO> {

    default List<MesProCapacityActualDO> selectListByLineIdsAndDate(Collection<Long> lineIds, LocalDateTime startDate) {
        if (lineIds == null || lineIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProCapacityActualDO>()
                .in(MesProCapacityActualDO::getLineId, lineIds)
                .ge(MesProCapacityActualDO::getCalendarDate, startDate)
                .eq(MesProCapacityActualDO::getEnabled, Boolean.TRUE)
                .orderByAsc(MesProCapacityActualDO::getCalendarDate)
                .orderByAsc(MesProCapacityActualDO::getShiftId));
    }

}
