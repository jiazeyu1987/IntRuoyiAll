package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

/** Flow 4 output consumed by active-order batch entry adapters. */
@Data
@Accessors(chain = true)
public class MesCompletionBackfillReceipt {

    private String receiptId;
    private Long tenantId;
    private Long activeOrderId;
    private Long workOrderId;
    private String workOrderCode;
    private String batchCode;
    private Long routeId;
    private Long routeVersionId;
    private String completionTransactionId;
    private Long expectedActiveOrderVersion;
    private String sourceVersion;
    private Long pickListBindingId;
    private Long pickListId;
    private Long batchPickListRelationId;
    private String sourceContextHash;
    private String sourceSnapshotHash;
    private String pickListHeaderSnapshotHash;
    private String pickListLineSnapshotHash;
    private String sourceBundleHash;
    private Long bindingVersion;
    private Integer productionProgress;
    private Integer inspectionProgress;
    private Long completionVersion;
    private String completionEventId;
    private Long batchRecordId;
    private Long processInspectionId;
    private Boolean hasActualLoss;
    private String lossDecision;
    private String lossReportStatus;
    private Long lossRecordId;
    private java.math.BigDecimal lossQuantity;
    private String status;
    private String receiptVersion;
    private String receiptHash;
    private String productionBackfillStatus;
    private String inspectionBackfillStatus;
    private String lossBackfillStatus;
    private String payloadHash;
    private String auditEventId;
    private String idempotencyKey;
    private java.util.List<MesBatchExecutionSourceEvidence> sourceEvidence;
}
