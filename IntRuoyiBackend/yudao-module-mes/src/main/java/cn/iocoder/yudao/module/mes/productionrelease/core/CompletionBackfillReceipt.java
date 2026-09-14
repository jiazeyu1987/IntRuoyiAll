package cn.iocoder.yudao.module.mes.productionrelease.core;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionPickListSource;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Immutable success receipt issued by flow 4 after the completion transaction commits.
 * It deliberately has no batch-execution status or retry fields; flow 6 owns provision state.
 */
@Data
@Accessors(chain = true)
public class CompletionBackfillReceipt {

    public static final String CANONICAL_NAME = "CompletionBackfillReceipt";
    public static final String STATUS_BACKFILL_SUCCEEDED = "BACKFILL_SUCCEEDED";

    private String receiptId;
    private Long tenantId;
    private Long activeOrderId;
    private Long workOrderId;
    private String batchCode;
    private Long routeId;
    private Long routeVersionId;
    private String pickListBindingId;
    private Long pickListId;
    private List<MesBatchExecutionPickListSource> pickListSources;
    private String sourceSnapshotHash;
    private Integer bindingVersion;
    private Integer completionVersion;
    private String completionTransactionId;
    private String completionEventId;
    private Long batchRecordId;
    private Long processInspectionId;
    private List<Long> batchRecordSourceIds;
    private List<Long> processInspectionSourceIds;
    private Boolean hasActualLoss;
    private String lossDecision;
    private String lossReportStatus;
    private Long lossRecordId;
    private String lossQuantity;
    private List<String> sourceEventIds;
    private String receiptHash;
    private String idempotencyKey;
    private String auditEventId;
    private String status;
    private LocalDateTime issuedAt;

    public boolean isSuccessfulFor(Long expectedActiveOrderId, String expectedSourceSnapshotHash) {
        return receiptId != null && !receiptId.isBlank()
                && tenantId != null
                && activeOrderId != null
                && activeOrderId.equals(expectedActiveOrderId)
                && workOrderId != null
                && pickListSources != null && !pickListSources.isEmpty()
                && pickListSources.stream().allMatch(source -> source != null
                && source.getPickListBindingId() != null
                && source.getPickListId() != null
                && source.getBindingVersion() != null && source.getBindingVersion() > 0
                && source.getSourceSnapshotHash() != null && !source.getSourceSnapshotHash().isBlank())
                && sourceSnapshotHash != null && !sourceSnapshotHash.isBlank()
                && sourceSnapshotHash.equals(expectedSourceSnapshotHash)
                && completionVersion != null && completionVersion > 0
                && completionTransactionId != null && !completionTransactionId.isBlank()
                && completionEventId != null && !completionEventId.isBlank()
                && batchRecordId != null
                && processInspectionId != null
                && batchRecordSourceIds != null && !batchRecordSourceIds.isEmpty()
                && processInspectionSourceIds != null && !processInspectionSourceIds.isEmpty()
                && hasActualLoss != null
                && lossDecision != null && !lossDecision.isBlank()
                && lossReportStatus != null && !lossReportStatus.isBlank()
                && receiptHash != null && !receiptHash.isBlank()
                && idempotencyKey != null && !idempotencyKey.isBlank()
                && auditEventId != null && !auditEventId.isBlank()
                && STATUS_BACKFILL_SUCCEEDED.equals(status)
                && issuedAt != null;
    }
}
