package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesPqcInspectionTaskMapper extends BaseMapperX<MesPqcInspectionTaskDO> {

    default MesPqcInspectionTaskDO selectPendingByActiveOrderProcess(Long activeOrderId, Long routeProcessId,
                                                                     Long processId) {
        return selectOne(new LambdaQueryWrapperX<MesPqcInspectionTaskDO>()
                .eq(MesPqcInspectionTaskDO::getActiveOrderId, activeOrderId)
                .eq(MesPqcInspectionTaskDO::getRouteProcessId, routeProcessId)
                .eq(MesPqcInspectionTaskDO::getProcessId, processId)
                .eq(MesPqcInspectionTaskDO::getTaskStatus, "PENDING")
                .orderByAsc(MesPqcInspectionTaskDO::getBusinessDate)
                .orderByAsc(MesPqcInspectionTaskDO::getInspectionType)
                .orderByAsc(MesPqcInspectionTaskDO::getRoundNo)
                .last("LIMIT 1"));
    }
}
