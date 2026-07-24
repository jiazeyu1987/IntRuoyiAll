package cn.iocoder.yudao.module.dcc.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DccDistributionMediumEnum implements ArrayValuable<String> {

    PUBLIC_FOLDER("PUBLIC_FOLDER", "Public Folder"),
    PAPER("PAPER", "Paper");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(DccDistributionMediumEnum::getCode)
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
