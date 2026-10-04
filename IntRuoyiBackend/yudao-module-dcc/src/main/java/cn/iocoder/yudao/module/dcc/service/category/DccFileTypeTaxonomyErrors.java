package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/** B维护分类的持久化错误；不修改主管理共用错误码入口。 */
public final class DccFileTypeTaxonomyErrors {
    private DccFileTypeTaxonomyErrors() {}
    public static final ErrorCode WRITE_INCOMPLETE = new ErrorCode(1_080_090_015,
            "文件类型分类写入不完整：{}，请重试");
}
