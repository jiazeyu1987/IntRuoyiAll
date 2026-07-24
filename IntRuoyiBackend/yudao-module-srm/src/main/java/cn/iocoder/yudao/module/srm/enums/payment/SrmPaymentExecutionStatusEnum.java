package cn.iocoder.yudao.module.srm.enums.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmPaymentExecutionStatusEnum {

    DRAFT("DRAFT", "草稿"),
    PENDING_APPROVAL("PENDING_APPROVAL", "待审批"),
    APPROVED("APPROVED", "审批通过"),
    REJECTED("REJECTED", "审批驳回"),
    PUSH_SUCCESS("PUSH_SUCCESS", "财务推送成功"),
    PUSH_FAILED("PUSH_FAILED", "财务推送失败");

    private final String status;
    private final String label;

    public static String getLabel(String status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .map(SrmPaymentExecutionStatusEnum::getLabel)
                .findFirst()
                .orElse(status);
    }
}
