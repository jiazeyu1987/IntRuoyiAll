package cn.iocoder.yudao.module.srm.enums.framework;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmFrameworkAgreementStatusEnum {

    EFFECTIVE("EFFECTIVE", "生效中");

    private final String status;
    private final String label;

    public static String getLabel(String status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .map(SrmFrameworkAgreementStatusEnum::getLabel)
                .findFirst()
                .orElse(status);
    }
}
