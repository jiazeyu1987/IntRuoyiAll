package cn.iocoder.yudao.module.mes.enums.pro;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES 排产工单差异处理状态枚举
 */
@Getter
@AllArgsConstructor
public enum MesProScheduleOrderDiffStatusEnum implements ArrayValuable<Integer> {

    NONE(0, "无差异"),
    PENDING(1, "待处理"),
    RESOLVED(2, "已处理"),
    IGNORED(3, "已忽略");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(MesProScheduleOrderDiffStatusEnum::getStatus)
            .toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
