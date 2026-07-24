package cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderOperationLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 排产工单操作追溯 Mapper
 */
@Mapper
public interface MesProScheduleOrderOperationLogMapper extends BaseMapperX<MesProScheduleOrderOperationLogDO> {

    default List<MesProScheduleOrderOperationLogDO> selectListByScheduleOrderId(Long scheduleOrderId) {
        return selectList(new LambdaQueryWrapperX<MesProScheduleOrderOperationLogDO>()
                .eq(MesProScheduleOrderOperationLogDO::getScheduleOrderId, scheduleOrderId)
                .orderByDesc(MesProScheduleOrderOperationLogDO::getCreateTime)
                .orderByDesc(MesProScheduleOrderOperationLogDO::getId));
    }

    default MesProScheduleOrderOperationLogDO selectLatestByOperationTypes(List<String> operationTypes) {
        return selectOne(new LambdaQueryWrapperX<MesProScheduleOrderOperationLogDO>()
                .in(MesProScheduleOrderOperationLogDO::getOperationType, operationTypes)
                .orderByDesc(MesProScheduleOrderOperationLogDO::getCreateTime)
                .orderByDesc(MesProScheduleOrderOperationLogDO::getId)
                .last("LIMIT 1"));
    }

}
