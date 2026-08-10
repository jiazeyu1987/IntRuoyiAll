package cn.iocoder.yudao.module.system.controller.admin.codextest;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestExecutionCancelReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCodeReadonlyExecutionStartReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestExecutionPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestExecutionRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestExecutionStartReqVO;
import cn.iocoder.yudao.module.system.service.codextest.CodexTestArtifactService;
import cn.iocoder.yudao.module.system.service.codextest.CodexTestExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - Codex 测试执行")
@RestController
@RequestMapping("/system/codex-test-execution")
@Validated
public class CodexTestExecutionController {

    @Resource
    private CodexTestExecutionService codexTestExecutionService;
    @Resource
    private CodexTestArtifactService codexTestArtifactService;

    @PostMapping("/start")
    @Operation(summary = "启动 Codex 测试执行")
    @PreAuthorize("@ss.hasPermission('system:codex-test:execute')")
    public CommonResult<Long> startExecution(@Valid @RequestBody CodexTestExecutionStartReqVO startReqVO) {
        return success(codexTestExecutionService.startExecution(startReqVO, getLoginUserId()));
    }

    @PostMapping("/start-code-readonly")
    @Operation(summary = "写入只读代码测试定义并启动 Codex 执行")
    @PreAuthorize("@ss.hasPermission('system:codex-test:execute')")
    public CommonResult<Long> startCodeReadonlyExecution(
            @Valid @RequestBody CodexTestCodeReadonlyExecutionStartReqVO startReqVO) {
        return success(codexTestExecutionService.startCodeReadonlyExecution(startReqVO, getLoginUserId()));
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消 Codex 测试执行")
    @PreAuthorize("@ss.hasPermission('system:codex-test:cancel')")
    public CommonResult<Boolean> cancelExecution(@Valid @RequestBody CodexTestExecutionCancelReqVO cancelReqVO) {
        codexTestExecutionService.cancelExecution(cancelReqVO);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得 Codex 测试执行分页")
    @PreAuthorize("@ss.hasPermission('system:codex-test:query')")
    public CommonResult<PageResult<CodexTestExecutionRespVO>> getExecutionPage(
            @Valid CodexTestExecutionPageReqVO pageReqVO) {
        return success(codexTestExecutionService.getExecutionPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得 Codex 测试执行详情")
    @Parameter(name = "id", description = "执行编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:codex-test:query')")
    public CommonResult<CodexTestExecutionRespVO> getExecution(@RequestParam("id") Long id) {
        return success(codexTestExecutionService.getExecution(id));
    }

    @GetMapping("/result")
    @Operation(summary = "获得当前用户发起的 Codex 测试执行结果")
    @Parameter(name = "id", description = "执行编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:codex-test:execute')")
    public CommonResult<CodexTestExecutionRespVO> getExecutionResult(@RequestParam("id") Long id) {
        return success(codexTestExecutionService.getExecutionResult(id, getLoginUserId()));
    }

    @GetMapping("/monitor")
    @Operation(summary = "获得 Codex 测试运行监控")
    @PreAuthorize("@ss.hasPermission('system:codex-test:query')")
    public CommonResult<List<CodexTestExecutionRespVO>> getExecutionMonitor() {
        return success(codexTestExecutionService.getExecutionMonitor());
    }

    @GetMapping("/artifact")
    @Operation(summary = "读取 Codex 测试失败截图")
    @Parameter(name = "id", description = "artifact 编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:codex-test:artifact')")
    public void getArtifact(@RequestParam("id") Long artifactId, HttpServletResponse response) throws IOException {
        CodexTestArtifactService.ArtifactFile artifactFile = codexTestArtifactService.getArtifactFile(artifactId);
        response.setContentType(artifactFile.contentType());
        Files.copy(artifactFile.file().toPath(), response.getOutputStream());
    }

}
