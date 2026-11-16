package cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionPickListPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import org.apache.ibatis.annotations.Mapper;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;
import org.apache.ibatis.annotations.Delete;

import java.util.Collection;

@Mapper
public interface ErpKingdeeProductionPickListMapper extends BaseMapperX<ErpKingdeeProductionPickListDO> {

    @Delete("DELETE FROM erp_kingdee_production_pick_list WHERE id = #{id}")
    int hardDeleteById(Long id);

    default PageResult<ErpKingdeeProductionPickListDO> selectPage(
            ErpProductionPickListPageReqVO reqVO) {
        return selectPageByProductionPickListIds(reqVO, null);
    }

    default PageResult<ErpKingdeeProductionPickListDO> selectPageByProductionPickListIds(
            ErpProductionPickListPageReqVO reqVO, Collection<Long> productionPickListIds) {
        return selectPage(reqVO, apply(reqVO, new LambdaQueryWrapperX<ErpKingdeeProductionPickListDO>()
                .eqIfPresent(ErpKingdeeProductionPickListDO::getSourceBillNo, reqVO.getSyncCode())
                .inIfPresent(ErpKingdeeProductionPickListDO::getId, productionPickListIds)
                .likeIfPresent(ErpKingdeeProductionPickListDO::getSourceBillNo,
                        reqVO.getSourceBillNo())
                .eqIfPresent(ErpKingdeeProductionPickListDO::getDocumentStatus,
                        reqVO.getDocumentStatus())
                .likeIfPresent(ErpKingdeeProductionPickListDO::getStockOrgName,
                        reqVO.getStockOrgName())
                .likeIfPresent(ErpKingdeeProductionPickListDO::getProductionOrgName,
                        reqVO.getProductionOrgName())
                .betweenIfPresent(ErpKingdeeProductionPickListDO::getBillDate,
                        reqVO.getBillDate())
                .orderByDesc(ErpKingdeeProductionPickListDO::getBillDate)
                .orderByDesc(ErpKingdeeProductionPickListDO::getId), listFilterFields()));
    }

    default ErpKingdeeProductionPickListDO selectBySource(String sourceFormId,
                                                           String sourceFid) {
        return selectOne(new LambdaQueryWrapperX<ErpKingdeeProductionPickListDO>()
                .eq(ErpKingdeeProductionPickListDO::getSourceFormId, sourceFormId)
                .eq(ErpKingdeeProductionPickListDO::getSourceFid, sourceFid));
    }


    static java.util.Map<String, cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.Field> listFilterFields() {
        return java.util.Map.ofEntries(
                java.util.Map.entry("id", number("erp_kingdee_production_pick_list.id")),
                java.util.Map.entry("sourceFormId", text("erp_kingdee_production_pick_list.source_form_id")),
                java.util.Map.entry("sourceFid", text("erp_kingdee_production_pick_list.source_fid")),
                java.util.Map.entry("sourceBillNo", text("erp_kingdee_production_pick_list.source_bill_no")),
                java.util.Map.entry("billDate", dateTime("erp_kingdee_production_pick_list.bill_date")),
                java.util.Map.entry("documentStatus", text("erp_kingdee_production_pick_list.document_status")),
                java.util.Map.entry("stockOrgNumber", text("erp_kingdee_production_pick_list.stock_org_number")),
                java.util.Map.entry("stockOrgName", text("erp_kingdee_production_pick_list.stock_org_name")),
                java.util.Map.entry("productionOrgNumber", text("erp_kingdee_production_pick_list.production_org_number")),
                java.util.Map.entry("productionOrgName", text("erp_kingdee_production_pick_list.production_org_name")),
                java.util.Map.entry("ownerNumber", text("erp_kingdee_production_pick_list.owner_number")),
                java.util.Map.entry("ownerName", text("erp_kingdee_production_pick_list.owner_name")),
                java.util.Map.entry("departmentNumber", text("erp_kingdee_production_pick_list.department_number")),
                java.util.Map.entry("departmentName", text("erp_kingdee_production_pick_list.department_name")),
                java.util.Map.entry("description", text("erp_kingdee_production_pick_list.description")),
                java.util.Map.entry("sourceModifyTime", dateTime("erp_kingdee_production_pick_list.source_modify_time")),
                java.util.Map.entry("lastSyncTime", dateTime("erp_kingdee_production_pick_list.last_sync_time")),
                java.util.Map.entry("createTime", dateTime("erp_kingdee_production_pick_list.create_time")),
                java.util.Map.entry("materialNames", text("(SELECT COALESCE(JSON_UNQUOTE(CONCAT(CHAR(34), REPLACE(SUBSTRING(CAST(eja.parts AS CHAR), 3, CHAR_LENGTH(CAST(eja.parts AS CHAR))-4), CONCAT(CHAR(34), ', ', CHAR(34)), '，'), CHAR(34))), '') FROM (SELECT (SELECT JSON_ARRAYAGG(TRIM(i.material_name)) OVER (ORDER BY i.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM erp_kingdee_production_pick_list_item i WHERE i.production_pick_list_id=erp_kingdee_production_pick_list.id AND i.deleted=0 AND i.tenant_id=erp_kingdee_production_pick_list.tenant_id AND i.material_name IS NOT NULL AND TRIM(i.material_name)<>'' AND NOT EXISTS (SELECT 1 FROM erp_kingdee_production_pick_list_item earlier WHERE earlier.production_pick_list_id=i.production_pick_list_id AND earlier.tenant_id=i.tenant_id AND earlier.deleted=0 AND earlier.id<i.id AND CAST(TRIM(earlier.material_name) AS BINARY)=CAST(TRIM(i.material_name) AS BINARY)) LIMIT 1) parts) eja)")),
                java.util.Map.entry("productionOrderNos", text("(SELECT COALESCE(JSON_UNQUOTE(CONCAT(CHAR(34), REPLACE(SUBSTRING(CAST(eja.parts AS CHAR), 3, CHAR_LENGTH(CAST(eja.parts AS CHAR))-4), CONCAT(CHAR(34), ', ', CHAR(34)), '，'), CHAR(34))), '') FROM (SELECT (SELECT JSON_ARRAYAGG(TRIM(i.production_order_no)) OVER (ORDER BY i.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM erp_kingdee_production_pick_list_item i WHERE i.production_pick_list_id=erp_kingdee_production_pick_list.id AND i.deleted=0 AND i.tenant_id=erp_kingdee_production_pick_list.tenant_id AND i.production_order_no IS NOT NULL AND TRIM(i.production_order_no)<>'' AND NOT EXISTS (SELECT 1 FROM erp_kingdee_production_pick_list_item earlier WHERE earlier.production_pick_list_id=i.production_pick_list_id AND earlier.tenant_id=i.tenant_id AND earlier.deleted=0 AND earlier.id<i.id AND CAST(TRIM(earlier.production_order_no) AS BINARY)=CAST(TRIM(i.production_order_no) AS BINARY)) LIMIT 1) parts) eja)")));
    }
}
