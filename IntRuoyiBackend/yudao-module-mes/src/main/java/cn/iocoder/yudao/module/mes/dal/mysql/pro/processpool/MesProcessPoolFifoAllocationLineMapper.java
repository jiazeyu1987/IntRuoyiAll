package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolFifoAllocationLineDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProcessPoolFifoAllocationLineMapper
        extends BaseMapperX<MesProcessPoolFifoAllocationLineDO> {

    String LIFECYCLE_CURRENT = "CURRENT";

    default List<MesProcessPoolFifoAllocationLineDO> selectListBySourceQuantityFragmentIdsForUpdate(
            Collection<Long> sourceQuantityFragmentIds) {
        if (sourceQuantityFragmentIds == null || sourceQuantityFragmentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolFifoAllocationLineDO>()
                .in(MesProcessPoolFifoAllocationLineDO::getSourceQuantityFragmentId, sourceQuantityFragmentIds)
                .eq(MesProcessPoolFifoAllocationLineDO::getLifecycleStatus, LIFECYCLE_CURRENT)
                .orderByAsc(MesProcessPoolFifoAllocationLineDO::getSourceQuantityFragmentId)
                .orderByAsc(MesProcessPoolFifoAllocationLineDO::getId)
                .last("FOR UPDATE"));
    }

    default Long selectCountBySourceQuantityFragmentId(Long sourceQuantityFragmentId) {
        if (sourceQuantityFragmentId == null) {
            return 0L;
        }
        return selectCount(new LambdaQueryWrapperX<MesProcessPoolFifoAllocationLineDO>()
                .eq(MesProcessPoolFifoAllocationLineDO::getSourceQuantityFragmentId, sourceQuantityFragmentId)
                .eq(MesProcessPoolFifoAllocationLineDO::getLifecycleStatus, LIFECYCLE_CURRENT));
    }

    default List<MesProcessPoolFifoAllocationLineDO> selectListBySourceEventIdForUpdate(Long sourceEventId) {
        if (sourceEventId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolFifoAllocationLineDO>()
                .eq(MesProcessPoolFifoAllocationLineDO::getSourceEventId, sourceEventId)
                .eq(MesProcessPoolFifoAllocationLineDO::getLifecycleStatus, LIFECYCLE_CURRENT)
                .orderByAsc(MesProcessPoolFifoAllocationLineDO::getId)
                .last("FOR UPDATE"));
    }

    default int supersedeCurrentRows(Collection<Long> ids, Integer supersededVersion) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return update(null, new LambdaUpdateWrapper<MesProcessPoolFifoAllocationLineDO>()
                .in(MesProcessPoolFifoAllocationLineDO::getId, ids)
                .eq(MesProcessPoolFifoAllocationLineDO::getLifecycleStatus, LIFECYCLE_CURRENT)
                .set(MesProcessPoolFifoAllocationLineDO::getLifecycleStatus, "SUPERSEDED")
                .set(MesProcessPoolFifoAllocationLineDO::getSupersededVersion, supersededVersion));
    }

}
