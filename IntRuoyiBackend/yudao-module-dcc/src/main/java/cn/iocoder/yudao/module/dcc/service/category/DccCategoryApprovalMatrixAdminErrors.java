package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public final class DccCategoryApprovalMatrixAdminErrors {
    private DccCategoryApprovalMatrixAdminErrors() {}
    public static final ErrorCode RULE_INVALID = new ErrorCode(1_080_092_003, "审阅矩阵第{}条规则无效：{}");
    public static final ErrorCode EFFECTIVE_TIME_REQUIRED = new ErrorCode(1_080_092_004, "审阅矩阵生效时间不能为空");
}
