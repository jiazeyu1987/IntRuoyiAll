package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MesProProcessPoolQuantityFragmentMapper extends BaseMapperX<MesProProcessPoolQuantityFragmentDO> {

    default List<MesProProcessPoolQuantityFragmentDO> selectListByEventId(Long eventId) {
        return selectList(new LambdaQueryWrapperX<MesProProcessPoolQuantityFragmentDO>()
                .eq(MesProProcessPoolQuantityFragmentDO::getEventId, eventId)
                .orderByAsc(MesProProcessPoolQuantityFragmentDO::getId));
    }

    default List<MesProProcessPoolQuantityFragmentDO> selectListByEventIdForUpdate(Long eventId) {
        return selectList(new LambdaQueryWrapperX<MesProProcessPoolQuantityFragmentDO>()
                .eq(MesProProcessPoolQuantityFragmentDO::getEventId, eventId)
                .orderByAsc(MesProProcessPoolQuantityFragmentDO::getId)
                .last("FOR UPDATE"));
    }

    default List<MesProProcessPoolQuantityFragmentDO> selectListByProductionSubmitEventIdForUpdate(
            Long productionSubmitEventId) {
        return selectList(new LambdaQueryWrapperX<MesProProcessPoolQuantityFragmentDO>()
                .eq(MesProProcessPoolQuantityFragmentDO::getProductionSubmitEventId, productionSubmitEventId)
                .orderByAsc(MesProProcessPoolQuantityFragmentDO::getId)
                .last("FOR UPDATE"));
    }

    default List<MesProProcessPoolQuantityFragmentDO> selectOutputListByProductionSubmitEventIdForUpdate(
            Long productionSubmitEventId) {
        if (productionSubmitEventId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProProcessPoolQuantityFragmentDO>()
                .eq(MesProProcessPoolQuantityFragmentDO::getProductionSubmitEventId, productionSubmitEventId)
                .eq(MesProProcessPoolQuantityFragmentDO::getSourceQuantityType,
                        MesProProcessPoolQuantityFragmentDO.SOURCE_QUANTITY_TYPE_OUTPUT)
                .orderByAsc(MesProProcessPoolQuantityFragmentDO::getId)
                .last("FOR UPDATE"));
    }

    default int deleteByEventIds(Collection<Long> eventIds) {
        return eventIds == null || eventIds.isEmpty() ? 0 : physicalDeleteByEventIds(eventIds);
    }

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_quantity_fragment WHERE event_id IN",
            "<foreach collection='eventIds' item='eventId' open='(' separator=',' close=')'>#{eventId}</foreach>",
            "OR production_submit_event_id IN",
            "<foreach collection='eventIds' item='eventId' open='(' separator=',' close=')'>#{eventId}</foreach>",
            "</script>"
    })
    int physicalDeleteByEventIds(@Param("eventIds") Collection<Long> eventIds);
}
