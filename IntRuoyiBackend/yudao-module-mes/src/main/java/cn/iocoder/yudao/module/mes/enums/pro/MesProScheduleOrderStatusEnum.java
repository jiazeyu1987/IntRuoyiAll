package cn.iocoder.yudao.module.mes.enums.pro;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES 排产工单状态枚举
 */
@Getter
@AllArgsConstructor
public enum MesProScheduleOrderStatusEnum implements ArrayValuable<Integer> {

    PREPARE(0, "待排产"),
    SCHEDULED(1, "已排产"),
    IN_PROGRESS(2, "生产中"),
    FINISHED(3, "已完成"),
    CANCELED(4, "已取消");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(MesProScheduleOrderStatusEnum::getStatus)
            .toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
