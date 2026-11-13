package cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionReplenishmentListPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionReplenishmentListDO;
import org.apache.ibatis.annotations.Mapper;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;
import org.apache.ibatis.annotations.Delete;

import java.util.Collection;

@Mapper
public interface ErpKingdeeProductionReplenishmentListMapper extends BaseMapperX<ErpKingdeeProductionReplenishmentListDO> {

    @Delete("DELETE FROM erp_kingdee_production_replenishment_list WHERE id = #{id}")
    int hardDeleteById(Long id);

    default PageResult<ErpKingdeeProductionReplenishmentListDO> selectPage(
            ErpProductionReplenishmentListPageReqVO reqVO) {
        return selectPageByProductionReplenishmentListIds(reqVO, null);
    }

    default PageResult<ErpKingdeeProductionReplenishmentListDO> selectPageByProductionReplenishmentListIds(
            ErpProductionReplenishmentListPageReqVO reqVO, Collection<Long> productionReplenishmentListIds) {
        return selectPage(reqVO, apply(reqVO, new LambdaQueryWrapperX<ErpKingdeeProductionReplenishmentListDO>()
                .eqIfPresent(ErpKingdeeProductionReplenishmentListDO::getSourceBillNo, reqVO.getSyncCode())
                .inIfPresent(ErpKingdeeProductionReplenishmentListDO::getId, productionReplenishmentListIds)
                .likeIfPresent(ErpKingdeeProductionReplenishmentListDO::getSourceBillNo,
                        reqVO.getSourceBillNo())
                .eqIfPresent(ErpKingdeeProductionReplenishmentListDO::getDocumentStatus,
                        reqVO.getDocumentStatus())
                .likeIfPresent(ErpKingdeeProductionReplenishmentListDO::getStockOrgName,
                        reqVO.getStockOrgName())
                .likeIfPresent(ErpKingdeeProductionReplenishmentListDO::getProductionOrgName,
                        reqVO.getProductionOrgName())
                .betweenIfPresent(ErpKingdeeProductionReplenishmentListDO::getBillDate,
                        reqVO.getBillDate())
                .orderByDesc(ErpKingdeeProductionReplenishmentListDO::getBillDate)
                .orderByDesc(ErpKingdeeProductionReplenishmentListDO::getId), listFilterFields()));
    }

    default ErpKingdeeProductionReplenishmentListDO selectBySource(String sourceFormId,
                                                           String sourceFid) {
        return selectOne(new LambdaQueryWrapperX<ErpKingdeeProductionReplenishmentListDO>()
                .eq(ErpKingdeeProductionReplenishmentListDO::getSourceFormId, sourceFormId)
                .eq(ErpKingdeeProductionReplenishmentListDO::getSourceFid, sourceFid));
    }


