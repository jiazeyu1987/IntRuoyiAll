package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListRespVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderProductionMaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 生产组长活跃订单用料清单")
@RestController
@RequestMapping("/mes/pro/process-pool/team-leader")
public class MesTeamLeaderActiveOrderProductionMaterialController {

    private final MesTeamLeaderActiveOrderProductionMaterialService service;

    public MesTeamLeaderActiveOrderProductionMaterialController(MesTeamLeaderActiveOrderProductionMaterialService service) {
        this.service = service;
    }

    @GetMapping("/active-order/production-material-lists")
    @Operation(summary = "查询当前生产组长活跃订单的完整生产用料清单")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<List<MesKingdeeProductionMaterialListRespVO>> getList(
            @RequestParam("activeOrderId") Long activeOrderId) {
        return success(service.getList(SecurityFrameworkUtils.getLoginUserId(), activeOrderId));
    }
}
