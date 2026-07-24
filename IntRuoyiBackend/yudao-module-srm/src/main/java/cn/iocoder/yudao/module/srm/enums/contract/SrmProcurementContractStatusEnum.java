package cn.iocoder.yudao.module.srm.enums.contract;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmProcurementContractStatusEnum {

    EFFECTIVE("EFFECTIVE", "生效中"),
    CANCELLED("CANCELLED", "已作废");

    private final String status;
    private final String label;

    public static String getLabel(String status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .map(SrmProcurementContractStatusEnum::getLabel)
                .findFirst()
                .orElse(status);
    }
}
