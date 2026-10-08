package cn.iocoder.yudao.module.system.controller.admin.user;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor;
import cn.iocoder.yudao.framework.common.biz.infra.logger.ApiAccessLogCommonApi;
import cn.iocoder.yudao.framework.common.biz.infra.logger.dto.ApiAccessLogCreateReqDTO;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.spring.SpringUtils;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.framework.apilog.core.filter.ApiAccessLogFilter;
import cn.iocoder.yudao.framework.web.core.filter.CacheRequestBodyWrapper;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.profile.UserProfileUpdatePasswordReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserUpdatePasswordReqVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor.ATTRIBUTE_HANDLER_METHOD;
import static cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor.ATTRIBUTE_PROTECTED_REQUEST_URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Production filter/interceptor and endpoint metadata; MVC outcome and log delivery are external seams. */
class PasswordAccessLogConfidentialityTest {

    private static final String OLD_SECRET = "old-fixture-secret";
    private static final String NEW_SECRET = "new-fixture-secret";
    private static final String RESET_SECRET = "reset-fixture-secret";
    private static final String QUERY_SECRET = "query-fixture-secret";

    @BeforeEach
    void initializeWebConfiguration() {
        new WebFrameworkUtils(new WebProperties());
    }

    @Test
    void profilePasswordLog_omitsRequestParametersAndKeepsAuditFacts() throws Exception {
        ApiAccessLogCreateReqDTO log = captureLog("/admin-api/system/user/profile/update-password", profileHandler(),
                "{\"oldPassword\":\"old-fixture-secret\",\"newPassword\":\"new-fixture-secret\"}");

        assertNull(log.getRequestParams());
        assertEquals("修改用户个人密码", log.getOperateName());
        assertEquals(0, log.getResultCode());
    }

    @Test
    void adminResetLog_omitsRequestParametersAndKeepsAuditFacts() throws Exception {
        ApiAccessLogCreateReqDTO log = captureLog("/admin-api/system/user/update-password", adminHandler(),
                "{\"id\":456,\"password\":\"reset-fixture-secret\"}");

        assertNull(log.getRequestParams()); // Target identity remains in the separate service LogRecord.
        assertEquals("重置用户密码", log.getOperateName());
        assertEquals(0, log.getResultCode());
    }

    @Test
    void profilePassword_normalBodyAndQueryAreAbsentFromActualInterceptorAndFilterLogs() throws Exception {
        RequestLogs captured = capturePipeline(profileHandler(), "/admin-api/system/user/profile/update-password",
                "{\"oldPassword\":\"" + OLD_SECRET + "\",\"newPassword\":\"" + NEW_SECRET + "\"}",
                true, false, null);
        assertPasswordLogs(captured, 0);
    }

    @Test
    void adminReset_normalBodyAndQueryAreAbsentFromActualInterceptorAndFilterLogs() throws Exception {
        RequestLogs captured = capturePipeline(adminHandler(), "/admin-api/system/user/update-password",
                "{\"id\":456,\"password\":\"" + RESET_SECRET + "\"}", true, false, null);
        assertPasswordLogs(captured, 0);
    }

    @Test
    void passwordEndpoints_malformedBodyAndQueryDoNotEnterRawSanitizerLogs() throws Exception {
        for (HandlerMethod handler : List.of(profileHandler(), adminHandler())) {
            String uri = handler.getBeanType() == UserController.class
                    ? "/admin-api/system/user/update-password" : "/admin-api/system/user/profile/update-password";
            String body = handler.getBeanType() == UserController.class
                    ? "{\"password\":\"" + RESET_SECRET + "\""
                    : "{\"oldPassword\":\"" + OLD_SECRET + "\",\"newPassword\":\"" + NEW_SECRET + "\"";
            assertThrows(JsonProcessingException.class, () -> new ObjectMapper().readTree(body));
            // MVC's parse-error result is supplied at the seam; this is not an MVC exception-logger test.
            RequestLogs captured = capturePipeline(handler, uri, body, true, true, null);
            assertPasswordLogs(captured, 400);
        }
    }

