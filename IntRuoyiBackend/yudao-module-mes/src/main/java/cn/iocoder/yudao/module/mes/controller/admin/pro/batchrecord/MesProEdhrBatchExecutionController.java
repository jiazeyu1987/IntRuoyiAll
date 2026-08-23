package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionCloseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionGoldenFingerBulkVoidReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionGoldenFingerBulkVoidRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionOpenOrCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionQualityRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionReexecuteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRouteOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionReviewTimelineRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionSpecialNodeAttachmentPrepareUploadReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionSpecialNodeAttachmentPrepareUploadRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionSpecialNodeAttachmentDeletePendingReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionSpecialNodeAttachmentSavePendingReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionSpecialNodeAttachmentVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionSpecialNodeCompleteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionSpecialNodeSkipReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionTaskOpenReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionTaskOpenRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionTaskPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceCaptureReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceReleaseDecisionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceabilityRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceSourcePrecheckRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceTxCReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchWorkbenchRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrLocalStateSampleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrLocalStateSampleRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceSourcePrecheckCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceTxCProducer;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchWorkbenchService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLocalStateSampleService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRehearsalReadinessCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRehearsalReadinessResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRehearsalReadinessService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrSpecialNodeAttachment;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrSpecialNodeAttachmentPrepareUploadCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrSpecialNodeAttachmentPrepareUploadResult;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseReportAttachmentPrepareCommand;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseReportAttachmentPrepareResult;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseReportNodeCompleteCommand;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseReportService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/pro/edhr-batch-execution")
@Validated
public class MesProEdhrBatchExecutionController {

    @Resource
    private MesProEdhrBatchExecutionService batchExecutionService;
    @Resource
    private MesProEdhrBatchTraceabilityService batchTraceabilityService;
    @Resource
    private MesProEdhrBatchTraceTxCProducer batchTraceTxCProducer;
    @Resource
    private MesProEdhrBatchWorkbenchService batchWorkbenchService;
    @Resource
    private MesProEdhrRehearsalReadinessService rehearsalReadinessService;
    @Resource
    private MesProEdhrLocalStateSampleService localStateSampleService;
    @Resource
    private MesProductionReleaseReportService productionReleaseReportService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:query')")
    public CommonResult<PageResult<EdhrBatchExecutionRespVO>> getPage(@Valid EdhrBatchExecutionPageReqVO reqVO) {
        return success(batchExecutionService.getPage(reqVO));
    }

    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:query')")
    public CommonResult<EdhrBatchExecutionRespVO> get(@RequestParam("id") Long id) {
        return success(batchExecutionService.get(id));
    }

    @GetMapping("/workbench")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:query')")
    public CommonResult<EdhrBatchWorkbenchRespVO> getWorkbench(@RequestParam("id") Long id) {
        return success(batchWorkbenchService.getWorkbench(id));
    }

    @GetMapping("/work-order-route-options")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-edhr-batch-execution:create', 'mes:pro-edhr-batch-execution:query')")
    public CommonResult<List<EdhrBatchExecutionRouteOptionRespVO>> listRouteOptionsByWorkOrder(
            @RequestParam("workOrderId") Long workOrderId) {
        return success(batchExecutionService.listRouteOptionsByWorkOrder(workOrderId));
    }

    @PostMapping("/open-or-create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:create')")
    public CommonResult<EdhrBatchExecutionRespVO> openOrCreate(@Valid @RequestBody EdhrBatchExecutionOpenOrCreateReqVO reqVO) {
        return success(batchExecutionService.openOrCreate(reqVO));
    }

    @PostMapping("/reexecute-rejected-batch")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:create')")
    public CommonResult<EdhrBatchExecutionRespVO> reexecuteRejectedBatch(
            @Valid @RequestBody EdhrBatchExecutionReexecuteReqVO reqVO) {
        return success(batchExecutionService.reexecuteRejectedBatch(reqVO));
    }

