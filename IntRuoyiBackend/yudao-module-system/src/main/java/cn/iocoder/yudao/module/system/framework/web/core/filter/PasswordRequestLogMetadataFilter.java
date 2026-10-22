package cn.iocoder.yudao.module.system.framework.web.core.filter;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.module.system.controller.admin.user.UserController;
import cn.iocoder.yudao.module.system.controller.admin.user.UserProfileController;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.profile.UserProfileUpdatePasswordReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserUpdatePasswordReqVO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.server.RequestPath;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor.ATTRIBUTE_HANDLER_METHOD;
import static cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor.ATTRIBUTE_PROTECTED_REQUEST_URI;

/** Supplies logging privacy metadata before authentication; never invokes the mapped endpoint. */
public final class PasswordRequestLogMetadataFilter extends OncePerRequestFilter {

    private final List<ProtectedEndpoint> endpoints;

    public PasswordRequestLogMetadataFilter(WebProperties properties, ObjectProvider<UserController> users,
                                           ObjectProvider<UserProfileController> profiles) {
        endpoints = List.of(
                endpoint(properties, UserController.class, "updateUserPassword", UserUpdatePasswordReqVO.class, users),
                endpoint(properties, UserProfileController.class, "updateUserProfilePassword",
                        UserProfileUpdatePasswordReqVO.class, profiles));
        Assert.state(!endpoints.get(0).pattern().getPatternString().equals(endpoints.get(1).pattern().getPatternString()),
                "Password log metadata mappings must be distinct");
    }

    private static ProtectedEndpoint endpoint(WebProperties properties, Class<?> controllerClass, String methodName,
                                              Class<?> requestClass, ObjectProvider<?> controller) {
        WebProperties.Api api = properties.getAdminApi();
        Assert.notNull(api, "Admin API configuration is required for password log privacy");
        Assert.hasText(api.getPrefix(), "Admin API prefix is required for password log privacy");
        Assert.hasText(api.getController(), "Admin controller pattern is required for password log privacy");
        Assert.state(AnnotatedElementUtils.hasAnnotation(controllerClass, RestController.class)
                        && new AntPathMatcher(".").match(api.getController(), controllerClass.getPackageName()),
                "Password controller must receive the configured admin API prefix");
        Method method;
        try {
            method = controllerClass.getMethod(methodName, requestClass);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Password log privacy endpoint method is missing", ex);
        }
        RequestMapping root = AnnotatedElementUtils.findMergedAnnotation(controllerClass, RequestMapping.class);
        PutMapping action = AnnotatedElementUtils.findMergedAnnotation(method, PutMapping.class);
        ApiAccessLog privacy = AnnotatedElementUtils.findMergedAnnotation(method, ApiAccessLog.class);
        Assert.state(root != null && action != null, "Password endpoint mappings are required");
        Assert.state(privacy != null && privacy.enable() && !privacy.requestEnable(),
                "Password access auditing must remain enabled without request parameters");
        String rootPath = singlePath(root.path());
        String actionPath = singlePath(action.path());
        Assert.state(api.getPrefix().startsWith("/") && !api.getPrefix().endsWith("/"),
                "Admin API prefix must be an absolute path without a trailing slash");
        PathPattern pattern = new PathPatternParser().parse(api.getPrefix() + rootPath + actionPath);
        Assert.state(!pattern.hasPatternSyntax(), "Password privacy endpoint must have a fixed path");
        return new ProtectedEndpoint(pattern, method, controller);
    }

    private static String singlePath(String[] paths) {
        Assert.state(paths.length == 1 && paths[0].startsWith("/"),
                "Password privacy mapping must contain one absolute path");
        return paths[0];
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        RequestPath path = RequestPath.parse(request.getRequestURI(), request.getContextPath());
        // All methods are private on these paths, including requests rejected before MVC dispatch.
        for (ProtectedEndpoint endpoint : endpoints) {
            if (endpoint.pattern().matches(path.pathWithinApplication())) {
                request.setAttribute(ATTRIBUTE_PROTECTED_REQUEST_URI,
                        request.getContextPath() + endpoint.pattern().getPatternString());
                request.setAttribute(ATTRIBUTE_HANDLER_METHOD,
                        new HandlerMethod(endpoint.controller().getObject(), endpoint.method()));
                break;
            }
        }
        chain.doFilter(request, response);
    }

    private record ProtectedEndpoint(PathPattern pattern, Method method, ObjectProvider<?> controller) {
    }
}
