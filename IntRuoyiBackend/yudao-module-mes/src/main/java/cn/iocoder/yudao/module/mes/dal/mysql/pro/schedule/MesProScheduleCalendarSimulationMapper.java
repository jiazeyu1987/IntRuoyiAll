package cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleCalendarSimulationDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProScheduleCalendarSimulationMapper extends BaseMapperX<MesProScheduleCalendarSimulationDO> {

    default MesProScheduleCalendarSimulationDO selectByTenantId(Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<MesProScheduleCalendarSimulationDO>()
                .apply("tenant_id = {0}", tenantId)
                .orderByDesc(MesProScheduleCalendarSimulationDO::getId)
                .last("LIMIT 1"));
    }

}
