package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesActiveOrderSignatureEvidenceRespVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesActiveOrderSignatureEvidenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Separate business entries retain the permission and scope of their existing detail page. */
@RestController
@RequiredArgsConstructor
public class MesActiveOrderSignatureEvidenceController {
    private final MesActiveOrderSignatureEvidenceService service;

    @GetMapping("/mes/pro/process-pool/team-leader/active-order/signature/get")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<MesActiveOrderSignatureEvidenceRespVO> team(
            @RequestParam("activeOrderId") Long activeOrderId, @RequestParam("signatureId") Long signatureId) {
        return CommonResult.success(MesActiveOrderSignatureEvidenceRespVO.from(
                service.getTeam(SecurityFrameworkUtils.getLoginUserId(), activeOrderId, signatureId)));
    }

    @GetMapping("/mes/pro/production-release/pqc/signature/get")
    @PreAuthorize("@ss.hasPermission('mes:pro-production-release:query')")
    public CommonResult<MesActiveOrderSignatureEvidenceRespVO> pqc(
            @RequestParam("applicationId") Long applicationId, @RequestParam("signatureId") Long signatureId) {
        return CommonResult.success(MesActiveOrderSignatureEvidenceRespVO.from(
                service.getPqc(SecurityFrameworkUtils.getLoginUserId(), applicationId, signatureId)));
    }

    @GetMapping("/mes/pro/edhr-batch-execution/signature/get")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:query')")
    public CommonResult<MesActiveOrderSignatureEvidenceRespVO> batch(
            @RequestParam(value = "batchExecutionId", required = false) Long batchExecutionId,
            @RequestParam(value = "activeOrderId", required = false) Long activeOrderId,
            @RequestParam("signatureId") Long signatureId) {
        return CommonResult.success(MesActiveOrderSignatureEvidenceRespVO.from(
                service.getBatch(batchExecutionId, activeOrderId, signatureId)));
    }
}
