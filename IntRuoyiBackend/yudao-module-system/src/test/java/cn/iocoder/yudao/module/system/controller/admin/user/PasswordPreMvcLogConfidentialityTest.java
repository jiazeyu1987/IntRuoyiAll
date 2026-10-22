package cn.iocoder.yudao.module.system.controller.admin.user;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.iocoder.yudao.framework.apilog.core.filter.ApiAccessLogFilter;
import cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor;
import cn.iocoder.yudao.framework.common.biz.infra.logger.ApiAccessLogCommonApi;
import cn.iocoder.yudao.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.iocoder.yudao.framework.common.biz.infra.logger.dto.ApiAccessLogCreateReqDTO;
import cn.iocoder.yudao.framework.common.biz.infra.logger.dto.ApiErrorLogCreateReqDTO;
import cn.iocoder.yudao.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.spring.SpringUtils;
import cn.iocoder.yudao.framework.security.config.SecurityProperties;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.filter.TokenAuthenticationFilter;
import cn.iocoder.yudao.framework.security.core.handler.AccessDeniedHandlerImpl;
import cn.iocoder.yudao.framework.security.core.handler.AuthenticationEntryPointImpl;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.security.TenantSecurityWebFilter;
import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.system.framework.web.core.filter.PasswordRequestLogMetadataFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.firewall.HttpStatusRequestRejectedHandler;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import static cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual pre-MVC components; only external token/tenant services and log delivery are mocked. */
@SpringJUnitConfig(PasswordPreMvcLogConfidentialityTest.Context.class)
@ActiveProfiles("local")
class PasswordPreMvcLogConfidentialityTest {
    private static final String SECRET = "matrix-confidential-fixture";
    private static final List<String> PATHS = List.of("/admin-api/system/user/update-password",
            "/admin-api/system/user/profile/update-password");
    private final WebProperties properties = new WebProperties();
    private final ApiAccessLogCommonApi access = mock(ApiAccessLogCommonApi.class);
    private final ApiErrorLogCommonApi errors = mock(ApiErrorLogCommonApi.class);
    private final OAuth2TokenCommonApi tokens = mock(OAuth2TokenCommonApi.class);
    private final TenantFrameworkService tenants = mock(TenantFrameworkService.class);
    private final GlobalExceptionHandler exceptions = new GlobalExceptionHandler("pre-mvc", errors);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final List<Logger> loggers = new ArrayList<>();
    private final List<Level> levels = new ArrayList<>();
    private PasswordRequestLogMetadataFilter metadata;

    @BeforeEach void setUp() {
        new WebFrameworkUtils(properties);
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("users", new UserController());
        beans.registerSingleton("profiles", new UserProfileController());
        metadata = new PasswordRequestLogMetadataFilter(properties, beans.getBeanProvider(UserController.class),
                beans.getBeanProvider(UserProfileController.class));
        logs.start();
        for (Class<?> type : List.of(ApiAccessLogFilter.class, ApiAccessLogInterceptor.class, GlobalExceptionHandler.class,
                AuthenticationEntryPointImpl.class, AccessDeniedHandlerImpl.class, TenantSecurityWebFilter.class)) {
            Logger logger = (Logger) LoggerFactory.getLogger(type);
            loggers.add(logger); levels.add(logger.getLevel());
            logger.setLevel(Level.DEBUG); logger.addAppender(logs);
        }
    }
    @AfterEach void tearDown() {
        for (int i = 0; i < loggers.size(); i++) {
            loggers.get(i).detachAppender(logs); loggers.get(i).setLevel(levels.get(i));
        }
        logs.stop(); SecurityContextHolder.clearContext(); TenantContextHolder.clear();
    }

