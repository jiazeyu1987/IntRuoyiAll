package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.mes.framework.web.core.filter.MesWorkflowFailureLogFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.web.method.HandlerMethod;
import io.swagger.v3.oas.annotations.Operation;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesWorkflowFailureLogFilterTest {
    static class ReviewController {
        @Operation(summary="统一审批中心提交审核")
        public void review() { }
    }
    @Test void actualApprovalCenterPathPreservesChineseAction() throws Exception {
        var log=mock(MesWorkflowInteractionLog.class);
        var filter=new MesWorkflowFailureLogFilter(new WebProperties(),log);
        var request=new MockHttpServletRequest("POST","/admin-api/approval-center/tasks/review");
        var handler=new HandlerMethod(new ReviewController(),ReviewController.class.getMethod("review"));
        filter.doFilter(request,new MockHttpServletResponse(),(req,res)->{
            WebFrameworkUtils.setLoginUserId(req,7L);
            req.setAttribute(cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor.ATTRIBUTE_HANDLER_METHOD,
                    handler);
            WebFrameworkUtils.setCommonResult(req,CommonResult.error(403,"无权限"));
        });
        verify(log).recordRequestFailure(eq(7L),eq("统一审批中心提交审核"),eq("POST"),eq("ReviewController#review"),eq(403));
    }
    @Test void authenticatedFailedRequestIsLoggedButSuccessAndLoggingEndpointAreNot() throws Exception {
        var log=mock(MesWorkflowInteractionLog.class);
        var filter=new MesWorkflowFailureLogFilter(new WebProperties(),log);
        var request=new MockHttpServletRequest("POST","/admin-api/mes/pro/frontline-feedback/submit");
        filter.doFilter(request,new MockHttpServletResponse(),(req,res)->{
            WebFrameworkUtils.setLoginUserId(req,7L);
            WebFrameworkUtils.setCommonResult((jakarta.servlet.http.HttpServletRequest)req,CommonResult.error(403,"无权限"));
        });
        verify(log).recordRequestFailure(eq(7L),eq("生产业务请求"),eq("POST"),eq("权限校验入口"),eq(403));
        reset(log);
        filter.doFilter(new MockHttpServletRequest("GET","/admin-api/mes/pro/orders"),new MockHttpServletResponse(),(req,res)->{
            WebFrameworkUtils.setLoginUserId(req,7L);
            WebFrameworkUtils.setCommonResult((jakarta.servlet.http.HttpServletRequest)req,CommonResult.success(true));
        });
        filter.doFilter(new MockHttpServletRequest("POST","/admin-api/mes/pro/workflow-interaction-log"),new MockHttpServletResponse(),(req,res)->{
            WebFrameworkUtils.setLoginUserId(req,7L);((jakarta.servlet.http.HttpServletResponse)res).setStatus(500);
        });
        verifyNoInteractions(log);
    }
}
