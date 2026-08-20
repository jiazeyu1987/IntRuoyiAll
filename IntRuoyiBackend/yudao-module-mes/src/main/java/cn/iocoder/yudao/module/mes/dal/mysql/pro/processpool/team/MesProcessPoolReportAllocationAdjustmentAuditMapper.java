package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationAdjustmentAuditDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    default int deleteByActiveOrderId(Long activeOrderId) {
        return activeOrderId == null ? 0 : physicalDeleteByActiveOrderId(activeOrderId);
    }

    @Delete("DELETE FROM mes_pro_process_pool_report_allocation_adjustment_audit WHERE active_order_id = #{activeOrderId}")
    int physicalDeleteByActiveOrderId(@Param("activeOrderId") Long activeOrderId);
}
