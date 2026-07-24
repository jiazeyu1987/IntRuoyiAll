package cn.iocoder.yudao.module.dcc.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DccAccessLevelEnum implements ArrayValuable<Integer> {

    PREVIEW(1, "预览"),
    DOWNLOAD(2, "下载"),
    MANAGE(3, "管理");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(DccAccessLevelEnum::getLevel).toArray(Integer[]::new);

    private final Integer level;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
