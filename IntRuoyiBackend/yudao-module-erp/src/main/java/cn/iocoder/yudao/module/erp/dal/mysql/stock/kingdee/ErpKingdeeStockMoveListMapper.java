package cn.iocoder.yudao.module.erp.dal.mysql.stock.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee.move.ErpKingdeeStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.kingdee.ErpKingdeeStockMoveListDO;
import org.apache.ibatis.annotations.Mapper;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

@Mapper
public interface ErpKingdeeStockMoveListMapper extends BaseMapperX<ErpKingdeeStockMoveListDO> {

    default PageResult<ErpKingdeeStockMoveListDO> selectPage(ErpKingdeeStockMovePageReqVO reqVO) {
        return selectPage(reqVO, apply(reqVO, new LambdaQueryWrapperX<ErpKingdeeStockMoveListDO>()
                .likeIfPresent(ErpKingdeeStockMoveListDO::getSourceBillNo, reqVO.getSourceBillNo())
                .eqIfPresent(ErpKingdeeStockMoveListDO::getDocumentStatus, reqVO.getDocumentStatus())
                .likeIfPresent(ErpKingdeeStockMoveListDO::getTransferDirect, reqVO.getTransferDirect())
                .betweenIfPresent(ErpKingdeeStockMoveListDO::getBillDate, reqVO.getBillDate())
                .orderByDesc(ErpKingdeeStockMoveListDO::getBillDate)
                .orderByDesc(ErpKingdeeStockMoveListDO::getId), listFilterFields()));
    }

    default ErpKingdeeStockMoveListDO selectBySource(String sourceFormId, String sourceFid) {
        return selectOne(new LambdaQueryWrapperX<ErpKingdeeStockMoveListDO>()
                .eq(ErpKingdeeStockMoveListDO::getSourceFormId, sourceFormId)
                .eq(ErpKingdeeStockMoveListDO::getSourceFid, sourceFid));
    }


    static java.util.Map<String, cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.Field> listFilterFields() {
        return java.util.Map.ofEntries(
                java.util.Map.entry("id", number("erp_kingdee_stock_move.id")),
                java.util.Map.entry("sourceFormId", text("erp_kingdee_stock_move.source_form_id")),
                java.util.Map.entry("sourceFid", text("erp_kingdee_stock_move.source_fid")),
                java.util.Map.entry("sourceBillNo", text("erp_kingdee_stock_move.source_bill_no")),
                java.util.Map.entry("billDate", dateTime("erp_kingdee_stock_move.bill_date")),
                java.util.Map.entry("documentStatus", text("erp_kingdee_stock_move.document_status")),
                java.util.Map.entry("transferDirect", text("erp_kingdee_stock_move.transfer_direct")),
                java.util.Map.entry("transferBizType", text("erp_kingdee_stock_move.transfer_biz_type")),
                java.util.Map.entry("remark", text("erp_kingdee_stock_move.remark")),
                java.util.Map.entry("sourceModifyTime", dateTime("erp_kingdee_stock_move.source_modify_time")),
                java.util.Map.entry("lastSyncTime", dateTime("erp_kingdee_stock_move.last_sync_time")),
                java.util.Map.entry("createTime", dateTime("erp_kingdee_stock_move.create_time")),
                java.util.Map.entry("materialNames", text("(SELECT COALESCE(JSON_UNQUOTE(CONCAT(CHAR(34), REPLACE(SUBSTRING(CAST(eja.parts AS CHAR), 3, CHAR_LENGTH(CAST(eja.parts AS CHAR))-4), CONCAT(CHAR(34), ', ', CHAR(34)), '，'), CHAR(34))), '') FROM (SELECT (SELECT JSON_ARRAYAGG(COALESCE(i.material_name, '')) OVER (ORDER BY i.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM erp_kingdee_stock_move_item i WHERE i.stock_move_id=erp_kingdee_stock_move.id AND i.deleted=0 AND i.tenant_id=erp_kingdee_stock_move.tenant_id LIMIT 1) parts) eja)")),
                java.util.Map.entry("fromWarehouseName", text("COALESCE(NULLIF((SELECT i.from_warehouse_name FROM erp_kingdee_stock_move_item i WHERE i.stock_move_id=erp_kingdee_stock_move.id AND i.deleted=0 AND i.tenant_id=erp_kingdee_stock_move.tenant_id ORDER BY i.id LIMIT 1), ''), '-')")),
                java.util.Map.entry("toWarehouseName", text("COALESCE(NULLIF((SELECT i.to_warehouse_name FROM erp_kingdee_stock_move_item i WHERE i.stock_move_id=erp_kingdee_stock_move.id AND i.deleted=0 AND i.tenant_id=erp_kingdee_stock_move.tenant_id ORDER BY i.id LIMIT 1), ''), '-')")));
    }
}
