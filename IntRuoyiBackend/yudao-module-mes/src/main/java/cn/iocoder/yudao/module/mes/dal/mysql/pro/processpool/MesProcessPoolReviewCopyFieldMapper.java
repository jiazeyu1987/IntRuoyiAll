package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolReviewCopyFieldDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MesProcessPoolReviewCopyFieldMapper extends BaseMapperX<MesProcessPoolReviewCopyFieldDO> {

    default List<MesProcessPoolReviewCopyFieldDO> selectListByReviewCopyId(Long reviewCopyId) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolReviewCopyFieldDO>()
                .eq(MesProcessPoolReviewCopyFieldDO::getReviewCopyId, reviewCopyId)
                .orderByAsc(MesProcessPoolReviewCopyFieldDO::getId));
    }

    default int deleteByEventIds(Collection<Long> eventIds) {
        return eventIds == null || eventIds.isEmpty() ? 0 : physicalDeleteByEventIds(eventIds);
    }

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_review_copy_field WHERE event_id IN",
            "<foreach collection='eventIds' item='eventId' open='(' separator=',' close=')'>#{eventId}</foreach>",
            "</script>"
    })
    int physicalDeleteByEventIds(@Param("eventIds") Collection<Long> eventIds);
}