    @PostMapping("/golden-finger/bulk-void")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:golden-finger')")
    public CommonResult<EdhrBatchExecutionGoldenFingerBulkVoidRespVO> goldenFingerBulkVoid(
            @Valid @RequestBody EdhrBatchExecutionGoldenFingerBulkVoidReqVO reqVO) {
        return success(batchExecutionService.goldenFingerBulkVoid(reqVO));
    }

    @PostMapping("/local-state-sample")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:create')")
    public CommonResult<EdhrLocalStateSampleRespVO> createLocalStateSample(
            @Valid @RequestBody EdhrLocalStateSampleReqVO reqVO) {
        return success(localStateSampleService.createLocalStateSample(reqVO));
    }

    @PostMapping("/task/open")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:update')")
    public CommonResult<EdhrBatchExecutionTaskOpenRespVO> openTask(@Valid @RequestBody EdhrBatchExecutionTaskOpenReqVO reqVO) {
        return success(batchExecutionService.openTask(reqVO));
    }

    @GetMapping("/task/preview")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:query')")
    public CommonResult<EdhrBatchExecutionTaskPreviewRespVO> previewTask(
            @RequestParam("batchExecutionId") Long batchExecutionId,
            @RequestParam("taskId") Long taskId) {
        return success(batchExecutionService.previewTask(batchExecutionId, taskId));
    }

    @PostMapping("/task/special-node/skip")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:update')")
    public CommonResult<EdhrBatchExecutionRespVO> skipSpecialNode(
            @Valid @RequestBody EdhrBatchExecutionSpecialNodeSkipReqVO reqVO) {
        return success(batchExecutionService.skipSpecialNode(reqVO.getTaskId(), reqVO.getReason(), reqVO.getPassword(),
                toServiceAttachments(reqVO.getAttachments())));
    }

    @PostMapping("/task/special-node/complete")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:update')")
    public CommonResult<?> completeSpecialNode(
            @Valid @RequestBody EdhrBatchExecutionSpecialNodeCompleteReqVO reqVO) {
        if (reqVO.getExpectedVersion() != null || reqVO.getIdempotencyKey() != null) {
            return success(productionReleaseReportService.complete(
                    SecurityFrameworkUtils.getLoginUserId(),
                    new MesProductionReleaseReportNodeCompleteCommand()
                            .setBatchTaskId(reqVO.getTaskId())
                            .setExpectedVersion(reqVO.getExpectedVersion())
                            .setIdempotencyKey(reqVO.getIdempotencyKey())
                            .setSterilizationBatchNo(reqVO.getSterilizationBatchNo())
                            .setAttachments(toServiceAttachments(reqVO.getAttachments()))));
        }
        return success(batchExecutionService.completeSpecialNode(reqVO.getTaskId(), reqVO.getSterilizationBatchNo(),
                toServiceAttachments(reqVO.getAttachments())));
    }

    @PostMapping("/task/special-node/attachment/prepare-upload")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:update')")
    public CommonResult<?> prepareSpecialNodeAttachmentUpload(
            @Valid EdhrBatchExecutionSpecialNodeAttachmentPrepareUploadReqVO reqVO,
            @RequestPart("file") MultipartFile file) throws IOException {
        if (reqVO.getExpectedVersion() != null || reqVO.getIdempotencyKey() != null) {
            MesProductionReleaseReportAttachmentPrepareResult result =
                    productionReleaseReportService.prepareAttachment(
                            SecurityFrameworkUtils.getLoginUserId(),
                            new MesProductionReleaseReportAttachmentPrepareCommand()
                                    .setBatchTaskId(reqVO.getTaskId())
                                    .setExpectedVersion(reqVO.getExpectedVersion())
                                    .setIdempotencyKey(reqVO.getIdempotencyKey())
                                    .setFileName(file.getOriginalFilename())
                                    .setContentType(file.getContentType())
                                    .setContent(file.getBytes()));
            return success(result);
        }
        MesProEdhrSpecialNodeAttachmentPrepareUploadResult result =
                batchExecutionService.prepareSpecialNodeAttachmentUpload(
                        new MesProEdhrSpecialNodeAttachmentPrepareUploadCommand()
                                .setTaskId(reqVO.getTaskId())
                                .setFileName(file.getOriginalFilename())
                                .setContentType(file.getContentType())
                                .setContent(file.getBytes()));
        return success(toResp(result));
    }

