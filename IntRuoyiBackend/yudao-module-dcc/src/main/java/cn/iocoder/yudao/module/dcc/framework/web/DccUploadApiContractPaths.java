package cn.iocoder.yudao.module.dcc.framework.web;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Set;

final class DccUploadApiContractPaths {

    private static final List<String> RELATIVE_PATHS = List.of(
            "/dcc/controlled-files/upload-preview",
            "/dcc/controlled-files/upload-temporary/status",
            "/dcc/controlled-files/upload-temporary/session-cleanup");

    private DccUploadApiContractPaths() {
    }

    static Set<String> resolve(WebProperties webProperties) {
        String adminPrefix = StrUtil.removeSuffix(webProperties.getAdminApi().getPrefix(), "/");
        return Set.copyOf(RELATIVE_PATHS.stream().map(path -> adminPrefix + path).toList());
    }

    static String requestPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
