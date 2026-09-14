package cn.iocoder.yudao.module.erp.enums.kingdeeautosync;

import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum ErpKingdeeTableAutoSyncTypeEnum {

    PRODUCT(ErpKingdeeSyncTypeEnum.PRODUCT.getType(), "ERP 商品 / MES 物料", "kingdeeProductItemSyncJob"),
    STOCK(ErpKingdeeSyncTypeEnum.STOCK.getType(), "ERP 库存", "kingdeeStockSyncJob"),
    STOCK_MOVE(ErpKingdeeSyncTypeEnum.STOCK_MOVE.getType(), "金蝶调拨单", "kingdeeStockMoveSyncJob"),
    PURCHASE_ORDER(ErpKingdeeSyncTypeEnum.PURCHASE_ORDER.getType(), "采购订单", "kingdeePurchaseOrderSyncJob"),
    SALE_ORDER(ErpKingdeeSyncTypeEnum.SALE_ORDER.getType(), "销售订单", "kingdeeSaleOrderSyncJob"),
    PRODUCTION_ORDER(ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER.getType(), "生产工单", "kingdeeProductionOrderSyncJob"),
    PRODUCTION_PICK_LIST(ErpKingdeeSyncTypeEnum.PRODUCTION_PICK_LIST.getType(), "生产领料单列表",
            "kingdeeProductionPickListSyncJob"),
    PRODUCTION_REPLENISHMENT_LIST(ErpKingdeeSyncTypeEnum.PRODUCTION_REPLENISHMENT_LIST.getType(), "生产补料单列表",
            "kingdeeProductionReplenishmentListSyncJob"),
    PRODUCTION_MATERIAL_LIST(ErpKingdeeSyncTypeEnum.PRODUCTION_MATERIAL_LIST.getType(), "生产用料清单",
            "kingdeeProductionMaterialListSyncJob"),
    BOM(ErpKingdeeSyncTypeEnum.BOM.getType(), "产品 BOM", "kingdeeBomSyncJob");

    private final String syncType;
    private final String label;
    private final String handlerName;

    public static List<ErpKingdeeTableAutoSyncTypeEnum> list() {
        return Arrays.asList(values());
    }

    public static ErpKingdeeTableAutoSyncTypeEnum requiredOf(String syncType) {
        return list().stream()
                .filter(item -> item.getSyncType().equals(syncType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported sync type: " + syncType));
    }
}
