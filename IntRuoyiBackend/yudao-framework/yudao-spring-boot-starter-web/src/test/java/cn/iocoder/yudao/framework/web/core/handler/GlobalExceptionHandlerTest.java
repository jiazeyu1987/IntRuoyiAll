package cn.iocoder.yudao.framework.web.core.handler;

import cn.iocoder.yudao.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    @Test
    void protectedStackTrace_keepsCausalDetailsWithinTextCapacityByCompressingSharedFrames() {
        StackTraceElement[] shared = new StackTraceElement[240];
        Arrays.setAll(shared, i -> new StackTraceElement("cn.iocoder.yudao.framework.ProductionInterceptor" + i,
                "invoke", "ProductionInterceptor.java", i + 1));
        Throwable cause = new SQLException("secret SQL and credentials", "HY000", 1205);
        cause.setStackTrace(shared);
        for (int i = 0; i < 5; i++) {
            Throwable wrapper = new IllegalStateException("secret request body", cause);
            StackTraceElement[] frames = new StackTraceElement[shared.length + 1];
            frames[0] = new StackTraceElement("BusinessLayer" + i, "execute", "BusinessLayer.java", 10 + i);
            System.arraycopy(shared, 0, frames, 1, shared.length);
            wrapper.setStackTrace(frames);
            cause = wrapper;
        }
        String stack = ReflectionTestUtils.invokeMethod(
                new GlobalExceptionHandler("test-app", mock(ApiErrorLogCommonApi.class)), "protectedStackTrace", cause);

        assertNotNull(stack);
        assertTrue(stack.getBytes(StandardCharsets.UTF_8).length < 65535);
        assertTrue(stack.contains("java.sql.SQLException"));
        assertTrue(stack.contains("vendorCode=1205 sqlState=HY000"));
        assertTrue(stack.contains("... 240 more"));
        for (int i = 0; i < 5; i++) assertTrue(stack.contains("BusinessLayer" + i + ".execute"));
        for (StackTraceElement frame : shared) assertEquals(1, stack.split(java.util.regex.Pattern.quote(frame.toString()), -1).length - 1);
        assertFalse(stack.contains("secret"));
    }

    @Test
    void protectedStackTrace_retainsDistinctFramesWhenCausesHaveNoSharedSuffix() {
        Throwable cause = new IllegalArgumentException("secret argument");
        cause.setStackTrace(new StackTraceElement[]{new StackTraceElement("Original", "fail", "Original.java", 3)});
        Throwable wrapper = new IllegalStateException("secret request", cause);
        wrapper.setStackTrace(new StackTraceElement[]{new StackTraceElement("Wrapper", "call", "Wrapper.java", 4)});
        String stack = ReflectionTestUtils.invokeMethod(
                new GlobalExceptionHandler("test-app", mock(ApiErrorLogCommonApi.class)), "protectedStackTrace", wrapper);

        assertNotNull(stack);
        assertTrue(stack.contains("Wrapper.call(Wrapper.java:4)"));
        assertTrue(stack.contains("Original.fail(Original.java:3)"));
        assertFalse(stack.contains(" more"));
        assertFalse(stack.contains("secret"));
    }

    @Test
    void accessDeniedException_prefersProjectAdviceOverLowerPriorityThirdPartyAdvice() throws Exception {
        ApiErrorLogCommonApi apiErrorLogApi = mock(ApiErrorLogCommonApi.class);
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler("test-app", apiErrorLogApi);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AccessDeniedController())
                .setControllerAdvice(globalExceptionHandler, new LegacyJimuLikeExceptionHandler())
                .build();

        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("没有该操作权限"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.success").doesNotExist());
    }

    @RestController
    static class AccessDeniedController {

        @GetMapping("/test/access-denied")
        public CommonResult<Boolean> throwAccessDenied() {
            throw new AuthorizationDeniedException("Access Denied");
        }
    }

    @ControllerAdvice(assignableTypes = Controller.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    static class LegacyJimuLikeExceptionHandler {

        @ResponseBody
        @ExceptionHandler(AuthorizationDeniedException.class)
        public Map<String, Object> handle(AuthorizationDeniedException ex) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            response.put("message", ex.getMessage());
            response.put("code", 500);
            response.put("result", null);
            response.put("timestamp", System.currentTimeMillis());
            return response;
        }
    }
}
