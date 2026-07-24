package cn.iocoder.yudao.module.srm.enums.procurement;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmProcurementPlanStatusEnum {

    DRAFT("DRAFT", "草稿"),
    SUBMITTED("SUBMITTED", "已提交"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已驳回"),
    GENERATED("GENERATED", "已生成项目");

    private final String status;
    private final String label;

    public static String getLabel(String status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .map(SrmProcurementPlanStatusEnum::getLabel)
                .findFirst()
                .orElse(status);
    }
}
