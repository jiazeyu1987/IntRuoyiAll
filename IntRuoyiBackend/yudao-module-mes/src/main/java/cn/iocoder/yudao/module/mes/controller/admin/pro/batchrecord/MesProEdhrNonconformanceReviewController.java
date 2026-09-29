package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.MesProcessPoolTeamLeaderController;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchExecutionRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationNcrCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewDisposeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewActiveOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewMaterialUploadRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @GetMapping("/active-order-list")
    @Operation(summary = "查询可创建不合格评审的活跃订单")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-nonconformance-review:create')")
    public CommonResult<List<MesProEdhrNonconformanceReviewActiveOrderRespVO>> getActiveOrderList() {
        return success(nonconformanceReviewService.listActiveOrderCandidates());
    }

    @PostMapping("/create-from-critical-deviations")
    @Operation(summary = "QA从关键偏差发起 eDHR 不合格评审并冻结批次")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-nonconformance-review:deviation-create')")
    public CommonResult<MesProEdhrNonconformanceReviewRespVO> createFromCriticalDeviations(
            @Valid @RequestBody MesProEdhrDeviationNcrCreateReqVO reqVO) {
        return success(nonconformanceReviewService.createCriticalDeviationReview(
                cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId(), reqVO));
    }

    @PostMapping("/reject-batch")
    @Operation(summary = "上市放行负责人驳回批次并发起不合格评审")
    @PreAuthorize("@ss.hasPermission('mes:pro-production-release:pqc-reject')")
    public CommonResult<MesProEdhrNonconformanceReviewRespVO> rejectBatch(
            @Valid @RequestBody MesProEdhrBatchExecutionRejectReqVO reqVO) {
        return success(nonconformanceReviewService.rejectBatch(reqVO));
    }

    @PostMapping("/dispose")
    @Operation(summary = "QA 处置 eDHR 不合格评审单")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-nonconformance-review:dispose')")
    public CommonResult<MesProEdhrNonconformanceReviewRespVO> dispose(
            @Valid @RequestBody MesProEdhrNonconformanceReviewDisposeReqVO reqVO) {
        return success(nonconformanceReviewService.dispose(reqVO));
    }

    @PostMapping("/{reviewId}/materials/upload")
    @Operation(summary = "上传 eDHR 不合格评审材料")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-nonconformance-review:dispose')")
    public CommonResult<MesProEdhrNonconformanceReviewMaterialUploadRespVO> uploadMaterial(
            @PathVariable("reviewId") Long reviewId, @RequestParam("file") MultipartFile file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("评审材料文件不能为空");
        }
        return success(nonconformanceReviewService.uploadMaterial(reviewId, file.getOriginalFilename(),
                file.getContentType(), file.getBytes()));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询 eDHR 不合格评审列表")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-nonconformance-review:query')")
    public CommonResult<PageResult<MesProEdhrNonconformanceReviewRespVO>> getPage(
            @Valid MesProEdhrNonconformanceReviewPageReqVO reqVO) {
        return success(nonconformanceReviewService.getPage(reqVO));
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

    @GetMapping("/active-order-detail")
    @Operation(summary = "获得不合格评审关联的活跃订单详情")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-nonconformance-review:query')")
    public CommonResult<MesTeamLeaderActiveOrderDetailRespVO> getActiveOrderDetail(
            @RequestParam("reviewId") Long reviewId) {
        return success(MesProcessPoolTeamLeaderController.toActiveOrderDetailRespVO(
                nonconformanceReviewService.getActiveOrderDetail(reviewId)));
    }

    @GetMapping("/batch-list")
    @Operation(summary = "查询批次关联 eDHR 不合格评审单")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-nonconformance-review:query')")
    public CommonResult<List<MesProEdhrNonconformanceReviewRespVO>> getBatchList(
            @RequestParam("batchExecutionId") Long batchExecutionId) {
        return success(nonconformanceReviewService.listByBatchExecutionId(batchExecutionId));
    }
}
