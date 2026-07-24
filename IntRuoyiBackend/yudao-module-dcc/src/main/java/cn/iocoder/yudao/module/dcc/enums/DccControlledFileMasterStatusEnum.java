package cn.iocoder.yudao.module.dcc.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DccControlledFileMasterStatusEnum implements ArrayValuable<String> {

    ACTIVE_CHAIN("ACTIVE_CHAIN", "Active chain"),
    OBSOLETE_CHAIN("OBSOLETE_CHAIN", "Obsolete chain");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(DccControlledFileMasterStatusEnum::getCode)
            .toArray(String[]::new);

    private final String code;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
