package cn.iocoder.yudao.module.dcc.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DccApprovalModeEnum implements ArrayValuable<Integer> {

    ANY_ONE(1, "任一人通过"),
    ALL_REQUIRED(2, "全部通过");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(DccApprovalModeEnum::getMode).toArray(Integer[]::new);

    private final Integer mode;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
