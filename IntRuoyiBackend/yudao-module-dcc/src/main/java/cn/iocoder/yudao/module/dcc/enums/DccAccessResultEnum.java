package cn.iocoder.yudao.module.dcc.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DccAccessResultEnum implements ArrayValuable<Integer> {

    DENIED(0, "拒绝"),
    ALLOWED(1, "允许");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(DccAccessResultEnum::getResult).toArray(Integer[]::new);

    private final Integer result;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
