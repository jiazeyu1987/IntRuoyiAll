package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationStateDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProcessPoolReportAllocationStateMapper
        extends BaseMapperX<MesProcessPoolReportAllocationStateDO> {

    default MesProcessPoolReportAllocationStateDO selectByEventId(Long eventId) {
        return eventId == null ? null : selectOne(MesProcessPoolReportAllocationStateDO::getEventId, eventId);
    }

    default MesProcessPoolReportAllocationStateDO selectByEventIdForUpdate(Long eventId) {
        if (eventId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolReportAllocationStateDO>()
                .eq(MesProcessPoolReportAllocationStateDO::getEventId, eventId)
                .last("FOR UPDATE"));
    }
}
