package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditDetailReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditExportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditExportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditHashVerificationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditSaveChangesReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditSaveRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityExportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityExportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSignatureTimeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditVerifyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditVerifyRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditChange;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditAttachmentChange;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHashVerification;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveChangesCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditValueType;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureTimeCommand;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/pro/batch-record-execution/field-audit")
@Validated
public class MesProBatchRecordExecutionFieldAuditController {

    @Resource
    private MesProBatchRecordExecutionFieldAuditService fieldAuditService;

    @PutMapping("/save-changes")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:field-audit-update') "
            + "or (#reqVO.workTaskId != null and @ss.hasPermission('mes:pro-batch-record-execution:update')) "
            + "or @ss.hasPermission('mes:pro-batch-record-execution:golden-finger')")
    public CommonResult<MesProBatchRecordExecutionFieldAuditSaveRespVO> saveChanges(
            @Valid @RequestBody MesProBatchRecordExecutionFieldAuditSaveChangesReqVO reqVO) {
        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(toCommand(reqVO));
        return success(toSaveResp(reqVO.getExecutionId(), result));
    }

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:field-audit-query')")
    public CommonResult<PageResult<MesProBatchRecordExecutionFieldAuditItemRespVO>> getPage(
            @Valid MesProBatchRecordExecutionFieldAuditPageReqVO pageReqVO) {
        return success(fieldAuditService.getPage(pageReqVO));
    }

    @GetMapping("/detail")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:field-audit-query')")
    public CommonResult<MesProBatchRecordExecutionFieldAuditDetailRespVO> getDetail(
            @Valid MesProBatchRecordExecutionFieldAuditDetailReqVO reqVO) {
        return success(fieldAuditService.getDetail(reqVO));
    }

    @GetMapping("/responsibility-summary")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:field-audit-query')")
    public CommonResult<MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO> getResponsibilitySummary(
            @Valid MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO reqVO) {
        return success(fieldAuditService.getResponsibilitySummary(reqVO));
    }

    @GetMapping("/responsibility-history")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:field-audit-query')")
    public CommonResult<MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO> getResponsibilityHistory(
            @Valid MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO reqVO) {
        return success(fieldAuditService.getResponsibilityHistory(reqVO));
    }

    @GetMapping("/responsibility-export")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:field-audit-export')")
    public CommonResult<MesProBatchRecordExecutionFieldResponsibilityExportRespVO> exportResponsibility(
            @Valid MesProBatchRecordExecutionFieldResponsibilityExportReqVO reqVO) {
        return success(fieldAuditService.exportResponsibility(reqVO));
    }

    @PostMapping("/verify-chain")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:field-audit-verify')")
    public CommonResult<MesProBatchRecordExecutionFieldAuditVerifyRespVO> verifyChain(
            @Valid @RequestBody MesProBatchRecordExecutionFieldAuditVerifyReqVO reqVO) {
        return success(fieldAuditService.verifyChain(reqVO));
    }

    @GetMapping("/export")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:field-audit-export')")
    public CommonResult<MesProBatchRecordExecutionFieldAuditExportRespVO> export(
            @Valid MesProBatchRecordExecutionFieldAuditExportReqVO reqVO) {
        return success(fieldAuditService.export(reqVO));
    }

    private MesProBatchRecordExecutionFieldAuditSaveChangesCommand toCommand(
            MesProBatchRecordExecutionFieldAuditSaveChangesReqVO reqVO) {
        return new MesProBatchRecordExecutionFieldAuditSaveChangesCommand()
                .setExecutionId(reqVO.getExecutionId())
                .setWorkTaskId(reqVO.getWorkTaskId())
                .setIdempotencyKey(reqVO.getIdempotencyKey())
                .setBaseCellValuesHash(reqVO.getBaseCellValuesHash())
                .setBaseFieldAuditRevision(reqVO.getBaseFieldAuditRevision())
                .setBaseFieldAuditHeadHash(reqVO.getBaseFieldAuditHeadHash())
                .setFillCarrier(reqVO.getFillCarrier())
                .setFillMode(reqVO.getFillMode())
                .setReasonCategory(reqVO.getReasonCategory())
                .setReasonText(reqVO.getReasonText())
                .setSignature(toSignatureCommand(reqVO.getSignature()))
                .setChanges((reqVO.getChanges() == null ? java.util.List.<MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.Change>of() : reqVO.getChanges()).stream()
                        .map(change -> new MesProBatchRecordExecutionFieldAuditChange()
                                .setFieldPath(change.getFieldPath())
                                .setFieldKey(change.getFieldKey())
                                .setRowIndex(change.getRowIndex())
                                .setColumnIndex(change.getColumnIndex())
                                .setValueType(MesProBatchRecordExecutionFieldAuditValueType.valueOf(change.getValueType()))
                                .setNewValueJson(change.getNewValueJson())
                                .setNewValueDisplay(change.getNewValueDisplay())
                                .setExpectedOldValueJson(change.getExpectedOldValueJson())
                                .setExpectedOldValueHash(change.getExpectedOldValueHash()))
                        .toList())
                .setAttachmentChanges((reqVO.getAttachmentChanges() == null
                        ? java.util.List.<MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.AttachmentChange>of()
                        : reqVO.getAttachmentChanges()).stream()
                        .map(change -> new MesProBatchRecordExecutionFieldAuditAttachmentChange()
                                .setWorkTaskId(change.getWorkTaskId())
                                .setFieldPath(change.getFieldPath())
                                .setFieldKey(change.getFieldKey())
                                .setFieldLabel(change.getFieldLabel())
                                .setRowIndex(change.getRowIndex())
                                .setColumnIndex(change.getColumnIndex())
                                .setAttachmentType(change.getAttachmentType())
                                .setAttachmentAction(change.getAttachmentAction())
                                .setAttachmentGroupKey(change.getAttachmentGroupKey())
                                .setFileId(change.getFileId())
                                .setFileUrl(change.getFileUrl())
                                .setStorageConfigId(change.getStorageConfigId())
                                .setStoragePath(change.getStoragePath())
                                .setFileName(change.getFileName())
                                .setContentType(change.getContentType())
                                .setFileSize(change.getFileSize())
                                .setSha256(change.getSha256())
                                .setStorageRetentionJson(change.getStorageRetentionJson())
                                .setExpectedPreviousAttachmentHash(change.getExpectedPreviousAttachmentHash()))
                        .toList());
    }

    private MesProBatchRecordExecutionFieldAuditSaveChangesCommand.Signature toSignatureCommand(
            MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.Signature signature) {
        return signature == null ? null : new MesProBatchRecordExecutionFieldAuditSaveChangesCommand.Signature()
                .setPassword(signature.getPassword())
                .setSignatureTimeCommand(toSignatureTimeCommand(signature.getSignatureTime()));
    }

    private MesProBatchRecordExecutionSignatureTimeCommand toSignatureTimeCommand(
            MesProBatchRecordExecutionSignatureTimeReqVO signatureTime) {
        if (signatureTime == null) {
            return null;
        }
        return new MesProBatchRecordExecutionSignatureTimeCommand()
                .setSelectedSignedAt(signatureTime.getSelectedSignedAt())
                .setSelectedTimeZone(signatureTime.getSelectedTimeZone())
                .setSelectedTimeReason(signatureTime.getSelectedTimeReason());
    }

    private MesProBatchRecordExecutionFieldAuditSaveRespVO toSaveResp(Long executionId,
                                                                      MesProBatchRecordExecutionFieldAuditSaveResult result) {
        return new MesProBatchRecordExecutionFieldAuditSaveRespVO()
                .setExecutionId(executionId)
                .setFieldAuditRevision(result.getFieldAuditRevision())
                .setFieldAuditHeadHash(result.getFieldAuditHeadHash())
                .setCellValuesHash(result.getCellValuesHash())
                .setAuditBatchId(result.getAuditBatchId())
                .setSignatureId(result.getSignatureId())
                .setChangedAt(result.getChangedAt())
                .setChangedFieldCount(result.getChangedFieldCount())
                .setHashVerification(toVerificationResp(result.getHashVerification()));
    }

    private MesProBatchRecordExecutionFieldAuditHashVerificationRespVO toVerificationResp(
            MesProBatchRecordExecutionFieldAuditHashVerification verification) {
        if (verification == null) {
            return null;
        }
        return new MesProBatchRecordExecutionFieldAuditHashVerificationRespVO()
                .setStatus(verification.getStatus() == null ? null : verification.getStatus().name())
                .setCalculatedHeadHash(verification.getCalculatedHeadHash())
                .setStoredHeadHash(verification.getStoredHeadHash())
                .setCheckedBatchCount(verification.getCheckedBatchCount())
                .setCheckedItemCount(verification.getCheckedItemCount())
                .setBrokenBatchId(verification.getBrokenBatchId())
                .setBrokenItemId(verification.getBrokenItemId())
                .setFailedReason(verification.getFailedReason())
                .setCheckedAt(verification.getCheckedAt());
    }
}
