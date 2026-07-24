package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteResourceCapacityPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteScheduleConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteScheduleConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteScheduleConfigService;
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

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 路线排产配置")
@RestController
@RequestMapping("/mes/pro/route-schedule-config")
@Validated
public class MesProRouteScheduleConfigController {

    @Resource
    private MesProRouteScheduleConfigService routeScheduleConfigService;

    @PostMapping("/save")
    @Operation(summary = "保存路线排产配置")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:schedule-config:update')")
    public CommonResult<Long> saveConfig(@Valid @RequestBody MesProRouteScheduleConfigSaveReqVO reqVO) {
        return success(routeScheduleConfigService.saveConfig(reqVO));
    }

    @GetMapping("/list-by-route-version")
    @Operation(summary = "按路线版本查询排产配置")
    @Parameter(name = "routeVersionId", description = "路线版本编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-route:schedule-config:query')")
    public CommonResult<List<MesProRouteScheduleConfigRespVO>> getConfigListByRouteVersion(
            @RequestParam("routeVersionId") Long routeVersionId) {
        return success(routeScheduleConfigService.getConfigRespListByRouteVersionId(routeVersionId));
    }

    @GetMapping("/resource-preview")
    @Operation(summary = "预览路线工序资源计算产能")
    @Parameter(name = "routeProcessId", description = "路线工序编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-route:schedule-config:query')")
    public CommonResult<MesProRouteResourceCapacityPreviewRespVO> getResourcePreview(
            @RequestParam("routeProcessId") Long routeProcessId) {
        return success(routeScheduleConfigService.getResourcePreview(routeProcessId));
    }

}
