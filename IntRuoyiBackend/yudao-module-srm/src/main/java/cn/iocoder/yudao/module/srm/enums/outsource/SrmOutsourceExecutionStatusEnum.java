package cn.iocoder.yudao.module.srm.enums.outsource;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmOutsourceExecutionStatusEnum {

    PENDING_ISSUE("PENDING_ISSUE", "待发料"),
    IN_PRODUCTION("IN_PRODUCTION", "加工中"),
    DELIVERED("DELIVERED", "已送货待检验"),
    INSPECTED("INSPECTED", "已检验待对账"),
    RECONCILED("RECONCILED", "已对账");

    private final String status;
    private final String label;

    public static String getLabel(String status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .map(SrmOutsourceExecutionStatusEnum::getLabel)
                .findFirst()
                .orElse(status);
    }
}
