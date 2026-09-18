package cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.MesProcessPoolTeamLeaderController;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListRespVO;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseOrderDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/mes/pro/production-release/pqc")
@RequiredArgsConstructor
public class MesPqcReleaseOrderDetailController {
    private final MesPqcReleaseOrderDetailService service;

    @GetMapping("/order-detail")
    @PreAuthorize("@ss.hasPermission('mes:pro-production-release:query')")
    public CommonResult<Result> get(@RequestParam("applicationId") Long applicationId) {
        var result = service.get(SecurityFrameworkUtils.getLoginUserId(), applicationId);
        return CommonResult.success(new Result(
                MesProcessPoolTeamLeaderController.toActiveOrderDetailRespVO(result.detail()),
                result.productionMaterialLists()));
    }

    public record Result(MesTeamLeaderActiveOrderDetailRespVO detail,
                         List<MesKingdeeProductionMaterialListRespVO> productionMaterialLists) {}
}