    @PostMapping("/task/special-node/attachment/delete-pending")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:update')")
    public CommonResult<Boolean> deletePendingSpecialNodeAttachment(
            @Valid @RequestBody EdhrBatchExecutionSpecialNodeAttachmentDeletePendingReqVO reqVO) {
        batchExecutionService.deletePendingSpecialNodeAttachment(reqVO.getTaskId(),
                toServiceAttachment(reqVO.getAttachment()), reqVO.getReason());
        return success(true);
    }

    @PostMapping("/task/special-node/attachment/save-pending")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:update')")
    public CommonResult<EdhrBatchExecutionRespVO> savePendingSpecialNodeAttachments(
            @Valid @RequestBody EdhrBatchExecutionSpecialNodeAttachmentSavePendingReqVO reqVO) {
        return success(batchExecutionService.savePendingSpecialNodeAttachments(reqVO.getBatchExecutionId(), reqVO.getReason()));
    }

    @PostMapping("/sync-status")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:update')")
    public CommonResult<EdhrBatchExecutionRespVO> syncStatus(@RequestParam("id") Long id) {
        return success(batchExecutionService.syncStatus(id));
    }

    @GetMapping("/review-timeline")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:query')")
    public CommonResult<EdhrBatchExecutionReviewTimelineRespVO> reviewTimeline(@RequestParam("id") Long id) {
        return success(batchExecutionService.getReviewTimeline(id));
    }

    @GetMapping("/traceability")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:query')")
    public CommonResult<MesProEdhrBatchTraceabilityRespVO> traceability(
            @RequestParam("batchExecutionId") Long batchExecutionId) {
        return success(batchTraceabilityService.getTraceability(batchExecutionId));
    }

    @GetMapping("/traceability/manifest")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:query')")
    public CommonResult<MesProEdhrBatchTraceabilityRespVO> traceabilityManifest(
            @RequestParam("batchExecutionId") Long batchExecutionId) {
        return success(batchTraceabilityService.getTraceability(batchExecutionId));
    }

    @GetMapping("/traceability/source-precheck")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:query')")
    public CommonResult<MesProEdhrBatchTraceSourcePrecheckRespVO> sourcePrecheck(
            @RequestParam("batchExecutionId") Long batchExecutionId,
            @RequestParam("originLinkId") Long originLinkId,
            @RequestParam(value = "expectedTraceLinkHash", required = false) String expectedTraceLinkHash,
            @RequestParam(value = "expectedSourceSnapshotHash", required = false) String expectedSourceSnapshotHash,
            @RequestParam(value = "expectedSourceVersion", required = false) Integer expectedSourceVersion) {
        return success(batchTraceabilityService.resolveSourcePrecheck(
                new MesProEdhrBatchTraceSourcePrecheckCommand()
                        .setBatchExecutionId(batchExecutionId)
                        .setOriginLinkId(originLinkId)
                        .setExpectedTraceLinkHash(expectedTraceLinkHash)
                        .setExpectedSourceSnapshotHash(expectedSourceSnapshotHash)
                        .setExpectedSourceVersion(expectedSourceVersion)));
    }

    @GetMapping("/traceability/list")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:query')")
    public CommonResult<List<MesProEdhrBatchTraceabilityRespVO>> traceabilityList(
            @RequestParam(value = "activeOrderId", required = false) Long activeOrderId,
            @RequestParam(value = "workOrderId", required = false) Long workOrderId,
            @RequestParam(value = "pickListId", required = false) Long pickListId,
            @RequestParam(value = "releaseApplicationId", required = false) Long releaseApplicationId,
            @RequestParam(value = "entryType", required = false) String entryType) {
        return success(batchTraceabilityService.listTraceability(activeOrderId, workOrderId, pickListId,
                releaseApplicationId, entryType));
    }

    @PostMapping("/traceability/capture")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:trace-capture')")
    public CommonResult<MesProEdhrBatchTraceabilityRespVO> captureTraceability(
            @Valid @RequestBody MesProEdhrBatchTraceCaptureReqVO reqVO) {
        reqVO.setCapturedBy(SecurityFrameworkUtils.getLoginUserId());
        return success(batchTraceabilityService.capture(reqVO));
    }

