package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Collection;

import java.util.List;

@Mapper
public interface MesProEdhrBatchExecutionOriginMapper extends BaseMapperX<MesProEdhrBatchExecutionOriginDO> {

    @Data
    class HistoryTimes {
        private Long batchExecutionId;
        private Long activeOrderId;
        private Long resolvedActiveOrderId;
        private LocalDateTime startedAt;
        private LocalDateTime pqcReleasedAt;
    }

    // 历史来源允许读取已移出的活跃订单；只按正式来源身份关联，禁止按工单/批号推断。
    @Select("""
            <script>
            SELECT DISTINCT o.batch_execution_id, o.active_order_id,
                   a.id AS resolved_active_order_id, a.joined_at AS started_at,
                   (SELECT MAX(p.pqc_decided_at)
                    FROM mes_pro_process_pool_active_order_release_application p
                    WHERE p.tenant_id = o.tenant_id AND p.deleted = 0
                      AND p.batch_execution_id = o.batch_execution_id
                      AND p.active_order_id = o.active_order_id AND p.work_order_id = o.work_order_id
                      AND p.pqc_decision = 'APPROVE') AS pqc_released_at
            FROM mes_pro_edhr_batch_execution_origin o
            LEFT JOIN mes_pro_process_pool_active_order a
              ON a.id = o.active_order_id AND a.tenant_id = o.tenant_id AND a.work_order_id = o.work_order_id
            WHERE o.tenant_id = #{tenantId} AND o.deleted = 0 AND o.active_order_id IS NOT NULL
              AND o.batch_execution_id IN
              <foreach collection="batchIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<HistoryTimes> selectHistoryTimes(@Param("tenantId") Long tenantId, @Param("batchIds") Collection<Long> batchIds);

    default MesProEdhrBatchExecutionOriginDO selectByBatchAndOriginKey(Long batchExecutionId, String originKey) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrBatchExecutionOriginDO>()
                .eq(MesProEdhrBatchExecutionOriginDO::getBatchExecutionId, batchExecutionId)
                .eq(MesProEdhrBatchExecutionOriginDO::getOriginKey, originKey));
    }

    default List<MesProEdhrBatchExecutionOriginDO> selectListByBatchExecutionId(Long batchExecutionId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrBatchExecutionOriginDO>()
                .eq(MesProEdhrBatchExecutionOriginDO::getBatchExecutionId, batchExecutionId)
                .orderByAsc(MesProEdhrBatchExecutionOriginDO::getId));
    }

    default List<MesProEdhrBatchExecutionOriginDO> selectListByTraceFilter(Long activeOrderId,
                                                                             Long workOrderId,
                                                                             Long pickListId,
                                                                             String entryType) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrBatchExecutionOriginDO>()
                .eqIfPresent(MesProEdhrBatchExecutionOriginDO::getActiveOrderId, activeOrderId)
                .eqIfPresent(MesProEdhrBatchExecutionOriginDO::getWorkOrderId, workOrderId)
                .eqIfPresent(MesProEdhrBatchExecutionOriginDO::getPickListId, pickListId)
                .eqIfPresent(MesProEdhrBatchExecutionOriginDO::getEntryType, entryType)
                .orderByDesc(MesProEdhrBatchExecutionOriginDO::getId));
    }
}