    static java.util.Map<String, cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.Field> listFilterFields() {
        return java.util.Map.ofEntries(
                java.util.Map.entry("id", number("erp_kingdee_production_replenishment_list.id")),
                java.util.Map.entry("sourceFormId", text("erp_kingdee_production_replenishment_list.source_form_id")),
                java.util.Map.entry("sourceFid", text("erp_kingdee_production_replenishment_list.source_fid")),
                java.util.Map.entry("sourceBillNo", text("erp_kingdee_production_replenishment_list.source_bill_no")),
                java.util.Map.entry("billDate", dateTime("erp_kingdee_production_replenishment_list.bill_date")),
                java.util.Map.entry("documentStatus", text("erp_kingdee_production_replenishment_list.document_status")),
                java.util.Map.entry("stockOrgNumber", text("erp_kingdee_production_replenishment_list.stock_org_number")),
                java.util.Map.entry("stockOrgName", text("erp_kingdee_production_replenishment_list.stock_org_name")),
                java.util.Map.entry("productionOrgNumber", text("erp_kingdee_production_replenishment_list.production_org_number")),
                java.util.Map.entry("productionOrgName", text("erp_kingdee_production_replenishment_list.production_org_name")),
                java.util.Map.entry("ownerNumber", text("erp_kingdee_production_replenishment_list.owner_number")),
                java.util.Map.entry("ownerName", text("erp_kingdee_production_replenishment_list.owner_name")),
                java.util.Map.entry("departmentNumber", text("erp_kingdee_production_replenishment_list.department_number")),
                java.util.Map.entry("departmentName", text("erp_kingdee_production_replenishment_list.department_name")),
                java.util.Map.entry("description", text("erp_kingdee_production_replenishment_list.description")),
                java.util.Map.entry("sourceModifyTime", dateTime("erp_kingdee_production_replenishment_list.source_modify_time")),
                java.util.Map.entry("lastSyncTime", dateTime("erp_kingdee_production_replenishment_list.last_sync_time")),
                java.util.Map.entry("createTime", dateTime("erp_kingdee_production_replenishment_list.create_time")),
                java.util.Map.entry("materialNames", text("COALESCE(JSON_UNQUOTE(CONCAT(CHAR(34), REPLACE(SUBSTRING(CAST((SELECT JSON_ARRAYAGG(TRIM(i.material_name)) OVER (ORDER BY i.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM erp_kingdee_production_replenishment_list_item i WHERE i.production_replenishment_list_id=erp_kingdee_production_replenishment_list.id AND i.deleted=0 AND i.tenant_id=erp_kingdee_production_replenishment_list.tenant_id AND i.material_name IS NOT NULL AND TRIM(i.material_name)<>'' AND NOT EXISTS (SELECT 1 FROM erp_kingdee_production_replenishment_list_item earlier WHERE earlier.production_replenishment_list_id=i.production_replenishment_list_id AND earlier.tenant_id=i.tenant_id AND earlier.deleted=0 AND earlier.id<i.id AND BINARY TRIM(earlier.material_name)=BINARY TRIM(i.material_name)) LIMIT 1) AS CHAR), 3, CHAR_LENGTH(CAST((SELECT JSON_ARRAYAGG(TRIM(i.material_name)) OVER (ORDER BY i.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM erp_kingdee_production_replenishment_list_item i WHERE i.production_replenishment_list_id=erp_kingdee_production_replenishment_list.id AND i.deleted=0 AND i.tenant_id=erp_kingdee_production_replenishment_list.tenant_id AND i.material_name IS NOT NULL AND TRIM(i.material_name)<>'' AND NOT EXISTS (SELECT 1 FROM erp_kingdee_production_replenishment_list_item earlier WHERE earlier.production_replenishment_list_id=i.production_replenishment_list_id AND earlier.tenant_id=i.tenant_id AND earlier.deleted=0 AND earlier.id<i.id AND BINARY TRIM(earlier.material_name)=BINARY TRIM(i.material_name)) LIMIT 1) AS CHAR))-4), CONCAT(CHAR(34), ', ', CHAR(34)), '，'), CHAR(34))), '')")),
                java.util.Map.entry("productionOrderNos", text("COALESCE(JSON_UNQUOTE(CONCAT(CHAR(34), REPLACE(SUBSTRING(CAST((SELECT JSON_ARRAYAGG(TRIM(i.production_order_no)) OVER (ORDER BY i.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM erp_kingdee_production_replenishment_list_item i WHERE i.production_replenishment_list_id=erp_kingdee_production_replenishment_list.id AND i.deleted=0 AND i.tenant_id=erp_kingdee_production_replenishment_list.tenant_id AND i.production_order_no IS NOT NULL AND TRIM(i.production_order_no)<>'' AND NOT EXISTS (SELECT 1 FROM erp_kingdee_production_replenishment_list_item earlier WHERE earlier.production_replenishment_list_id=i.production_replenishment_list_id AND earlier.tenant_id=i.tenant_id AND earlier.deleted=0 AND earlier.id<i.id AND BINARY TRIM(earlier.production_order_no)=BINARY TRIM(i.production_order_no)) LIMIT 1) AS CHAR), 3, CHAR_LENGTH(CAST((SELECT JSON_ARRAYAGG(TRIM(i.production_order_no)) OVER (ORDER BY i.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM erp_kingdee_production_replenishment_list_item i WHERE i.production_replenishment_list_id=erp_kingdee_production_replenishment_list.id AND i.deleted=0 AND i.tenant_id=erp_kingdee_production_replenishment_list.tenant_id AND i.production_order_no IS NOT NULL AND TRIM(i.production_order_no)<>'' AND NOT EXISTS (SELECT 1 FROM erp_kingdee_production_replenishment_list_item earlier WHERE earlier.production_replenishment_list_id=i.production_replenishment_list_id AND earlier.tenant_id=i.tenant_id AND earlier.deleted=0 AND earlier.id<i.id AND BINARY TRIM(earlier.production_order_no)=BINARY TRIM(i.production_order_no)) LIMIT 1) AS CHAR))-4), CONCAT(CHAR(34), ', ', CHAR(34)), '，'), CHAR(34))), '')")));
    }
}
