package cn.iocoder.yudao.module.erp.enums.sync;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErpKingdeeSyncTypeEnum {

    PRODUCT("PRODUCT"),
    STOCK("STOCK"),
    STOCK_MOVE("STOCK_MOVE"),
    PURCHASE_ORDER("PURCHASE_ORDER"),
    SALE_ORDER("SALE_ORDER"),
    PRODUCTION_ORDER("PRODUCTION_ORDER"),
    PRODUCTION_MATERIAL_LIST("PRODUCTION_MATERIAL_LIST"),
    BOM("BOM");

    private final String type;

}
