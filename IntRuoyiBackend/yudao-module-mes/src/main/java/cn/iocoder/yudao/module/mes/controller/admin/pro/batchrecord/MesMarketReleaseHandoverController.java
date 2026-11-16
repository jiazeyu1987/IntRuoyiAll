package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesMarketReleaseHandoverService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mes/pro/edhr-release/handover")
@Validated
@RequiredArgsConstructor
public class MesMarketReleaseHandoverController {
    private final MesMarketReleaseHandoverService service;
    public record Command(@NotNull @Positive Long batchExecutionId, Long recipientUserId,
                          @NotBlank @Size(max=1000) String reason,
                          @NotBlank @Size(max=64) String expectedVersion,
                          @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{8,100}") String idempotencyKey) {}
    @GetMapping("/access")
    @Operation(summary="核验批次上市放行访问权限")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-edhr-release:query','mes:pro-edhr-batch-execution:query')")
    public CommonResult<cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesMarketReleaseAccess.Access> access(
            @RequestParam(required=false) Long batchExecutionId,@RequestParam(required=false) Long activeOrderId) {
        return CommonResult.success(service.accessState(batchExecutionId,activeOrderId));
    }
    @GetMapping("/context")
    @Operation(summary="查看上市放行交接信息")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:query')")
    public CommonResult<MesMarketReleaseHandoverService.Context> context(@RequestParam @Positive Long batchExecutionId) {
        return CommonResult.success(service.context(batchExecutionId));
    }
    @PostMapping("/assign")
    @Operation(summary="交接上市放行任务")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:approve')")
    public CommonResult<MesMarketReleaseHandoverService.Context> handover(@Valid @RequestBody Command command) {
        return CommonResult.success(service.handover(command.batchExecutionId(),command.recipientUserId(),command.reason(),command.expectedVersion(),command.idempotencyKey()));
    }
    @PostMapping("/recall")
    @Operation(summary="收回上市放行交接任务")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:query')")
    public CommonResult<MesMarketReleaseHandoverService.Context> recall(@Valid @RequestBody Command command) {
        return CommonResult.success(service.recall(command.batchExecutionId(),command.reason(),command.expectedVersion(),command.idempotencyKey()));
    }
}
