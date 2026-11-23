package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor.ATTRIBUTE_HANDLER_METHOD;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.*;

@Tag(name = "管理后台 - eDHR 不合格评审")
@RestController
@RequestMapping("/mes/pro/edhr-nonconformance-review")
@Validated
public class MesProEdhrNonconformanceReviewController {

    @Resource
    private MesProEdhrNonconformanceReviewService nonconformanceReviewService;
    @Resource
    private GlobalExceptionHandler globalExceptionHandler;

    // 只公开经过核对的固定业务提示，不回显异常携带的密码、材料或请求值。
    private static final Map<Integer, String> SAFE_DISPOSITION_MESSAGES = Map.ofEntries(
            Map.entry(PRO_EDHR_NONCONFORMANCE_REVIEW_NOT_EXISTS.getCode(), PRO_EDHR_NONCONFORMANCE_REVIEW_NOT_EXISTS.getMsg()),
            Map.entry(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID.getCode(), "当前批次或评审单状态不允许处置，请刷新后重试"),
            Map.entry(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID.getCode(), "评审来源或关联数据无效，请核对后重试"),
            Map.entry(PRO_EDHR_NONCONFORMANCE_REVIEW_DISPOSITION_INVALID.getCode(), PRO_EDHR_NONCONFORMANCE_REVIEW_DISPOSITION_INVALID.getMsg()),
            Map.entry(PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED.getCode(), "请填写处置结论、评审意见和电子签名密码"),
            Map.entry(PRO_EDHR_NONCONFORMANCE_REVIEW_WORK_ORDER_STATE_REQUIRED.getCode(), PRO_EDHR_NONCONFORMANCE_REVIEW_WORK_ORDER_STATE_REQUIRED.getMsg()));

    @ExceptionHandler(ServiceException.class)
    public CommonResult<?> handleServiceException(ServiceException exception, HttpServletRequest request) {
        CommonResult<?> result = globalExceptionHandler.serviceExceptionHandler(exception);
        if (request.getAttribute(ATTRIBUTE_HANDLER_METHOD) instanceof HandlerMethod handler) {
            ApiAccessLog privacy = handler.getMethodAnnotation(ApiAccessLog.class);
            String safeMessage = SAFE_DISPOSITION_MESSAGES.get(exception.getCode());
            if (privacy != null && !privacy.requestEnable() && safeMessage != null) {
                result.setMsg(safeMessage);
            }
        }
        return result;
    }

    @Resource
    private cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesQaReviewerCandidateService reviewerCandidates;

    @GetMapping("/reviewer-candidates")
    @Operation(summary = "查询本租户启用且具备QA处置权限的评审责任人")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-edhr-nonconformance-review:create', 'mes:pro-edhr-nonconformance-review:deviation-create', 'mes:pro-production-release:pqc-reject', 'mes:pro-edhr-release:approve', 'mes:pro-edhr-release:reject')")
    public CommonResult<List<cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesQaReviewerCandidateRespVO>> getReviewerCandidates() {
        return success(reviewerCandidates.list());
    }

    @PostMapping("/assign-reviewer")
    @Operation(summary = "为尚未分派的待评审单明确指派QA责任人")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-nonconformance-review:create')")
    public CommonResult<MesProEdhrNonconformanceReviewRespVO> assignReviewer(
            @Valid @RequestBody cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewerAssignReqVO reqVO) {
        return success(nonconformanceReviewService.assignReviewer(reqVO.getId(), reqVO.getQaReviewerUserId()));
    }

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
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-production-release:pqc-reject', 'mes:pro-edhr-release:approve')")
    public CommonResult<MesProEdhrNonconformanceReviewRespVO> rejectBatch(
            @Valid @RequestBody MesProEdhrBatchExecutionRejectReqVO reqVO) {
        return success(nonconformanceReviewService.rejectBatch(reqVO));
    }

    @PostMapping("/dispose")
    @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable = false)
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
    @PreAuthorize("isAuthenticated()")
    public CommonResult<PageResult<MesProEdhrNonconformanceReviewRespVO>> getPage(
            @Valid MesProEdhrNonconformanceReviewPageReqVO reqVO) {
        return success(nonconformanceReviewService.getPage(reqVO));
    }

    @GetMapping("/pending-page")
    @Operation(summary = "分页查询 QA 冻结批次不合格评审列表")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<PageResult<MesProEdhrNonconformanceReviewRespVO>> getPendingPage(
            @Valid MesProEdhrNonconformanceReviewPageReqVO reqVO) {
        return success(nonconformanceReviewService.getPendingPage(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得 eDHR 不合格评审单")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<MesProEdhrNonconformanceReviewRespVO> get(@RequestParam("id") Long id) {
        return success(nonconformanceReviewService.get(id));
    }

    @GetMapping("/summary")
    @Operation(summary = "获得不合格评审只读摘要及原周期QA任务")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewSummaryRespVO> getSummary(
            @RequestParam("id") Long id) {
        return success(nonconformanceReviewService.getSummary(id));
    }

    @GetMapping("/active-order-detail")
    @Operation(summary = "获得不合格评审关联的活跃订单详情")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<MesTeamLeaderActiveOrderDetailRespVO> getActiveOrderDetail(
            @RequestParam("reviewId") Long reviewId) {
        return success(MesProcessPoolTeamLeaderController.toActiveOrderDetailRespVO(
                nonconformanceReviewService.getActiveOrderDetail(reviewId)));
    }

    @GetMapping("/batch-list")
    @Operation(summary = "查询批次关联 eDHR 不合格评审单")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<List<MesProEdhrNonconformanceReviewRespVO>> getBatchList(
            @RequestParam("batchExecutionId") Long batchExecutionId) {
        return success(nonconformanceReviewService.listByBatchExecutionId(batchExecutionId));
    }
}
