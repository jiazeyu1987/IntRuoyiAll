package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface MesProProcessPoolQuantityFragmentMapper extends BaseMapperX<MesProProcessPoolQuantityFragmentDO> {

    default List<MesProProcessPoolQuantityFragmentDO> selectListByEventId(Long eventId) {
        return selectList(new LambdaQueryWrapperX<MesProProcessPoolQuantityFragmentDO>()
                .eq(MesProProcessPoolQuantityFragmentDO::getEventId, eventId)
                .orderByAsc(MesProProcessPoolQuantityFragmentDO::getId));
    }

    default List<MesProProcessPoolQuantityFragmentDO> selectAvailableOutputListForUpdate(Long processId) {
        return selectList(new LambdaQueryWrapperX<MesProProcessPoolQuantityFragmentDO>()
                .eq(MesProProcessPoolQuantityFragmentDO::getProcessId, processId)
                .eq(MesProProcessPoolQuantityFragmentDO::getSourceQuantityType, "OUTPUT")
                .eq(MesProProcessPoolQuantityFragmentDO::getAllocationStatus,
                        MesProProcessPoolQuantityFragmentDO.ALLOCATION_STATUS_AVAILABLE)
                .eq(MesProProcessPoolQuantityFragmentDO::getLocked, Boolean.FALSE)
                .gt(MesProProcessPoolQuantityFragmentDO::getAvailableQuantity, BigDecimal.ZERO)
                .orderByAsc(MesProProcessPoolQuantityFragmentDO::getCreateTime)
                .orderByAsc(MesProProcessPoolQuantityFragmentDO::getId)
                .last("FOR UPDATE"));
    }

    default int updateAllocationProgress(Long id, BigDecimal allocatedDelta,
                                         BigDecimal availableQuantity, String allocationStatus) {
        return update(null, new LambdaUpdateWrapper<MesProProcessPoolQuantityFragmentDO>()
                .eq(MesProProcessPoolQuantityFragmentDO::getId, id)
                .eq(MesProProcessPoolQuantityFragmentDO::getLocked, Boolean.FALSE)
                .setSql("allocated_quantity = allocated_quantity + " + allocatedDelta.toPlainString())
                .set(MesProProcessPoolQuantityFragmentDO::getAvailableQuantity, availableQuantity)
                .set(MesProProcessPoolQuantityFragmentDO::getAllocationStatus, allocationStatus));
    }
}
