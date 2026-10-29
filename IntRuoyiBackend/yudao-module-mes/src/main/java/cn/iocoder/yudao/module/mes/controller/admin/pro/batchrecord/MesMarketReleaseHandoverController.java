package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesMarketReleaseHandoverService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
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
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-edhr-release:query','mes:pro-edhr-batch-execution:query')")
    public CommonResult<cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesMarketReleaseAccess.Access> access(
            @RequestParam(required=false) Long batchExecutionId,@RequestParam(required=false) Long activeOrderId) {
        return CommonResult.success(service.accessState(batchExecutionId,activeOrderId));
    }
    @GetMapping("/context")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:query')")
    public CommonResult<MesMarketReleaseHandoverService.Context> context(@RequestParam @Positive Long batchExecutionId) {
        return CommonResult.success(service.context(batchExecutionId));
    }
    @PostMapping("/assign")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:approve')")
    public CommonResult<MesMarketReleaseHandoverService.Context> handover(@Valid @RequestBody Command command) {
        return CommonResult.success(service.handover(command.batchExecutionId(),command.recipientUserId(),command.reason(),command.expectedVersion(),command.idempotencyKey()));
    }
    @PostMapping("/recall")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:query')")
    public CommonResult<MesMarketReleaseHandoverService.Context> recall(@Valid @RequestBody Command command) {
        return CommonResult.success(service.recall(command.batchExecutionId(),command.reason(),command.expectedVersion(),command.idempotencyKey()));
    }
}
