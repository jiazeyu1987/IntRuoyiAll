package cn.iocoder.yudao.module.signature.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface SignatureErrorCodeConstants {

    ErrorCode ESIGN_LOGIN_REQUIRED = new ErrorCode(1_047_000_000, "电子签名要求当前实名用户已登录");
    ErrorCode ESIGN_COMMAND_INVALID = new ErrorCode(1_047_000_001, "电子签名命令非法，原因：{}");
    ErrorCode ESIGN_ACTION_NOT_REGISTERED = new ErrorCode(1_047_000_002, "电子签名动作未登记：{}:{}");
    ErrorCode ESIGN_SUBJECT_NOT_SIGNABLE = new ErrorCode(1_047_000_003, "对象当前不可签名，原因：{}");
    ErrorCode ESIGN_DUPLICATE_IDEMPOTENCY_KEY = new ErrorCode(1_047_000_004, "电子签名幂等键已被不同内容使用");

    ErrorCode GXP_AUDIT_COMMAND_INVALID = new ErrorCode(1_047_001_000, "GxP 审计命令非法，原因：{}");
    ErrorCode GXP_AUDIT_IDEMPOTENCY_CONFLICT = new ErrorCode(1_047_001_001, "GxP 审计幂等键已被不同内容使用");

}
