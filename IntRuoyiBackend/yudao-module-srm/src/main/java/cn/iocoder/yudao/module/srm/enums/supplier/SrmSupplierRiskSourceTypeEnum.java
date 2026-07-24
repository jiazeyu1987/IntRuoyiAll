package cn.iocoder.yudao.module.srm.enums.supplier;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmSupplierRiskSourceTypeEnum {

    ACCESS_REQUEST("ACCESS_REQUEST", "准入申请"),
    PROCUREMENT_PLAN("PROCUREMENT_PLAN", "采购计划"),
    NON_TENDER_PROJECT("NON_TENDER_PROJECT", "非招标项目"),
    TENDER_PROJECT("TENDER_PROJECT", "招标项目"),
    PROCUREMENT_CONTRACT("PROCUREMENT_CONTRACT", "采购合同");

    private final String type;
    private final String label;

    public static boolean contains(String type) {
        return Arrays.stream(values()).anyMatch(item -> item.type.equals(type));
    }

    public static String getLabel(String type) {
        return Arrays.stream(values())
                .filter(item -> item.type.equals(type))
                .map(SrmSupplierRiskSourceTypeEnum::getLabel)
                .findFirst()
                .orElse(type);
    }
}
