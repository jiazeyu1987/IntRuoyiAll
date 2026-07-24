package cn.iocoder.yudao.module.mes.enums.pro;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MesProScheduleDailyCompareStatusEnum {

    NORMAL(0),
    AHEAD(1),
    BEHIND(2),
    NO_PLAN(3),
    NO_FEEDBACK(4);

    private final Integer status;

}
