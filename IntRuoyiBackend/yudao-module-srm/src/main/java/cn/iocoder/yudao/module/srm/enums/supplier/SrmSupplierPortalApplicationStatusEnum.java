package cn.iocoder.yudao.module.srm.enums.supplier;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmSupplierPortalApplicationStatusEnum {

    DRAFT("DRAFT", "草稿"),
    SUBMITTED("SUBMITTED", "已提交"),
    APPROVED("APPROVED", "审核通过"),
    REJECTED("REJECTED", "审核驳回");

    private final String status;
    private final String label;

    public static boolean contains(String status) {
        return Arrays.stream(values()).anyMatch(item -> item.status.equals(status));
    }

    public static String getLabel(String status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .map(SrmSupplierPortalApplicationStatusEnum::getLabel)
                .findFirst()
                .orElse(status);
    }
}
