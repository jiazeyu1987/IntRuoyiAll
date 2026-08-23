package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/** Prepared authoritative Tx-A evidence. Writing is performed inside the caller transaction. */
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderCompletionBackfillDraft {

    private Long tenantId;
    private Long workOrderId;
    private String batchCode;
    private Long routeId;
    private Long routeVersionId;
    private String sourceSnapshotHash;
    private String formalSourceSnapshotJson;
    private String signatureSnapshotJson;
    private String batchRecordSourceIdsJson;
    private String processInspectionSourceIdsJson;
    private String lossSourceHash;
    private String batchRecordStatus;
    private String processInspectionStatus;
    private String lossReportStatus;
    private Boolean hasActualLoss;
    private BigDecimal lossQuantity;
    private Long lossRecordId;
    private String zeroLossConfirmationSnapshot;
    private String lossConditionFactsJson;
    private Long materializedBy;
}
