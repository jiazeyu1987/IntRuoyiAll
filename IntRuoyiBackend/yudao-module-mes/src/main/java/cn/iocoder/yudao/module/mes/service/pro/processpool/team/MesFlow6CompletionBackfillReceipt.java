package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

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
    private LocalDateTime createdAt;
    private String sourceSnapshotHash;
    private String formalSourceSnapshotJson;
    private String signatureSnapshotJson;
    private Integer completionVersion;
    private String status;
    private Boolean hasActualLoss;
    private BigDecimal lossQuantity;
    private String lossReportStatus;
    private Long lossRecordId;
    private String batchRecordSourceIdsJson;
    private String processInspectionSourceIdsJson;
    private String zeroLossConfirmationSnapshot;
    private String receiptHash;
}
