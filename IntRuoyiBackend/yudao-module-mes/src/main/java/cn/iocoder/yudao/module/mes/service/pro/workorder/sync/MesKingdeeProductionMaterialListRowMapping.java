package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionMaterialList;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.hutool.core.util.StrUtil;
import java.time.LocalDateTime;

final class MesKingdeeProductionMaterialListRowMapping {
    private MesKingdeeProductionMaterialListRowMapping() { }

    static boolean shouldBackfillErpColumns(MesKingdeeProductionMaterialListDO existing,
                                             ErpKingdeeProductionMaterialList row) {
        return (StrUtil.isBlank(existing.getDrawingNumber()) && StrUtil.isNotBlank(row.getDrawingNumber()))
                || (existing.getDemandTime() == null && row.getDemandTime() != null);
    }

    static MesKingdeeProductionMaterialListDO map(ErpKingdeeProductionMaterialList row,
            MesProWorkOrderDO workOrder, MesMdItemDO productItem, MesMdItemDO childItem, Long bomId) {
        String productCode = requireProductCode(row, productItem);
        MesKingdeeProductionMaterialListDO target = MesKingdeeProductionMaterialListDO.builder()
                .sourceFormId(row.getFormId())
                .sourceBillNo(row.getBillNo())
                .sourceEntryId(row.getEntryId())
                .productCode(productCode)
                .productionOrderNo(row.getProductionOrderNo())
                .productionOrderLineNo(row.getProductionOrderLineNo())
                .productionOrderStatus(row.getProductionOrderStatus())
                .childMaterialCode(row.getChildMaterialCode())
                .childMaterialName(row.getChildMaterialName())
                .childMaterialSpecification(row.getChildMaterialSpecification())
                .childMaterialType(row.getChildMaterialType())
                .numerator(row.getNumerator())
                .denominator(row.getDenominator())
                .childUnitName(row.getChildUnitName())
                .drawingNumber(row.getDrawingNumber())
                .requiredQuantity(row.getRequiredQuantity())
                .issueMethod(row.getIssueMethod())
                .demandTime(row.getDemandTime())
                .sourceModifyTime(row.getSourceModifyTime())
                .lastSyncTime(LocalDateTime.now())
                .rawPayload(row.getRawPayload())
                .build();
        if (workOrder != null) {
            target.setWorkOrderId(workOrder.getId());
            target.setWorkOrderCode(workOrder.getCode());
            target.setProductId(workOrder.getProductId());
            target.setWorkOrderBomId(bomId);
        } else if (productItem != null) {
            target.setProductId(productItem.getId());
        }
        if (childItem != null) {
            target.setChildMaterialId(childItem.getId());
        }
        return target;
    }

    private static String requireProductCode(ErpKingdeeProductionMaterialList row, MesMdItemDO productItem) {
        if (StrUtil.isNotBlank(row.getProductCode())) {
            return row.getProductCode();
        }
        if (productItem != null && StrUtil.isNotBlank(productItem.getCode())) {
            return productItem.getCode();
        }
        throw new IllegalStateException("ERP production material list missing product code and local work order product: "
                + row.getProductionOrderNo());
    }

}
