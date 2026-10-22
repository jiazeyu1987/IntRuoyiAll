package cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 清理仅限无下游引用的工单；历史引用（包括逻辑删除记录）也必须保留。 */
@Mapper
public interface MesProWorkOrderErpCleanupMapper {
    @Select("""
        SELECT CASE
          WHEN EXISTS (SELECT 1 FROM mes_pro_schedule_order WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已进入排产，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_pro_task WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已有生产任务，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_pro_process_pool_active_order WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已进入活跃订单流程，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_pro_process_pool WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已有工序执行记录，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_pro_feedback WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已有生产报工记录，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_pro_andon_record WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已有安灯呼叫记录，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_independent_batch_prerequisite_receipt WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已有批次准入凭据，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_pro_edhr_batch_execution WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已进入批次执行流程，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_pro_batch_record_execution WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已有批记录，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_qc_ipqc WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已有过程检验记录，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_wm_product_issue WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已有生产领料记录，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_wm_product_produce WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已有产品产出记录，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_wm_item_consume WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已有物料消耗记录，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_wm_product_receipt WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已有产品入库记录，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_wm_outsource_issue WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已有外协发料记录，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_wm_outsource_receipt WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已有外协入库记录，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_wm_return_issue WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已有生产退料记录，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_wm_package_line WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已有包装记录，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_wm_batch WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已有批次追溯记录，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_wm_sn WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已有序列号追溯记录，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_pro_card WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}) THEN '已有生产流转卡，保留工单'
          WHEN EXISTS (SELECT 1 FROM mes_pro_work_order WHERE tenant_id = #{tenantId} AND parent_id = #{workOrderId}) THEN '已有子工单，保留工单'
          ELSE NULL
        END
        """)
    String selectRetainReason(@Param("tenantId") Long tenantId, @Param("workOrderId") Long workOrderId);

    // 释放正式来源唯一键，避免未来同一 ERP 订单恢复后无法重新同步；业务工单仍由逻辑删除保留。
    @Delete("DELETE FROM mes_kingdee_production_order_sync_record WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}")
    int deleteSyncRecords(@Param("tenantId") Long tenantId, @Param("workOrderId") Long workOrderId);

    @Delete("DELETE FROM mes_kingdee_production_material_list WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}")
    int deleteMaterialSnapshots(@Param("tenantId") Long tenantId, @Param("workOrderId") Long workOrderId);

    @Delete("DELETE FROM mes_pro_schedule_issue WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}")
    int deleteScheduleIssues(@Param("tenantId") Long tenantId, @Param("workOrderId") Long workOrderId);
}
