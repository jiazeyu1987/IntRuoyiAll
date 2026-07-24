package cn.iocoder.yudao.module.system.controller.admin.codextest;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCasePageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCaseRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCaseSaveReqVO;
import cn.iocoder.yudao.module.system.service.codextest.CodexTestCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - Codex 测试项")
@RestController
@RequestMapping("/system/codex-test-case")
@Validated
public class CodexTestCaseController {

    @Resource
    private CodexTestCaseService codexTestCaseService;

    @PostMapping("/create")
    @Operation(summary = "创建 Codex 测试项")
    @PreAuthorize("@ss.hasPermission('system:codex-test:create')")
    public CommonResult<Long> createCase(@Valid @RequestBody CodexTestCaseSaveReqVO createReqVO) {
        return success(codexTestCaseService.createCase(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 Codex 测试项")
    @PreAuthorize("@ss.hasPermission('system:codex-test:update')")
    public CommonResult<Boolean> updateCase(@Valid @RequestBody CodexTestCaseSaveReqVO updateReqVO) {
        codexTestCaseService.updateCase(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 Codex 测试项")
    @Parameter(name = "id", description = "测试项编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:codex-test:delete')")
    public CommonResult<Boolean> deleteCase(@RequestParam("id") Long id) {
        codexTestCaseService.deleteCase(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 Codex 测试项")
    @Parameter(name = "id", description = "测试项编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:codex-test:query')")
    public CommonResult<CodexTestCaseRespVO> getCase(@RequestParam("id") Long id) {
        return success(codexTestCaseService.getCase(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得 Codex 测试项分页")
    @PreAuthorize("@ss.hasPermission('system:codex-test:query')")
    public CommonResult<PageResult<CodexTestCaseRespVO>> getCasePage(@Valid CodexTestCasePageReqVO pageReqVO) {
        return success(codexTestCaseService.getCasePage(pageReqVO));
    }

}
