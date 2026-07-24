package cn.iocoder.yudao.module.srm.enums.supplier;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmSupplierOnboardingStageStatusEnum {

    NOT_STARTED("NOT_STARTED", "未开始"),
    PENDING("PENDING", "待审核"),
    PASSED("PASSED", "已通过"),
    REJECTED("REJECTED", "已驳回");

    private final String status;
    private final String label;

    public static boolean contains(String status) {
        return Arrays.stream(values()).anyMatch(item -> item.status.equals(status));
    }

    public static String getLabel(String status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .map(SrmSupplierOnboardingStageStatusEnum::getLabel)
                .findFirst()
                .orElse(status);
    }
}
