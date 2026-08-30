package cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo.MesPqcProductionReleaseApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo.MesPqcProductionReleaseDecisionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo.MesPqcProductionReleasePageItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo.MesPqcProductionReleasePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo.MesPqcProductionReleaseRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo.MesProductionReleaseReportUploadTaskRespVO;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcProductionReleaseApproveCommand;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcProductionReleaseDecisionResult;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcProductionReleasePageItem;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcProductionReleasePageQuery;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcProductionReleaseRejectCommand;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcProductionReleaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 生产放行")
@RestController
@RequestMapping("/mes/pro/production-release")
@Validated
public class MesProductionReleaseController {

    private final MesPqcProductionReleaseService pqcProductionReleaseService;

    public MesProductionReleaseController(MesPqcProductionReleaseService pqcProductionReleaseService) {
        this.pqcProductionReleaseService = pqcProductionReleaseService;
    }

    @PostMapping("/pqc/approve")
    @Operation(summary = "PQC 批准生产放行申请")
    @PreAuthorize("@ss.hasPermission('mes:pro-production-release:pqc-approve')")
    public CommonResult<MesPqcProductionReleaseDecisionRespVO> approve(
            @Valid @RequestBody MesPqcProductionReleaseApproveReqVO reqVO) {
        return success(toResp(pqcProductionReleaseService.approve(SecurityFrameworkUtils.getLoginUserId(),
                new MesPqcProductionReleaseApproveCommand()
                        .setApplicationId(reqVO.getApplicationId())
                        .setPqcReleaseWorkTaskId(reqVO.getPqcReleaseWorkTaskId())
                        .setExpectedVersion(reqVO.getExpectedVersion())
                        .setIdempotencyKey(reqVO.getIdempotencyKey())
                        .setApprovalOpinion(reqVO.getApprovalOpinion())
                        .setSignaturePassword(reqVO.getSignaturePassword())
                        .setEntryType(reqVO.getEntryType())
                        .setEntryBusinessId(reqVO.getEntryBusinessId())
                        .setSourceCredentialType(reqVO.getSourceCredentialType())
                        .setSourceCredentialId(reqVO.getSourceCredentialId())
                        .setSourceRelationId(reqVO.getSourceRelationId())
                        .setSourceContextHash(reqVO.getSourceContextHash())
                        .setTenantId(reqVO.getTenantId())
                        .setActiveOrderId(reqVO.getActiveOrderId())
                        .setWorkOrderCode(reqVO.getWorkOrderCode())
                        .setPickListBindingId(reqVO.getPickListBindingId())
                        .setPickListId(reqVO.getPickListId())
                        .setBindingVersion(reqVO.getBindingVersion())
                        .setBatchPickListRelationId(reqVO.getBatchPickListRelationId())
                        .setSourceSnapshotHash(reqVO.getSourceSnapshotHash())
                        .setExpectedSourceVersion(reqVO.getExpectedSourceVersion())
                        .setPayloadHash(reqVO.getPayloadHash())
                        .setCompletionTransactionId(reqVO.getCompletionTransactionId())
                        .setExpectedActiveOrderVersion(reqVO.getExpectedActiveOrderVersion())
                        .setCompletionVersion(reqVO.getCompletionVersion())
                        .setSourceVersion(reqVO.getSourceVersion())
                        .setSourceBundleHash(reqVO.getSourceBundleHash())
                        .setCompletionBackfillReceiptId(reqVO.getCompletionBackfillReceiptId())
                        .setCompletionBackfillReceiptHash(reqVO.getCompletionBackfillReceiptHash())
                        .setPickListHeaderSnapshotHash(reqVO.getPickListHeaderSnapshotHash())
                        .setPickListLineSnapshotHash(reqVO.getPickListLineSnapshotHash())
                        .setSourceEvidence(reqVO.getSourceEvidence())
                        .setCompletionBackfillReceipt(reqVO.getCompletionBackfillReceipt())
                        .setIndependentReceipt(reqVO.getIndependentReceipt()))));
    }

    @PostMapping("/pqc/reject")
    @Operation(summary = "PQC 拒绝生产放行申请")
    @PreAuthorize("@ss.hasPermission('mes:pro-production-release:pqc-approve')")
    public CommonResult<MesPqcProductionReleaseDecisionRespVO> reject(
            @Valid @RequestBody MesPqcProductionReleaseRejectReqVO reqVO) {
        return success(toResp(pqcProductionReleaseService.reject(SecurityFrameworkUtils.getLoginUserId(),
                new MesPqcProductionReleaseRejectCommand()
                        .setApplicationId(reqVO.getApplicationId())
                        .setPqcReleaseWorkTaskId(reqVO.getPqcReleaseWorkTaskId())
                        .setExpectedVersion(reqVO.getExpectedVersion())
                        .setIdempotencyKey(reqVO.getIdempotencyKey())
                        .setRejectReason(reqVO.getRejectReason()))));
    }

