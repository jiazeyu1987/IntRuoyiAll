package cn.iocoder.yudao.module.system.framework.web.core.filter;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.apilog.core.filter.ApiAccessLogFilter;
import cn.iocoder.yudao.framework.common.biz.infra.logger.ApiAccessLogCommonApi;
import cn.iocoder.yudao.framework.common.biz.infra.logger.dto.ApiAccessLogCreateReqDTO;
import cn.iocoder.yudao.framework.common.enums.WebFilterOrderEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.framework.web.core.filter.CacheRequestBodyWrapper;
import cn.iocoder.yudao.module.system.controller.admin.user.UserController;
import cn.iocoder.yudao.module.system.controller.admin.user.UserProfileController;
import cn.iocoder.yudao.module.system.framework.web.config.SystemWebConfiguration;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor.ATTRIBUTE_HANDLER_METHOD;
import static cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor.ATTRIBUTE_PROTECTED_REQUEST_URI;
import static cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor.getLogRequestUri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real endpoint beans/metadata and access filter; the authentication rejection is a downstream seam. */
class PasswordRequestLogMetadataFilterTest {

    private final UserController users = new UserController();
    private final UserProfileController profiles = new UserProfileController();
    private final DefaultListableBeanFactory beans = controllerBeans();

    @Test
    void bothPasswordPaths_receiveActualBeanAndMethodBeforeTheChain() throws Exception {
        assertMapped("PUT", "/admin-api/system/user/update-password", "", new WebProperties(), users,
                "updateUserPassword");
        assertMapped("PUT", "/admin-api/system/user/profile/update-password", "", new WebProperties(), profiles,
                "updateUserProfilePassword");
    }

    @Test
    void customPrefixAndContextPath_followTheConfiguredMvcPath() throws Exception {
        WebProperties properties = new WebProperties();
        properties.getAdminApi().setPrefix("/internal-api");
        assertMapped("PUT", "/application/internal-api/system/user/update-password", "/application", properties,
                users, "updateUserPassword");
        assertMapped("PUT", "/application/internal-api/system/user/profile/update-password", "/application",
                properties, profiles, "updateUserProfilePassword");
    }

    @Test
    void encodedSegmentsAndMatrixParameters_useParsedMvcPathSemantics() throws Exception {
        assertMapped("PUT", "/admin-api/system/user/%70rofile/update%2Dpassword", "", new WebProperties(), profiles,
                "updateUserProfilePassword");
        assertMapped("PUT", "/admin-api/system/user;segment=fixture/update-password", "", new WebProperties(), users,
                "updateUserPassword");
    }

    @Test
    void wrongHttpMethods_areStillPrivateWithoutExecutingAnEndpoint() throws Exception {
        for (String method : List.of("GET", "POST", "DELETE", "OPTIONS", "PATCH")) {
            assertMapped(method, "/admin-api/system/user/update-password", "", new WebProperties(), users,
                    "updateUserPassword");
            assertMapped(method, "/admin-api/system/user/profile/update-password", "", new WebProperties(), profiles,
                    "updateUserProfilePassword");
        }
    }

    @Test
    void ordinaryAndSimilarPaths_leaveExistingMetadataAndResponseUnchanged() throws Exception {
        for (String path : List.of("/admin-api/system/user/update", "/admin-api/system/user/update-password-extra",
                "/app-api/system/user/update-password", "/admin-api/system/user/profile/update-password/extra")) {
            MockHttpServletRequest request = new MockHttpServletRequest("PUT", path);
            Object previous = new Object();
            request.setAttribute(ATTRIBUTE_HANDLER_METHOD, previous);
            MockHttpServletResponse response = new MockHttpServletResponse();
            response.setStatus(202);
            AtomicBoolean called = new AtomicBoolean();
            filter(new WebProperties()).doFilter(request, response, (sameRequest, sameResponse) -> {
                called.set(true);
                assertSame(request, sameRequest);
                assertSame(response, sameResponse);
                assertSame(previous, request.getAttribute(ATTRIBUTE_HANDLER_METHOD));
            });
            assertTrue(called.get());
            assertEquals(202, response.getStatus());
        }
    }

