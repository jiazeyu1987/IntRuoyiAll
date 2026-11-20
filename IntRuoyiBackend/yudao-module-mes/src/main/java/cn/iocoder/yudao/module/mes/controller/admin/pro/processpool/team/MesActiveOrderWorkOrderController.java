package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderRespVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesActiveOrderWorkOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mes/pro/process-pool/team-leader/active-order/work-order")
public class MesActiveOrderWorkOrderController {
    private final MesActiveOrderWorkOrderService service;

    @GetMapping
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<MesProWorkOrderRespVO> detail(@RequestParam("activeOrderId") Long activeOrderId) {
        return CommonResult.success(service.detail(SecurityFrameworkUtils.getLoginUserId(), activeOrderId));
    }

    public record FillBatchCodeRequest(@NotNull Long activeOrderId,
                                      @NotBlank @Size(max = 64) String batchCode) {}

    @PutMapping("/batch-code")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Boolean> fillBatchCode(@Valid @RequestBody FillBatchCodeRequest request) {
        service.fillBatchCode(SecurityFrameworkUtils.getLoginUserId(), request.activeOrderId(), request.batchCode());
        return CommonResult.success(true);
    }
}
