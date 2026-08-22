package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseCheckItemPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseCheckItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleasePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleasePrecheckReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseWithdrawReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseService;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFinalizationCommand;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "管理后台 - eDHR放行管理")
@RestController
@RequestMapping("/mes/pro/edhr-release")
@Validated
public class MesProEdhrReleaseController {

    @Resource
    private MesProEdhrReleaseService releaseService;

    @GetMapping("/page")
    @Operation(summary = "分页查询 eDHR 放行候选")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:query')")
    public CommonResult<PageResult<MesProEdhrReleaseRespVO>> getPage(@Valid MesProEdhrReleasePageReqVO reqVO) {
        return success(releaseService.getPage(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得 eDHR 放行预检事务")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:query')")
    public CommonResult<MesProEdhrReleaseRespVO> get(@RequestParam("id") Long id) {
        return success(releaseService.get(id));
    }

    @PostMapping("/precheck")
    @Operation(summary = "执行 eDHR 放行前检查")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:precheck')")
    public CommonResult<MesProEdhrReleaseRespVO> precheck(@Valid @RequestBody MesProEdhrReleasePrecheckReqVO reqVO) {
        return success(releaseService.precheck(reqVO));
    }

    @PostMapping("/submit")
    @Operation(summary = "负责人电子签名放行 eDHR 批次")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:submit')")
    public CommonResult<MesProEdhrReleaseRespVO> submit(@Valid @RequestBody MesProEdhrReleaseSubmitReqVO reqVO) {
        return success(releaseService.submit(reqVO));
    }

    @PostMapping("/approve")
    @Operation(summary = "批准 eDHR 放行")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:approve')")
    public CommonResult<MesProEdhrReleaseRespVO> approve(@Valid @RequestBody MesProEdhrReleaseApproveReqVO reqVO) {
        return success(releaseService.approve(reqVO));
    }

    @PostMapping("/finalize")
    @Operation(summary = "统一完成 eDHR 放行最终化")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:approve')")
    public CommonResult<MesProEdhrReleaseRespVO> finalizeRelease(
            @Valid @RequestBody MesReleaseFinalizationCommand command) {
        return success(releaseService.finalizeRelease(command));
    }

    @PostMapping("/reject")
    @Operation(summary = "驳回 eDHR 放行")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:reject')")
    public CommonResult<MesProEdhrReleaseRespVO> reject(@Valid @RequestBody MesProEdhrReleaseRejectReqVO reqVO) {
        return success(releaseService.reject(reqVO));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "撤回 eDHR 放行")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:withdraw')")
    public CommonResult<MesProEdhrReleaseRespVO> withdraw(@Valid @RequestBody MesProEdhrReleaseWithdrawReqVO reqVO) {
        return success(releaseService.withdraw(reqVO));
    }

    @GetMapping("/check-item/page")
    @Operation(summary = "分页查询 eDHR 放行前检查项")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:query')")
    public CommonResult<PageResult<MesProEdhrReleaseCheckItemRespVO>> getCheckItemPage(
            @Valid MesProEdhrReleaseCheckItemPageReqVO reqVO) {
        return success(releaseService.getCheckItemPage(reqVO));
    }

    @GetMapping("/event/page")
    @Operation(summary = "分页查询 eDHR 放行事务事件")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:event-query')")
    public CommonResult<PageResult<MesProEdhrReleaseEventRespVO>> getEventPage(
            @Valid MesProEdhrReleaseEventPageReqVO reqVO) {
        return success(releaseService.getEventPage(reqVO));
    }
}
