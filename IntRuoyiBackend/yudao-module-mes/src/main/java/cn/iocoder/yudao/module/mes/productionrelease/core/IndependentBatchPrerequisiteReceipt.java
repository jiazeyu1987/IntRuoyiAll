package cn.iocoder.yudao.module.mes.productionrelease.core;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/** Historical release snapshot shape; independent receipts no longer authorize finalization. */
@Data
@Accessors(chain = true)
public class IndependentBatchPrerequisiteReceipt {

    public static final String CANONICAL_NAME = "IndependentBatchPrerequisiteReceipt";

    private String receiptId;
    private Long tenantId;
    private String entryType;
    private Long batchExecutionId;
    private Long workOrderId;
    private String workOrderCode;
    private Long routeId;
    private String routeVersion;
    private String batchCode;
    private String sourceRelationId;
    private String sourceRelation;
    private List<String> sourceIds;
    private String sourceSnapshotHash;
    private String businessReason;
    private String issuerSystem;
    private Long issuerUserId;
    private String issuerUserRole;
    private String receiptHash;
    private Long issuedBy;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private String revocationReason;
    private Integer credentialVersion;
    private String payloadHash;
    private String signature;
    private String auditEventId;
    private String idempotencyKey;
    private Integer version;

}
