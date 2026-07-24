package cn.iocoder.yudao.module.srm.controller.admin.nonbidding;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.nonbidding.vo.*;
import cn.iocoder.yudao.module.srm.service.nonbidding.SrmNonBiddingProcurementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - SRM 非招标项目")
@RestController
@RequestMapping("/srm/non-bidding-project")
@Validated
public class SrmNonBiddingProjectController {

    @Resource
    private SrmNonBiddingProcurementService nonBiddingProcurementService;

    @GetMapping("/page")
    @Operation(summary = "获得非招标项目分页")
    @PreAuthorize("@ss.hasPermission('srm:non-bidding-project:query')")
    public CommonResult<PageResult<SrmNonBiddingProjectRespVO>> getProjectPage(@Valid SrmNonBiddingProjectPageReqVO pageReqVO) {
        return success(nonBiddingProcurementService.getProjectPage(pageReqVO));
    }

    @GetMapping("/contractable-page")
    @Operation(summary = "获得可建合同的非招标项目分页")
    @PreAuthorize("@ss.hasPermission('srm:non-bidding-project:contract')")
    public CommonResult<PageResult<SrmNonBiddingProjectRespVO>> getContractableProjectPage(@Valid SrmNonBiddingProjectPageReqVO pageReqVO) {
        return success(nonBiddingProcurementService.getContractableProjectPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得非招标项目详情")
    @Parameter(name = "id", description = "非招标项目编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('srm:non-bidding-project:query')")
    public CommonResult<SrmNonBiddingProjectRespVO> getProject(@RequestParam("id") Long id) {
        return success(nonBiddingProcurementService.getProject(id));
    }

    @PostMapping("/publish")
    @Operation(summary = "发布非招标项目")
    @PreAuthorize("@ss.hasPermission('srm:non-bidding-project:publish')")
    public CommonResult<SrmNonBiddingProjectRespVO> publishProject(@Valid @RequestBody SrmNonBiddingPublishReqVO publishReqVO) {
        return success(nonBiddingProcurementService.publishProject(publishReqVO));
    }

    @PostMapping("/quote")
    @Operation(summary = "提交非招标报价")
    @PreAuthorize("@ss.hasPermission('srm:non-bidding-project:quote')")
    public CommonResult<SrmNonBiddingProjectRespVO> submitQuote(@Valid @RequestBody SrmNonBiddingQuoteReqVO quoteReqVO) {
        return success(nonBiddingProcurementService.submitQuote(quoteReqVO));
    }

    @PostMapping("/deal")
    @Operation(summary = "确认非招标成交")
    @PreAuthorize("@ss.hasPermission('srm:non-bidding-project:deal')")
    public CommonResult<SrmNonBiddingProjectRespVO> confirmDeal(@Valid @RequestBody SrmNonBiddingDealReqVO dealReqVO) {
        return success(nonBiddingProcurementService.confirmDeal(dealReqVO));
    }
}
