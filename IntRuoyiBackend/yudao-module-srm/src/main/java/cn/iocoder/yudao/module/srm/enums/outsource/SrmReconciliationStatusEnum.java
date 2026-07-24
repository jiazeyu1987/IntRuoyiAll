package cn.iocoder.yudao.module.srm.enums.outsource;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmReconciliationStatusEnum {

    RECONCILED("RECONCILED", "已对账");

    private final String status;
    private final String label;

    public static String getLabel(String status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .map(SrmReconciliationStatusEnum::getLabel)
                .findFirst()
                .orElse(status);
    }
}
