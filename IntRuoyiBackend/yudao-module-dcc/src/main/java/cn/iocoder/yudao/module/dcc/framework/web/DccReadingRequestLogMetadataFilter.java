package cn.iocoder.yudao.module.dcc.framework.web;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.module.dcc.controller.admin.filepreview.DccPreviewReadingSessionController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.server.RequestPath;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor.ATTRIBUTE_HANDLER_METHOD;
import static cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor.ATTRIBUTE_PROTECTED_REQUEST_URI;

/** Reading evidence stays private even when authentication rejects a request before MVC. */
public final class DccReadingRequestLogMetadataFilter extends OncePerRequestFilter {
    private record Endpoint(PathPattern pattern, Method method) { }
    private final List<Endpoint> endpoints;
    private final ObjectProvider<DccPreviewReadingSessionController> controller;

    public DccReadingRequestLogMetadataFilter(WebProperties properties,
            ObjectProvider<DccPreviewReadingSessionController> controller) {
        this.controller = controller;
        var api = properties.getAdminApi();
        Assert.notNull(api, "DCC reading admin API configuration is required");
        Assert.hasText(api.getPrefix(), "DCC reading admin API prefix is required");
        Assert.hasText(api.getController(), "DCC reading admin controller pattern is required");
        Assert.state(api.getPrefix().startsWith("/") && !api.getPrefix().endsWith("/"),
                "DCC reading admin API prefix must be absolute without a trailing slash");
        Assert.state(new AntPathMatcher(".").match(api.getController(),
                        DccPreviewReadingSessionController.class.getPackageName()),
                "DCC reading controller must receive the admin API prefix");
        List<Endpoint> mappings = new ArrayList<>();
        for (Method method : DccPreviewReadingSessionController.class.getDeclaredMethods()) {
            var mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (mapping == null) continue;
            var privacy = AnnotatedElementUtils.findMergedAnnotation(method, ApiAccessLog.class);
            Assert.state(privacy != null && privacy.enable() && !privacy.requestEnable(),
                    "DCC reading access auditing must remain enabled without request parameters");
            Assert.state(mapping.path().length == 1 && mapping.path()[0].startsWith("/"),
                    "DCC reading endpoint must have one absolute mapping");
            mappings.add(new Endpoint(new PathPatternParser().parse(api.getPrefix() + mapping.path()[0]), method));
        }
        Assert.notEmpty(mappings, "DCC reading endpoint mappings are required");
        endpoints = List.copyOf(mappings);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var path = RequestPath.parse(request.getRequestURI(), request.getContextPath());
        // Mark every HTTP method on these paths, including rejected methods, without reading evidence.
        for (Endpoint endpoint : endpoints) {
            if (!endpoint.pattern().matches(path.pathWithinApplication())) continue;
            request.setAttribute(ATTRIBUTE_PROTECTED_REQUEST_URI,
                    request.getContextPath() + endpoint.pattern().getPatternString());
            request.setAttribute(ATTRIBUTE_HANDLER_METHOD, new HandlerMethod(controller.getObject(), endpoint.method()));
            break;
        }
        chain.doFilter(request, response);
    }
}
