package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 工序池班组长工作台")
@RestController
@RequestMapping("/mes/pro/process-pool/team-leader")
@RequiredArgsConstructor
public class MesFixedTestOrderDataController {
    private final MesFixedTestOrderDataService dataService;

    @PostMapping("/active-order/simulation/test-data")
    @Operation(summary = "按芋道源码删除并重建固定测试订单的领料单、补料单和生产用料清单")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<MesFixedTestOrderDataService.Result> prepare() {
        return success(dataService.prepare(SecurityFrameworkUtils.getLoginUserId()));
    }
}
