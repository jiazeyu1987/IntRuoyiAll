package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

@Mapper
public interface MesPqcProcessInspectionAggregateDetailMapper
        extends BaseMapperX<MesPqcProcessInspectionAggregateDetailDO> {

    default List<MesPqcProcessInspectionAggregateDetailDO> selectListByEventId(Long eventId) {
        if (eventId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesPqcProcessInspectionAggregateDetailDO>()
                .eq(MesPqcProcessInspectionAggregateDetailDO::getEventId, eventId)
                .orderByAsc(MesPqcProcessInspectionAggregateDetailDO::getSampleNo)
                .orderByAsc(MesPqcProcessInspectionAggregateDetailDO::getItemCode)
                .orderByAsc(MesPqcProcessInspectionAggregateDetailDO::getId));
    }
}
