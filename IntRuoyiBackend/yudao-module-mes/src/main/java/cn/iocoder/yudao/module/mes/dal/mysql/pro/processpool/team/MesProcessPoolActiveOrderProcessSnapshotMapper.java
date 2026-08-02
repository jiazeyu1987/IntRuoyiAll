package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProcessPoolActiveOrderProcessSnapshotMapper
        extends BaseMapperX<MesProcessPoolActiveOrderProcessSnapshotDO> {

    default MesProcessPoolActiveOrderProcessSnapshotDO selectByActiveOrderAndProcess(
            Long activeOrderId, Long routeProcessId, Long processId) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderProcessSnapshotDO>()
                .eq(MesProcessPoolActiveOrderProcessSnapshotDO::getActiveOrderId, activeOrderId)
                .eq(MesProcessPoolActiveOrderProcessSnapshotDO::getRouteProcessId, routeProcessId)
                .eq(MesProcessPoolActiveOrderProcessSnapshotDO::getProcessId, processId));
    }
}
