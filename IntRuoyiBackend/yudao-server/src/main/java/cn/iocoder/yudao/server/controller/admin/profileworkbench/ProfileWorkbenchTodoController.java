package cn.iocoder.yudao.server.controller.admin.profileworkbench;

import cn.iocoder.yudao.server.profileworkbench.*;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import java.util.Set;
@RestController
@RequestMapping("/system/profile-workbench-todo")
public class ProfileWorkbenchTodoController {
    @Resource private ProfileWorkbenchTodoQueryService service;
    private static final Set<String> PAGE_PARAMS = Set.of("enabledSources", "pageNo", "pageSize", "visibility", "taskType",
        "quickFilter.fieldKey", "quickFilter.operator", "quickFilter.value", "sort.key", "sort.order");
    @GetMapping("/page")
    public CommonResult<ProfileWorkbenchTodoPageRespVO> page(@Valid @ModelAttribute ProfileWorkbenchTodoPageReqVO query, HttpServletRequest request) {
        validateParameters(request, PAGE_PARAMS);
        return CommonResult.success(service.page(query));
    }
    @GetMapping("/count")
    public CommonResult<ProfileWorkbenchTodoCountRespVO> count(@Valid @ModelAttribute ProfileWorkbenchTodoCountReqVO query, HttpServletRequest request) {
        validateParameters(request, Set.of("enabledSources"));
        return CommonResult.success(service.count(query));
    }
    private static void validateParameters(HttpServletRequest request, Set<String> allowed) {
        request.getParameterMap().forEach((key, values) -> {
            if (!allowed.contains(key) || (!"enabledSources".equals(key) && values.length != 1))
                throw exception(new ErrorCode(400, "不支持的工作台查询参数"));
            if ("enabledSources".equals(key)) {
                for (String value : values) {
                    try {
                        cn.iocoder.yudao.module.system.api.profileworkbench.ProfileWorkbenchTodoSourceId.valueOf(value);
                    } catch (IllegalArgumentException ex) {
                        throw exception(new ErrorCode(400, "工作台来源必须使用固定枚举及重复参数"));
                    }
                }
            }
        });
    }
}
