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

    default List<MesProcessPoolFifoAllocationLineDO> selectListByTargetWorkOrderIdsAndRouteProcessIdForUpdate(
            Collection<Long> targetWorkOrderIds, Long targetRouteProcessId) {
        if (targetWorkOrderIds == null || targetWorkOrderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolFifoAllocationLineDO>()
                .in(MesProcessPoolFifoAllocationLineDO::getTargetWorkOrderId, targetWorkOrderIds)
                .eq(MesProcessPoolFifoAllocationLineDO::getTargetRouteProcessId, targetRouteProcessId)
                .eq(MesProcessPoolFifoAllocationLineDO::getAllocationStatus,
                        MesProcessPoolFifoAllocationLineDO.STATUS_ALLOCATED)
                .orderByAsc(MesProcessPoolFifoAllocationLineDO::getTargetWorkOrderId)
                .orderByAsc(MesProcessPoolFifoAllocationLineDO::getId)
                .last("FOR UPDATE"));
    }

}
