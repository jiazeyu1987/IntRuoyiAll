package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.dcc.MesRouteDccProjectBindingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.dcc.MesRouteDccProjectBindingSaveReqVO;
import cn.iocoder.yudao.module.mes.service.pro.route.MesRouteDccProjectBindingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 工艺路线 DCC 项目代码关系")
@RestController
@RequestMapping("/mes/pro/route")
@Validated
public class MesRouteDccProjectBindingController {

    @Resource
    private MesRouteDccProjectBindingService bindingService;

    @GetMapping("/dcc-project-binding")
    @Operation(summary = "获得工艺路线 DCC 项目代码关系")
    @Parameter(name = "routeId", description = "工艺路线ID", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-route:query')")
    public CommonResult<MesRouteDccProjectBindingRespVO> getBinding(
            @RequestParam("routeId") @NotNull Long routeId) {
        return success(bindingService.getBinding(routeId));
    }

    @PutMapping("/dcc-project-binding")
    @Operation(summary = "保存工艺路线 DCC 项目代码关系")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:update')")
    public CommonResult<MesRouteDccProjectBindingRespVO> saveBinding(
            @Valid @RequestBody MesRouteDccProjectBindingSaveReqVO reqVO) {
        return success(bindingService.saveBinding(reqVO));
    }

    @DeleteMapping("/dcc-project-binding")
    @Operation(summary = "解除工艺路线 DCC 项目代码关系")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:update')")
    public CommonResult<MesRouteDccProjectBindingRespVO> deleteBinding(
            @RequestParam("routeId") @NotNull Long routeId,
            @RequestParam("expectedVersion") @NotNull Long expectedVersion) {
        return success(bindingService.deleteBinding(routeId, expectedVersion));
    }
}
