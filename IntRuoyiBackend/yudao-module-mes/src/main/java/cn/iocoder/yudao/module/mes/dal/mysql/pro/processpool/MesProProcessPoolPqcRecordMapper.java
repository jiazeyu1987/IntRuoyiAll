package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface MesProProcessPoolPqcRecordMapper extends BaseMapperX<MesProProcessPoolPqcRecordDO> {

    default MesProProcessPoolPqcRecordDO selectByEventId(Long eventId) {
        return selectOne(new LambdaQueryWrapperX<MesProProcessPoolPqcRecordDO>()
                .eq(MesProProcessPoolPqcRecordDO::getEventId, eventId));
    }

    default List<MesProProcessPoolPqcRecordDO> selectListByProductionSubmitEventId(Long productionSubmitEventId) {
        return selectList(new LambdaQueryWrapperX<MesProProcessPoolPqcRecordDO>()
                .eq(MesProProcessPoolPqcRecordDO::getProductionSubmitEventId, productionSubmitEventId)
                .orderByAsc(MesProProcessPoolPqcRecordDO::getId));
    }

    default int updateProcessInspectionAggregatedIfPending(Long tenantId, Long eventId, Long reviewId,
                                                           LocalDateTime aggregatedAt) {
        return update(null, new LambdaUpdateWrapper<MesProProcessPoolPqcRecordDO>()
                .set(MesProProcessPoolPqcRecordDO::getProcessInspectionAggregationStatus,
                        MesProProcessPoolPqcRecordDO.PROCESS_INSPECTION_AGGREGATION_STATUS_AGGREGATED)
                .set(MesProProcessPoolPqcRecordDO::getProcessInspectionReviewId, reviewId)
                .set(MesProProcessPoolPqcRecordDO::getProcessInspectionAggregatedAt, aggregatedAt)
                .eq(MesProProcessPoolPqcRecordDO::getTenantId, tenantId)
                .eq(MesProProcessPoolPqcRecordDO::getEventId, eventId)
                .eq(MesProProcessPoolPqcRecordDO::getProcessInspectionAggregationStatus,
                        MesProProcessPoolPqcRecordDO.PROCESS_INSPECTION_AGGREGATION_STATUS_PENDING));
    }

    default int updateProcessInspectionAggregated(Long tenantId, Long eventId, Long reviewId,
                                                  LocalDateTime aggregatedAt) {
        return update(null, new LambdaUpdateWrapper<MesProProcessPoolPqcRecordDO>()
                .set(MesProProcessPoolPqcRecordDO::getProcessInspectionAggregationStatus,
                        MesProProcessPoolPqcRecordDO.PROCESS_INSPECTION_AGGREGATION_STATUS_AGGREGATED)
                .set(MesProProcessPoolPqcRecordDO::getProcessInspectionReviewId, reviewId)
                .set(MesProProcessPoolPqcRecordDO::getProcessInspectionAggregatedAt, aggregatedAt)
                .eq(MesProProcessPoolPqcRecordDO::getTenantId, tenantId)
                .eq(MesProProcessPoolPqcRecordDO::getEventId, eventId));
    }

    default int deleteByEventIds(Collection<Long> eventIds) {
        return eventIds == null || eventIds.isEmpty() ? 0 : physicalDeleteByEventIds(eventIds);
    }

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_pqc_record WHERE event_id IN",
            "<foreach collection='eventIds' item='eventId' open='(' separator=',' close=')'>#{eventId}</foreach>",
            "</script>"
    })
    int physicalDeleteByEventIds(@Param("eventIds") Collection<Long> eventIds);
}
