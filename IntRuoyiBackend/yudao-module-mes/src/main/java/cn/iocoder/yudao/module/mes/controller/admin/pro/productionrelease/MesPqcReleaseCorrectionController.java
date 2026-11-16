package cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseCorrectionService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesActiveOrderDossierFileService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/pro/production-release/pqc/correction")
@Validated
@RequiredArgsConstructor
public class MesPqcReleaseCorrectionController {
    private final MesPqcReleaseCorrectionService service;
    private final MesActiveOrderDossierFileService dossierFiles;

    @PostMapping("/receive")
    @Operation(summary = "原申请人主动接收历史PQC驳回补正任务")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<String> receive(@Valid @RequestBody ReceiveRequest request) {
        return success(service.receive(SecurityFrameworkUtils.getLoginUserId(), request.applicationId()).toString());
    }

    @GetMapping("/get")
    @Operation(summary = "取得原申请人PQC放行补正任务")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task:query')")
    public CommonResult<MesPqcReleaseCorrectionService.Detail> get(@RequestParam @Positive Long workTaskId) {
        return success(service.get(SecurityFrameworkUtils.getLoginUserId(), workTaskId));
    }

    @GetMapping("/files")
    @Operation(summary = "读取本人PQC补正任务的正式资料")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task:query')")
    public CommonResult<MesActiveOrderDossierFileService.Result> files(@RequestParam @Positive Long workTaskId) {
        Long actor = SecurityFrameworkUtils.getLoginUserId();
        var detail = service.get(actor, workTaskId);
        return success(dossierFiles.list(actor, new MesActiveOrderDossierFileService.Query(detail.activeOrderId(), detail.applicationId())));
    }

    @PostMapping("/resubmit")
    @Operation(summary = "原申请人补正后重新申请PQC生产放行")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<MesPqcReleaseCorrectionService.Receipt> resubmit(@Valid @RequestBody ResubmitRequest request) {
        return success(service.resubmit(SecurityFrameworkUtils.getLoginUserId(),
                new MesPqcReleaseCorrectionService.Resubmit(request.workTaskId(), request.expectedVersion(),
                        request.idempotencyKey(), request.correctionNote())));
    }

    public record ResubmitRequest(@NotNull @Positive Long workTaskId,
                                  @NotNull @Positive Integer expectedVersion,
                                  @NotBlank @Size(max = 128) String idempotencyKey,
                                  @NotBlank @Size(max = 500) String correctionNote) { }
    public record ReceiveRequest(@NotNull @Positive Long applicationId) { }
}
