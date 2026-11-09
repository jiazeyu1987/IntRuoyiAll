package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public final class DccCategoryApprovalMatrixAdminErrors {
    private DccCategoryApprovalMatrixAdminErrors() {}
    public static final ErrorCode CUSTOM_MATRIX_PUBLIC_FORBIDDEN = new ErrorCode(1_080_092_007, "其他类型使用本次申请自定义矩阵，不能新增公共审批配置");
    public static final ErrorCode CONFIGURATION_REQUIRED = new ErrorCode(1_080_092_005, "新增文件类型必须选择已有生效矩阵或新增立即生效矩阵；其他类型请明确选择本次自定义矩阵");
    public static final ErrorCode MATRIX_IN_USE = new ErrorCode(1_080_092_006, "审批矩阵已绑定文件类型或已被文件使用，不能删除；可由文控修改并保留版本记录");
    public static final ErrorCode RULE_INVALID = new ErrorCode(1_080_092_003, "审阅矩阵第{}条规则无效：{}");
    public static final ErrorCode EFFECTIVE_TIME_REQUIRED = new ErrorCode(1_080_092_004, "审阅矩阵生效时间不能为空");
}
