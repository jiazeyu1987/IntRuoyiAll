package cn.iocoder.yudao.module.srm.enums.supplier;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmSupplierRiskStatusEnum {

    OPEN("OPEN", "未处理"),
    RESOLVED("RESOLVED", "已处理");

    private final String status;
    private final String label;

    public static boolean contains(String status) {
        return Arrays.stream(values()).anyMatch(item -> item.status.equals(status));
    }

    public static String getLabel(String status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .map(SrmSupplierRiskStatusEnum::getLabel)
                .findFirst()
                .orElse(status);
    }
}
