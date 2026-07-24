package cn.iocoder.yudao.module.dcc.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DccAccessSubjectTypeEnum implements ArrayValuable<Integer> {

    USER(1, "用户"),
    DEPT(2, "部门"),
    ROLE(3, "角色"),
    POSITION(4, "岗位");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(DccAccessSubjectTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