    @GetMapping("/get")
    @Operation(summary = "取得生产放行申请权威回执")
    @Parameter(name = "applicationId", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-production-release:query')")
    public CommonResult<MesPqcProductionReleaseDecisionRespVO> get(
            @RequestParam("applicationId") @NotNull Long applicationId) {
        return success(toResp(pqcProductionReleaseService.get(
                SecurityFrameworkUtils.getLoginUserId(), applicationId)));
    }

    @GetMapping("/pqc/page")
    @Operation(summary = "取得 PQC 生产放行分页")
    @PreAuthorize("@ss.hasPermission('mes:pro-production-release:query')")
    public CommonResult<PageResult<MesPqcProductionReleasePageItemRespVO>> getPqcReleasePage(
            @Valid MesPqcProductionReleasePageReqVO reqVO) {
        PageResult<MesPqcProductionReleasePageItem> page = pqcProductionReleaseService.getPqcReleasePage(
                SecurityFrameworkUtils.getLoginUserId(), new MesPqcProductionReleasePageQuery()
                        .setPageNo(reqVO.getPageNo())
                        .setPageSize(reqVO.getPageSize())
                        .setViewStatus(reqVO.getViewStatus())
                        .setWorkOrderCode(reqVO.getWorkOrderCode())
                        .setBatchCode(reqVO.getBatchCode()));
        return success(new PageResult<>(page.getList().stream().map(this::toPageItemResp).toList(), page.getTotal()));
    }

    private MesPqcProductionReleasePageItemRespVO toPageItemResp(MesPqcProductionReleasePageItem item) {
        return new MesPqcProductionReleasePageItemRespVO()
                .setApplicationId(item.getApplicationId())
                .setPqcReleaseWorkTaskId(item.getPqcReleaseWorkTaskId())
                .setVersion(item.getVersion())
                .setViewStatus(item.getViewStatus())
                .setApplicationStatus(item.getApplicationStatus())
                .setActiveOrderId(item.getActiveOrderId())
                .setWorkOrderId(item.getWorkOrderId())
                .setWorkOrderCode(item.getWorkOrderCode())
                .setBatchCode(item.getBatchCode())
                .setProductId(item.getProductId())
                .setBatchExecutionId(item.getBatchExecutionId())
                .setAppliedAt(item.getAppliedAt())
                .setAppliedBy(item.getAppliedBy())
                .setDecidedAt(item.getDecidedAt())
                .setDecidedBy(item.getDecidedBy())
                .setUnderReview(item.getUnderReview())
                .setNonconformanceReviewId(item.getNonconformanceReviewId())
                .setNonconformanceDisposition(item.getNonconformanceDisposition())
                .setNonconformanceReason(item.getNonconformanceReason())
                .setNonconformanceClosedAt(item.getNonconformanceClosedAt());
    }

    private MesPqcProductionReleaseDecisionRespVO toResp(MesPqcProductionReleaseDecisionResult result) {
        return new MesPqcProductionReleaseDecisionRespVO()
                .setApplicationId(result.getApplicationId())
                .setPqcReleaseWorkTaskId(result.getPqcReleaseWorkTaskId())
                .setDecision(result.getDecision())
                .setStatus(result.getStatus())
                .setRejectReason(result.getRejectReason())
                .setBatchExecutionId(result.getBatchExecutionId())
                .setSignatureId(result.getSignatureId())
                .setBatchRecordEvidenceIds(result.getBatchRecordEvidenceIds())
                .setProcessInspectionEvidenceIds(result.getProcessInspectionEvidenceIds())
                .setLossReportEvidenceIds(result.getLossReportEvidenceIds())
                .setReportUploadTasks(result.getReportUploadTasks().stream()
                        .map(item -> new MesProductionReleaseReportUploadTaskRespVO()
                                .setNodeType(item.getNodeType())
                                .setBatchTaskId(item.getBatchTaskId())
                                .setWorkTaskId(item.getWorkTaskId())
                                .setCandidateUserIds(item.getCandidateUserIds())
                                .setStatus(item.getStatus()))
                        .toList())
                .setSourceSnapshotHash(result.getSourceSnapshotHash())
                .setReportSnapshotHash(result.getReportSnapshotHash())
                .setVersion(result.getVersion())
                .setDecidedBy(result.getDecidedBy())
                .setDecidedAt(result.getDecidedAt());
    }
}
