package cn.iocoder.yudao.module.dcc.service.directory;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public final class DccDirectoryAdminErrors {
    private DccDirectoryAdminErrors() {}

    public static final ErrorCode CODE_EXISTS = new ErrorCode(1_080_092_009,
            "同一上级目录下的目录编码已被使用，请更换编码（已删除目录的编码也不可重复）");
}
