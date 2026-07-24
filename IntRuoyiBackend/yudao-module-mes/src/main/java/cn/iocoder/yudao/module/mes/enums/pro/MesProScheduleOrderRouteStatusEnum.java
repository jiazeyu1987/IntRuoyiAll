package cn.iocoder.yudao.module.mes.enums.pro;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MesProScheduleOrderRouteStatusEnum {

    READY(0),
    MISSING(1);

    private final Integer status;

}
