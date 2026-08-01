package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProProcessPoolMapper extends BaseMapperX<MesProProcessPoolDO> {

    default List<MesProProcessPoolDO> selectActiveList() {
        return selectList(new LambdaQueryWrapperX<MesProProcessPoolDO>()
                .eq(MesProProcessPoolDO::getPoolStatus, MesProProcessPoolDO.STATUS_ACTIVE)
                .orderByDesc(MesProProcessPoolDO::getLatestSubmitTime)
                .orderByDesc(MesProProcessPoolDO::getId));
    }

    default MesProProcessPoolDO selectActiveByWorkOrderAndRoute(Long workOrderId, Long routeId) {
        return selectOne(new LambdaQueryWrapperX<MesProProcessPoolDO>()
                .eq(MesProProcessPoolDO::getWorkOrderId, workOrderId)
                .eq(MesProProcessPoolDO::getRouteId, routeId)
                .eq(MesProProcessPoolDO::getPoolStatus, MesProProcessPoolDO.STATUS_ACTIVE)
                .orderByDesc(MesProProcessPoolDO::getLatestSubmitTime)
                .orderByDesc(MesProProcessPoolDO::getId)
                .last("LIMIT 1"));
    }

    default MesProProcessPoolDO selectActiveByWorkOrderRouteProcess(Long workOrderId, Long routeId,
                                                                    Long routeProcessId, Long processId) {
        return selectOne(new LambdaQueryWrapperX<MesProProcessPoolDO>()
                .eq(MesProProcessPoolDO::getWorkOrderId, workOrderId)
                .eq(MesProProcessPoolDO::getRouteId, routeId)
                .eq(MesProProcessPoolDO::getRouteProcessId, routeProcessId)
                .eq(MesProProcessPoolDO::getProcessId, processId)
                .eq(MesProProcessPoolDO::getPoolStatus, MesProProcessPoolDO.STATUS_ACTIVE)
                .orderByDesc(MesProProcessPoolDO::getLatestSubmitTime)
                .orderByDesc(MesProProcessPoolDO::getId)
                .last("LIMIT 1"));
    }

    default MesProProcessPoolDO selectByContext(Long workOrderId, Long routeId, Long routeProcessId,
                                                Long processId, Long deviceId, Long workstationId) {
        return selectOne(new LambdaQueryWrapperX<MesProProcessPoolDO>()
                .eq(MesProProcessPoolDO::getWorkOrderId, workOrderId)
                .eq(MesProProcessPoolDO::getRouteId, routeId)
                .eq(MesProProcessPoolDO::getRouteProcessId, routeProcessId)
                .eq(MesProProcessPoolDO::getProcessId, processId)
                .eq(MesProProcessPoolDO::getDeviceId, deviceId)
                .eq(MesProProcessPoolDO::getWorkstationId, workstationId));
    }
}
