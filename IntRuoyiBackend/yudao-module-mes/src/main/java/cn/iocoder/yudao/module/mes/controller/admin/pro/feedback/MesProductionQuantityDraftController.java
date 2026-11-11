package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProductionQuantityDraftDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesProductionQuantityDraftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mes/pro/production-quantity-draft")
public class MesProductionQuantityDraftController {
    private final MesProductionQuantityDraftService service;

    @GetMapping("/own")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<List<MesProductionQuantityDraftDO>> own() { return CommonResult.success(service.own()); }

    @GetMapping("/leader")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<List<MesProductionQuantityDraftDO>> leader() { return CommonResult.success(service.leader()); }

    @PostMapping("/review")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:review')")
    public CommonResult<Boolean> review(@Valid @RequestBody MesProductionQuantityDraftService.Review command) {
        service.review(command);
        return CommonResult.success(true);
    }
}