    @Test void realAuthenticationAndAuthorizationHandlers_preserveResponseAndAuditFailure() throws Exception {
        for (String path : PATHS) {
            reset(access);
            Capture unauthenticated = run(path, (request, response) -> new AuthenticationEntryPointImpl().commence(
                    (HttpServletRequest) request, (jakarta.servlet.http.HttpServletResponse) response,
                    new InsufficientAuthenticationException(SECRET)), false);
            assertRejection(unauthenticated, 401, path);
            reset(access);
            Capture forbidden = run(path, (request, response) -> new AccessDeniedHandlerImpl().handle(
                    (HttpServletRequest) request, (jakarta.servlet.http.HttpServletResponse) response,
                    new AccessDeniedException(SECRET, new IllegalStateException(SECRET))), false);
            assertRejection(forbidden, 403, path);
        }
    }

    @Test void realTokenFilter_internalFailureAndErrorLogDeliveryFailureStayPrivate() throws Exception {
        when(tokens.checkAccessToken(anyString())).thenThrow(new IllegalStateException(SECRET));
        doThrow(new IllegalStateException(SECRET)).when(errors).createApiErrorLogAsync(any());
        for (String path : PATHS) {
            reset(access, errors);
            doThrow(new IllegalStateException(SECRET)).when(errors).createApiErrorLogAsync(any());
            TokenAuthenticationFilter token = new TokenAuthenticationFilter(new SecurityProperties(), exceptions, tokens);
            Capture captured = run(path, (request, response) -> token.doFilter(request, response,
                    (nextRequest, nextResponse) -> fail("Token failure must not reach a controller")), false);
            assertRejection(captured, 500, path);
            ArgumentCaptor<ApiErrorLogCreateReqDTO> error = ArgumentCaptor.forClass(ApiErrorLogCreateReqDTO.class);
            verify(errors).createApiErrorLogAsync(error.capture());
            assertEquals(path, error.getValue().getRequestUrl());
            assertFalse(JsonUtils.toJsonString(error.getValue()).contains(SECRET));
        }
    }

