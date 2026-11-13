package cn.iocoder.yudao.module.erp.dal.mysql.stock.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee.ErpKingdeeInventoryListPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.kingdee.ErpKingdeeInventoryListDO;
import org.apache.ibatis.annotations.Mapper;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

@Mapper
public interface ErpKingdeeInventoryListMapper extends BaseMapperX<ErpKingdeeInventoryListDO> {

    default PageResult<ErpKingdeeInventoryListDO> selectPage(ErpKingdeeInventoryListPageReqVO reqVO) {
        return selectPage(reqVO, apply(reqVO, new LambdaQueryWrapperX<ErpKingdeeInventoryListDO>()
                .eqIfPresent(ErpKingdeeInventoryListDO::getMaterialNumber, reqVO.getSyncCode())
                .likeIfPresent(ErpKingdeeInventoryListDO::getMaterialNumber, reqVO.getMaterialNumber())
                .likeIfPresent(ErpKingdeeInventoryListDO::getMaterialName, reqVO.getMaterialName())
                .likeIfPresent(ErpKingdeeInventoryListDO::getWarehouseName, reqVO.getWarehouseName())
                .likeIfPresent(ErpKingdeeInventoryListDO::getLotNumber, reqVO.getLotNumber())
                .orderByDesc(ErpKingdeeInventoryListDO::getSourceModifyTime)
                .orderByDesc(ErpKingdeeInventoryListDO::getId), listFilterFields()));
    }

    default ErpKingdeeInventoryListDO selectBySourceLine(String sourceLineKey) {
        return selectOne(ErpKingdeeInventoryListDO::getSourceLineKey, sourceLineKey);
    }


    static java.util.Map<String, cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.Field> listFilterFields() {
        return java.util.Map.ofEntries(
                java.util.Map.entry("id", number("erp_kingdee_inventory_list.id")),
                java.util.Map.entry("sourceFormId", text("erp_kingdee_inventory_list.source_form_id")),
                java.util.Map.entry("sourceLineKey", text("erp_kingdee_inventory_list.source_line_key")),
                java.util.Map.entry("materialNumber", text("erp_kingdee_inventory_list.material_number")),
                java.util.Map.entry("materialName", text("erp_kingdee_inventory_list.material_name")),
                java.util.Map.entry("materialSpecification", text("erp_kingdee_inventory_list.material_specification")),
                java.util.Map.entry("warehouseNumber", text("erp_kingdee_inventory_list.warehouse_number")),
                java.util.Map.entry("warehouseName", text("erp_kingdee_inventory_list.warehouse_name")),
                java.util.Map.entry("lotNumber", text("erp_kingdee_inventory_list.lot_number")),
                java.util.Map.entry("unitName", text("erp_kingdee_inventory_list.unit_name")),
                java.util.Map.entry("quantity", number("erp_kingdee_inventory_list.quantity")),
                java.util.Map.entry("stockOrgNumber", text("erp_kingdee_inventory_list.stock_org_number")),
                java.util.Map.entry("stockOrgName", text("erp_kingdee_inventory_list.stock_org_name")),
                java.util.Map.entry("sourceModifyTime", dateTime("erp_kingdee_inventory_list.source_modify_time")),
                java.util.Map.entry("lastSyncTime", dateTime("erp_kingdee_inventory_list.last_sync_time")),
                java.util.Map.entry("createTime", dateTime("erp_kingdee_inventory_list.create_time")));
    }
}
