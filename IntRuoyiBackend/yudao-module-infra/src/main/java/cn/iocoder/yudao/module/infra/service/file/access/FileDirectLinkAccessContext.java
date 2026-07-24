package cn.iocoder.yudao.module.infra.service.file.access;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public record FileDirectLinkAccessContext(String sourceIp, String userAgent, String requestId) {

    public static final String DCC_REQUEST_ID_HEADER = "X-DCC-Request-Id";

    public FileDirectLinkAccessContext {
        sourceIp = requireText(sourceIp, "sourceIp");
        userAgent = requireText(userAgent, "userAgent");
        requestId = requireText(requestId, "requestId");
    }

    public static FileDirectLinkAccessContext from(HttpServletRequest request) {
        return new FileDirectLinkAccessContext(ServletUtils.getClientIP(request), ServletUtils.getUserAgent(request),
                resolveRequestId(request));
    }

    private static String resolveRequestId(HttpServletRequest request) {
        String requestId = StrUtil.trimToNull(request.getHeader(DCC_REQUEST_ID_HEADER));
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
        return "DCC-DIRECT-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private static String requireText(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return StrUtil.trim(value);
    }
}
