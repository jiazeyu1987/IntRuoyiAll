package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeEffectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeImpactPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeImpactRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeRecalculateImpactReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeSubmitReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrUnifiedChangeService;
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
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - eDHR统一变更")
@RestController
@RequestMapping("/mes/pro/edhr-change/unified")
@Validated
public class MesProEdhrUnifiedChangeController {

    @Resource
    private MesProEdhrUnifiedChangeService unifiedChangeService;

    @GetMapping("/page")
    @Operation(summary = "分页查询 eDHR 统一变更")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:unified-query')")
    public CommonResult<PageResult<MesProEdhrUnifiedChangeRespVO>> getPage(
            @Valid MesProEdhrUnifiedChangePageReqVO reqVO) {
        return success(unifiedChangeService.getPage(reqVO));
    }

    @GetMapping("/impact/page")
    @Operation(summary = "分页查询 eDHR 统一变更影响范围")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:impact-query')")
    public CommonResult<PageResult<MesProEdhrUnifiedChangeImpactRespVO>> getImpactPage(
            @Valid MesProEdhrUnifiedChangeImpactPageReqVO reqVO) {
        return success(unifiedChangeService.getImpactPage(reqVO));
    }

    @GetMapping("/event/page")
    @Operation(summary = "分页查询 eDHR 统一变更事件")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:event-query')")
    public CommonResult<PageResult<MesProEdhrUnifiedChangeEventRespVO>> getEventPage(
            @Valid MesProEdhrUnifiedChangeEventPageReqVO reqVO) {
        return success(unifiedChangeService.getEventPage(reqVO));
    }

    @PostMapping("/create")
    @Operation(summary = "创建 eDHR 统一变更")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:unified-create')")
    public CommonResult<MesProEdhrUnifiedChangeRespVO> create(
            @Valid @RequestBody MesProEdhrUnifiedChangeCreateReqVO reqVO) {
        return success(unifiedChangeService.create(reqVO));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交 eDHR 统一变更")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:unified-submit')")
    public CommonResult<MesProEdhrUnifiedChangeRespVO> submit(
            @Valid @RequestBody MesProEdhrUnifiedChangeSubmitReqVO reqVO) {
        return success(unifiedChangeService.submit(reqVO));
    }

    @PostMapping("/recalculate-impact")
    @Operation(summary = "复算 eDHR 统一变更影响范围")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:unified-submit')")
    public CommonResult<MesProEdhrUnifiedChangeRespVO> recalculateImpact(
            @Valid @RequestBody MesProEdhrUnifiedChangeRecalculateImpactReqVO reqVO) {
        return success(unifiedChangeService.recalculateImpact(reqVO));
    }

    @PostMapping("/approve")
    @Operation(summary = "审批 eDHR 统一变更")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:unified-approve')")
    public CommonResult<MesProEdhrUnifiedChangeRespVO> approve(
            @Valid @RequestBody MesProEdhrUnifiedChangeApproveReqVO reqVO) {
        return success(unifiedChangeService.approve(reqVO));
    }

    @PostMapping("/effect")
    @Operation(summary = "申请 eDHR 统一变更生效")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:unified-effect')")
    public CommonResult<MesProEdhrUnifiedChangeRespVO> requestEffect(
            @Valid @RequestBody MesProEdhrUnifiedChangeEffectReqVO reqVO) {
        return success(unifiedChangeService.requestEffect(reqVO));
    }
}
