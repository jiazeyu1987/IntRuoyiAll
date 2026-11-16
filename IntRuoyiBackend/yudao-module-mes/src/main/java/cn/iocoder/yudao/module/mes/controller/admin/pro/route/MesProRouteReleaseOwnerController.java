package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteReleaseOwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 工艺路线放行责任人")
@RestController
@RequestMapping("/mes/pro/route")
@RequiredArgsConstructor
public class MesProRouteReleaseOwnerController {
    private final MesProRouteReleaseOwnerService service;

    @GetMapping("/release-owner")
    @Operation(summary = "只读查询当前租户的正式放行角色及成员")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-route:query', 'mes:pro-route:schedule-config:query', 'mes:pro-route:batch-record-config:query')")
    public CommonResult<MesProRouteReleaseOwnerService.Owner> getOwner(@RequestParam("code") String code) {
        return success(service.getOwner(code));
    }
}
