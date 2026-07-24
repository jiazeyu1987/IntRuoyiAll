package cn.iocoder.yudao.module.dcc.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DccAccessTypeEnum implements ArrayValuable<Integer> {

    QUERY(0, "查询"),
    PREVIEW(1, "预览"),
    DOWNLOAD(2, "下载");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(DccAccessTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
