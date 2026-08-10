package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationAdjustmentAuditDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProcessPoolReportAllocationAdjustmentAuditMapper
        extends BaseMapperX<MesProcessPoolReportAllocationAdjustmentAuditDO> {

    default List<MesProcessPoolReportAllocationAdjustmentAuditDO> selectListByEventId(Long eventId) {
        if (eventId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolReportAllocationAdjustmentAuditDO>()
                .eq(MesProcessPoolReportAllocationAdjustmentAuditDO::getEventId, eventId)
                .orderByAsc(MesProcessPoolReportAllocationAdjustmentAuditDO::getAllocationVersion)
                .orderByAsc(MesProcessPoolReportAllocationAdjustmentAuditDO::getId));
    }
}
