package cn.iocoder.yudao.module.erp.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_CONNECTION_TYPE_UNSUPPORTED;

@Getter
@AllArgsConstructor
public enum ErpKingdeeConnectionTypeEnum {

    TEST("TEST", "测试账套"),
    PRODUCTION("PRODUCTION", "正式账套");

    private final String type;
    private final String name;

    public static ErpKingdeeConnectionTypeEnum requiredOf(String type) {
        return Arrays.stream(values())
                .filter(item -> item.getType().equals(type))
                .findFirst()
                .orElseThrow(() -> exception(KINGDEE_CONNECTION_TYPE_UNSUPPORTED, type));
    }

}
