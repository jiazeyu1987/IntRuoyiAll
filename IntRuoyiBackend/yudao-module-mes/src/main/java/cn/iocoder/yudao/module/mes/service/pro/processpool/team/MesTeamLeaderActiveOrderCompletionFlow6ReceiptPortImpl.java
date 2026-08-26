package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_RECEIPT_NOT_FOUND;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_RECEIPT_TAMPERED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING;

@Service
public class MesTeamLeaderActiveOrderCompletionFlow6ReceiptPortImpl
        implements MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort {

    private final MesProcessPoolActiveOrderCompletionReceiptMapper receiptMapper;

    public MesTeamLeaderActiveOrderCompletionFlow6ReceiptPortImpl(
            MesProcessPoolActiveOrderCompletionReceiptMapper receiptMapper) {
        this.receiptMapper = receiptMapper;
    }

    @Override
    public MesFlow6CompletionBackfillReceipt getByReceiptId(Long receiptId, Long tenantId) {
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = receiptId == null ? null
                : receiptMapper.selectByIdAndTenantId(receiptId, tenantId);
        if (receipt == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_RECEIPT_NOT_FOUND, receiptId);
        }
        if (receipt.getTenantId() == null || !receipt.getTenantId().equals(tenantId)) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_RECEIPT_NOT_FOUND, receiptId);
        }
        if (receipt.getReceiptHash() == null || receipt.getReceiptHash().isBlank()
                || !receipt.getReceiptHash().equals(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt))) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_RECEIPT_TAMPERED, receiptId);
        }
        if (!MesProcessPoolActiveOrderCompletionReceiptDO.STATUS_SUCCESS.equals(receipt.getCompletionStatus())
                || !MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS
                .equals(receipt.getBatchRecordStatus())
                || !MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS
                .equals(receipt.getProcessInspectionStatus())
                || receipt.getBatchCode() == null || receipt.getBatchCode().isBlank()
                || receipt.getRouteId() == null || receipt.getRouteVersionId() == null
                || receipt.getBatchRecordId() == null || receipt.getProcessInspectionId() == null
                || receipt.getRequestIdempotencyKey() == null || receipt.getRequestIdempotencyKey().isBlank()
                || !MesProcessPoolActiveOrderCompletionReceiptDO.RECEIPT_STATUS_BACKFILL_SUCCEEDED
                .equals(receipt.getReceiptStatus())
                || receipt.getCreateTime() == null
                || receipt.getSourceSnapshotHash() == null || receipt.getSourceSnapshotHash().isBlank()
                || receipt.getFormalSourceSnapshotJson() == null || receipt.getFormalSourceSnapshotJson().isBlank()
                || receipt.getSignatureSnapshotJson() == null || receipt.getSignatureSnapshotJson().isBlank()
                || receipt.getCompletedVersion() == null || !lossFactsAreValid(receipt)) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING,
                    receipt.getActiveOrderId(), "FLOW6_RECEIPT_NOT_READY");
        }
        return new MesFlow6CompletionBackfillReceipt()
                .setReceiptId(receipt.getId())
                .setActiveOrderId(receipt.getActiveOrderId())
                .setWorkOrderId(receipt.getWorkOrderId())
                .setBatchCode(receipt.getBatchCode())
                .setRouteId(receipt.getRouteId())
                .setRouteVersionId(receipt.getRouteVersionId())
                .setTenantId(receipt.getTenantId())
                .setRequestIdempotencyKey(receipt.getRequestIdempotencyKey())
                .setExpectedActiveOrderVersion(receipt.getExpectedVersion() == null ? null
                        : receipt.getExpectedVersion().longValue())
                .setCompletionTransactionId(receipt.getRequestIdempotencyKey())
                .setCompletionEventId(receipt.getRequestIdempotencyKey())
                .setCreatedAt(receipt.getCreateTime())
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash())
                .setFormalSourceSnapshotJson(receipt.getFormalSourceSnapshotJson())
                .setSignatureSnapshotJson(receipt.getSignatureSnapshotJson())
                .setCompletionVersion(receipt.getCompletedVersion())
                .setStatus(MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED)
                .setBatchRecordStatus(receipt.getBatchRecordStatus())
                .setProcessInspectionStatus(receipt.getProcessInspectionStatus())
                .setBatchRecordId(receipt.getBatchRecordId())
                .setProcessInspectionId(receipt.getProcessInspectionId())
                .setHasActualLoss(receipt.getHasActualLoss())
                .setLossQuantity(receipt.getLossQuantity())
                .setLossReportStatus(receipt.getLossReportStatus())
                .setLossRecordId(receipt.getLossRecordId())
                .setBatchRecordSourceIdsJson(receipt.getBatchRecordSourceIdsJson())
                .setProcessInspectionSourceIdsJson(receipt.getProcessInspectionSourceIdsJson())
                .setZeroLossConfirmationSnapshot(receipt.getZeroLossConfirmationSnapshot())
                .setReceiptHash(receipt.getReceiptHash());
    }

    private static boolean lossFactsAreValid(MesProcessPoolActiveOrderCompletionReceiptDO receipt) {
        if (receipt.getHasActualLoss() == null || receipt.getLossQuantity() == null
                || receipt.getLossQuantity().signum() < 0 || receipt.getLossConditionFactsJson() == null
                || receipt.getLossConditionFactsJson().isBlank()) {
            return false;
        }
        if (Boolean.TRUE.equals(receipt.getHasActualLoss())) {
            return MesProcessPoolActiveOrderCompletionReceiptDO.LOSS_REPORT_STATUS_SUCCESS
                    .equals(receipt.getLossReportStatus())
                    && receipt.getLossQuantity().compareTo(BigDecimal.ZERO) > 0
                    && receipt.getLossRecordId() != null;
        }
        return MesProcessPoolActiveOrderCompletionReceiptDO.LOSS_REPORT_STATUS_NOT_REQUIRED
                .equals(receipt.getLossReportStatus())
                && receipt.getLossQuantity().compareTo(BigDecimal.ZERO) == 0
                && receipt.getLossRecordId() == null
                && receipt.getZeroLossConfirmationSnapshot() != null
                && !receipt.getZeroLossConfirmationSnapshot().isBlank();
    }
}
