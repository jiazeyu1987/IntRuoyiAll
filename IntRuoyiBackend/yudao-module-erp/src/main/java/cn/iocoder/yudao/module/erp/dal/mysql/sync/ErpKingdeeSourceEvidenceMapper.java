package cn.iocoder.yudao.module.erp.dal.mysql.sync;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface ErpKingdeeSourceEvidenceMapper {
    @Select("SELECT EXISTS (SELECT 1 FROM erp_product WHERE tenant_id = #{tenantId}) OR EXISTS (SELECT 1 FROM erp_kingdee_event_callback WHERE tenant_id = #{tenantId}) OR EXISTS (SELECT 1 FROM erp_kingdee_customer_sync_record WHERE tenant_id = #{tenantId}) OR EXISTS (SELECT 1 FROM erp_kingdee_inventory_list WHERE tenant_id = #{tenantId}) OR EXISTS (SELECT 1 FROM erp_kingdee_production_pick_list WHERE tenant_id = #{tenantId}) OR EXISTS (SELECT 1 FROM erp_kingdee_production_pick_list_item WHERE tenant_id = #{tenantId}) OR EXISTS (SELECT 1 FROM erp_kingdee_production_replenishment_list WHERE tenant_id = #{tenantId}) OR EXISTS (SELECT 1 FROM erp_kingdee_production_replenishment_list_item WHERE tenant_id = #{tenantId}) OR EXISTS (SELECT 1 FROM erp_kingdee_purchase_order_sync_record WHERE tenant_id = #{tenantId}) OR EXISTS (SELECT 1 FROM erp_kingdee_sale_order_sync_record WHERE tenant_id = #{tenantId}) OR EXISTS (SELECT 1 FROM erp_kingdee_stock_move WHERE tenant_id = #{tenantId}) OR EXISTS (SELECT 1 FROM erp_kingdee_stock_move_item WHERE tenant_id = #{tenantId}) OR EXISTS (SELECT 1 FROM erp_kingdee_supplier_sync_record WHERE tenant_id = #{tenantId}) OR EXISTS (SELECT 1 FROM erp_kingdee_sync_run WHERE tenant_id = #{tenantId}) OR EXISTS (SELECT 1 FROM erp_kingdee_sync_watermark WHERE tenant_id = #{tenantId}) OR EXISTS (SELECT 1 FROM erp_kingdee_warehouse_sync_record WHERE tenant_id = #{tenantId})")
    boolean hasEvidence(@Param("tenantId") Long tenantId);
}
