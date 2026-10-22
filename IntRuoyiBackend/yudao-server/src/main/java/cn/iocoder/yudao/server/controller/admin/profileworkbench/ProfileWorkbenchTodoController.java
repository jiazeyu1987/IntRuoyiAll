package cn.iocoder.yudao.server.controller.admin.profileworkbench;
import cn.iocoder.yudao.server.profileworkbench.*;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.INVALID_QUERY;
@RestController @RequestMapping("/workbench")
public class ProfileWorkbenchTodoController {
    @Resource private WorkbenchQueryApplication service;
    @Resource private ObjectMapper mapper;
    @GetMapping("/sources") public CommonResult<WorkbenchSourceRegistry.Directory> sources() { return CommonResult.success(service.directory()); }
    @PostMapping("/context") @ApiAccessLog(requestEnable=false)
    public CommonResult<WorkbenchContextResponse> context(@RequestBody JsonNode body) {
        fields(body,Set.of("registryRevision","sources","contextEpoch","querySpec"));
        strings(body,Set.of("registryRevision","contextEpoch"));
        if(!body.has("sources") || !body.get("sources").isArray())throw INVALID_QUERY.exception();
        body.get("sources").forEach(value->{if(!value.isTextual())throw INVALID_QUERY.exception();});
        JsonNode spec=body.get("querySpec"); fields(spec,Set.of("visibility","taskType","quickFilter","sort"));
        strings(spec,Set.of("visibility"));
        if(spec.hasNonNull("taskType") && !spec.get("taskType").isTextual())throw INVALID_QUERY.exception();
        if(spec.hasNonNull("quickFilter")) { fields(spec.get("quickFilter"),Set.of("fieldKey","operator","value"));strings(spec.get("quickFilter"),Set.of("fieldKey","operator","value")); }
        if(spec.hasNonNull("sort")) { fields(spec.get("sort"),Set.of("key","order"));strings(spec.get("sort"),Set.of("key","order")); }
        return CommonResult.success(service.context(convert(body,WorkbenchContextRequest.class)));
    }
    @GetMapping("/tasks/page") @ApiAccessLog(requestEnable=false)
    public CommonResult<ProfileWorkbenchTodoPageRespVO> page(HttpServletRequest request) {
        parameters(request,Set.of("queryVersion","contextEpoch","pageNo","pageSize"));
        return CommonResult.success(service.page(request.getParameter("queryVersion"),request.getParameter("contextEpoch"),integer(request,"pageNo"),integer(request,"pageSize")));
    }
    @GetMapping("/tasks/count") @ApiAccessLog(requestEnable=false)
    public CommonResult<ProfileWorkbenchTodoCountRespVO> count(HttpServletRequest request) {
        parameters(request,Set.of("queryVersion","contextEpoch"));return CommonResult.success(service.count(request.getParameter("queryVersion"),request.getParameter("contextEpoch")));
    }
    @PostMapping("/tasks/visibility") @ApiAccessLog(requestEnable=false)
    public CommonResult<WorkbenchQueryApplication.VisibilityResponse> visibility(@RequestBody JsonNode body) {
        fields(body,Set.of("queryVersion","contextEpoch","taskKey","operation"));
        strings(body,Set.of("queryVersion","contextEpoch","taskKey","operation"));
        return CommonResult.success(service.visibility(convert(body,WorkbenchVisibilityRequest.class)));
    }
    private <T>T convert(JsonNode body,Class<T> type) {
        try{return mapper.treeToValue(body,type);}catch(Exception failure){throw INVALID_QUERY.exception(failure);}
    }
    private static void fields(JsonNode body,Set<String> allowed) {
        if(body==null || !body.isObject())throw INVALID_QUERY.exception();
        body.fieldNames().forEachRemaining(key->{if(!allowed.contains(key))throw INVALID_QUERY.exception();});
    }
    private static void strings(JsonNode body,Set<String> names) {
        names.forEach(name->{if(!body.has(name)||!body.get(name).isTextual())throw INVALID_QUERY.exception();});
    }
    private static void parameters(HttpServletRequest request,Set<String> allowed) {
        if(!request.getParameterMap().keySet().equals(allowed))throw INVALID_QUERY.exception();
        request.getParameterMap().forEach((key,values)->{if(!allowed.contains(key)||values.length!=1)throw INVALID_QUERY.exception();});
    }
    private static int integer(HttpServletRequest request,String name) {
        String value=request.getParameter(name);if(value==null||!value.matches("[1-9][0-9]*"))throw INVALID_QUERY.exception();
        try{return Integer.parseInt(value);}catch(NumberFormatException failure){throw INVALID_QUERY.exception(failure);}
    }
}
