package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionSourceEvidence;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionPickListSource;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Immutable Flow-4 handoff contract consumed by the Flow-6 Tx-B provisioner. */
@Data
@Accessors(chain = true)
public class MesFlow6CompletionBackfillReceipt {

    public static final String STATUS_BACKFILL_SUCCEEDED = "BACKFILL_SUCCEEDED";

    private Long receiptId;
    private Long activeOrderId;
    private Long workOrderId;
    private String batchCode;
    private Long routeId;
    private Long routeVersionId;
    private Long tenantId;
    private String requestIdempotencyKey;
    private String workOrderCode;
    private LocalDateTime createdAt;
    private String sourceSnapshotHash;
    private String formalSourceSnapshotJson;
    private String signatureSnapshotJson;
    private Integer completionVersion;
    private Long expectedActiveOrderVersion;
    private String completionTransactionId;
    private String completionEventId;
    private String sourceVersion;
    private String sourceBundleHash;
    private Long pickListBindingId;
    private Long pickListId;
    private java.util.List<MesBatchExecutionPickListSource> pickListSources;
    private Long batchPickListRelationId;
    private Long bindingVersion;
    private String pickListHeaderSnapshotHash;
    private String pickListLineSnapshotHash;
    private String status;
    /** Frozen result of the three Tx-A backfill branches; Flow-6 may only consume SUCCESS values. */
    private String batchRecordStatus;
    private String processInspectionStatus;
    private Long batchRecordId;
    private Long processInspectionId;
    private Boolean hasActualLoss;
    private BigDecimal lossQuantity;
    private String lossReportStatus;
    private Long lossRecordId;
    private String batchRecordSourceIdsJson;
    private String processInspectionSourceIdsJson;
    private String zeroLossConfirmationSnapshot;
    private String receiptHash;
    private String receiptVersion;
    private String payloadHash;
    private String auditEventId;
    private java.util.List<MesBatchExecutionSourceEvidence> sourceEvidence;
}
