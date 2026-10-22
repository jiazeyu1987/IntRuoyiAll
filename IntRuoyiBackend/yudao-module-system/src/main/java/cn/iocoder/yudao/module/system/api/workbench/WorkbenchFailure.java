package cn.iocoder.yudao.module.system.api.workbench;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;

/** Stable request failures handled by the existing ServiceException HTTP boundary. */
public enum WorkbenchFailure {
    MODULE_DISABLED(0, "该工作台模块已停用"),
    SOURCE_NOT_REGISTERED(1, "工作台来源未注册"),
    CONTRACT_VERSION_UNSUPPORTED(2, "工作台协议或来源目录版本已变更"),
    INVALID_QUERY(3, "工作台查询条件非法"),
    IDENTITY_CONTEXT_MISSING(4, "工作台身份上下文缺失"),
    SOURCE_FORBIDDEN(5, "没有该工作台来源的访问权限"),
    VISIBILITY_CONTEXT_MISSING(6, "个人工作台版本上下文未初始化"),
    VISIBILITY_VERSION_CONFLICT(7, "个人工作台版本已变更，请刷新"),
    QUERY_TOKEN_INVALID(8, "工作台查询凭据无效"),
    QUERY_TOKEN_EXPIRED(9, "工作台查询凭据已过期，请刷新"),
    QUERY_CONTEXT_CHANGED(10, "工作台查询上下文已变更，请刷新"),
    SOURCE_QUERY_FAILED(11, "工作台来源查询失败"),
    SOURCE_CONTRACT_INVALID(12, "工作台来源返回数据不符合协议"),
    QUERY_BUDGET_EXCEEDED(13, "工作台查询超过十秒预算"),
    TASK_NOT_ELIGIBLE(14, "该任务当前不符合个人工作台操作条件"),
    VISIBILITY_WRITE_CONFLICT(15, "个人工作台操作发生竞争，请刷新后重试"),
    QUERY_TOKEN_STORE_FAILED(16, "工作台查询凭据存储失败"),
    VISIBILITY_WRITE_FAILED(17, "个人工作台配置写入失败"),
    PROJECTION_NOT_CAUGHT_UP(18, "工作台投影尚未追平，请稍后刷新"),
    SNAPSHOT_MISMATCH(19, "工作台版本已变更，请刷新"),
    VERSION_EXPIRED(20, "工作台授权上下文已过期，请刷新"),
    PROJECTION_CONTEXT_MISSING(21, "工作台索引上下文未初始化"),
    PROJECTION_BATCH_INVALID(22, "工作台事件批次不完整或版本冲突"),
    PROJECTION_ACTIVATION_REQUIRED(23, "工作台索引尚未完成启用验收");

    private final ErrorCode errorCode;
    WorkbenchFailure(int offset, String message) {
        errorCode = new ErrorCode(1_002_034_000 + offset, message);
    }
    public int code() { return errorCode.getCode(); }
    public ServiceException exception() { return new ServiceException(errorCode); }
    public ServiceException exception(Throwable cause) {
        ServiceException failure = exception();
        failure.initCause(cause);
        return failure;
    }
}
