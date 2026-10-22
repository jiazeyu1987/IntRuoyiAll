package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesActiveOrderCorrectionEvidenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MesActiveOrderCorrectionEvidenceController {
    private final MesActiveOrderCorrectionEvidenceService service;
    @GetMapping("/mes/pro/process-pool/team-leader/active-order/corrections")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<MesActiveOrderCorrectionEvidenceService.Timeline> team(@RequestParam("activeOrderId") Long activeOrderId) {
        return CommonResult.success(service.getTeam(SecurityFrameworkUtils.getLoginUserId(), activeOrderId));
    }
    @GetMapping("/mes/pro/production-release/pqc/corrections")
    @PreAuthorize("@ss.hasPermission('mes:pro-production-release:query')")
    public CommonResult<MesActiveOrderCorrectionEvidenceService.Timeline> pqc(@RequestParam("applicationId") Long applicationId) {
        return CommonResult.success(service.getPqc(SecurityFrameworkUtils.getLoginUserId(), applicationId));
    }
    @GetMapping("/mes/pro/edhr-batch-execution/corrections")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:query')")
    public CommonResult<MesActiveOrderCorrectionEvidenceService.Timeline> batch(
            @RequestParam(value = "batchExecutionId", required = false) Long batchExecutionId,
            @RequestParam(value = "activeOrderId", required = false) Long activeOrderId) {
        return CommonResult.success(service.getBatch(batchExecutionId, activeOrderId));
    }
}
