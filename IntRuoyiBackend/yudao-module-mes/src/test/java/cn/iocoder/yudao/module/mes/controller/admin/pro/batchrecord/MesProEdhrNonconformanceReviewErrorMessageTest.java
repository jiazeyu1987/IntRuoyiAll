package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor;
import cn.iocoder.yudao.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
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

class MesProEdhrNonconformanceReviewErrorMessageTest {
    @Test void protectedBusinessMessagesAreSpecificWithoutLeakingDynamicDetails() throws Exception {
        var service = mock(MesProEdhrNonconformanceReviewService.class);
        var audit = mock(ApiErrorLogCommonApi.class);
        var global = new GlobalExceptionHandler("test", audit);
        var controller = new MesProEdhrNonconformanceReviewController();
        ReflectionTestUtils.setField(controller, "nonconformanceReviewService", service);
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
        var contextualMessages = java.util.Map.of(
                PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID.getCode(), "当前批次或评审单状态不允许处置，请刷新后重试",
                PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID.getCode(), "评审来源或关联数据无效，请核对后重试",
                PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED.getCode(), "请填写处置结论、评审意见和电子签名密码");
        for (var code : new cn.iocoder.yudao.framework.common.exception.ErrorCode[]{
                PRO_EDHR_NONCONFORMANCE_REVIEW_NOT_EXISTS, PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID,
                PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID, PRO_EDHR_NONCONFORMANCE_REVIEW_DISPOSITION_INVALID,
                PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED, PRO_EDHR_NONCONFORMANCE_REVIEW_WORK_ORDER_STATE_REQUIRED}) {
            doThrow(new ServiceException(code.getCode(), "sensitive-test-value")).when(service).dispose(any());
            mvc.perform(post("/mes/pro/edhr-nonconformance-review/dispose").contentType("application/json")
                    .content("{\"id\":1,\"disposition\":\"rework\",\"reviewOpinion\":\"test\",\"signaturePassword\":\"sensitive-test-value\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(code.getCode()))
                    .andExpect(jsonPath("$.msg").value(contextualMessages.getOrDefault(code.getCode(), code.getMsg())));
        }
        doThrow(new ServiceException(1999999999, "sensitive-test-value")).when(service).dispose(any());
        mvc.perform(post("/mes/pro/edhr-nonconformance-review/dispose").contentType("application/json")
                .content("{\"id\":1,\"disposition\":\"rework\",\"reviewOpinion\":\"test\",\"signaturePassword\":\"sensitive-test-value\"}"))
                .andExpect(jsonPath("$.code").value(1999999999))
                .andExpect(jsonPath("$.msg").value("操作未完成，请检查输入后重试"));
        verify(audit, times(7)).createApiErrorLogAsync(argThat(log ->
                "参数禁止记录".equals(log.getRequestParams())
                && "参数禁止记录".equals(log.getExceptionMessage())
                && !log.getExceptionStackTrace().contains("sensitive-test-value")));
        when(service.get(1L)).thenThrow(new ServiceException(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID.getCode(), "普通业务原始提示"));
        mvc.perform(get("/mes/pro/edhr-nonconformance-review/get").param("id", "1"))
                .andExpect(jsonPath("$.msg").value("普通业务原始提示"));
    }
}
