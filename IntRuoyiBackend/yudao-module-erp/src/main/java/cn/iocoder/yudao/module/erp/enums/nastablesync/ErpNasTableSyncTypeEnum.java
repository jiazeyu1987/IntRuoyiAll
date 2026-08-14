package cn.iocoder.yudao.module.erp.enums.nastablesync;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum ErpNasTableSyncTypeEnum {

    PRODUCT("PRODUCT", "产品", "产品"),
    STOCK("STOCK", "库存", "库存"),
    PURCHASE_ORDER("PURCHASE_ORDER", "采购订单", "采购订单"),
    SALE_ORDER("SALE_ORDER", "销售订单", "销售订单");

    private final String type;
    private final String label;
    private final String defaultSheetName;

    public static List<ErpNasTableSyncTypeEnum> list() {
        return Arrays.asList(values());
    }

    public static ErpNasTableSyncTypeEnum requiredOf(String type) {
        return list().stream()
                .filter(item -> item.type.equals(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(type));
    }
}
