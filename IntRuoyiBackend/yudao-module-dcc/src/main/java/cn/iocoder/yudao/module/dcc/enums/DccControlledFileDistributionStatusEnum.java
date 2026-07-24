package cn.iocoder.yudao.module.dcc.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DccControlledFileDistributionStatusEnum implements ArrayValuable<String> {

    PENDING("PENDING", "Pending"),
    SENT("SENT", "Sent"),
    READ("READ", "Read"),
    ACKNOWLEDGED("ACKNOWLEDGED", "Acknowledged"),
    RECOVERED("RECOVERED", "Recovered");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(DccControlledFileDistributionStatusEnum::getCode)
            .toArray(String[]::new);

    private final String code;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
