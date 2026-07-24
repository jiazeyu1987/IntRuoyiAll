package cn.iocoder.yudao.module.mes.enums.pro;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MES 工艺流程类型枚举
 */
@Getter
@AllArgsConstructor
public enum MesProRouteFlowConfigTypeEnum {

    SCHEDULE("SCHEDULE", "排产"),
    BATCH("BATCH", "批处理");

    private final String type;
    private final String name;

    public static MesProRouteFlowConfigTypeEnum valueOfType(String type) {
        for (MesProRouteFlowConfigTypeEnum value : values()) {
            if (value.type.equalsIgnoreCase(type)) {
                return value;
            }
        }
        return null;
    }

}
