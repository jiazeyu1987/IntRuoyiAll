package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesActiveOrderReplenishmentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mes/pro/process-pool/team-leader/active-order/replenishments")
public class MesActiveOrderReplenishmentController {
    private final MesActiveOrderReplenishmentQueryService service;

    @GetMapping
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<java.util.List<MesActiveOrderReplenishmentQueryService.Line>> get(@RequestParam("activeOrderId") Long orderId) {
        return CommonResult.success(service.list(SecurityFrameworkUtils.getLoginUserId(), orderId));
    }
}
