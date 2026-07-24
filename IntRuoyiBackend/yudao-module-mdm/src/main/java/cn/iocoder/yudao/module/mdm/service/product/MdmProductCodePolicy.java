package cn.iocoder.yudao.module.mdm.service.product;

import cn.hutool.core.util.StrUtil;

public final class MdmProductCodePolicy {

    private MdmProductCodePolicy() {
    }

    public static String normalize(String value) {
        return StrUtil.trimToNull(value);
    }

    public static boolean isValidDccProductCode(String value) {
        return StrUtil.isNotBlank(value) && value.matches("[A-Za-z0-9]{14}");
    }

}
