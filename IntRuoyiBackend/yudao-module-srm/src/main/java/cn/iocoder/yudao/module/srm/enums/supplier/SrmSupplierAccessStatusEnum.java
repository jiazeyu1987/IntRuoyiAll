package cn.iocoder.yudao.module.srm.enums.supplier;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmSupplierAccessStatusEnum {

    PENDING("PENDING", "待审核"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已驳回");

    private final String status;
    private final String label;

    public static boolean contains(String status) {
        return Arrays.stream(values()).anyMatch(item -> item.status.equals(status));
    }

    public static String getLabel(String status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .map(SrmSupplierAccessStatusEnum::getLabel)
                .findFirst()
                .orElse(status);
    }
}
