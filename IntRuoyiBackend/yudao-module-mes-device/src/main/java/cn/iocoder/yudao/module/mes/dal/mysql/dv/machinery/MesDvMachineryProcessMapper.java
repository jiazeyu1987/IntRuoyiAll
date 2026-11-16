package cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesDvMachineryProcessMapper extends BaseMapperX<MesDvMachineryProcessDO> {

    default List<MesDvMachineryProcessDO> selectListByMachineryId(Long machineryId) {
        return selectList(new LambdaQueryWrapperX<MesDvMachineryProcessDO>()
                .eq(MesDvMachineryProcessDO::getMachineryId, machineryId)
                .orderByAsc(MesDvMachineryProcessDO::getSourceRowNo)
                .orderByAsc(MesDvMachineryProcessDO::getId));
    }

    default List<MesDvMachineryProcessDO> selectListByProcessIds(Collection<Long> processIds) {
        if (processIds == null || processIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesDvMachineryProcessDO>()
                .in(MesDvMachineryProcessDO::getProcessId, processIds)
                .orderByAsc(MesDvMachineryProcessDO::getSourceRowNo)
                .orderByAsc(MesDvMachineryProcessDO::getId));
    }

    default List<MesDvMachineryProcessDO> selectListByProcessId(Long processId) {
        if (processId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesDvMachineryProcessDO>()
                .eq(MesDvMachineryProcessDO::getProcessId, processId)
                .orderByAsc(MesDvMachineryProcessDO::getSourceRowNo)
                .orderByAsc(MesDvMachineryProcessDO::getId));
    }

    default void deleteByMachineryIds(Collection<Long> machineryIds) {
        if (machineryIds == null || machineryIds.isEmpty()) {
            return;
        }
        delete(new LambdaQueryWrapperX<MesDvMachineryProcessDO>()
                .in(MesDvMachineryProcessDO::getMachineryId, machineryIds));
    }

    default List<MesDvMachineryProcessDO> selectListByMachineryIds(Collection<Long> machineryIds) {
        if (machineryIds == null || machineryIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesDvMachineryProcessDO>()
                .in(MesDvMachineryProcessDO::getMachineryId, machineryIds));
    }

    default void updateCapacityByMachineryIdAndProcessId(Long machineryId, Long processId,
                                                         BigDecimal standardHourlyCapacity,
                                                         BigDecimal tenHalfHourDailyCapacity) {
        update(null, new LambdaUpdateWrapper<MesDvMachineryProcessDO>()
                .eq(MesDvMachineryProcessDO::getMachineryId, machineryId)
                .eq(MesDvMachineryProcessDO::getProcessId, processId)
                .set(MesDvMachineryProcessDO::getStandardHourlyCapacity, standardHourlyCapacity)
                .set(MesDvMachineryProcessDO::getTenHalfHourDailyCapacity, tenHalfHourDailyCapacity));
    }
}
