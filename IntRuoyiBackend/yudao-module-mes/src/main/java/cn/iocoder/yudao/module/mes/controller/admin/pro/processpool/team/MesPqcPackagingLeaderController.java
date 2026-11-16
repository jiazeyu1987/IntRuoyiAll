package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesPqcPackagingLeaderOrderService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/pro-process-pool/pqc-packaging-leader")
public class MesPqcPackagingLeaderController {
    private final MesPqcPackagingLeaderOrderService service;
    public MesPqcPackagingLeaderController(MesPqcPackagingLeaderOrderService service) { this.service = service; }

    @GetMapping("/active-orders")
    @Operation(summary = "PQC包装组长查看本租户全部在制活跃订单")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query') and @ss.hasAnyRoles('pqc_packaging_leader_permission')")
    public CommonResult<List<MesPqcPackagingLeaderOrderService.OrderRow>> listOrders() {
        return success(service.listOrders(SecurityFrameworkUtils.getLoginUserId()));
    }
}
