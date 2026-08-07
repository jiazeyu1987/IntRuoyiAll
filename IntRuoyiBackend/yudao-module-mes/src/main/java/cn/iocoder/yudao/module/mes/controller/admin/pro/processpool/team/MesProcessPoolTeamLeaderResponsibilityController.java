package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderResponsibleRouteRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesRouteStartProductionLeaderAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 生产组长正式负责工艺路线")
@RestController
@RequestMapping("/mes/pro/process-pool/team-leader")
public class MesProcessPoolTeamLeaderResponsibilityController {

    private final MesRouteStartProductionLeaderAuthorizationService authorizationService;

    public MesProcessPoolTeamLeaderResponsibilityController(
            MesRouteStartProductionLeaderAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @GetMapping("/responsible-routes")
    @Operation(summary = "查询生产组长正式负责工艺路线")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<List<MesTeamLeaderResponsibleRouteRespVO>> getResponsibleRoutes() {
        return success(authorizationService.listResponsibleRoutes(SecurityFrameworkUtils.getLoginUserId()).stream()
                .map(MesProcessPoolTeamLeaderResponsibilityController::toRespVO)
                .toList());
    }

    private static MesTeamLeaderResponsibleRouteRespVO toRespVO(MesProRouteDO route) {
        return new MesTeamLeaderResponsibleRouteRespVO()
                .setRouteId(route.getId())
                .setRouteCode(route.getCode())
                .setRouteName(route.getName());
    }

}