    @Test
    void matchingOnly_readsNoHeadersParametersOrBody() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn("PUT");
        when(request.getRequestURI()).thenReturn("/admin-api/system/user/profile/update-password");
        when(request.getContextPath()).thenReturn("");
        AtomicBoolean called = new AtomicBoolean();
        filter(new WebProperties()).doFilter(request, response, (sameRequest, sameResponse) -> {
            called.set(true);
            assertSame(request, sameRequest);
            assertSame(response, sameResponse);
        });
        assertTrue(called.get());
        verify(request, never()).getParameterMap();
        verify(request, never()).getInputStream();
        verify(request, never()).getReader();
        verify(request, never()).getContentType();
        verify(request, never()).getHeader(anyString());
        verifyNoInteractions(response);
    }

    @Test
    void beforeMvcRejection_actualAccessFilterOmitsNormalAndMalformedPasswordParameters() throws Exception {
        for (String path : List.of("/admin-api/system/user/update-password",
                "/admin-api/system/user/profile/update-password")) {
            for (String body : List.of("{\"oldPassword\":\"old-fixture-secret\",\"newPassword\":\"new-fixture-secret\","
                    + "\"password\":\"reset-fixture-secret\"}", "{\"newPassword\":\"new-fixture-secret\"")) {
                ApiAccessLogCommonApi logApi = mock(ApiAccessLogCommonApi.class);
                ApiAccessLogFilter accessFilter = new ApiAccessLogFilter(new WebProperties(), "privacy-test", logApi);
                MockHttpServletRequest raw = new MockHttpServletRequest("PUT", path);
                raw.setContentType("application/json");
                raw.setContent(body.getBytes(StandardCharsets.UTF_8));
                raw.addParameter("unrecognizedParameter", "query-fixture-secret");
                HttpServletRequest request = new CacheRequestBodyWrapper(raw);
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter(new WebProperties()).doFilter(request, response, (markedRequest, unchangedResponse) ->
                        accessFilter.doFilter(markedRequest, unchangedResponse, (rejectedRequest, rejectedResponse) -> {
                            assertInstanceOf(HandlerMethod.class, rejectedRequest.getAttribute(ATTRIBUTE_HANDLER_METHOD));
                            new cn.iocoder.yudao.framework.security.core.handler.AuthenticationEntryPointImpl()
                                    .commence((HttpServletRequest) rejectedRequest, response,
                                            new org.springframework.security.authentication.InsufficientAuthenticationException("fixture"));
                            // No MVC interceptor/Controller is invoked on this rejection path.
                        }));
                assertEquals(200, response.getStatus());
                assertTrue(response.getContentAsString().contains("401"));
                ArgumentCaptor<ApiAccessLogCreateReqDTO> logged = ArgumentCaptor.forClass(ApiAccessLogCreateReqDTO.class);
                verify(logApi).createApiAccessLogAsync(logged.capture());
                assertNull(logged.getValue().getRequestParams());
                assertEquals(401, logged.getValue().getResultCode());
                assertEquals(path, logged.getValue().getRequestUrl());
                assertNotNull(logged.getValue().getOperateName());
                assertNotNull(logged.getValue().getDuration());
            }
        }
    }

    @Test
    void downstreamException_propagatesAsTheSameFailure() throws Exception {
        ServletException failure = new ServletException("fixture request failure");
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/admin-api/system/user/update-password");
        ServletException thrown = assertThrows(ServletException.class, () -> filter(new WebProperties()).doFilter(
                request, new MockHttpServletResponse(), (sameRequest, sameResponse) -> { throw failure; }));
        assertSame(failure, thrown);
        assertSame(users, ((HandlerMethod) request.getAttribute(ATTRIBUTE_HANDLER_METHOD)).getBean());
    }

    @Test
    void registration_precedesTenantContextAndAccessLogAndDoesNotResolveProvidersAtStartup() {
        ObjectProvider<UserController> userProvider = mock(ObjectProvider.class);
        ObjectProvider<UserProfileController> profileProvider = mock(ObjectProvider.class);
        FilterRegistrationBean<PasswordRequestLogMetadataFilter> registration = new SystemWebConfiguration()
                .passwordRequestLogMetadataFilter(new WebProperties(), userProvider, profileProvider);
        assertEquals(WebFilterOrderEnum.API_ACCESS_LOG_FILTER - 2, registration.getOrder());
        assertTrue(registration.getOrder() < WebFilterOrderEnum.TENANT_CONTEXT_FILTER);
        assertTrue(registration.getOrder() > WebFilterOrderEnum.REQUEST_BODY_CACHE_FILTER);
        assertInstanceOf(PasswordRequestLogMetadataFilter.class, registration.getFilter());
        verifyNoInteractions(userProvider, profileProvider);
    }

    @Test
    void invalidPrefixOrControllerPrefixContract_failsDuringConstruction() {
        WebProperties properties = new WebProperties();
        properties.getAdminApi().setPrefix("");
        assertThrows(IllegalArgumentException.class, () -> filter(properties));
        properties.getAdminApi().setPrefix("/admin-api");
        properties.getAdminApi().setController("**.controller.app.**");
        assertThrows(IllegalStateException.class, () -> filter(properties));
    }

    private void assertMapped(String method, String uri, String context, WebProperties properties,
                              Object bean, String methodName) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setContextPath(context);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean();
        filter(properties).doFilter(request, response, (sameRequest, sameResponse) -> {
            called.set(true);
            assertSame(request, sameRequest);
            assertSame(response, sameResponse);
            HandlerMethod metadata = assertInstanceOf(HandlerMethod.class,
                    request.getAttribute(ATTRIBUTE_HANDLER_METHOD));
            assertSame(bean, metadata.getBean());
            assertEquals(methodName, metadata.getMethod().getName());
            ApiAccessLog privacy = metadata.getMethodAnnotation(ApiAccessLog.class);
            assertNotNull(privacy);
            assertFalse(privacy.requestEnable());
            String canonical = context + properties.getAdminApi().getPrefix()
                    + (methodName.equals("updateUserPassword") ? "/system/user/update-password"
                    : "/system/user/profile/update-password");
            assertEquals(canonical, request.getAttribute(ATTRIBUTE_PROTECTED_REQUEST_URI));
            assertEquals(canonical, getLogRequestUri(request));
        });
        assertTrue(called.get());
        assertEquals(200, response.getStatus());
        assertEquals("", response.getContentAsString());
    }

    private PasswordRequestLogMetadataFilter filter(WebProperties properties) {
        return new SystemWebConfiguration().passwordRequestLogMetadataFilter(properties,
                beans.getBeanProvider(UserController.class), beans.getBeanProvider(UserProfileController.class)).getFilter();
    }

    private DefaultListableBeanFactory controllerBeans() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerSingleton("userController", users);
        factory.registerSingleton("userProfileController", profiles);
        return factory;
    }
}
