package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProcessPoolSubmissionReviewMapper extends BaseMapperX<MesProcessPoolSubmissionReviewDO> {

    default List<MesProcessPoolSubmissionReviewDO> selectListByEventId(Long eventId) {
        if (eventId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolSubmissionReviewDO>()
                .eq(MesProcessPoolSubmissionReviewDO::getEventId, eventId)
                .orderByAsc(MesProcessPoolSubmissionReviewDO::getId));
    }
}
