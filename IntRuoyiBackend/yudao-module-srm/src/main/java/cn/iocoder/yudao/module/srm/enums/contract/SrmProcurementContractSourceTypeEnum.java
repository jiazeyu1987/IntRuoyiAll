package cn.iocoder.yudao.module.srm.enums.contract;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmProcurementContractSourceTypeEnum {

    NON_BIDDING("NON_BIDDING", "非招标项目"),
    TENDER("TENDER", "招标项目");

    private final String sourceType;
    private final String label;

    public static boolean contains(String sourceType) {
        return Arrays.stream(values()).anyMatch(item -> item.sourceType.equals(sourceType));
    }

    public static String getLabel(String sourceType) {
        return Arrays.stream(values())
                .filter(item -> item.sourceType.equals(sourceType))
                .map(SrmProcurementContractSourceTypeEnum::getLabel)
                .findFirst()
                .orElse(sourceType);
    }
}
