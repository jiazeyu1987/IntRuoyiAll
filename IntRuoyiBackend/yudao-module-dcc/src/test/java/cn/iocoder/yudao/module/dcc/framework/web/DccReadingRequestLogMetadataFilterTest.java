package cn.iocoder.yudao.module.dcc.framework.web;

import cn.iocoder.yudao.framework.apilog.core.filter.ApiAccessLogFilter;
import cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor;
import cn.iocoder.yudao.framework.common.biz.infra.logger.ApiAccessLogCommonApi;
import cn.iocoder.yudao.framework.common.biz.infra.logger.dto.ApiAccessLogCreateReqDTO;
import cn.iocoder.yudao.framework.common.enums.WebFilterOrderEnum;
import cn.iocoder.yudao.framework.common.util.spring.SpringUtils;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.module.dcc.controller.admin.filepreview.DccPreviewReadingSessionController;
import cn.iocoder.yudao.module.dcc.service.filepreview.DccOnlineFilePreviewReadingSessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.HandlerMethod;

import static cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DccReadingRequestLogMetadataFilterTest {
    private final DccPreviewReadingSessionController controller = new DccPreviewReadingSessionController();

    private DccReadingRequestLogMetadataFilter filter(WebProperties properties) {
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("readingController", controller);
        return new DccWebConfiguration(properties).dccReadingRequestLogMetadataFilter(
                beans.getBeanProvider(DccPreviewReadingSessionController.class)).getFilter();
    }

    @ParameterizedTest
    @CsvSource({
            "POST,/dcc/controlled-files/{fileId}/reading-session/start,startControlled",
            "POST,/dcc/controlled-files/{fileId}/reading-session/heartbeat,heartbeatControlled",
            "POST,/dcc/controlled-files/{fileId}/reading-session/stop,stopControlled",
            "GET,/dcc/controlled-files/{fileId}/reading-summary,summaryControlled",
            "POST,/dcc/file-preview/files/{fileId}/reading-session/start,start",
            "POST,/dcc/file-preview/files/{fileId}/reading-session/heartbeat,heartbeat",
            "POST,/dcc/file-preview/files/{fileId}/reading-session/stop,stop",
            "GET,/dcc/file-preview/files/{fileId}/reading-summary,summary"
    })
    void loggingAndAuthenticationRejectionKeepEvidencePrivate(String verb, String pattern, String methodName)
            throws Exception {
        for (boolean rejected : new boolean[]{false, true}) {
            for (String context : new String[]{"", "/application"}) {
                var properties = new WebProperties();
                properties.getAdminApi().setPrefix("/internal-api");
                new cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils(properties);
                String canonical = context + "/internal-api" + pattern;
                var request = new MockHttpServletRequest(verb, canonical.replace("{fileId}", "910"));
                request.setContextPath(context);
                request.setContentType("application/json");
                request.setContent("{\"viewerTokenNonce\":\"private-evidence\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                var response = new MockHttpServletResponse();
                var logs = mock(ApiAccessLogCommonApi.class);
                var access = new ApiAccessLogFilter(properties, "reading-test", logs);
                try (var spring = mockStatic(SpringUtils.class)) {
                    spring.when(SpringUtils::isProd).thenReturn(false);
                    filter(properties).doFilter(request, response, (marked, unchanged) -> {
                        assertTrue(isRequestLoggingDisabled(request));
                        assertEquals(canonical, getLogRequestUri(request));
                        var handler = (HandlerMethod) request.getAttribute(ATTRIBUTE_HANDLER_METHOD);
                        assertSame(controller, handler.getBean());
                        assertEquals(methodName, handler.getMethod().getName());
                        access.doFilter(marked, unchanged, (req, res) -> {
                            if (rejected) response.setStatus(401);
                            else {
                                var interceptor = new ApiAccessLogInterceptor();
                                assertTrue(interceptor.preHandle(request, response, handler));
                                interceptor.afterCompletion(request, response, handler, null);
                            }
                        });
                    });
                }
                var saved = ArgumentCaptor.forClass(ApiAccessLogCreateReqDTO.class);
                verify(logs).createApiAccessLogAsync(saved.capture());
                assertEquals(canonical, saved.getValue().getRequestUrl());
                assertNull(saved.getValue().getRequestParams());
                assertNull(saved.getValue().getResponseBody());
                assertEquals(rejected ? 401 : 0, saved.getValue().getResultCode());
                assertEquals(rejected ? 401 : 200, response.getStatus());
            }
        }
    }

    @Test
    void realMvcStartReachesReadingServiceWithLoggingInterceptorEnabled() throws Exception {
        var service = mock(DccOnlineFilePreviewReadingSessionService.class);
        ReflectionTestUtils.setField(controller, "service", service);
        var properties = new WebProperties();
        properties.getAdminApi().setPrefix("/dcc-test");
        var mvc = MockMvcBuilders.standaloneSetup(controller).addInterceptors(new ApiAccessLogInterceptor())
                .addFilters(filter(properties)).setCustomHandlerMapping(() -> {
                    var mapping = new org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping();
                    mapping.setPathPrefixes(java.util.Map.of("/dcc-test", type -> true));
                    return mapping;
                }).build();
        try (var spring = mockStatic(SpringUtils.class)) {
            spring.when(SpringUtils::isProd).thenReturn(false);
            mvc.perform(post("/dcc-test/dcc/file-preview/files/910/reading-session/start")
                            .header("X-DCC-Viewer-Token", "private-viewer-token")
                            .header("User-Agent", "reading-regression-test")
                            .contentType("application/json")
                            .content("{\"clientSessionId\":\"client-1\",\"accessEventCode\":\"event\",\"watermarkTraceCode\":\"trace\",\"viewerTokenId\":\"id\",\"viewerTokenNonce\":\"nonce\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        }
        verify(service).start(isNull(), eq(910L), argThat(req -> "private-viewer-token".equals(req.viewerToken())), any());
    }

    @Test
    void unrelatedRoutesAreUntouchedAndBusinessErrorsPropagate() throws Exception {
        var request = new MockHttpServletRequest("POST", "/admin-api/dcc/controlled-files/910/reading-session/start-extra");
        filter(new WebProperties()).doFilter(request, new MockHttpServletResponse(), (req, res) ->
                assertNull(req.getAttribute(ATTRIBUTE_HANDLER_METHOD)));
        var failure = new jakarta.servlet.ServletException("business-failure");
        var reading = new MockHttpServletRequest("DELETE", "/admin-api/dcc/controlled-files/910/reading-session/start");
        assertSame(failure, assertThrows(jakarta.servlet.ServletException.class, () ->
                filter(new WebProperties()).doFilter(reading, new MockHttpServletResponse(), (req, res) -> {
                    assertTrue(isRequestLoggingDisabled(reading));
                    throw failure;
                })));
    }

    @Test
    void registrationPrecedesAuthenticationAndDoesNotResolveControllerAtStartup() {
        var beans = new DefaultListableBeanFactory();
        var registration = new DccWebConfiguration(new WebProperties()).dccReadingRequestLogMetadataFilter(
                beans.getBeanProvider(DccPreviewReadingSessionController.class));
        assertEquals(WebFilterOrderEnum.API_ACCESS_LOG_FILTER - 2, registration.getOrder());
        assertTrue(registration.getOrder() < WebFilterOrderEnum.TENANT_CONTEXT_FILTER);
    }
}
