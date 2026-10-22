package cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.gxpaudit.MesGxpAuditControllerSupport;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.MesProcessPoolTeamLeaderController;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListRespVO;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseOrderDetailService;
import cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo.GxpAuditEventPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo.GxpAuditEventRespVO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/mes/pro/production-release/pqc")
@RequiredArgsConstructor
public class MesPqcReleaseOrderDetailController {
    private final MesPqcReleaseOrderDetailService service;
    private final GxpAuditQueryService gxpAuditQueryService;

    @GetMapping("/order-detail")
    @PreAuthorize("@ss.hasPermission('mes:pro-production-release:query')")
    public CommonResult<Result> get(@RequestParam("applicationId") Long applicationId) {
        var result = service.get(SecurityFrameworkUtils.getLoginUserId(), applicationId);
        return CommonResult.success(new Result(
                MesProcessPoolTeamLeaderController.toActiveOrderDetailRespVO(result.detail()),
                result.productionMaterialLists()));
    }

    @GetMapping("/audit/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-production-release:query')")
    public CommonResult<PageResult<GxpAuditEventRespVO>> auditPage(
            @Valid GxpAuditEventPageReqVO reqVO,
            @RequestParam("applicationId") Long applicationId) {
        var orderDetail = service.get(SecurityFrameworkUtils.getLoginUserId(), applicationId);
        return CommonResult.success(MesGxpAuditControllerSupport.toPage(gxpAuditQueryService.page(
                MesGxpAuditControllerSupport.toQuery(reqVO, "ACTIVE_ORDER",
                        orderDetail.detail().getActiveOrderId()))));
    }

    @GetMapping("/audit/get")
    @PreAuthorize("@ss.hasPermission('mes:pro-production-release:query')")
    public CommonResult<GxpAuditEventRespVO> auditGet(
            @RequestParam("applicationId") Long applicationId,
            @RequestParam("eventId") Long eventId) {
        var orderDetail = service.get(SecurityFrameworkUtils.getLoginUserId(), applicationId);
        var event = gxpAuditQueryService.get(eventId);
        if (event == null) {
            return CommonResult.success(null);
        }
        var relations = gxpAuditQueryService.listRelations(eventId);
        if (!MesGxpAuditControllerSupport.containsRelation(
                relations, "ACTIVE_ORDER", orderDetail.detail().getActiveOrderId())) {
            return CommonResult.success(null);
        }
        return CommonResult.success(MesGxpAuditControllerSupport.toResponse(event, relations));
    }

    public record Result(MesTeamLeaderActiveOrderDetailRespVO detail,
                         List<MesKingdeeProductionMaterialListRespVO> productionMaterialLists) {}
}
