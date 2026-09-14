package cn.iocoder.yudao.module.dcc.framework.web;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
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
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.HEADER_TENANT_ID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_SLOT_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;

public class DccApiHttpContractFilter extends OncePerRequestFilter {

    private final Set<String> targetPaths;
    private final WebProperties webProperties;

    public DccApiHttpContractFilter(WebProperties webProperties) {
        this.webProperties = webProperties;
        this.targetPaths = DccUploadApiContractPaths.resolve(webProperties);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = DccUploadApiContractPaths.requestPath(request);
        return !targetPaths.contains(requestPath) && !DccWorkflowApiContractPaths.matches(webProperties, requestPath);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, responseWrapper);
            applyHttpContract(request, responseWrapper);
        } finally {
            responseWrapper.copyBodyToResponse();
        }
    }

    private void applyHttpContract(HttpServletRequest request, ContentCachingResponseWrapper responseWrapper)
            throws ServletException, IOException {
        if (responseWrapper.getContentSize() == 0 || !isJson(responseWrapper.getContentType())) {
            return;
        }
        JsonNode responseBody;
        try {
            responseBody = JsonUtils.parseTree(responseWrapper.getContentAsByteArray());
        } catch (RuntimeException ex) {
            throw new ServletException("DCC target endpoint returned invalid JSON", ex);
        }
        JsonNode codeNode = responseBody.get("code");
        if (codeNode == null || !codeNode.canConvertToInt()) {
            throw new ServletException("DCC target endpoint response is missing integer business code");
        }
        int businessCode = codeNode.intValue();
        if (businessCode == 0 || businessCode == 200) {
            return;
        }
        if (businessCode == HttpStatus.FORBIDDEN.value() && hasNonPositiveTenantHeader(request)) {
            replaceWithBadTenantResponse(responseWrapper);
            return;
        }
        responseWrapper.setStatus(resolveHttpStatus(businessCode));
    }

    private int resolveHttpStatus(int businessCode) {
        if (businessCode == HttpStatus.BAD_REQUEST.value()
                || businessCode == HttpStatus.UNAUTHORIZED.value()
                || businessCode == HttpStatus.FORBIDDEN.value()
                || businessCode == HttpStatus.NOT_FOUND.value()
                || businessCode == HttpStatus.METHOD_NOT_ALLOWED.value()
                || businessCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            return businessCode;
        }
        if (businessCode == CONTROLLED_FILE_UPLOAD_SLOT_CONFLICT.getCode()) {
            return HttpStatus.CONFLICT.value();
        }
        if (businessCode == CONTROLLED_FILE_NOT_EXISTS.getCode()
                || businessCode == FILE_CATEGORY_NOT_EXISTS.getCode()) {
            return HttpStatus.NOT_FOUND.value();
        }
        return HttpStatus.BAD_REQUEST.value();
    }

    private boolean hasNonPositiveTenantHeader(HttpServletRequest request) {
        String rawTenantId = request.getHeader(HEADER_TENANT_ID);
        if (rawTenantId == null || !rawTenantId.trim().matches("[+-]?[0-9]+")) {
            return false;
        }
        try {
            return Long.parseLong(rawTenantId.trim()) <= 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void replaceWithBadTenantResponse(ContentCachingResponseWrapper responseWrapper) throws IOException {
        responseWrapper.resetBuffer();
        responseWrapper.setStatus(HttpStatus.BAD_REQUEST.value());
        responseWrapper.setContentType(MediaType.APPLICATION_JSON_VALUE);
        responseWrapper.getOutputStream().write(JsonUtils.toJsonByte(CommonResult.error(BAD_REQUEST.getCode(),
                "DCC 请求必须显式携带正整数 tenant-id")));
    }

    private boolean isJson(String contentType) {
        return contentType != null && contentType.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE);
    }
}
