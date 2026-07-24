package cn.iocoder.yudao.module.dcc.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DCC file category lifecycle stage.
 */
public enum DccFileCategoryLifecycleStageEnum {

    PLAN("PLAN", "01 plan 策划"),
    INPUT("INPUT", "02 input 输入"),
    OUTPUT("OUTPUT", "03 output 输出"),
    VERIFICATION("VERIFICATION", "04 verification 验证"),
    VALIDATION("VALIDATION", "05 validation 确认"),
    TRANSFER("TRANSFER", "06 transfer 转移");

    public static final Set<String> CODES = Arrays.stream(values())
            .map(DccFileCategoryLifecycleStageEnum::getCode)
            .collect(Collectors.toUnmodifiableSet());

    private final String code;
    private final String label;

    DccFileCategoryLifecycleStageEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static boolean isValid(String code) {
        return code != null && CODES.contains(code.trim().toUpperCase());
    }
}
