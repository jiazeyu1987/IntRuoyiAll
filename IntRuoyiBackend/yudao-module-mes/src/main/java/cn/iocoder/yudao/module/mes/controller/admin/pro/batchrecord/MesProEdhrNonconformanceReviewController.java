package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewDisposeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
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

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - eDHR 不合格评审")
@RestController
@RequestMapping("/mes/pro/edhr-nonconformance-review")
@Validated
public class MesProEdhrNonconformanceReviewController {

    @Resource
    private MesProEdhrNonconformanceReviewService nonconformanceReviewService;

    @PostMapping("/create")
    @Operation(summary = "创建 eDHR 不合格评审单并冻结批次")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-nonconformance-review:create')")
    public CommonResult<MesProEdhrNonconformanceReviewRespVO> create(
            @Valid @RequestBody MesProEdhrNonconformanceReviewCreateReqVO reqVO) {
        return success(nonconformanceReviewService.create(reqVO));
    }

    @PostMapping("/dispose")
    @Operation(summary = "QA 处置 eDHR 不合格评审单")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-nonconformance-review:dispose')")
    public CommonResult<MesProEdhrNonconformanceReviewRespVO> dispose(
            @Valid @RequestBody MesProEdhrNonconformanceReviewDisposeReqVO reqVO) {
        return success(nonconformanceReviewService.dispose(reqVO));
    }

    @GetMapping("/pending-page")
    @Operation(summary = "分页查询 QA 冻结批次不合格评审列表")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-nonconformance-review:query')")
    public CommonResult<PageResult<MesProEdhrNonconformanceReviewRespVO>> getPendingPage(
            @Valid MesProEdhrNonconformanceReviewPageReqVO reqVO) {
        return success(nonconformanceReviewService.getPendingPage(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得 eDHR 不合格评审单")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-nonconformance-review:query')")
    public CommonResult<MesProEdhrNonconformanceReviewRespVO> get(@RequestParam("id") Long id) {
        return success(nonconformanceReviewService.get(id));
    }

    @GetMapping("/batch-list")
    @Operation(summary = "查询批次关联 eDHR 不合格评审单")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-nonconformance-review:query')")
    public CommonResult<List<MesProEdhrNonconformanceReviewRespVO>> getBatchList(
            @RequestParam("batchExecutionId") Long batchExecutionId) {
        return success(nonconformanceReviewService.listByBatchExecutionId(batchExecutionId));
    }
}