    @Test void realTenantFilter_missingCrossTenantAndValidationFailureKeepTheirCodes() throws Exception {
        TenantSecurityWebFilter tenant = new TenantSecurityWebFilter(properties, new TenantProperties(), Set.of(),
                exceptions, tenants);
        for (String path : PATHS) {
            for (int mode = 0; mode < 3; mode++) {
                reset(access, tenants, errors); SecurityContextHolder.clearContext(); TenantContextHolder.clear();
                if (mode == 1) {
                    TenantContextHolder.setTenantId(2L);
                    SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(123L).setUserType(2).setTenantId(1L),
                            new MockHttpServletRequest());
                } else if (mode == 2) {
                    TenantContextHolder.setTenantId(1L);
                    doThrow(new IllegalStateException(SECRET)).when(tenants).validTenant(1L);
                }
                Capture captured = run(path, (request, response) -> tenant.doFilter(request, response,
                        (nextRequest, nextResponse) -> fail("Tenant rejection must not reach a controller")), false);
                assertRejection(captured, mode == 0 ? 400 : mode == 1 ? 403 : 500, path);
                if (mode < 2) { verifyNoInteractions(tenants); }
            }
        }
    }

    @Test void actualSecurityFirewall_matrixRejectionUsesHttpStatusWithoutResponseParsing() throws Exception {
        FilterChainProxy security = new FilterChainProxy(new DefaultSecurityFilterChain(AnyRequestMatcher.INSTANCE,
                List.of()));
        security.setFirewall(new StrictHttpFirewall());
        security.setRequestRejectedHandler(new HttpStatusRequestRejectedHandler());
        for (String path : PATHS) {
            reset(access);
            AtomicBoolean downstream = new AtomicBoolean();
            Capture captured = run(path, (request, response) -> security.doFilter(request, response,
                    (nextRequest, nextResponse) -> downstream.set(true)), false);
            assertFalse(downstream.get());
            assertEquals(400, captured.response().getStatus());
            assertEquals(400, captured.log().getResultCode());
            assertEquals(path, captured.log().getRequestUrl());
            assertPrivate(captured);
        }
    }

    @Test void matrixUriAndLogDeliveryFailure_stayPrivateInActualInterceptor() throws Exception {
        for (String path : PATHS) {
            reset(access);
            doThrow(new IllegalStateException(SECRET)).when(access).createApiAccessLogAsync(any());
            Capture captured = run(path, (request, response) -> {
                HttpServletRequest http = (HttpServletRequest) request;
                ApiAccessLogInterceptor interceptor = new ApiAccessLogInterceptor();
                HandlerMethod handler = (HandlerMethod) http.getAttribute(ATTRIBUTE_HANDLER_METHOD);
                interceptor.preHandle(http, (jakarta.servlet.http.HttpServletResponse) response, handler);
                WebFrameworkUtils.setCommonResult(request, CommonResult.error(400, "请求参数不正确"));
                interceptor.afterCompletion(http, (jakarta.servlet.http.HttpServletResponse) response, handler, null);
            }, false);
            assertEquals(path, captured.log().getRequestUrl()); assertPrivate(captured);
        }
    }

    @Test void ordinaryPaths_keepResponseRawUriThrowableAndExistingResultAttributeBehavior() throws Exception {
        String ordinary = "/admin-api/ordinary/update";
        Capture denied = run(ordinary, (request, response) -> new AccessDeniedHandlerImpl().handle(
                (HttpServletRequest) request, (jakarta.servlet.http.HttpServletResponse) response,
                new AccessDeniedException("ordinary diagnostic")), true);
        assertEquals(403, JsonUtils.parseObject(denied.response().getContentAsString(), CommonResult.class).getCode());
        assertNull(WebFrameworkUtils.getCommonResult(denied.request()));
        assertEquals(0, denied.log().getResultCode()); // Ordinary audit semantics are outside this fix.
        assertEquals(denied.request().getRequestURI(), denied.log().getRequestUrl());
        assertTrue(logs.list.stream().anyMatch(event -> event.getThrowableProxy() != null));
        reset(access);
        Capture unauthorized = run(ordinary, (request, response) -> new AuthenticationEntryPointImpl().commence(
                (HttpServletRequest) request, (jakarta.servlet.http.HttpServletResponse) response,
                new InsufficientAuthenticationException("ordinary diagnostic")), true);
        assertNull(WebFrameworkUtils.getCommonResult(unauthorized.request()));
        assertEquals(401, JsonUtils.parseObject(unauthorized.response().getContentAsString(), CommonResult.class).getCode());
        reset(access);
        Capture httpError = run(ordinary, (request, response) ->
                ((jakarta.servlet.http.HttpServletResponse) response).setStatus(400), true);
        assertNull(WebFrameworkUtils.getCommonResult(httpError.request())); assertEquals(0, httpError.log().getResultCode());
    }

    @Test void missingFixedUri_isAnExplicitPrerequisiteFailure() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", PATHS.get(0));
        request.setAttribute(ATTRIBUTE_HANDLER_METHOD, new HandlerMethod(new UserController(),
                UserController.class.getMethod("updateUserPassword",
                        cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserUpdatePasswordReqVO.class)));
        assertThrows(IllegalStateException.class, () -> getLogRequestUri(request));
        assertThrows(IllegalStateException.class, () -> new ApiAccessLogFilter(properties, "pre-mvc", access)
                .doFilter(request, new MockHttpServletResponse(), (nextRequest, nextResponse) -> fail("Missing metadata")));
        verifyNoInteractions(access);
    }

    @Test void ordinaryTokenAndTenantFilters_preserveExistingResultAttributeBehavior() throws Exception {
        String path = "/admin-api/ordinary/update";
        when(tokens.checkAccessToken(anyString())).thenThrow(new IllegalStateException("ordinary token error"));
        TokenAuthenticationFilter token = new TokenAuthenticationFilter(new SecurityProperties(), exceptions, tokens);
        Capture tokenFailure = run(path, (request, response) -> token.doFilter(request, response,
                (nextRequest, nextResponse) -> fail("Token error must stop the chain")), true);
        assertNull(WebFrameworkUtils.getCommonResult(tokenFailure.request()));
        assertEquals(500, JsonUtils.parseObject(tokenFailure.response().getContentAsString(), CommonResult.class).getCode());
        assertEquals(0, tokenFailure.log().getResultCode());
        assertEquals(tokenFailure.request().getRequestURI(), tokenFailure.log().getRequestUrl());
        assertTrue(logs.list.stream().anyMatch(event -> event.getThrowableProxy() != null));
        TenantSecurityWebFilter tenant = new TenantSecurityWebFilter(properties, new TenantProperties(), Set.of(),
                exceptions, tenants);
        for (int mode = 0; mode < 3; mode++) {
            reset(access, tenants, errors); SecurityContextHolder.clearContext(); TenantContextHolder.clear();
            if (mode == 1) {
                TenantContextHolder.setTenantId(2L);
                SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(123L).setUserType(2).setTenantId(1L),
                        new MockHttpServletRequest());
            } else if (mode == 2) {
                TenantContextHolder.setTenantId(1L);
                doThrow(new IllegalStateException("ordinary tenant error")).when(tenants).validTenant(1L);
            }
            Capture captured = run(path, (request, response) -> tenant.doFilter(request, response,
                    (nextRequest, nextResponse) -> fail("Tenant error must stop the chain")), true);
            assertNull(WebFrameworkUtils.getCommonResult(captured.request()));
            assertEquals(mode == 0 ? 400 : mode == 1 ? 403 : 500,
                    JsonUtils.parseObject(captured.response().getContentAsString(), CommonResult.class).getCode());
            assertEquals(0, captured.log().getResultCode());
            assertEquals(captured.request().getRequestURI(), captured.log().getRequestUrl());
        }
    }

    private Capture run(String path, FilterChain rejection, boolean ordinary) throws Exception {
        logs.list.clear();
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", path.replace("/user/", "/user;newPassword=" + SECRET + "/"));
        if (ordinary) { request.setRequestURI(path + ";description=ordinary-fixture"); }
        request.setServletPath(request.getRequestURI());
        request.setContentType("application/json"); request.addHeader("Authorization", "Bearer fixture-token");
        request.addParameter("newPassword", SECRET);
        request.setContent(("{\"newPassword\":\"" + SECRET + "\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        metadata.doFilter(request, response, (markedRequest, markedResponse) ->
                new ApiAccessLogFilter(properties, "pre-mvc", access).doFilter(markedRequest, markedResponse, rejection));
        ArgumentCaptor<ApiAccessLogCreateReqDTO> capture = ArgumentCaptor.forClass(ApiAccessLogCreateReqDTO.class);
        verify(access).createApiAccessLogAsync(capture.capture());
        return new Capture(request, response, capture.getValue());
    }
    private void assertRejection(Capture capture, int code, String path) throws Exception {
        assertEquals(200, capture.response().getStatus()); // Original CommonResult response behavior.
        assertEquals(code, JsonUtils.parseObject(capture.response().getContentAsString(), CommonResult.class).getCode());
        assertEquals(code, capture.log().getResultCode());
        assertEquals(code, WebFrameworkUtils.getCommonResult(capture.request()).getCode());
        assertEquals(path, capture.log().getRequestUrl()); assertPrivate(capture);
    }
    private void assertPrivate(Capture capture) throws Exception {
        assertNull(capture.log().getRequestParams());
        assertFalse(JsonUtils.toJsonString(capture.log()).contains(SECRET));
        assertFalse(capture.response().getContentAsString().contains(SECRET));
        for (ILoggingEvent event : logs.list) {
            assertFalse(event.getFormattedMessage().contains(SECRET));
            assertNull(event.getThrowableProxy());
        }
    }
    private record Capture(MockHttpServletRequest request, MockHttpServletResponse response, ApiAccessLogCreateReqDTO log) { }
    @Configuration(proxyBeanMethods = false) static class Context {
        @Bean SpringUtils springUtils() { return new SpringUtils(); }
    }
}
