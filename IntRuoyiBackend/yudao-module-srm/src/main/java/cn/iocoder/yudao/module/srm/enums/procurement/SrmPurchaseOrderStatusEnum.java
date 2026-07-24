package cn.iocoder.yudao.module.srm.enums.procurement;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmPurchaseOrderStatusEnum {

    PENDING_CONFIRM("PENDING_CONFIRM", "待供应商确认"),
    CONFIRMED("CONFIRMED", "已确认"),
    CHANGE_PENDING("CHANGE_PENDING", "变更待确认"),
    CANCELLED("CANCELLED", "已取消");

    private final String status;
    private final String label;

    public static String getLabel(String status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .map(SrmPurchaseOrderStatusEnum::getLabel)
                .findFirst()
                .orElse(status);
    }
}
