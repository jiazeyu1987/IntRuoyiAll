package cn.iocoder.yudao.module.srm.enums.supplier;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmSupplierRiskLevelEnum {

    LOW("LOW", "低"),
    MEDIUM("MEDIUM", "中"),
    HIGH("HIGH", "高");

    private final String level;
    private final String label;

    public static boolean contains(String level) {
        return Arrays.stream(values()).anyMatch(item -> item.level.equals(level));
    }

    public static String getLabel(String level) {
        return Arrays.stream(values())
                .filter(item -> item.level.equals(level))
                .map(SrmSupplierRiskLevelEnum::getLabel)
                .findFirst()
                .orElse(level);
    }
}
