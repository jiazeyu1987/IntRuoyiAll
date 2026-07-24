package cn.iocoder.yudao.module.srm.enums.procurement;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmProcurementMethodEnum {

    TENDER("TENDER", "招标"),
    NON_BIDDING("NON_BIDDING", "非招标");

    private final String method;
    private final String label;

    public static boolean contains(String method) {
        return Arrays.stream(values()).anyMatch(item -> item.method.equals(method));
    }

    public static String getLabel(String method) {
        return Arrays.stream(values())
                .filter(item -> item.method.equals(method))
                .map(SrmProcurementMethodEnum::getLabel)
                .findFirst()
                .orElse(method);
    }
}
