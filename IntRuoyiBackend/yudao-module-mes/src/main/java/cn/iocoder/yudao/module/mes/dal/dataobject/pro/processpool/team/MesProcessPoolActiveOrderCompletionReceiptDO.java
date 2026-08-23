package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable Tx-A completion/backfill receipt. It deliberately has no batchExecutionId
 * or mutable Flow-6 provision state.
 */
@TableName("mes_pro_process_pool_active_order_completion_receipt")
@KeySequence("mes_pro_process_pool_active_order_completion_receipt_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolActiveOrderCompletionReceiptDO extends TenantBaseDO {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String RECEIPT_STATUS_BACKFILL_SUCCEEDED = "BACKFILL_SUCCEEDED";
    public static final String PROVISION_HANDOFF_PENDING_FLOW6 = "PENDING_FLOW6";
    public static final String BACKFILL_STATUS_SUCCESS = "SUCCESS";
    public static final String LOSS_REPORT_STATUS_SUCCESS = "SUCCESS";
    public static final String LOSS_REPORT_STATUS_NOT_REQUIRED = "NOT_REQUIRED";

    @TableId
    private Long id;

    private Long activeOrderId;
    private Long workOrderId;
    private String batchCode;
    private Long routeId;
    private Long routeVersionId;
    private Long leaderUserId;
    private String requestIdempotencyKey;
    private String requestPayloadHash;
    private String sourceSnapshotHash;
    private String formalSourceSnapshotJson;
    private String signatureSnapshotJson;
    private String receiptHash;
    private Integer expectedVersion;
    private Integer completedVersion;

    private String receiptStatus;
    private String completionStatus;
    private String batchRecordStatus;
    private String processInspectionStatus;
    private String lossReportStatus;
    private Boolean hasActualLoss;
    private BigDecimal lossQuantity;
    private Long lossRecordId;
    private String zeroLossConfirmationSnapshot;
    private String lossConditionFactsJson;
    private String batchRecordSourceIdsJson;
    private String processInspectionSourceIdsJson;
    private String lossSourceHash;
    private String provisionHandoff;
    private LocalDateTime completedAt;
    private Long completedBy;
}
