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
    @Operation(summary = "补齐固定测试订单来源单据，跳过已有数据")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<MesFixedTestOrderDataService.Result> prepare() {
        return success(dataService.prepare(SecurityFrameworkUtils.getLoginUserId()));
    }

    @PostMapping("/active-order/simulation/test-data-2")
    @Operation(summary = "补齐第二套固定测试订单及关联单据，跳过已有数据")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<MesFixedTestOrderDataService.Result> prepareSecond() {
        return success(dataService.prepareSecond(SecurityFrameworkUtils.getLoginUserId()));
    }
}
