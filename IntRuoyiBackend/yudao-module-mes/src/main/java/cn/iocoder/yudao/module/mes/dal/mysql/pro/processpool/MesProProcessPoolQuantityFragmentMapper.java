package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProProcessPoolQuantityFragmentMapper extends BaseMapperX<MesProProcessPoolQuantityFragmentDO> {

    default List<MesProProcessPoolQuantityFragmentDO> selectListByEventId(Long eventId) {
        return selectList(new LambdaQueryWrapperX<MesProProcessPoolQuantityFragmentDO>()
                .eq(MesProProcessPoolQuantityFragmentDO::getEventId, eventId)
                .orderByAsc(MesProProcessPoolQuantityFragmentDO::getId));
    }

    default List<MesProProcessPoolQuantityFragmentDO> selectListByProductionSubmitEventIdForUpdate(
            Long productionSubmitEventId) {
        return selectList(new LambdaQueryWrapperX<MesProProcessPoolQuantityFragmentDO>()
                .eq(MesProProcessPoolQuantityFragmentDO::getProductionSubmitEventId, productionSubmitEventId)
                .orderByAsc(MesProProcessPoolQuantityFragmentDO::getId)
                .last("FOR UPDATE"));
    }
}
