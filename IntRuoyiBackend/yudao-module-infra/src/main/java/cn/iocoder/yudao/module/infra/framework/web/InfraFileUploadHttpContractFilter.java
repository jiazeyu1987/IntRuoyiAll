package cn.iocoder.yudao.module.infra.framework.web;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Locale;

import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_UPLOAD_EXECUTABLE_BLOCKED;

public class InfraFileUploadHttpContractFilter extends OncePerRequestFilter {

    private final String targetPath;

    public InfraFileUploadHttpContractFilter(WebProperties webProperties) {
        this.targetPath = resolveTargetPath(webProperties);
    }

    static String resolveTargetPath(WebProperties webProperties) {
        return StrUtil.removeSuffix(webProperties.getAdminApi().getPrefix(), "/") + "/infra/file/upload";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI().substring(request.getContextPath().length());
        return !targetPath.equals(requestPath);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, responseWrapper);
            applyHttpContract(responseWrapper);
        } finally {
            responseWrapper.copyBodyToResponse();
        }
    }

    private void applyHttpContract(ContentCachingResponseWrapper responseWrapper) throws ServletException {
        if (responseWrapper.getContentSize() == 0 || !isJson(responseWrapper.getContentType())) {
            return;
        }
        JsonNode responseBody;
        try {
            responseBody = JsonUtils.parseTree(responseWrapper.getContentAsByteArray());
        } catch (RuntimeException ex) {
            throw new ServletException("Infra file upload endpoint returned invalid JSON", ex);
        }
        JsonNode codeNode = responseBody.get("code");
        if (codeNode == null || !codeNode.canConvertToInt()) {
            throw new ServletException("Infra file upload endpoint response is missing integer business code");
        }
        if (codeNode.intValue() == FILE_UPLOAD_EXECUTABLE_BLOCKED.getCode()) {
            responseWrapper.setStatus(HttpStatus.BAD_REQUEST.value());
        }
    }

    private boolean isJson(String contentType) {
        return contentType != null && contentType.toLowerCase(Locale.ROOT).contains(MediaType.APPLICATION_JSON_VALUE);
    }
}
