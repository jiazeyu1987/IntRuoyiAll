package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowGraphRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowValidationRespVO;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 工艺路线工序流转关系图")
@RestController
@RequestMapping("/mes/pro/route-process-flow")
@Validated
public class MesProRouteProcessFlowController {

    @Resource
    private MesProRouteProcessFlowService flowService;

    @GetMapping("/get")
    @Operation(summary = "获得工艺路线工序流转关系图")
    @Parameter(name = "routeId", description = "工艺路线编号", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-route:query', 'mes:pro-route:schedule-config:query', 'mes:pro-route:batch-record-config:query')")
    public CommonResult<MesProRouteProcessFlowGraphRespVO> getGraph(
            @RequestParam("routeId") Long routeId,
            @RequestParam(value = "routeVersionId", required = false) Long routeVersionId) {
        return success(flowService.getGraph(routeId, routeVersionId));
    }

    @PostMapping("/validate")
    @Operation(summary = "校验工艺路线工序流转关系图")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:update')")
    public CommonResult<MesProRouteProcessFlowValidationRespVO> validateGraph(
            @Valid @RequestBody MesProRouteProcessFlowSaveReqVO reqVO) {
        return success(flowService.validateGraph(reqVO));
    }

    @PostMapping("/save")
    @Operation(summary = "保存工艺路线工序流转关系图")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:update')")
    public CommonResult<MesProRouteProcessFlowValidationRespVO> saveGraph(
            @Valid @RequestBody MesProRouteProcessFlowSaveReqVO reqVO) {
        return success(flowService.saveGraph(reqVO));
    }

}
