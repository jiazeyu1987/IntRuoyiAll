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
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

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
