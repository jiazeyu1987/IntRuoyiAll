package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolWorkOrderAbnormalDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MesProcessPoolWorkOrderAbnormalMapper extends BaseMapperX<MesProcessPoolWorkOrderAbnormalDO> {

    default List<MesProcessPoolWorkOrderAbnormalDO> selectListByWorkOrderId(Long workOrderId) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolWorkOrderAbnormalDO>()
                .eq(MesProcessPoolWorkOrderAbnormalDO::getWorkOrderId, workOrderId)
                .orderByAsc(MesProcessPoolWorkOrderAbnormalDO::getMarkedAt)
                .orderByAsc(MesProcessPoolWorkOrderAbnormalDO::getId));
    }

    default List<MesProcessPoolWorkOrderAbnormalDO> selectOpenListByWorkOrderIds(Collection<Long> workOrderIds) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolWorkOrderAbnormalDO>()
                .in(MesProcessPoolWorkOrderAbnormalDO::getWorkOrderId, workOrderIds)
                .in(MesProcessPoolWorkOrderAbnormalDO::getReportStatus,
                        MesProcessPoolWorkOrderAbnormalDO.REPORT_STATUS_REPORTED,
                        MesProcessPoolWorkOrderAbnormalDO.REPORT_STATUS_PROCESSING)
                .orderByDesc(MesProcessPoolWorkOrderAbnormalDO::getReportedAt)
                .orderByDesc(MesProcessPoolWorkOrderAbnormalDO::getId));
    }
}
