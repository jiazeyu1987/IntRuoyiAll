package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationStateDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

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

    default int deleteByEventIds(Collection<Long> eventIds) {
        return eventIds == null || eventIds.isEmpty() ? 0 : physicalDeleteByEventIds(eventIds);
    }

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_report_allocation_state WHERE event_id IN",
            "<foreach collection='eventIds' item='eventId' open='(' separator=',' close=')'>#{eventId}</foreach>",
            "</script>"
    })
    int physicalDeleteByEventIds(@Param("eventIds") Collection<Long> eventIds);
}
