package cn.iocoder.yudao.module.system.enums.user;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 用户账号生命周期单据类型。
 */
@Getter
@AllArgsConstructor
public enum UserLifecycleDocumentTypeEnum implements ArrayValuable<String> {

    RESIGNATION("RESIGNATION", "离职单"),
    TRANSFER("TRANSFER", "转岗单");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(UserLifecycleDocumentTypeEnum::getType)
            .toArray(String[]::new);

    private final String type;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    public static boolean isValid(String type) {
        return Arrays.asList(ARRAYS).contains(type);
    }

}
