package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesSimpleParserProjectService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/pro/batch-record-report/simple-parser")
@RequiredArgsConstructor
public class MesSimpleParserProjectController {
    private final MesSimpleParserProjectService service;

    @GetMapping("/projects")
    @Operation(summary = "获取解析页面全部启用项目代码，不按项目成员权限过滤")
    @PreAuthorize("@ss.hasAnyPermissions('form:parser:query', 'form:parser:production-batch-record', 'form:parser:production-batch-record:publish')")
    public CommonResult<List<MesSimpleParserProjectService.ProjectOption>> projects(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(service.options(keyword));
    }
}
