package cn.iocoder.yudao.module.dcc.service.position;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public final class DccApprovalPositionAdminErrors {
    public static final ErrorCode NAME_EXISTS = new ErrorCode(1_080_094_001, "审批角色名称已存在，请使用已有角色或更换名称");
    public static final ErrorCode WRITE_INCOMPLETE = new ErrorCode(1_080_094_002, "审批角色写入不完整");

    private DccApprovalPositionAdminErrors() {}
}
