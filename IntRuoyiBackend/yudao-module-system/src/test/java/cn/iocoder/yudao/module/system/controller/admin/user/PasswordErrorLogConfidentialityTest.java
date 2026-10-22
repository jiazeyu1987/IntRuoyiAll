package cn.iocoder.yudao.module.system.controller.admin.user;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import cn.iocoder.yudao.framework.apilog.core.filter.ApiAccessLogFilter;
import cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor;
import cn.iocoder.yudao.framework.common.biz.infra.logger.ApiAccessLogCommonApi;
import cn.iocoder.yudao.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.iocoder.yudao.framework.common.biz.infra.logger.dto.ApiAccessLogCreateReqDTO;
import cn.iocoder.yudao.framework.common.biz.infra.logger.dto.ApiErrorLogCreateReqDTO;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.spring.SpringUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.framework.web.core.filter.CacheRequestBodyFilter;
import cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler;
import cn.iocoder.yudao.framework.web.core.handler.GlobalResponseBodyHandler;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.profile.UserProfileUpdatePasswordReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserUpdatePasswordReqVO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.framework.web.core.filter.PasswordRequestLogMetadataFilter;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import jakarta.servlet.ServletInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor.ATTRIBUTE_HANDLER_METHOD;
import static cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor.ATTRIBUTE_PROTECTED_REQUEST_URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real MVC parsing, controllers, advice, interceptor and filters; only service/log delivery are mocked. */
@SpringJUnitConfig(PasswordErrorLogConfidentialityTest.Context.class)
@ActiveProfiles("local")
class PasswordErrorLogConfidentialityTest {

    private static final String PROFILE = "/system/user/profile/update-password";
    private static final String RESET = "/system/user/update-password";
    private static final String OLD = "old-error-log-secret";
    private static final String NEW = "new-error-log-secret";
    private static final String PASSWORD = "reset-error-log-secret";
    private static final String QUERY = "query-error-log-secret";
    private static final String PROFILE_BODY = "{\"oldPassword\":\"" + OLD + "\",\"newPassword\":\"" + NEW + "\"}";
    private static final String RESET_BODY = "{\"id\":456,\"password\":\"" + PASSWORD + "\"}";

    @Autowired private AdminUserService userService;
    @Autowired private ApiAccessLogCommonApi accessApi;
    @Autowired private ApiErrorLogCommonApi errorApi;

    private MockMvc mvc;
    private GlobalExceptionHandler handler;
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final List<Logger> loggers = new ArrayList<>();
    private final List<Level> levels = new ArrayList<>();

    @BeforeEach
    void setUp() {
        reset(userService, accessApi, errorApi);
        handler = new GlobalExceptionHandler("password-error-regression", errorApi);
        UserController admin = new UserController();
        UserProfileController profile = new UserProfileController();
        ReflectionTestUtils.setField(admin, "userService", userService);
        ReflectionTestUtils.setField(profile, "userService", userService);
        WebProperties properties = new WebProperties();
        new WebFrameworkUtils(properties);
        DefaultListableBeanFactory controllers = new DefaultListableBeanFactory();
        controllers.registerSingleton("users", admin);
        controllers.registerSingleton("profiles", profile);
        PasswordRequestLogMetadataFilter metadata = new PasswordRequestLogMetadataFilter(properties,
                controllers.getBeanProvider(UserController.class), controllers.getBeanProvider(UserProfileController.class));
        mvc = MockMvcBuilders.standaloneSetup(admin, profile, new OrdinaryController())
                .setCustomHandlerMapping(() -> {
                    RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
                    mapping.setPathPrefixes(Map.of("/admin-api", type -> true));
                    return mapping;
                })
                .setControllerAdvice(handler, new GlobalResponseBodyHandler())
                .addInterceptors(new ApiAccessLogInterceptor())
                .addFilters(new CacheRequestBodyFilter(), metadata, new ApiAccessLogFilter(properties,
                        "password-error-regression", accessApi))
                .build();
        logs.start();
        for (Class<?> type : List.of(GlobalExceptionHandler.class, ApiAccessLogInterceptor.class, ApiAccessLogFilter.class)) {
            Logger logger = (Logger) LoggerFactory.getLogger(type);
            loggers.add(logger);
            levels.add(logger.getLevel());
            logger.setLevel(Level.INFO);
            logger.addAppender(logs);
        }
    }

