package cn.iocoder.yudao.module.dcc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DccControlledFileChangeTypeEnum {

    NEW("NEW", "新建"),
    REVISION("REVISION", "升版"),
    OBSOLETE("OBSOLETE", "作废");

    private final String code;
    private final String name;

    public static DccControlledFileChangeTypeEnum of(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    public static boolean isValid(String code) {
        return of(code) != null;
    }
}
