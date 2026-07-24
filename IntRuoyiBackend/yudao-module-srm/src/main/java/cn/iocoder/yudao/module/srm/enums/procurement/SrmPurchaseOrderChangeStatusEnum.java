package cn.iocoder.yudao.module.srm.enums.procurement;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmPurchaseOrderChangeStatusEnum {

    PENDING_CONFIRM("PENDING_CONFIRM", "待供应商确认"),
    CONFIRMED("CONFIRMED", "已确认"),
    REJECTED("REJECTED", "已拒绝"),
    WITHDRAWN("WITHDRAWN", "已撤回");

    private final String status;
    private final String label;

    public static String getLabel(String status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .map(SrmPurchaseOrderChangeStatusEnum::getLabel)
                .findFirst()
                .orElse(status);
    }
}
