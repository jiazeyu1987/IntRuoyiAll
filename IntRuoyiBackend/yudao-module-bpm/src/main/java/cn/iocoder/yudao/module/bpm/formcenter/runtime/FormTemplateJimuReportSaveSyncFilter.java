package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FormTemplateJimuReportSaveSyncFilter extends OncePerRequestFilter {

    private static final String JMREPORT_SAVE_PATH = "/jmreport/save";
    private static final String FORM_TEMPLATE_REPORT_PREFIX = "FORMTPL:";
    private static final List<String> TENANT_HEADERS = List.of(
            "tenant-id", "JmReport-Tenant-Id", "tenantId", "X-Tenant-Id");

    private final FormCenterRuntimeService formCenterRuntimeService;

    public FormTemplateJimuReportSaveSyncFilter(FormCenterRuntimeService formCenterRuntimeService) {
        this.formCenterRuntimeService = formCenterRuntimeService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isJimuSaveRequest(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        CachedJimuSaveRequest cachedRequest = new CachedJimuSaveRequest(request, body);
        String reportId;
        try {
            String requestBody = new String(body, resolveCharset(request.getCharacterEncoding()));
            reportId = resolveFormTemplateReportId(requestBody);
        } catch (FormCenterException ex) {
            writeJimuError(response, ex);
            return;
        }
        if (reportId == null) {
            filterChain.doFilter(cachedRequest, response);
            return;
        }

        Long tenantId;
        try {
            tenantId = resolveRequiredTenantId(request);
            formCenterRuntimeService.validateTemplateJimuReportSaveWritable(reportId, tenantId);
        } catch (FormCenterException ex) {
            writeJimuError(response, ex);
            return;
        }

        Long oldTenantId = TenantContextHolder.getTenantId();
        boolean oldIgnore = TenantContextHolder.isIgnore();
        TenantContextHolder.setTenantId(tenantId);
        TenantContextHolder.setIgnore(false);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(cachedRequest, responseWrapper);
            String responseBody = new String(responseWrapper.getContentAsByteArray(),
                    resolveCharset(responseWrapper.getCharacterEncoding()));
            if (isJimuSaveSuccessResponse(responseBody, responseWrapper.getStatus())) {
                formCenterRuntimeService.syncTemplateJimuReportSave(reportId, tenantId);
            }
            responseWrapper.copyBodyToResponse();
        } catch (FormCenterException ex) {
            writeJimuError(responseWrapper, ex);
            responseWrapper.copyBodyToResponse();
        } finally {
            TenantContextHolder.setTenantId(oldTenantId);
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

    private boolean isJimuSaveRequest(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestUri;
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            path = requestUri.substring(contextPath.length());
        }
        return Objects.equals(JMREPORT_SAVE_PATH, path);
    }

    private String resolveFormTemplateReportId(String requestBody) {
        if (requestBody == null || requestBody.isBlank()) {
            return null;
        }
        Map<String, Object> root;
        try {
            root = JsonUtils.parseObject(requestBody, new TypeReference<Map<String, Object>>() {});
        } catch (RuntimeException ex) {
            if (requestBody.contains(FORM_TEMPLATE_REPORT_PREFIX)) {
                throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                        "Form template Jimu save payload is invalid: " + ex.getMessage());
            }
            return null;
        }
        String reportId = normalizeText(readPath(root, "designerObj", "id"));
        if (reportId == null) {
            reportId = normalizeText(root.get("excel_config_id"));
        }
        if (reportId == null) {
            reportId = normalizeText(root.get("id"));
        }
        return reportId != null && reportId.startsWith(FORM_TEMPLATE_REPORT_PREFIX) ? reportId : null;
    }

    private Object readPath(Map<String, Object> root, String mapKey, String valueKey) {
        Object nested = root.get(mapKey);
        if (!(nested instanceof Map<?, ?> nestedMap)) {
            return null;
        }
        return nestedMap.get(valueKey);
    }

    private String normalizeText(Object value) {
        if (!(value instanceof String text)) {
            return null;
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Long resolveRequiredTenantId(HttpServletRequest request) {
        for (String header : TENANT_HEADERS) {
            String rawTenantId = request.getHeader(header);
            if (rawTenantId == null || rawTenantId.isBlank()) {
                continue;
            }
            try {
                return Long.valueOf(rawTenantId.trim());
            } catch (NumberFormatException ex) {
                throw new FormCenterException(FormCenterErrorCode.FORM_ACTION_CONTEXT_INVALID,
                        "Tenant context is invalid for form template Jimu save");
            }
        }
        throw new FormCenterException(FormCenterErrorCode.FORM_ACTION_CONTEXT_INVALID,
                "Tenant context is required for form template Jimu save");
    }

    private boolean isJimuSaveSuccessResponse(String responseBody, int httpStatus) {
        if (httpStatus < 200 || httpStatus >= 300) {
            return false;
        }
        if (responseBody == null || responseBody.isBlank()) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Jimu save response is empty");
        }
        Map<String, Object> root;
        try {
            root = JsonUtils.parseObject(responseBody, new TypeReference<Map<String, Object>>() {});
        } catch (RuntimeException ex) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Jimu save response is invalid: " + ex.getMessage());
        }
        Object success = root == null ? null : root.get("success");
        if (success instanceof Boolean successValue) {
            return successValue;
        }
        if (success instanceof String successText && ("true".equalsIgnoreCase(successText)
                || "false".equalsIgnoreCase(successText))) {
            return Boolean.parseBoolean(successText);
        }
        throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                "Jimu save response success flag is missing");
    }

    private void writeJimuError(HttpServletResponse response, FormCenterException ex) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("code", ex.getErrorCode().getCode());
        result.put("message", ex.getMessage());
        result.put("msg", ex.getMessage());
        response.resetBuffer();
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JsonUtils.toJsonString(result));
        response.flushBuffer();
    }

    private Charset resolveCharset(String charsetName) {
        if (charsetName == null || charsetName.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(charsetName);
        } catch (RuntimeException ex) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Jimu save charset is invalid: " + charsetName);
        }
    }

    private static final class CachedJimuSaveRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedJimuSaveRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body == null ? new byte[0] : body;
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
            return new ServletInputStream() {

                @Override
                public boolean isFinished() {
                    return inputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }

                @Override
                public int read() {
                    return inputStream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
