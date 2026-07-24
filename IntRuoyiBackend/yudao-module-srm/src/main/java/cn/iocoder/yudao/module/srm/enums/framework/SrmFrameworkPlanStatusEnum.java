package cn.iocoder.yudao.module.srm.enums.framework;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmFrameworkPlanStatusEnum {

    DRAFT("DRAFT", "草稿"),
    SUBMITTED("SUBMITTED", "已提交"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已驳回"),
    AGREEMENT_CREATED("AGREEMENT_CREATED", "已生成协议");

    private final String status;
    private final String label;

    public static String getLabel(String status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .map(SrmFrameworkPlanStatusEnum::getLabel)
                .findFirst()
                .orElse(status);
    }
}