    @AfterEach
    void tearDown() {
        for (int i = 0; i < loggers.size(); i++) {
            loggers.get(i).detachAppender(logs);
            loggers.get(i).setLevel(levels.get(i));
        }
        logs.stop();
        SecurityContextHolder.clearContext();
    }

    @Test
    void bothPasswordEndpoints_successKeepsAuditFactsWithoutRequestValues() throws Exception {
        perform(PROFILE, PROFILE_BODY, 0);
        verify(userService).updateUserPassword(eq(123L), any(UserProfileUpdatePasswordReqVO.class));
        assertProtectedAccess(0);
        verifyNoInteractions(errorApi);
        reset(accessApi);
        perform(RESET, RESET_BODY, 0);
        verify(userService).updateUserPassword(456L, PASSWORD);
        assertProtectedAccess(0);
        assertProtectedConsole();
    }

    @Test
    void bothPasswordEndpoints_badJsonReturnsBadRequestBeforeServiceAndProtectsParserException() throws Exception {
        for (String path : List.of(PROFILE, RESET)) {
            reset(userService, accessApi, errorApi);
            String body = path.equals(PROFILE) ? PROFILE_BODY.substring(0, PROFILE_BODY.length() - 1)
                    : "{\"id\":\"" + PASSWORD + "\",\"password\":\"" + PASSWORD + "\"}";
            perform(path, body, 400);
            verifyNoInteractions(userService);
            assertProtectedError(path, 400);
        }
    }

    @Test
    void bothPasswordEndpoints_validationFailureDoesNotLogRejectedValues() throws Exception {
        for (String path : List.of(PROFILE, RESET)) {
            reset(userService, accessApi, errorApi);
            String body = path.equals(PROFILE) ? "{\"oldPassword\":\"" + OLD + "\",\"newPassword\":\"\"}"
                    : "{\"password\":\"" + PASSWORD + "\"}";
            perform(path, body, 400);
            verifyNoInteractions(userService);
            assertProtectedError(path, 400);
        }
    }

    @Test
    void bothPasswordEndpoints_internalCauseAndSuppressedMessagesAreAbsentAndResultRemainsFailure() throws Exception {
        for (String path : List.of(PROFILE, RESET)) {
            reset(userService, accessApi, errorApi);
            RuntimeException failure = new IllegalStateException(OLD, new IllegalArgumentException(NEW));
            failure.addSuppressed(new RuntimeException(PASSWORD + QUERY));
            stubFailure(path, failure);
            perform(path, path.equals(PROFILE) ? PROFILE_BODY : RESET_BODY, 500);
            assertProtectedError(path, 500);
        }
    }

    @Test
    void passwordBusinessError_preservesCodeWithoutEchoingExceptionMessageInAccessLogOrResponse() throws Exception {
        for (boolean wrapped : List.of(false, true)) {
            reset(userService, accessApi, errorApi);
            ServiceException serviceFailure = new ServiceException(1002005001, PASSWORD);
            serviceFailure.initCause(new IllegalArgumentException(NEW));
            stubFailure(RESET, wrapped ? new IllegalStateException(OLD, serviceFailure) : serviceFailure);
            MvcResult result = perform(RESET, RESET_BODY, 1002005001);
            assertNoSecrets(result.getResponse().getContentAsString());
            assertProtectedError(RESET, 1002005001);
        }
    }

