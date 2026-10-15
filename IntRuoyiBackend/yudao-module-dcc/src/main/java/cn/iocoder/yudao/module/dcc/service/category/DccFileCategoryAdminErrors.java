package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public final class DccFileCategoryAdminErrors {
    private DccFileCategoryAdminErrors() {}
    public static final ErrorCode WRITE_INCOMPLETE = new ErrorCode(1_080_092_001, "文件类别写入不完整：{}");
    public static final ErrorCode USED_TYPE_CHANGE_FORBIDDEN = new ErrorCode(1_080_092_002,
            "文件类别已被文件使用，不能改变其文件类型身份");
}
