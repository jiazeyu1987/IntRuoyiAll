package cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDiffDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 排产工单差异 Mapper
 */
@Mapper
public interface MesProScheduleOrderDiffMapper extends BaseMapperX<MesProScheduleOrderDiffDO> {

    default List<MesProScheduleOrderDiffDO> selectListByScheduleOrderId(Long scheduleOrderId) {
        return selectList(new LambdaQueryWrapperX<MesProScheduleOrderDiffDO>()
                .eq(MesProScheduleOrderDiffDO::getScheduleOrderId, scheduleOrderId)
                .orderByDesc(MesProScheduleOrderDiffDO::getId));
    }

}