    @Test
    void errorLogDeliveryFailure_doesNotSendSecretThrowableToConsoleOrChangeErrorResult() throws Exception {
        stubFailure(RESET, new IllegalStateException(PASSWORD));
        doThrow(new IllegalStateException(QUERY, new RuntimeException(OLD)))
                .when(errorApi).createApiErrorLogAsync(any());
        perform(RESET, RESET_BODY, 500);
        assertProtectedError(RESET, 500);
        assertTrue(logs.list.stream().anyMatch(log -> log.getFormattedMessage().contains("[createExceptionLog]")));
    }

    @Test
    void nonMvcDispatch_usesHandlerMetadataAndDoesNotReadProtectedBodyOrQuery() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", RESET) {
            @Override public Map<String, String[]> getParameterMap() { throw new AssertionError("query must not be read"); }
            @Override public BufferedReader getReader() { throw new AssertionError("body must not be read"); }
            @Override public ServletInputStream getInputStream() { throw new AssertionError("body must not be read"); }
        };
        request.setAttribute(ATTRIBUTE_HANDLER_METHOD, new HandlerMethod(new UserController(),
                UserController.class.getMethod("updateUserPassword", UserUpdatePasswordReqVO.class)));
        request.setAttribute(ATTRIBUTE_PROTECTED_REQUEST_URI, RESET);
        WebFrameworkUtils.setLoginUserType(request, UserTypeEnum.ADMIN.getValue());
        List<Throwable> failures = List.of(new org.springframework.security.access.AccessDeniedException(PASSWORD),
                new HttpMessageNotReadableException(OLD, new ServletServerHttpRequest(request)),
                new IllegalStateException(NEW), new ServiceException(1002005001, PASSWORD));
        List<Integer> codes = List.of(403, 400, 500, 1002005001);
        for (int i = 0; i < failures.size(); i++) {
            reset(errorApi);
            CommonResult<?> result = handler.allExceptionHandler(request, failures.get(i));
            assertEquals(codes.get(i), result.getCode());
            assertNoSecrets(result.getMsg());
            ArgumentCaptor<ApiErrorLogCreateReqDTO> error = ArgumentCaptor.forClass(ApiErrorLogCreateReqDTO.class);
            verify(errorApi).createApiErrorLogAsync(error.capture());
            assertNull(error.getValue().getRequestParams());
            assertNoSecrets(JsonUtils.toJsonString(error.getValue()));
        }
        assertProtectedConsole();
    }

    @Test
    void ordinaryEndpoint_retainsRawErrorAndRequestLoggingByDefault() throws Exception {
        mvc.perform(post("/admin-api/ordinary/error")
                        .contentType("application/json").param("description", "ordinary-query")
                        .content("{\"description\":\"ordinary-body\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        ArgumentCaptor<ApiErrorLogCreateReqDTO> error = ArgumentCaptor.forClass(ApiErrorLogCreateReqDTO.class);
        verify(errorApi).createApiErrorLogAsync(error.capture());
        assertTrue(error.getValue().getExceptionMessage().contains("ordinary-error"));
        assertTrue(error.getValue().getExceptionStackTrace().contains("ordinary-error"));
        assertTrue(error.getValue().getRequestParams().contains("ordinary-body"));
        assertTrue(error.getValue().getRequestParams().contains("ordinary-query"));
        assertTrue(logs.list.stream().anyMatch(log -> log.getThrowableProxy() != null));
    }

    private MvcResult perform(String path, String body, int code) throws Exception {
        return mvc.perform(put("/admin-api" + path)
                        .contentType("application/json").characterEncoding("UTF-8")
                        .param("unrecognizedParameter", QUERY).content(body)
                        .with(request -> {
                            SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(123L)
                                    .setUserType(UserTypeEnum.ADMIN.getValue()), request);
                            return request;
                        }))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(code))
                .andExpect(result -> {
                    if (code != 0) {
                        assertFalse(result.getResponse().getContentAsString().contains("参数禁止记录"));
                    }
                }).andReturn();
    }

    private void stubFailure(String path, RuntimeException failure) {
        if (path.equals(PROFILE)) {
            doThrow(failure).when(userService).updateUserPassword(eq(123L), any(UserProfileUpdatePasswordReqVO.class));
        } else {
            doThrow(failure).when(userService).updateUserPassword(456L, PASSWORD);
        }
    }

    private void assertProtectedError(String path, int code) {
        ArgumentCaptor<ApiErrorLogCreateReqDTO> captor = ArgumentCaptor.forClass(ApiErrorLogCreateReqDTO.class);
        verify(errorApi).createApiErrorLogAsync(captor.capture());
        ApiErrorLogCreateReqDTO error = captor.getValue();
        assertNull(error.getRequestParams());
        assertEquals("参数禁止记录", error.getExceptionMessage());
        assertEquals("参数禁止记录", error.getExceptionRootCauseMessage());
        assertTrue(error.getExceptionStackTrace().contains("参数禁止记录"));
        assertNotNull(error.getExceptionName());
        assertNotNull(error.getExceptionClassName());
        assertNotNull(error.getExceptionMethodName());
        assertNotNull(error.getExceptionTime());
        assertEquals("/admin-api" + path, error.getRequestUrl());
        assertEquals(123L, error.getUserId());
        assertNoSecrets(JsonUtils.toJsonString(error));
        assertProtectedAccess(code);
        assertProtectedConsole();
    }

    private void assertProtectedAccess(int code) {
        ArgumentCaptor<ApiAccessLogCreateReqDTO> captor = ArgumentCaptor.forClass(ApiAccessLogCreateReqDTO.class);
        verify(accessApi).createApiAccessLogAsync(captor.capture());
        assertNull(captor.getValue().getRequestParams());
        assertEquals(code, captor.getValue().getResultCode());
        if (code == 400) {
            assertEquals("请求参数不正确", captor.getValue().getResultMsg());
        } else if (code == 1002005001) {
            assertEquals("操作未完成，请检查输入后重试", captor.getValue().getResultMsg());
        }
        assertNotNull(captor.getValue().getOperateName());
        assertNoSecrets(JsonUtils.toJsonString(captor.getValue()));
    }

    private void assertProtectedConsole() {
        assertTrue(logs.list.stream().anyMatch(log -> log.getFormattedMessage().contains("参数禁止记录")));
        for (ILoggingEvent event : logs.list) {
            assertNoSecrets(event.getFormattedMessage());
            assertNoSecrets(event.getThrowableProxy());
            assertNull(event.getThrowableProxy(), "Protected pipeline must not pass original Throwable to console logging");
        }
    }

    private void assertNoSecrets(String value) {
        if (value != null) {
            for (String secret : List.of(OLD, NEW, PASSWORD, QUERY)) {
                assertFalse(value.contains(secret), "Log/response contains a confidential fixture value");
            }
        }
    }

    private void assertNoSecrets(IThrowableProxy throwable) {
        if (throwable == null) { return; }
        assertNoSecrets(throwable.getMessage());
        for (var frame : throwable.getStackTraceElementProxyArray()) { assertNoSecrets(frame.toString()); }
        assertNoSecrets(throwable.getCause());
        if (throwable.getSuppressed() != null) {
            for (IThrowableProxy suppressed : throwable.getSuppressed()) { assertNoSecrets(suppressed); }
        }
    }

    @RestController
    public static class OrdinaryController {
        @PostMapping("/ordinary/error")
        public CommonResult<Boolean> error(@RequestBody Map<String, String> body) {
            throw new IllegalStateException("ordinary-error");
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class Context {
        @Bean SpringUtils springUtils() { return new SpringUtils(); }
        @Bean AdminUserService userService() { return mock(AdminUserService.class); }
        @Bean ApiAccessLogCommonApi accessApi() { return mock(ApiAccessLogCommonApi.class); }
        @Bean ApiErrorLogCommonApi errorApi() { return mock(ApiErrorLogCommonApi.class); }
    }
}
