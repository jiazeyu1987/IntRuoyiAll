package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApprovalActionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApprovalPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApprovalRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFormReviewSignReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFormReviewSignRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSaveDraftReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSignaturePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSignatureRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionTrackingEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionTrackingPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionTrackingRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 批记录执行")
@RestController
@RequestMapping("/mes/pro/batch-record-execution")
@Validated
public class MesProBatchRecordExecutionController {

    @Resource
    private MesProBatchRecordExecutionService executionService;

    @GetMapping("/entry-context")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:create')")
    public CommonResult<MesProBatchRecordExecutionEntryContextRespVO> getEntryContext(
            @Valid MesProBatchRecordExecutionEntryContextReqVO reqVO) {
        return success(executionService.getEntryContext(reqVO));
    }

    @PostMapping("/open-or-create-by-context")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:create')")
    public CommonResult<MesProBatchRecordExecutionOpenOrCreateByContextRespVO> openOrCreateByContext(
            @Valid @RequestBody MesProBatchRecordExecutionOpenOrCreateByContextReqVO reqVO) {
        return success(executionService.openOrCreateByContext(reqVO));
    }

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:query')")
    public CommonResult<PageResult<MesProBatchRecordExecutionRespVO>> getExecutionPage(
            @Valid MesProBatchRecordExecutionPageReqVO pageReqVO) {
        return success(executionService.getBatchRecordExecutionPage(pageReqVO));
    }

    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:query')")
    public CommonResult<MesProBatchRecordExecutionRespVO> getExecution(@RequestParam("id") Long id,
                                                                       @RequestParam(value = "workTaskId", required = false)
                                                                       Long workTaskId) {
        return success(executionService.getBatchRecordExecution(id, workTaskId));
    }

    @PutMapping("/save-draft")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:update')")
    public CommonResult<Boolean> saveDraft(@Valid @RequestBody MesProBatchRecordExecutionSaveDraftReqVO reqVO) {
        executionService.saveBatchRecordExecutionDraft(reqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-batch-record-execution:update', "
            + "'mes:pro-batch-record-execution:golden-finger')")
    public CommonResult<Boolean> submit(@Valid @RequestBody MesProBatchRecordExecutionSubmitReqVO reqVO) {
        executionService.submitBatchRecordExecution(reqVO);
        return success(true);
    }

    @PutMapping("/cosign")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:update')")
    public CommonResult<MesProBatchRecordExecutionFormReviewSignRespVO> cosign(
            @Valid @RequestBody MesProBatchRecordExecutionFormReviewSignReqVO reqVO) {
        return success(executionService.cosignBatchRecordExecution(reqVO));
    }

    @GetMapping("/approval-pending-page")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:approve')")
    public CommonResult<PageResult<MesProBatchRecordExecutionApprovalRespVO>> getApprovalPendingPage(
            @Valid MesProBatchRecordExecutionApprovalPageReqVO pageReqVO) {
        return success(executionService.getApprovalPendingPage(pageReqVO));
    }

    @GetMapping("/approval-done-page")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:approve')")
    public CommonResult<PageResult<MesProBatchRecordExecutionApprovalRespVO>> getApprovalDonePage(
            @Valid MesProBatchRecordExecutionApprovalPageReqVO pageReqVO) {
        return success(executionService.getApprovalDonePage(pageReqVO));
    }

    @GetMapping("/approval-detail")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:approve')")
    public CommonResult<MesProBatchRecordExecutionApprovalRespVO> getApprovalDetail(
            @RequestParam("id") Long id,
            @RequestParam(value = "bpmTaskId", required = false) String bpmTaskId,
            @RequestParam(value = "workTaskId", required = false) Long workTaskId) {
        return success(executionService.getApprovalDetail(id, bpmTaskId, workTaskId));
    }

    public CommonResult<MesProBatchRecordExecutionApprovalRespVO> getApprovalDetail(Long id, String bpmTaskId) {
        return getApprovalDetail(id, bpmTaskId, null);
    }

    @PutMapping("/approve")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:approve')")
    public CommonResult<MesProBatchRecordExecutionApprovalActionRespVO> approve(@Valid @RequestBody MesProBatchRecordExecutionApproveReqVO reqVO) {
        return success(executionService.approveBatchRecordExecution(reqVO));
    }

    @PutMapping("/reject")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:approve')")
    public CommonResult<MesProBatchRecordExecutionApprovalActionRespVO> reject(@Valid @RequestBody MesProBatchRecordExecutionRejectReqVO reqVO) {
        return success(executionService.rejectBatchRecordExecution(reqVO));
    }

    @GetMapping("/tracking-page")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:track')")
    public CommonResult<PageResult<MesProBatchRecordExecutionTrackingRespVO>> getTrackingPage(
            @Valid MesProBatchRecordExecutionTrackingPageReqVO pageReqVO) {
        return success(executionService.getTrackingPage(pageReqVO));
    }

    @GetMapping("/tracking-timeline")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:track')")
    public CommonResult<List<MesProBatchRecordExecutionTrackingEventRespVO>> getTrackingTimeline(
            @RequestParam("executionId") Long executionId) {
        return success(executionService.getTrackingTimeline(executionId));
    }

    @GetMapping("/signature-page")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:signature-query')")
    public CommonResult<PageResult<MesProBatchRecordExecutionSignatureRespVO>> getSignaturePage(
            @Valid MesProBatchRecordExecutionSignaturePageReqVO pageReqVO) {
        return success(executionService.getSignaturePage(pageReqVO));
    }
}
