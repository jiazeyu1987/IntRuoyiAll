package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor;
import cn.iocoder.yudao.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MesProEdhrReleaseErrorMessageTest {
    @Test void protectedBusinessMessagesAreSpecificWithoutLeakingDynamicDetails() throws Exception {
        var service = mock(MesProEdhrReleaseService.class);
        var audit = mock(ApiErrorLogCommonApi.class);
        var global = new GlobalExceptionHandler("test", audit);
        var controller = new MesProEdhrReleaseController();
        ReflectionTestUtils.setField(controller, "releaseService", service);
        ReflectionTestUtils.setField(controller, "globalExceptionHandler", global);
        var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(global)
                .addInterceptors(new HandlerInterceptor() {
                    @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                        cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.setLoginUserType(request, 2);
                        request.setAttribute(ApiAccessLogInterceptor.ATTRIBUTE_HANDLER_METHOD, handler);
                        request.setAttribute(ApiAccessLogInterceptor.ATTRIBUTE_PROTECTED_REQUEST_URI, request.getRequestURI());
                        return true;
                    }
                }).build();
        for (var code : new cn.iocoder.yudao.framework.common.exception.ErrorCode[]{
                PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS, PRO_EDHR_RELEASE_PRECHECK_REQUIRED,
                PRO_EDHR_RELEASE_STATUS_INVALID, PRO_EDHR_RELEASE_REASON_REQUIRED,
                PRO_EDHR_RELEASE_SIGNOFF_REQUIRED, PRO_EDHR_RELEASE_IDEMPOTENCY_KEY_REQUIRED,
                PRO_EDHR_RELEASE_SIGNATURE_PASSWORD_REQUIRED}) {
            doThrow(new ServiceException(code.getCode(), "sensitive-test-value")).when(service).reject(any());
            mvc.perform(post("/mes/pro/edhr-release/reject").contentType("application/json")
                    .content("{\"releaseTransactionId\":1,\"idempotencyKey\":\"test\",\"rejectReason\":\"test\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(code.getCode()))
                    .andExpect(jsonPath("$.msg").value(code.getMsg()));
        }
        doThrow(new ServiceException(PRO_EDHR_RELEASE_OWNER_INVALID.getCode(), "sensitive-test-value")).when(service).reject(any());
        mvc.perform(post("/mes/pro/edhr-release/reject").contentType("application/json")
                .content("{\"releaseTransactionId\":1,\"idempotencyKey\":\"test\",\"rejectReason\":\"test\"}"))
                .andExpect(jsonPath("$.msg").value("放行责任人未配置或当前用户不是放行责任人，请核对后重试"));
        doThrow(new ServiceException(1999999999, "sensitive-test-value")).when(service).reject(any());
        mvc.perform(post("/mes/pro/edhr-release/reject").contentType("application/json")
                .content("{\"releaseTransactionId\":1,\"idempotencyKey\":\"test\",\"rejectReason\":\"test\"}"))
                .andExpect(jsonPath("$.code").value(1999999999))
                .andExpect(jsonPath("$.msg").value("操作未完成，请检查输入后重试"));
        verify(audit, times(9)).createApiErrorLogAsync(argThat(log ->
                "参数禁止记录".equals(log.getRequestParams())
                && "参数禁止记录".equals(log.getExceptionMessage())
                && !log.getExceptionStackTrace().contains("sensitive-test-value")));
        when(service.get(1L)).thenThrow(new ServiceException(PRO_EDHR_RELEASE_STATUS_INVALID.getCode(), "普通业务原始提示"));
        mvc.perform(get("/mes/pro/edhr-release/get").param("id", "1"))
                .andExpect(jsonPath("$.msg").value("普通业务原始提示"));
    }
}
