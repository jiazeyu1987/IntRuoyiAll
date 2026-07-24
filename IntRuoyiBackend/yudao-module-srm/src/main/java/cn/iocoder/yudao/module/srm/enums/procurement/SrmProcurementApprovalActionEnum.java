package cn.iocoder.yudao.module.srm.enums.procurement;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmProcurementApprovalActionEnum {

    SUBMIT("SUBMIT", "提交"),
    APPROVE("APPROVE", "通过"),
    REJECT("REJECT", "驳回");

    private final String action;
    private final String label;

    public static String getLabel(String action) {
        return Arrays.stream(values())
                .filter(item -> item.action.equals(action))
                .map(SrmProcurementApprovalActionEnum::getLabel)
                .findFirst()
                .orElse(action);
    }
}
