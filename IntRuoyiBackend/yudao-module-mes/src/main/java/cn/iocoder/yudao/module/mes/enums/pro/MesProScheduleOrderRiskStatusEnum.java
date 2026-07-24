package cn.iocoder.yudao.module.mes.enums.pro;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES 排产工单风险状态枚举
 */
@Getter
@AllArgsConstructor
public enum MesProScheduleOrderRiskStatusEnum implements ArrayValuable<Integer> {

    NONE(0, "无风险"),
    WARNING(1, "有风险"),
    BLOCKED(2, "已阻塞");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(MesProScheduleOrderRiskStatusEnum::getStatus)
            .toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