    @Test
    void passwordInterceptor_requestLoggingDisabledDoesNotReadBodyOrQuery() throws Exception {
        for (HandlerMethod handler : List.of(profileHandler(), adminHandler())) {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/admin-api/system/user/update-password");
            when(request.getAttribute(ATTRIBUTE_PROTECTED_REQUEST_URI)).thenReturn("/admin-api/system/user/update-password");
            when(request.getAttribute(ATTRIBUTE_HANDLER_METHOD)).thenReturn(handler);
            ApiAccessLogInterceptor interceptor = new ApiAccessLogInterceptor();
            try (MockedStatic<SpringUtils> spring = mockStatic(SpringUtils.class)) {
                spring.when(SpringUtils::isProd).thenReturn(false);
                assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), handler));
            }
            verify(request, never()).getParameterMap();
            verify(request, never()).getReader();
            verify(request, never()).getInputStream();
            verify(request, never()).getContentType();
        }
    }

    @Test
    void passwordLogging_doesNotSwallowDownstreamFailureOrMarkItSuccessful() throws Exception {
        ServletException businessFailure = new ServletException(OLD_SECRET, new IllegalStateException(NEW_SECRET));
        RequestLogs captured = capturePipeline(profileHandler(), "/admin-api/system/user/profile/update-password",
                "{\"oldPassword\":\"" + OLD_SECRET + "\"", true, false, businessFailure);
        assertSame(businessFailure, captured.thrown());
        assertPasswordLogs(captured, 500);
        assertEquals(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getMsg(), captured.accessLog().getResultMsg());
    }

    @Test
    void passwordAccessFilter_preexistingMetadataPreventsReadingBodyAndQuery() throws Exception {
        for (HandlerMethod handler : List.of(profileHandler(), adminHandler())) {
            ApiAccessLogCommonApi logApi = mock(ApiAccessLogCommonApi.class);
            ApiAccessLogFilter filter = new ApiAccessLogFilter(new WebProperties(), "password-regression", logApi);
            MockHttpServletRequest request = spy(new MockHttpServletRequest("PUT",
                    "/admin-api/system/user/update-password"));
            request.setContentType("application/json");
            request.setContent(("{\"newPassword\":\"" + NEW_SECRET + "\"").getBytes(StandardCharsets.UTF_8));
            request.addParameter("unrecognizedParameter", QUERY_SECRET);
            request.setAttribute(ATTRIBUTE_HANDLER_METHOD, handler);
            request.setAttribute(ATTRIBUTE_PROTECTED_REQUEST_URI, "/admin-api/system/user/update-password");
            filter.doFilter(request, new MockHttpServletResponse(), (sameRequest, sameResponse) -> { });
            verify(request, never()).getParameterMap();
            verify(request, never()).getReader();
            verify(request, never()).getInputStream();
            verify(request, never()).getContentType();
            ArgumentCaptor<ApiAccessLogCreateReqDTO> logged = ArgumentCaptor.forClass(ApiAccessLogCreateReqDTO.class);
            verify(logApi).createApiAccessLogAsync(logged.capture());
            assertNull(logged.getValue().getRequestParams());
        }
    }

    @Test
    void passwordLogDeliveryFailure_logsOnlyPrivacyMarkerUrlAndExceptionType() throws Exception {
        IllegalStateException deliveryFailure = new IllegalStateException(RESET_SECRET,
                new IllegalArgumentException(QUERY_SECRET));
        for (HandlerMethod handler : List.of(profileHandler(), adminHandler())) {
            RequestLogs captured = capturePipeline(handler, "/admin-api/system/user/update-password",
                    "{\"password\":\"" + RESET_SECRET + "\"}", true, false, null, deliveryFailure);
            assertPasswordLogs(captured, 0);
            ILoggingEvent deliveryLog = captured.events().stream().filter(event ->
                    event.getFormattedMessage().contains("[createApiAccessLog]")).findFirst().orElseThrow();
            assertTrue(deliveryLog.getFormattedMessage().contains("requestParameters=disabled"));
            assertTrue(deliveryLog.getFormattedMessage().contains("/admin-api/system/user/update-password"));
            assertTrue(deliveryLog.getFormattedMessage().contains(IllegalStateException.class.getName()));
            assertNull(deliveryLog.getThrowableProxy());
        }
    }

    @Test
    void ordinaryEndpoint_exceptionResultAndDeliveryFailureKeepExistingDiagnosticBehavior() throws Exception {
        HandlerMethod handler = new HandlerMethod(new OrdinaryEndpoint(), OrdinaryEndpoint.class.getMethod("update"));
        ServletException businessFailure = new ServletException("ordinary outer failure",
                new IllegalArgumentException("ordinary root failure"));
        IllegalStateException deliveryFailure = new IllegalStateException("ordinary delivery failure");
        RequestLogs captured = capturePipeline(handler, "/admin-api/ordinary/update",
                "{\"description\":\"ordinary-body-fixture\"}", true, false, businessFailure, deliveryFailure);
        assertSame(businessFailure, captured.thrown());
        assertTrue(captured.accessLog().getResultMsg().contains("ordinary root failure"));
        assertTrue(captured.accessLog().getRequestParams().contains("ordinary-body-fixture"));
        ILoggingEvent deliveryLog = captured.events().stream().filter(event ->
                event.getFormattedMessage().contains("[createApiAccessLog]")).findFirst().orElseThrow();
        assertNotNull(deliveryLog.getThrowableProxy());
        assertEquals("ordinary delivery failure", deliveryLog.getThrowableProxy().getMessage());
        assertTrue(deliveryLog.getFormattedMessage().contains("ordinary-body-fixture"));
    }

    @Test
    void ordinaryEndpoint_defaultRequestLoggingStillRecordsBodyAndExistingQueryBehavior() throws Exception {
        HandlerMethod handler = new HandlerMethod(new OrdinaryEndpoint(), OrdinaryEndpoint.class.getMethod("update"));
        RequestLogs body = capturePipeline(handler, "/admin-api/ordinary/update",
                "{\"description\":\"ordinary-body-fixture\"}", true, false, null);
        assertTrue(body.accessLog().getRequestParams().contains("ordinary-body-fixture"));
        assertTrue(body.accessLog().getRequestParams().contains(QUERY_SECRET));
        assertTrue(body.events().stream().anyMatch(event -> event.getFormattedMessage().contains("ordinary-body-fixture")));

        RequestLogs query = capturePipeline(handler, "/admin-api/ordinary/update", "", false, false, null);
        assertTrue(query.accessLog().getRequestParams().contains(QUERY_SECRET));
        assertTrue(query.events().stream().anyMatch(event -> event.getFormattedMessage().contains(QUERY_SECRET)));
    }

    private HandlerMethod profileHandler() throws NoSuchMethodException {
        return new HandlerMethod(new UserProfileController(), UserProfileController.class.getMethod(
                "updateUserProfilePassword", UserProfileUpdatePasswordReqVO.class));
    }

    private HandlerMethod adminHandler() throws NoSuchMethodException {
        return new HandlerMethod(new UserController(), UserController.class.getMethod(
                "updateUserPassword", UserUpdatePasswordReqVO.class));
    }

    private void assertPasswordLogs(RequestLogs captured, int resultCode) {
        ApiAccessLogCreateReqDTO accessLog = captured.accessLog();
        assertNull(accessLog.getRequestParams());
        assertEquals(resultCode, accessLog.getResultCode());
        assertNotNull(accessLog.getBeginTime());
        assertNotNull(accessLog.getEndTime());
        assertNotNull(accessLog.getDuration());
        assertNotNull(accessLog.getOperateName());
        assertNoSecrets(JsonUtils.toJsonString(accessLog));
        assertTrue(captured.events().stream().anyMatch(event ->
                event.getFormattedMessage().contains("参数禁止记录")));
        assertTrue(captured.events().stream().anyMatch(event ->
                event.getFormattedMessage().contains("完成请求 URL(")));
        for (ILoggingEvent event : captured.events()) {
            assertNoSecrets(event.getFormattedMessage());
            assertNoSecrets(event.getThrowableProxy());
            assertFalse(event.getFormattedMessage().contains("[sanitizeJson]"));
        }
    }

    private void assertNoSecrets(String value) {
        if (value == null) {
            return;
        }
        for (String secret : List.of(OLD_SECRET, NEW_SECRET, RESET_SECRET, QUERY_SECRET)) {
            assertFalse(value.contains(secret), "Captured log must not contain a password/query fixture");
        }
    }

    private void assertNoSecrets(IThrowableProxy throwable) {
        if (throwable == null) {
            return;
        }
        assertNoSecrets(throwable.getMessage());
        assertNoSecrets(throwable.getClassName());
        for (var stackTrace : throwable.getStackTraceElementProxyArray()) {
            assertNoSecrets(stackTrace.toString());
        }
        assertNoSecrets(throwable.getCause());
        if (throwable.getSuppressed() != null) {
            for (IThrowableProxy suppressed : throwable.getSuppressed()) {
                assertNoSecrets(suppressed);
            }
        }
    }

    private RequestLogs capturePipeline(HandlerMethod handler, String uri, String body, boolean json,
                                        boolean badRequest, ServletException downstreamFailure) throws Exception {
        return capturePipeline(handler, uri, body, json, badRequest, downstreamFailure, null);
    }

    private RequestLogs capturePipeline(HandlerMethod handler, String uri, String body, boolean json,
                                        boolean badRequest, ServletException downstreamFailure,
                                        RuntimeException deliveryFailure) throws Exception {
        ApiAccessLogCommonApi logApi = mock(ApiAccessLogCommonApi.class);
        if (deliveryFailure != null) {
            doThrow(deliveryFailure).when(logApi).createApiAccessLogAsync(any(ApiAccessLogCreateReqDTO.class));
        }
        ApiAccessLogFilter filter = new ApiAccessLogFilter(new WebProperties(), "password-regression", logApi);
        ApiAccessLogInterceptor interceptor = new ApiAccessLogInterceptor();
        MockHttpServletRequest rawRequest = new MockHttpServletRequest("PUT", uri);
        rawRequest.setContentType(json ? "application/json" : "text/plain");
        rawRequest.setCharacterEncoding(StandardCharsets.UTF_8.name());
        rawRequest.setContent(body.getBytes(StandardCharsets.UTF_8));
        rawRequest.addParameter("unrecognizedParameter", QUERY_SECRET);
        // Metadata exists before the access filter, as supplied by the system privacy filter.
        rawRequest.setAttribute(ATTRIBUTE_HANDLER_METHOD, handler);
        if (handler.getMethodAnnotation(cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog.class) != null) {
            rawRequest.setAttribute(ATTRIBUTE_PROTECTED_REQUEST_URI, uri);
        }
        HttpServletRequest request = json ? new CacheRequestBodyWrapper(rawRequest) : rawRequest;
        WebFrameworkUtils.setLoginUserId(request, 123L);
        WebFrameworkUtils.setLoginUserType(request, UserTypeEnum.ADMIN.getValue());
        MockHttpServletResponse response = new MockHttpServletResponse();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        Logger filterLogger = (Logger) LoggerFactory.getLogger(ApiAccessLogFilter.class);
        Logger interceptorLogger = (Logger) LoggerFactory.getLogger(ApiAccessLogInterceptor.class);
        Level filterLevel = filterLogger.getLevel();
        Level interceptorLevel = interceptorLogger.getLevel();
        appender.start();
        filterLogger.setLevel(Level.INFO);
        interceptorLogger.setLevel(Level.INFO);
        filterLogger.addAppender(appender);
        interceptorLogger.addAppender(appender);
        ServletException thrown = null;
        try (MockedStatic<SpringUtils> spring = mockStatic(SpringUtils.class)) {
            spring.when(SpringUtils::isProd).thenReturn(false);
            try {
                filter.doFilter(request, response, (chainRequest, chainResponse) -> {
                    assertTrue(interceptor.preHandle(request, response, handler));
                    try {
                        if (downstreamFailure != null) {
                            throw downstreamFailure;
                        }
                        WebFrameworkUtils.setCommonResult(request, badRequest
                                ? CommonResult.error(400, "请求参数错误") : CommonResult.success(true));
                        response.setStatus(badRequest ? 400 : 200);
                    } finally {
                        interceptor.afterCompletion(request, response, handler, downstreamFailure);
                    }
                });
            } catch (ServletException ex) {
                thrown = ex;
            }
        } finally {
            filterLogger.detachAppender(appender);
            interceptorLogger.detachAppender(appender);
            filterLogger.setLevel(filterLevel);
            interceptorLogger.setLevel(interceptorLevel);
            appender.stop();
        }
        if (downstreamFailure == null) {
            assertNull(thrown);
            assertEquals(badRequest ? 400 : 200, response.getStatus());
        }
        ArgumentCaptor<ApiAccessLogCreateReqDTO> captor = ArgumentCaptor.forClass(ApiAccessLogCreateReqDTO.class);
        verify(logApi).createApiAccessLogAsync(captor.capture());
        assertAuditFacts(captor.getValue(), uri);
        return new RequestLogs(captor.getValue(), List.copyOf(appender.list), thrown);
    }

    private record RequestLogs(ApiAccessLogCreateReqDTO accessLog, List<ILoggingEvent> events,
                               ServletException thrown) {
    }

    public static class OrdinaryEndpoint {
        @Operation(summary = "普通接口测试")
        public void update() {
        }
    }

    private ApiAccessLogCreateReqDTO captureLog(String uri, HandlerMethod handler, String body) throws Exception {
        ApiAccessLogCommonApi logApi = mock(ApiAccessLogCommonApi.class);
        ApiAccessLogFilter filter = new ApiAccessLogFilter(new WebProperties(), "password-regression", logApi);
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", uri);
        request.setContentType("application/json");
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.setAttribute(ATTRIBUTE_HANDLER_METHOD, handler);
        request.setAttribute(ATTRIBUTE_PROTECTED_REQUEST_URI, uri);
        WebFrameworkUtils.setLoginUserId(request, 123L);
        WebFrameworkUtils.setLoginUserType(request, UserTypeEnum.ADMIN.getValue());
        WebFrameworkUtils.setCommonResult(request, CommonResult.success(true));

        filter.doFilter(request, new MockHttpServletResponse(), (ignoredRequest, ignoredResponse) -> { });

        ArgumentCaptor<ApiAccessLogCreateReqDTO> captor = ArgumentCaptor.forClass(ApiAccessLogCreateReqDTO.class);
        verify(logApi).createApiAccessLogAsync(captor.capture());
        ApiAccessLogCreateReqDTO log = captor.getValue();
        assertAuditFacts(log, uri);
        return log;
    }

    private void assertAuditFacts(ApiAccessLogCreateReqDTO log, String uri) {
        assertEquals(123L, log.getUserId());
        assertEquals(UserTypeEnum.ADMIN.getValue(), log.getUserType());
        assertEquals(uri, log.getRequestUrl());
        assertEquals("PUT", log.getRequestMethod());
    }
}
