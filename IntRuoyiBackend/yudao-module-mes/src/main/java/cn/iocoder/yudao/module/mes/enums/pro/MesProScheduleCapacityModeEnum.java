package cn.iocoder.yudao.module.mes.enums.pro;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MesProScheduleCapacityModeEnum {

    RESOURCE_CALCULATED("RESOURCE_CALCULATED"),
    MANUAL_OVERRIDE("MANUAL_OVERRIDE"),
    FINITE_HOURLY("FINITE_HOURLY"),
    INFINITE_FORMULA("INFINITE_FORMULA");

    private final String mode;

    public static boolean isManualOverrideLike(String mode) {
        return MANUAL_OVERRIDE.getMode().equals(mode) || FINITE_HOURLY.getMode().equals(mode);
    }

    public static boolean isPersistableMode(String mode) {
        return RESOURCE_CALCULATED.getMode().equals(mode)
                || MANUAL_OVERRIDE.getMode().equals(mode)
                || INFINITE_FORMULA.getMode().equals(mode);
    }

}
