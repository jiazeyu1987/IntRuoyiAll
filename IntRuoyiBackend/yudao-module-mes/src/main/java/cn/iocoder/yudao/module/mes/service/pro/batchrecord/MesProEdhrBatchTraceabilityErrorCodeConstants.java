package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public final class MesProEdhrBatchTraceabilityErrorCodeConstants {

    private MesProEdhrBatchTraceabilityErrorCodeConstants() {
    }

    public static final ErrorCode BATCH_NOT_EXISTS = new ErrorCode(1_040_760_400, "批次执行不存在");
    public static final ErrorCode TRACE_CAPTURE_BLOCKED = new ErrorCode(1_040_760_401, "批次来源映射被阻断：{}");
    public static final ErrorCode TRACE_IDEMPOTENCY_CONFLICT = new ErrorCode(1_040_760_402, "批次来源幂等键与已存在来源不一致");
    public static final ErrorCode TRACE_SOURCE_CONFLICT = new ErrorCode(1_040_760_403, "批次来源快照或来源身份冲突");
    public static final ErrorCode TRACE_PERSIST_FAILED = new ErrorCode(1_040_760_404, "批次来源映射持久化失败");
    public static final ErrorCode RELEASE_DECISION_SOURCE_REQUIRED = new ErrorCode(1_040_760_405, "放行决定必须携带正式来源快照");
    public static final ErrorCode FLOW8_SOURCE_PRECHECK_REQUIRED = new ErrorCode(1_040_760_406, "流程8来源预检快照不完整");
    public static final ErrorCode FLOW8_SOURCE_PRECHECK_STALE = new ErrorCode(1_040_760_407, "流程8来源在预检后发生变化");
    public static final ErrorCode FLOW8_TRACE_LINK_ORIGIN_MISMATCH = new ErrorCode(1_040_760_408, "流程8批次与来源关系不一致");
}
