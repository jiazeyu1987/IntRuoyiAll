package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resource.MesProRouteResourcePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resource.MesProRouteResourceRespVO;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 产品工艺资源")
@RestController
@RequestMapping("/mes/pro/route-resource")
@Validated
public class MesProRouteResourceController {

    @Resource
    private MesProRouteResourceService routeResourceService;

    @GetMapping("/page")
    @Operation(summary = "获得产品工艺资源大表分页")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-route:query', 'mes:pro-route:schedule-config:query', "
            + "'mes:pro-mes-process:query')")
    public CommonResult<PageResult<MesProRouteResourceRespVO>> getResourcePage(
            @Valid MesProRouteResourcePageReqVO pageReqVO) {
        return success(routeResourceService.getResourcePage(pageReqVO));
    }
}
