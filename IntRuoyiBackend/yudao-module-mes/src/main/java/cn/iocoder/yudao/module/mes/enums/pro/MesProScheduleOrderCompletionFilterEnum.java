package cn.iocoder.yudao.module.mes.enums.pro;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum MesProScheduleOrderCompletionFilterEnum implements ArrayValuable<String> {

    INCOMPLETE("INCOMPLETE", "未完成"),
    ALL("ALL", "全部"),
    COMPLETED("COMPLETED", "已完成");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(MesProScheduleOrderCompletionFilterEnum::getValue)
            .toArray(String[]::new);

    private final String value;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
