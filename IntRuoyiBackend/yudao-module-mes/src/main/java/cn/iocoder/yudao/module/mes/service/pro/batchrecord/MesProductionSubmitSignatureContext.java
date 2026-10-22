package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/** Exact server-authorized identity of one production submission, stable on retries. */
public record MesProductionSubmitSignatureContext(Long activeOrderId, Long routeProcessId,
                                                   Long processId, String submissionIdempotencyKey) {
    public static final String SOURCE_TYPE = "MES_PRODUCTION_SUBMISSION";
    public static final ErrorCode CONTEXT_INVALID = new ErrorCode(1_040_760_454,
            "生产提交签名缺少准确的活跃订单、工序或提交身份");

    public MesProductionSubmitSignatureContext {
        if (activeOrderId == null || activeOrderId <= 0 || routeProcessId == null || routeProcessId <= 0
                || processId == null || processId <= 0 || submissionIdempotencyKey == null
                || submissionIdempotencyKey.isBlank()) {
            throw exception(CONTEXT_INVALID);
        }
    }

    public String sourceName() {
        return JsonUtils.toJsonString(this);
    }

    /** Fixed-length evidence fingerprint fits the employee projection's 128-character column. */
    public String projectionSourceName() {
        return "PRODUCTION_SUBMISSION_SHA256:" + DigestUtil.sha256Hex(sourceName());
    }
}
