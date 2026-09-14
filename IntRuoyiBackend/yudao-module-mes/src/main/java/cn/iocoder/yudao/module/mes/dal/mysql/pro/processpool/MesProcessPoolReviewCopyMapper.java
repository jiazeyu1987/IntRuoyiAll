package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolReviewCopyDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

@Mapper
public interface MesProcessPoolReviewCopyMapper extends BaseMapperX<MesProcessPoolReviewCopyDO> {

    default MesProcessPoolReviewCopyDO selectByReviewerSignatureId(Long reviewerSignatureId) {
        if (reviewerSignatureId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolReviewCopyDO>()
                .eq(MesProcessPoolReviewCopyDO::getReviewerSignatureId, reviewerSignatureId));
    }

    default int deleteByEventIds(Collection<Long> eventIds) {
        return eventIds == null || eventIds.isEmpty() ? 0 : physicalDeleteByEventIds(eventIds);
    }

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_review_copy WHERE event_id IN",
            "<foreach collection='eventIds' item='eventId' open='(' separator=',' close=')'>#{eventId}</foreach>",
            "</script>"
    })
    int physicalDeleteByEventIds(@Param("eventIds") Collection<Long> eventIds);
}
