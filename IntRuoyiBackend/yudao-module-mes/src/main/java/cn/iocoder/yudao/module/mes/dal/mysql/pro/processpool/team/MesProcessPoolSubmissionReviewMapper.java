package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProcessPoolSubmissionReviewMapper extends BaseMapperX<MesProcessPoolSubmissionReviewDO> {

    default List<MesProcessPoolSubmissionReviewDO> selectFormalReviewsByEventIds(Long tenantId,
                                                                               Collection<Long> eventIds) {
        if (eventIds.isEmpty()) return Collections.emptyList();
        return selectList(new LambdaQueryWrapperX<MesProcessPoolSubmissionReviewDO>()
                .eq(MesProcessPoolSubmissionReviewDO::getTenantId, tenantId)
                .in(MesProcessPoolSubmissionReviewDO::getEventId, eventIds));
    }

    default List<MesProcessPoolSubmissionReviewDO> selectProductionReviewsByReviewer(Long tenantId, Long reviewerId) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolSubmissionReviewDO>()
                .eq(MesProcessPoolSubmissionReviewDO::getTenantId, tenantId)
                .eq(MesProcessPoolSubmissionReviewDO::getLeaderType, "PRODUCTION")
                .eq(MesProcessPoolSubmissionReviewDO::getLeaderUserId, reviewerId));
    }

    /** Freeze only the allocation-bound review, without waiting on another writer's lock. */
    default MesProcessPoolSubmissionReviewDO selectByIdForUpdateNowait(Long id) {
        java.util.Objects.requireNonNull(id, "production freeze review id");
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolSubmissionReviewDO>()
                .eq(MesProcessPoolSubmissionReviewDO::getId, id).last("FOR UPDATE NOWAIT"));
    }

    default List<MesProcessPoolSubmissionReviewDO> selectListByEventId(Long eventId) {
        if (eventId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolSubmissionReviewDO>()
                .eq(MesProcessPoolSubmissionReviewDO::getEventId, eventId)
                .orderByAsc(MesProcessPoolSubmissionReviewDO::getId));
    }

    default List<MesProcessPoolSubmissionReviewDO> selectListByEventIdForUpdate(Long eventId) {
        if (eventId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolSubmissionReviewDO>()
                .eq(MesProcessPoolSubmissionReviewDO::getEventId, eventId)
                .orderByAsc(MesProcessPoolSubmissionReviewDO::getId)
                .last("FOR UPDATE"));
    }

    default MesProcessPoolSubmissionReviewDO selectLatestByEventIdForUpdate(Long eventId) {
        if (eventId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolSubmissionReviewDO>()
                .eq(MesProcessPoolSubmissionReviewDO::getEventId, eventId)
                .orderByDesc(MesProcessPoolSubmissionReviewDO::getReviewedAt)
                .orderByDesc(MesProcessPoolSubmissionReviewDO::getId)
                .last("LIMIT 1 FOR UPDATE"));
    }

    default int deleteByEventIds(Collection<Long> eventIds) {
        return eventIds == null || eventIds.isEmpty() ? 0 : physicalDeleteByEventIds(eventIds);
    }

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_submission_review WHERE event_id IN",
            "<foreach collection='eventIds' item='eventId' open='(' separator=',' close=')'>#{eventId}</foreach>",
            "</script>"
    })
    int physicalDeleteByEventIds(@Param("eventIds") Collection<Long> eventIds);
}
