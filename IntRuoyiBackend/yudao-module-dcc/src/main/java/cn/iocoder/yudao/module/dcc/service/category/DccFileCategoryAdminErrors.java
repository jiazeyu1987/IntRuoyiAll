package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public final class DccFileCategoryAdminErrors {
    public static final ErrorCode CODE_EXISTS = new ErrorCode(1_080_092_008, "类别编码已被使用，请更换类别编码（已删除类别的编码也不可重复）");
    private DccFileCategoryAdminErrors() {}
    public static final ErrorCode WRITE_INCOMPLETE = new ErrorCode(1_080_092_001, "文件类别写入不完整：{}");
    public static final ErrorCode USED_TYPE_CHANGE_FORBIDDEN = new ErrorCode(1_080_092_002,
            "文件类别已被文件使用，不能改变其文件类型身份");
}
