package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrReleaseRespVO {

    private Long releaseTransactionId;

    private String releaseCode;

    private Long batchExecutionId;

    private String batchExecutionCode;

    private Long workOrderId;

    private String workOrderCode;

    private String batchCode;

    private Long productId;

    private String productCode;

    private String productName;

    private Long routeId;

    private String routeCode;

    private String routeName;

    private Integer batchExecutionStatus;

    private String dhrStatus;

    private String inspectionStatus;

    private String deviationStatus;

    private String reworkStatus;

    private String scrapStatus;

    private String inventoryStatus;

    private String releaseStatus;

    private Integer requiredCheckCount;

    private Integer failedCheckCount;

    private Integer blockingCheckCount;

    private LocalDateTime lastPrecheckAt;

    private String precheckSummary;

    private String precheckSnapshotJson;

    private String submitIdempotencyKey;

    private Long submittedBy;

    private LocalDateTime submittedAt;

    private String approvalIdempotencyKey;

    private Long approvedBy;

    private LocalDateTime approvedAt;

    private String approvalSignoffEvidenceHash;

    private String approvalOpinion;

    private Long rejectedBy;

    private LocalDateTime rejectedAt;

    private String rejectReason;

    private Long withdrawnBy;

    private LocalDateTime withdrawnAt;

    private String withdrawReason;

    private Long releaseApprovalWorkTaskId;

    private Integer version;
}
