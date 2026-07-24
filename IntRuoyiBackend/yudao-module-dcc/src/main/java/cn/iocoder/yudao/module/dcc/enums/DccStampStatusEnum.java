package cn.iocoder.yudao.module.dcc.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DccStampStatusEnum implements ArrayValuable<Integer> {

    PENDING(0, "待处理"),
    SUCCESS(1, "成功"),
    FAILED(2, "失败");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(DccStampStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
