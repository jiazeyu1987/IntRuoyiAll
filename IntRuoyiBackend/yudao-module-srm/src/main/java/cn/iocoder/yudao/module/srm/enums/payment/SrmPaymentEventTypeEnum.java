package cn.iocoder.yudao.module.srm.enums.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmPaymentEventTypeEnum {

    CREATE("CREATE", "创建付款申请"),
    SUBMIT("SUBMIT", "提交审批"),
    APPROVE("APPROVE", "审批通过"),
    REJECT("REJECT", "审批驳回"),
    PUSH_SUCCESS("PUSH_SUCCESS", "财务推送成功"),
    PUSH_FAILED("PUSH_FAILED", "财务推送失败");

    private final String eventType;
    private final String label;

    public static String getLabel(String eventType) {
        return Arrays.stream(values())
                .filter(item -> item.eventType.equals(eventType))
                .map(SrmPaymentEventTypeEnum::getLabel)
                .findFirst()
                .orElse(eventType);
    }
}
