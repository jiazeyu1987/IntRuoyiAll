package cn.iocoder.yudao.module.dcc.enums;

import java.util.Set;

public final class DccProjectCodeStatusConstants {

    public static final String ENABLE = "ENABLE";
    public static final String DISABLE = "DISABLE";

    private static final Set<String> VALUES = Set.of(ENABLE, DISABLE);

    public static boolean isValid(String status) {
        return VALUES.contains(status);
    }

    private DccProjectCodeStatusConstants() {
    }
}
