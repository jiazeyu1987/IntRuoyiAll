package cn.iocoder.yudao.module.system.controller.admin.codextest;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestArtifactRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerCheckpointResultReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerClaimReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerClaimRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerCompleteCaseReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerHeartbeatReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerHeartbeatRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerProgressReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerRegisterReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerRegisterRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerStatusRespVO;
import cn.iocoder.yudao.module.system.service.codextest.CodexTestArtifactService;
import cn.iocoder.yudao.module.system.service.codextest.CodexTestRunnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.function.Supplier;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.HEADER_TENANT_ID;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_RESULT_SCHEMA_INVALID;

@Tag(name = "管理后台 - Codex Runner 协议")
@RestController
@RequestMapping("/system/codex-test-runner")
@Validated
public class CodexTestRunnerController {

    public static final String RUNNER_TOKEN_HEADER = "X-Codex-Runner-Token";

    @Resource
    private CodexTestRunnerService codexTestRunnerService;
    @Resource
    private CodexTestArtifactService codexTestArtifactService;

    @GetMapping("/status")
    @Operation(summary = "查询 Codex Runner 在线状态")
    @PreAuthorize("@ss.hasPermission('system:codex-test:query')")
    public CommonResult<CodexTestRunnerStatusRespVO> getRunnerStatus() {
        return success(codexTestRunnerService.getRunnerStatus());
    }

    @PostMapping("/register")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "注册 Codex Runner")
    public CommonResult<CodexTestRunnerRegisterRespVO> registerRunner(
            @Valid @RequestBody CodexTestRunnerRegisterReqVO registerReqVO,
            @RequestHeader(value = RUNNER_TOKEN_HEADER, required = false) String token,
            @RequestHeader(value = HEADER_TENANT_ID, required = false) Long managementTenantId) {
        return executeWithRunnerTenant(managementTenantId,
                () -> success(codexTestRunnerService.registerRunner(registerReqVO, token)));
    }

    @PostMapping("/claim")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "Codex Runner 领取任务")
    public CommonResult<CodexTestRunnerClaimRespVO> claimTasks(
            @Valid @RequestBody CodexTestRunnerClaimReqVO claimReqVO,
            @RequestHeader(value = RUNNER_TOKEN_HEADER, required = false) String token,
            @RequestHeader(value = HEADER_TENANT_ID, required = false) Long managementTenantId) {
        return executeWithRunnerTenant(managementTenantId,
                () -> success(codexTestRunnerService.claimTasks(claimReqVO, token)));
    }

    @PostMapping("/heartbeat")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "Codex Runner 心跳")
    public CommonResult<CodexTestRunnerHeartbeatRespVO> heartbeat(
            @Valid @RequestBody CodexTestRunnerHeartbeatReqVO heartbeatReqVO,
            @RequestHeader(value = RUNNER_TOKEN_HEADER, required = false) String token,
            @RequestHeader(value = HEADER_TENANT_ID, required = false) Long managementTenantId) {
        return executeWithRunnerTenant(managementTenantId,
                () -> success(codexTestRunnerService.heartbeat(heartbeatReqVO, token)));
    }

    @PostMapping("/checkpoint-result")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "Codex Runner 回写检查点结果")
    public CommonResult<Boolean> saveCheckpointResult(
            @Valid @RequestBody CodexTestRunnerCheckpointResultReqVO resultReqVO,
            @RequestHeader(value = RUNNER_TOKEN_HEADER, required = false) String token,
            @RequestHeader(value = HEADER_TENANT_ID, required = false) Long managementTenantId) {
        return executeWithRunnerTenant(managementTenantId, () -> {
            codexTestRunnerService.saveCheckpointResult(resultReqVO, token);
            return success(true);
        });
    }

    @PostMapping("/progress")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "Codex Runner 回写执行进度")
    public CommonResult<Boolean> reportProgress(
            @Valid @RequestBody CodexTestRunnerProgressReqVO progressReqVO,
            @RequestHeader(value = RUNNER_TOKEN_HEADER, required = false) String token,
            @RequestHeader(value = HEADER_TENANT_ID, required = false) Long managementTenantId) {
        return executeWithRunnerTenant(managementTenantId, () -> {
            codexTestRunnerService.reportProgress(progressReqVO, token);
            return success(true);
        });
    }

    @PostMapping("/complete-case")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "Codex Runner 完成执行项")
    public CommonResult<Boolean> completeCase(
            @Valid @RequestBody CodexTestRunnerCompleteCaseReqVO completeReqVO,
            @RequestHeader(value = RUNNER_TOKEN_HEADER, required = false) String token,
            @RequestHeader(value = HEADER_TENANT_ID, required = false) Long managementTenantId) {
        return executeWithRunnerTenant(managementTenantId, () -> {
            codexTestRunnerService.completeCase(completeReqVO, token);
            return success(true);
        });
    }

    @PostMapping("/artifact")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "Codex Runner 上传临时截图")
    public CommonResult<CodexTestArtifactRespVO> uploadArtifact(
            @RequestParam("executionCaseId") Long executionCaseId,
            @RequestParam("checkpointSort") Integer checkpointSort,
            @RequestParam("artifactType") String artifactType,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = RUNNER_TOKEN_HEADER, required = false) String token,
            @RequestHeader(value = HEADER_TENANT_ID, required = false) Long managementTenantId) {
        return executeWithRunnerTenant(managementTenantId, () -> {
            codexTestRunnerService.validateRunnerToken(token);
            return success(codexTestArtifactService.saveArtifact(executionCaseId, checkpointSort, artifactType, file));
        });
    }

    private <T> CommonResult<T> executeWithRunnerTenant(Long managementTenantId,
                                                       Supplier<CommonResult<T>> supplier) {
        if (managementTenantId == null) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "Runner 管理租户不能为空");
        }
        Long oldTenantId = TenantContextHolder.getTenantId();
        boolean oldIgnore = TenantContextHolder.isIgnore();
        try {
            TenantContextHolder.setTenantId(managementTenantId);
            TenantContextHolder.setIgnore(false);
            return supplier.get();
        } finally {
            TenantContextHolder.setTenantId(oldTenantId);
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

}