    @PostMapping("/traceability/tx-c")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:trace-capture')")
    public CommonResult<cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceTxCResult> produceTxC(
            @Valid @RequestBody MesProEdhrBatchTraceTxCReqVO reqVO) {
        reqVO.setCapturedBy(SecurityFrameworkUtils.getLoginUserId());
        return success(batchTraceTxCProducer.produce(reqVO));
    }

    @PostMapping("/traceability/release-decision")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:trace-capture')")
    public CommonResult<MesProEdhrBatchTraceabilityRespVO> appendReleaseDecisionTrace(
            @Valid @RequestBody MesProEdhrBatchTraceReleaseDecisionReqVO reqVO) {
        reqVO.setCapturedBy(SecurityFrameworkUtils.getLoginUserId());
        return success(batchTraceabilityService.appendReleaseDecision(reqVO));
    }

    @GetMapping("/rehearsal-readiness")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:query')")
    public CommonResult<MesProEdhrRehearsalReadinessResult> rehearsalReadiness(
            @RequestParam("routeId") Long routeId,
            @RequestParam("executorUserId") Long executorUserId,
            @RequestParam("approverUserId") Long approverUserId,
            @RequestParam("archiverUserId") Long archiverUserId) {
        return success(rehearsalReadinessService.check(new MesProEdhrRehearsalReadinessCommand()
                .setRouteId(routeId)
                .setExecutorUserId(executorUserId)
                .setApproverUserId(approverUserId)
                .setArchiverUserId(archiverUserId)));
    }

    @PostMapping("/close")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:close')")
    public CommonResult<EdhrBatchExecutionRespVO> close(@Valid @RequestBody EdhrBatchExecutionCloseReqVO reqVO) {
        return success(batchExecutionService.close(reqVO));
    }

    @PostMapping("/quality-reject")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:quality-reject')")
    public CommonResult<EdhrBatchExecutionRespVO> qualityReject(@Valid @RequestBody EdhrBatchExecutionQualityRejectReqVO reqVO) {
        return success(batchExecutionService.qualityReject(reqVO));
    }

    private List<MesProEdhrSpecialNodeAttachment> toServiceAttachments(
            List<EdhrBatchExecutionSpecialNodeAttachmentVO> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        return attachments.stream()
                .map(this::toServiceAttachment)
                .toList();
    }

    private MesProEdhrSpecialNodeAttachment toServiceAttachment(
            EdhrBatchExecutionSpecialNodeAttachmentVO attachment) {
        return new MesProEdhrSpecialNodeAttachment()
                .setUploadToken(attachment.getUploadToken())
                .setFileId(attachment.getFileId())
                .setFileUrl(attachment.getFileUrl())
                .setStorageConfigId(attachment.getStorageConfigId())
                .setStoragePath(attachment.getStoragePath())
                .setFileName(attachment.getFileName())
                .setContentType(attachment.getContentType())
                .setFileSize(attachment.getFileSize())
                .setSha256(attachment.getSha256())
                .setStorageRetentionJson(attachment.getStorageRetentionJson())
                .setStorageRetentionHash(attachment.getStorageRetentionHash());
    }

    private EdhrBatchExecutionSpecialNodeAttachmentPrepareUploadRespVO toResp(
            MesProEdhrSpecialNodeAttachmentPrepareUploadResult result) {
        return new EdhrBatchExecutionSpecialNodeAttachmentPrepareUploadRespVO()
                .setUploadToken(result.getUploadToken())
                .setFileId(result.getFileId())
                .setFileUrl(result.getFileUrl())
                .setStorageConfigId(result.getStorageConfigId())
                .setStoragePath(result.getStoragePath())
                .setFileName(result.getFileName())
                .setContentType(result.getContentType())
                .setFileSize(result.getFileSize())
                .setSha256(result.getSha256())
                .setStorageRetentionJson(result.getStorageRetentionJson())
                .setStorageRetentionHash(result.getStorageRetentionHash());
    }

}
