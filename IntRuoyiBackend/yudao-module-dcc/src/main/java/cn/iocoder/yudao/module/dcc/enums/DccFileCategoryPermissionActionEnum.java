package cn.iocoder.yudao.module.dcc.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DccFileCategoryPermissionActionEnum implements ArrayValuable<String> {

    VIEW("VIEW", "View"),
    UPLOAD("UPLOAD", "Upload"),
    DOWNLOAD("DOWNLOAD", "Download"),
    OBSOLETE("OBSOLETE", "Obsolete"),
    REVIEW("REVIEW", "Review"),
    APPROVE("APPROVE", "Approve"),
    DISTRIBUTE("DISTRIBUTE", "Distribute");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(DccFileCategoryPermissionActionEnum::getCode)
            .toArray(String[]::new);

    private final String code;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
