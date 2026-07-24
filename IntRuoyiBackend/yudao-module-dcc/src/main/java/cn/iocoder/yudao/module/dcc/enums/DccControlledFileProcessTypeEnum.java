package cn.iocoder.yudao.module.dcc.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DccControlledFileProcessTypeEnum implements ArrayValuable<String> {

    CONTROLLED_FILE("CONTROLLED_FILE", "Controlled File"),
    EXTERNAL_REVIEW("EXTERNAL_REVIEW", "External Review");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(DccControlledFileProcessTypeEnum::getCode)
            .toArray(String[]::new);

    private final String code;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    public static boolean isValid(String code) {
        return Arrays.stream(values()).anyMatch(item -> item.code.equals(code));
    }
}
