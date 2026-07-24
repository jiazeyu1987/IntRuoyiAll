package cn.iocoder.yudao.module.dcc.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DccRouteStatusEnum implements ArrayValuable<Integer> {

    DRAFT(0, "草稿"),
    ACTIVE(1, "启用"),
    DISABLED(2, "停用");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(DccRouteStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
