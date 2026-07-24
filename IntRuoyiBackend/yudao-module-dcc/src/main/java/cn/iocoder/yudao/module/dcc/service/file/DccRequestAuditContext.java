package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

public record DccRequestAuditContext(String sourceIp, String userAgent, String requestId) {

    public static final String REQUEST_ID_HEADER = "X-DCC-Request-Id";

    public DccRequestAuditContext {
        sourceIp = requireText(sourceIp, "sourceIp");
        userAgent = requireText(userAgent, "userAgent");
        requestId = StrUtil.trimToNull(requestId);
    }

    public DccRequestAuditContext withRequestId(String requestId) {
        return new DccRequestAuditContext(sourceIp, userAgent, requestId);
    }

    public String requireRequestId(String purpose) {
        if (StrUtil.isBlank(requestId)) {
            throw invalidParamException("DCC audit requestId is required for {}", purpose);
        }
        return requestId;
    }

    public String requestIdOr(String explicitRequestId) {
        if (StrUtil.isNotBlank(requestId)) {
            return requestId;
        }
        return requireText(explicitRequestId, "requestId");
    }

    public static DccRequestAuditContext from(HttpServletRequest request, String explicitRequestId) {
        return new DccRequestAuditContext(ServletUtils.getClientIP(request), ServletUtils.getUserAgent(request),
                resolveRequestId(request, explicitRequestId));
    }

    private static String resolveRequestId(HttpServletRequest request, String explicitRequestId) {
        String requestId = StrUtil.trimToNull(request.getHeader(REQUEST_ID_HEADER));
        if (requestId != null) {
            return requestId;
        }
        requestId = StrUtil.trimToNull(request.getHeader("X-Request-Id"));
        if (requestId != null) {
            return requestId;
        }
        requestId = StrUtil.trimToNull(request.getHeader("trace-id"));
        if (requestId != null) {
            return requestId;
        }
        if (StrUtil.isNotBlank(explicitRequestId)) {
            return StrUtil.trim(explicitRequestId);
        }
        return "DCC-REQ-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private static String requireText(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            throw invalidParamException("DCC audit {} is required", fieldName);
        }
        return StrUtil.trim(value);
    }
}
