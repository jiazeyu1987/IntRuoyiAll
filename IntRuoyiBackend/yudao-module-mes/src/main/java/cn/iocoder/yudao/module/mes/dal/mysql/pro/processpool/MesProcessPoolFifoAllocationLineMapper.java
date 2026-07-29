package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolFifoAllocationLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProcessPoolFifoAllocationLineMapper
        extends BaseMapperX<MesProcessPoolFifoAllocationLineDO> {

    default List<MesProcessPoolFifoAllocationLineDO> selectListBySourceQuantityFragmentIdsForUpdate(
            Collection<Long> sourceQuantityFragmentIds) {
        if (sourceQuantityFragmentIds == null || sourceQuantityFragmentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolFifoAllocationLineDO>()
                .in(MesProcessPoolFifoAllocationLineDO::getSourceQuantityFragmentId, sourceQuantityFragmentIds)
                .orderByAsc(MesProcessPoolFifoAllocationLineDO::getSourceQuantityFragmentId)
                .orderByAsc(MesProcessPoolFifoAllocationLineDO::getId)
                .last("FOR UPDATE"));
    }

    default Long selectCountBySourceQuantityFragmentId(Long sourceQuantityFragmentId) {
        if (sourceQuantityFragmentId == null) {
            return 0L;
        }
        return selectCount(MesProcessPoolFifoAllocationLineDO::getSourceQuantityFragmentId,
                sourceQuantityFragmentId);
    }

}
