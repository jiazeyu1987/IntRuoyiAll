package cn.iocoder.yudao.module.mes.framework.web.core.filter;

import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesWorkflowInteractionLog;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import java.io.IOException;
import static cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor.ATTRIBUTE_HANDLER_METHOD;

/** Records authenticated unsuccessful requests independently from the rolled-back business transaction. */
public class MesWorkflowFailureLogFilter extends OncePerRequestFilter {
    private final MesWorkflowInteractionLog log;
    private final String prefix;
    public MesWorkflowFailureLogFilter(WebProperties properties, MesWorkflowInteractionLog log) {
        this.log=log; this.prefix=properties.getAdminApi().getPrefix();
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path=request.getRequestURI().substring(request.getContextPath().length());
        return !(path.startsWith(prefix+"/mes/pro/") || path.startsWith(prefix+"/approval-center/"))
                || path.startsWith(prefix+"/mes/pro/workflow-interaction-log");
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Exception failure=null;
        try { chain.doFilter(request,response); }
        catch (IOException | ServletException | RuntimeException ex) { failure=ex; throw ex; }
        finally {
            Long actor=WebFrameworkUtils.getLoginUserId(request);
            var result=WebFrameworkUtils.getCommonResult(request);
            int code=result!=null ? result.getCode() : failure!=null ? 500 : response.getStatus()>=400 ? response.getStatus() : 0;
            if(actor!=null && code!=0) {
                Object mapped=request.getAttribute(ATTRIBUTE_HANDLER_METHOD);
                var operation=mapped instanceof HandlerMethod method ? method.getMethodAnnotation(Operation.class) : null;
                String endpoint=mapped instanceof HandlerMethod method ? method.getBeanType().getSimpleName()+"#"+method.getMethod().getName() : "权限校验入口";
                log.recordRequestFailure(actor,operation==null ? "生产业务请求" : operation.summary(),request.getMethod(),endpoint,code);
            }
        }
    }
}
