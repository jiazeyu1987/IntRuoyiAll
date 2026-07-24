package cn.iocoder.yudao.module.srm.enums.outsource;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SrmOutsourceEventTypeEnum {

    CREATE("CREATE", "创建委外执行"),
    ISSUE("ISSUE", "发料通知"),
    PROGRESS("PROGRESS", "加工进度回传"),
    RECEIVE("RECEIVE", "送收货回传"),
    INSPECT("INSPECT", "来料检验"),
    RECONCILE("RECONCILE", "对账确认");

    private final String eventType;
    private final String label;

    public static String getLabel(String eventType) {
        return Arrays.stream(values())
                .filter(item -> item.eventType.equals(eventType))
                .map(SrmOutsourceEventTypeEnum::getLabel)
                .findFirst()
                .orElse(eventType);
    }
}
