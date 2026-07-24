package cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleCalendarRuleDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProScheduleCalendarRuleMapper extends BaseMapperX<MesProScheduleCalendarRuleDO> {

    default MesProScheduleCalendarRuleDO selectByTenantId(Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<MesProScheduleCalendarRuleDO>()
                .apply("tenant_id = {0}", tenantId)
                .orderByDesc(MesProScheduleCalendarRuleDO::getId)
                .last("LIMIT 1"));
    }

}
