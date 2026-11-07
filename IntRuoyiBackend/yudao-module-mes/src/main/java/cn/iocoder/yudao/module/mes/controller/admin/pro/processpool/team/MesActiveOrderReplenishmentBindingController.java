package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesActiveOrderReplenishmentBindingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mes/pro/process-pool/team-leader/active-order/replenishment-bindings")
public class MesActiveOrderReplenishmentBindingController {
    private final MesActiveOrderReplenishmentBindingService service;

    @GetMapping
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<MesActiveOrderReplenishmentBindingService.Page> get(@RequestParam("activeOrderId") Long orderId) {
        return CommonResult.success(service.get(SecurityFrameworkUtils.getLoginUserId(), orderId));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Long> bind(@Valid @RequestBody MesActiveOrderReplenishmentBindingService.BindRequest request) {
        return CommonResult.success(service.bind(SecurityFrameworkUtils.getLoginUserId(), request));
    }
}
