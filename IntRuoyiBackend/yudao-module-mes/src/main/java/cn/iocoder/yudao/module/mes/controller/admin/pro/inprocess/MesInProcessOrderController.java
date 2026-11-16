package cn.iocoder.yudao.module.mes.controller.admin.pro.inprocess;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.inprocess.vo.*;
import cn.iocoder.yudao.module.mes.service.pro.inprocess.MesInProcessOrderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mes/pro/in-process-order")
@Validated
@RequiredArgsConstructor
public class MesInProcessOrderController {
    private final MesInProcessOrderService service;

    @GetMapping("/page")
    @Operation(summary = "跨组长查询当前租户在制工单")
    @PreAuthorize("@ss.hasPermission('mes:pro-in-process-order:query')")
    public CommonResult<PageResult<MesInProcessOrderRespVO>> page(@Valid MesInProcessOrderPageReqVO request) {
        return CommonResult.success(service.page(request));
    }
}
